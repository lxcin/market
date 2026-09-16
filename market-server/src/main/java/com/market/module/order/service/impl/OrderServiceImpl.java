package com.market.module.order.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.market.common.exception.BusinessException;
import com.market.common.metrics.BusinessMetrics;
import com.market.module.cart.entity.CartItemVO;
import com.market.module.cart.service.CartService;
import com.market.module.coupon.entity.CouponTemplate;
import com.market.module.coupon.mapper.CouponTemplateMapper;
import com.market.module.coupon.mapper.UserCouponMapper;
import com.market.module.coupon.service.CouponService;
import com.market.module.inventory.service.InventoryService;
import com.market.module.order.entity.Order;
import com.market.module.order.entity.OrderItem;
import com.market.module.order.entity.OrderItemVO;
import com.market.module.order.entity.OrderStatus;
import com.market.module.order.entity.OrderVO;
import com.market.module.order.mapper.OrderItemMapper;
import com.market.module.order.mapper.OrderMapper;
import com.market.module.order.service.OrderDelayQueue;
import com.market.module.order.service.OrderService;
import com.market.module.outbox.service.OutboxService;
import com.market.module.product.entity.Book;
import com.market.module.product.mapper.BookMapper;
import com.market.module.user.entity.Address;
import com.market.module.user.mapper.AddressMapper;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final CartService cartService;
    private final CouponService couponService;
    private final InventoryService inventoryService;
    private final BookMapper bookMapper;
    private final AddressMapper addressMapper;
    private final UserCouponMapper userCouponMapper;
    private final CouponTemplateMapper couponTemplateMapper;
    private final BusinessMetrics businessMetrics;
    private final OutboxService outboxService;
    private final OrderDelayQueue orderDelayQueue;

    @Override
    @Observed(name = "order.create")
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(Long userId, String idempotentKey, Long addressId, Long userCouponId) {
        Map<String, Object> summary = cartService.getCartSummary(userId);
        List<CartItemVO> allItems = (List<CartItemVO>) summary.get("items");

        List<CartItemVO> checkedItems = allItems.stream()
                .filter(i -> Boolean.TRUE.equals(i.getChecked()))
                .collect(Collectors.toList());

        if (checkedItems.isEmpty()) {
            throw new BusinessException("购物车中没有选中商品");
        }

        OrderVO vo = doCreateOrder(userId, idempotentKey, addressId, userCouponId, checkedItems);
        cartService.removeChecked(userId);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO directBuy(Long userId, String idempotentKey, Long bookId, Integer quantity,
                             Long addressId, Long userCouponId) {
        Book book = bookMapper.selectById(bookId);
        if (book == null || book.getStatus() != 1) {
            throw new BusinessException("商品不存在或已下架");
        }

        if (quantity == null || quantity <= 0) {
            throw new BusinessException("商品数量不合法");
        }

        CartItemVO item = new CartItemVO();
        item.setBookId(book.getId());
        item.setTitle(book.getTitle());
        item.setAuthor(book.getAuthor());
        item.setCoverImage(book.getCoverImage());
        item.setPrice(book.getPrice());
        item.setQuantity(quantity);
        item.setChecked(true);

        return doCreateOrder(userId, idempotentKey, addressId, userCouponId, List.of(item));
    }

    private OrderVO doCreateOrder(Long userId, String idempotentKey, Long addressId,
                                   Long userCouponId, List<CartItemVO> items) {
        Order existing = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getIdempotentKey, idempotentKey));
        if (existing != null) {
            return buildOrderVO(existing);
        }

        Address address = addressMapper.selectById(addressId);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException("收货地址不存在");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItemVO item : items) {
            totalAmount = totalAmount.add(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal payAmount = totalAmount;
        String couponName = null;

        if (userCouponId != null) {
            Map<String, Object> preview = couponService.previewDiscount(userId, userCouponId, totalAmount);
            discountAmount = (BigDecimal) preview.get("discountAmount");
            payAmount = (BigDecimal) preview.get("finalAmount");
            couponName = (String) preview.get("templateName");
        }

        String orderNo = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + RandomUtil.randomNumbers(6);

        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(discountAmount);
        order.setPayAmount(payAmount);
        order.setStatus(0);
        order.setCouponId(userCouponId);
        order.setAddressId(addressId);
        order.setReceiverName(address.getReceiverName());
        order.setReceiverPhone(address.getPhone());
        order.setReceiverAddress(address.getProvince() + address.getCity()
                + address.getDistrict() + address.getDetail());
        order.setIdempotentKey(idempotentKey);
        order.setCreatedAt(LocalDateTime.now());

        orderMapper.insert(order);

        List<OrderItemVO> itemVOs = new ArrayList<>();
        List<Map<String, Object>> outboxItems = new ArrayList<>();
        for (CartItemVO cartItem : items) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setBookId(cartItem.getBookId());
            orderItem.setBookTitle(cartItem.getTitle());
            orderItem.setBookCover(cartItem.getCoverImage());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(cartItem.getPrice());
            orderItemMapper.insert(orderItem);

            // 下单预占库存：可用库存 -，锁定库存 +
            inventoryService.reserveStock(cartItem.getBookId(), cartItem.getQuantity());

            OrderItemVO itemVO = new OrderItemVO();
            itemVO.setBookId(orderItem.getBookId());
            itemVO.setBookTitle(orderItem.getBookTitle());
            itemVO.setBookCover(orderItem.getBookCover());
            itemVO.setQuantity(orderItem.getQuantity());
            itemVO.setPrice(orderItem.getPrice());
            itemVOs.add(itemVO);
            outboxItems.add(Map.of("bookId", orderItem.getBookId(), "quantity", orderItem.getQuantity()));
        }

        // 本地消息表：与订单同事务写入，销量异步最终一致
        outboxService.publish("ORDER_CREATED", "ORDER", order.getId(),
                Map.of("orderId", order.getId(), "items", outboxItems));

        if (userCouponId != null) {
            boolean used = couponService.useCoupon(userCouponId, order.getId());
            if (!used) {
                throw new BusinessException("优惠券已被使用或不存在");
            }
        }

        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setDiscountAmount(order.getDiscountAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setStatus(order.getStatus());
        vo.setStatusDesc(getStatusDesc(order.getStatus()));
        vo.setItems(itemVOs);
        vo.setReceiverName(order.getReceiverName());
        vo.setReceiverPhone(order.getReceiverPhone());
        vo.setReceiverAddress(order.getReceiverAddress());
        vo.setCouponName(couponName);
        vo.setCreatedAt(order.getCreatedAt());
        businessMetrics.orderCreated();
        // 投递延时关单任务（到期未支付则自动关闭并释放库存）
        orderDelayQueue.scheduleCancel(order.getId());
        return vo;
    }

    @Override
    public OrderVO getOrder(Long orderId, Long userId, boolean admin) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!admin && !order.getUserId().equals(userId)) {
            throw new BusinessException("无权查看该订单");
        }
        return buildOrderVO(order);
    }

    @Override
    public Page<OrderVO> pageUserOrders(Long userId, Integer page, Integer size) {
        Page<Order> orderPage = new Page<>(page, size);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreatedAt);
        IPage<Order> result = orderMapper.selectPage(orderPage, wrapper);

        Page<OrderVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<OrderVO> voList = result.getRecords().stream()
                .map(this::buildOrderVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此订单");
        }

        int rows = orderMapper.cancelOrderCas(orderId);
        if (rows <= 0) {
            throw new BusinessException("订单状态不可取消");
        }

        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        for (OrderItem item : items) {
            inventoryService.releaseStock(item.getBookId(), item.getQuantity());
        }

        if (order.getCouponId() != null) {
            boolean refunded = couponService.refundCoupon(order.getCouponId());
            if (!refunded) {
                log.warn("优惠券退还失败: userCouponId={}", order.getCouponId());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelTimeoutOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            return;
        }
        // CAS：仅待支付可关，已支付/已取消则跳过（幂等）
        int rows = orderMapper.cancelOrderCas(orderId);
        if (rows <= 0) {
            return;
        }
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        for (OrderItem item : items) {
            inventoryService.releaseStock(item.getBookId(), item.getQuantity());
        }
        if (order.getCouponId() != null) {
            couponService.refundCoupon(order.getCouponId());
        }
        log.info("订单超时自动关闭: orderId={}", orderId);
    }

    private OrderVO buildOrderVO(Order order) {
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));

        List<OrderItemVO> itemVOs = items.stream().map(item -> {
            OrderItemVO vo = new OrderItemVO();
            vo.setBookId(item.getBookId());
            vo.setBookTitle(item.getBookTitle());
            vo.setBookCover(item.getBookCover());
            vo.setQuantity(item.getQuantity());
            vo.setPrice(item.getPrice());
            return vo;
        }).collect(Collectors.toList());

        String couponName = null;
        if (order.getCouponId() != null) {
            var userCoupon = userCouponMapper.selectById(order.getCouponId());
            if (userCoupon != null) {
                CouponTemplate template = couponTemplateMapper.selectById(userCoupon.getCouponTemplateId());
                if (template != null) {
                    couponName = template.getName();
                }
            }
        }

        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setDiscountAmount(order.getDiscountAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setStatus(order.getStatus());
        vo.setStatusDesc(getStatusDesc(order.getStatus()));
        vo.setItems(itemVOs);
        vo.setReceiverName(order.getReceiverName());
        vo.setReceiverPhone(order.getReceiverPhone());
        vo.setReceiverAddress(order.getReceiverAddress());
        vo.setCouponName(couponName);
        vo.setCreatedAt(order.getCreatedAt());
        return vo;
    }

    private String getStatusDesc(Integer status) {
        return OrderStatus.of(status).getDesc();
    }
}
