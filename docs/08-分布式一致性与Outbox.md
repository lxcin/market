# 08 · 分布式一致性与 Outbox

## 1. 一致性问题地图
| 场景 | 一致性挑战 | 方案 |
|---|---|---|
| 下单（订单 + 库存预占 + 副作用） | 业务写库与"发消息/副作用"原子 | **本地消息表 Outbox**（同事务） |
| 订单超时关单 | 定时轮询延迟大、扫库昂贵 | **Redisson 延时队列**（精确）+ 低频兜底 |
| 购物车持久化 | Redis 主存储落库不可靠 | **Redis Stream** 可靠队列 |
| 秒杀落库 | 高并发 + 可靠 | Redis Stream + 幂等 |
| 支付记账 | 外部回调重复/乱序/掉单 | 验签 + 金额核对 + CAS 幂等 + **对账补偿** |
| 派生数据（销量） | 与订单强一致成本高 | Outbox 异步最终一致 + ES 实时刷新 |

## 2. 本地消息表（Transactional Outbox）
### 表结构 `t_outbox`
`id、event_type、aggregate_type、aggregate_id、payload(JSON)、status(0待/1已/2失败)、retry_count、last_error、created_at/updated_at`。

### 工作方式
1. **发布（事务内）**：业务方法在 `@Transactional` 中调用 `OutboxService.publish(...)`，与业务数据**同一事务**落库 → 要么都成功，要么都回滚。
2. **投递（独立事务）**：`OutboxDispatcher` 每 2s 扫描 `status=0`（LIMIT 50），按 `eventType` 分发处理器，成功置 1，异常 `retry_count++`、记录 `last_error`，≥5 次置 2。
3. **幂等**：处理器需幂等；订单销量累加由"事件只投递一次 + 同事务置状态"保证。

### 事件与处理器
| event_type | 处理器 | 动作 |
|---|---|---|
| ORDER_CREATED | `OrderCreatedHandler` | 累加商品销量 + **实时刷新 ES** |
| PAYMENT_SUCCESS | `PaymentSuccessHandler` | 通知/报表占位（可扩展） |

### 代码位置
`module/outbox/{entity,mapper,service,service/handler,task}`；集成点 `OrderServiceImpl.doCreateOrder`、`PaymentServiceImpl.handleNotify`。

## 3. 延时关单（Redisson 延时队列）
### 背景
原 `OrderTimeoutTask` 每 60s 扫库（`WHERE status=0 AND created_at<deadline`），精度差、随订单量增长昂贵。

### 现方案
- 下单时 `OrderDelayQueue.scheduleCancel(orderId)`：`RDelayedQueue.offer(orderId, payTimeoutSeconds, SECONDS)`。
  - 底层 Redis **ZSet**（score=到期时间戳），**重启不丢**。
- `OrderTimeoutConsumer`：**专用线程** `take()` **阻塞消费**（无轮询空转），到期订单 → `orderService.cancelTimeoutOrder(orderId)`。
- `cancelTimeoutOrder`：CAS `cancelOrderCas`（仅待支付可关）→ `releaseStock` 释放预占 → 退还优惠券；**幂等**。
- `OrderTimeoutTask` 降级为**每 5 分钟兜底扫描**（防延时消息丢失）。
- 超时可配：`order.pay-timeout-seconds`（默认 1800）。

### 验证
- 15s 超时：下单 `stock=299/locked=1` → 20s 后 `status=4`、`stock=300/locked=0`；日志线程 `order-timeout-consumer`。
- 10s 超时 `take` 模式复验通过。

## 4. 购物车可靠落库
见文档 04：`cart:stream`（组 `cart-group`）+ 阻塞消费 + PEL 重试 + 幂等 upsert/delete。

## 5. 三处对账
| 对账 | 不变量 | 触发 | 输出 |
|---|---|---|---|
| 支付 | 本地成功支付 ↔ 网关账单 | 定时/手动 | 差异 + 掉单补偿 |
| 库存 | `locked_stock == Σ待支付订单数量` | 每 5min/手动 | 差异列表 |
| 优惠券 | `remaining == total - 已发放` | 每 5min/手动 | 差异列表 |

- 库存对账 `InventoryReconcileTask`、券对账 `CouponReconcileTask`。
- 管理端：`GET /api/admin/consistency/inventory|coupon`。
- 指标：`business_reconcile_mismatch_total`（可配告警）。

## 6. 验证汇总
- Outbox：`ORDER_CREATED`、`PAYMENT_SUCCESS` 均 `status=1`。
- 库存对账 `checked=30, mismatch=0`；券对账 `checked=7, mismatch=0`。
- 下单预占/支付提交/超时释放全链路数值正确。

## 7. 取舍
- Outbox 事件可能被重复投递（至少一次语义）→ 处理器必须幂等；本项目通过"同事务置状态 + 派生操作幂等"化解。
- 延时队列依赖客户端定时器搬运（Redisson 实现），但元素持久化在 Redis，重启可恢复。
- 单体应用内闭环；拆服务后这些机制（Outbox/对账/幂等键）正是分布式事务（Saga/TCC）的基础设施。
