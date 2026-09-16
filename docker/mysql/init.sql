CREATE DATABASE IF NOT EXISTS market DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE market;

-- ============================================================
-- 用户表
-- ============================================================
DROP TABLE IF EXISTS t_user;
CREATE TABLE t_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码(BCrypt)',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    avatar VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    role VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色: USER/ADMIN',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1正常 0禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0未删除 1已删除',
    INDEX idx_phone (phone),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================================
-- 图书分类表
-- ============================================================
DROP TABLE IF EXISTS t_category;
CREATE TABLE t_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父分类ID, 0为顶级',
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    sort INT NOT NULL DEFAULT 0 COMMENT '排序',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书分类表';

-- ============================================================
-- 图书商品表
-- ============================================================
DROP TABLE IF EXISTS t_book;
CREATE TABLE t_book (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL COMMENT '书名',
    author VARCHAR(100) NOT NULL COMMENT '作者',
    isbn VARCHAR(20) DEFAULT NULL COMMENT 'ISBN号',
    publisher VARCHAR(100) DEFAULT NULL COMMENT '出版社',
    publish_date DATE DEFAULT NULL COMMENT '出版日期',
    category_id BIGINT NOT NULL COMMENT '分类ID',
    price DECIMAL(10,2) NOT NULL COMMENT '原价',
    rate DOUBLE NOT NULL DEFAULT 0 COMMENT '评分(0-10)',
    cover_image VARCHAR(255) DEFAULT NULL COMMENT '封面图片URL',
    description TEXT DEFAULT NULL COMMENT '简介',
    detail TEXT DEFAULT NULL COMMENT '详情(富文本)',
    sales INT NOT NULL DEFAULT 0 COMMENT '销量',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1在售 0下架',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_category_id (category_id),
    INDEX idx_author (author),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书商品表';

-- ============================================================
-- 库存表 (与商品分离, 保障库存一致性)
-- ============================================================
DROP TABLE IF EXISTS t_inventory;
CREATE TABLE t_inventory (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    book_id BIGINT NOT NULL UNIQUE COMMENT '图书ID',
    stock INT NOT NULL DEFAULT 0 COMMENT '可用库存',
    locked_stock INT NOT NULL DEFAULT 0 COMMENT '锁定库存(预占)',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_book_id (book_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存表';

-- ============================================================
-- 购物车表 (MySQL持久化备份, 主数据在Redis)
-- ============================================================
DROP TABLE IF EXISTS t_cart;
CREATE TABLE t_cart (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    book_id BIGINT NOT NULL COMMENT '图书ID',
    quantity INT NOT NULL DEFAULT 1 COMMENT '数量',
    checked TINYINT NOT NULL DEFAULT 1 COMMENT '是否勾选: 1勾选 0未勾选',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_book (user_id, book_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

-- ============================================================
-- 优惠券模板表
-- ============================================================
DROP TABLE IF EXISTS t_coupon_template;
CREATE TABLE t_coupon_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '优惠券名称',
    type TINYINT NOT NULL COMMENT '类型: 1满减 2折扣 3直减',
    threshold_amount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '使用门槛金额',
    discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '减免金额(满减/直减用)',
    discount_rate DECIMAL(3,2) NOT NULL DEFAULT 1.00 COMMENT '折扣率(折扣券用, 如0.85=85折)',
    total_count INT NOT NULL COMMENT '发放总量',
    remaining_count INT NOT NULL COMMENT '剩余数量',
    per_user_limit INT NOT NULL DEFAULT 1 COMMENT '每人限领',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME NOT NULL COMMENT '结束时间',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1启用 0停用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_status_time (status, start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='优惠券模板表';

-- ============================================================
-- 用户优惠券表
-- ============================================================
DROP TABLE IF EXISTS t_user_coupon;
CREATE TABLE t_user_coupon (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    coupon_template_id BIGINT NOT NULL COMMENT '优惠券模板ID',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0未使用 1已使用 2已过期',
    order_id BIGINT DEFAULT NULL COMMENT '使用的订单ID',
    used_time DATETIME DEFAULT NULL COMMENT '使用时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_status (user_id, status),
    INDEX idx_template_id (coupon_template_id),
    UNIQUE KEY uk_order_id (order_id),
    UNIQUE KEY uk_user_template (user_id, coupon_template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户优惠券表';

-- ============================================================
-- 收货地址表
-- ============================================================
DROP TABLE IF EXISTS t_address;
CREATE TABLE t_address (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    receiver_name VARCHAR(50) NOT NULL COMMENT '收货人',
    phone VARCHAR(20) NOT NULL COMMENT '手机号',
    province VARCHAR(50) NOT NULL COMMENT '省份',
    city VARCHAR(50) NOT NULL COMMENT '城市',
    district VARCHAR(50) NOT NULL COMMENT '区/县',
    detail VARCHAR(255) NOT NULL COMMENT '详细地址',
    is_default TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认: 1是 0否',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收货地址表';

-- ============================================================
-- 订单表
-- ============================================================
DROP TABLE IF EXISTS t_order;
CREATE TABLE t_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(32) NOT NULL UNIQUE COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    total_amount DECIMAL(10,2) NOT NULL COMMENT '商品总金额',
    discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '优惠金额',
    pay_amount DECIMAL(10,2) NOT NULL COMMENT '实付金额',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0待支付 1已支付 2已发货 3已完成 4已取消',
    coupon_id BIGINT DEFAULT NULL COMMENT '使用的优惠券ID(用户优惠券)',
    address_id BIGINT NOT NULL COMMENT '收货地址ID',
    receiver_name VARCHAR(50) NOT NULL COMMENT '收货人(快照)',
    receiver_phone VARCHAR(20) NOT NULL COMMENT '手机号(快照)',
    receiver_address VARCHAR(255) NOT NULL COMMENT '地址(快照)',
    pay_time DATETIME DEFAULT NULL COMMENT '支付时间',
    idempotent_key VARCHAR(64) DEFAULT NULL COMMENT '幂等键',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_user_id (user_id),
    INDEX idx_order_no (order_no),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    UNIQUE KEY uk_idempotent_key (idempotent_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- ============================================================
-- 订单明细表
-- ============================================================
DROP TABLE IF EXISTS t_order_item;
CREATE TABLE t_order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL COMMENT '订单ID',
    book_id BIGINT NOT NULL COMMENT '图书ID',
    book_title VARCHAR(200) NOT NULL COMMENT '图书名称(快照)',
    book_cover VARCHAR(255) DEFAULT NULL COMMENT '封面(快照)',
    quantity INT NOT NULL COMMENT '数量',
    price DECIMAL(10,2) NOT NULL COMMENT '单价(快照)',
    INDEX idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

-- ============================================================
-- 评论表
-- ============================================================
DROP TABLE IF EXISTS t_comment;
CREATE TABLE t_comment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    book_id BIGINT NOT NULL COMMENT '图书ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    content VARCHAR(1000) NOT NULL COMMENT '评论内容',
    rate TINYINT NOT NULL DEFAULT 5 COMMENT '评分(1-5)',
    parent_id BIGINT DEFAULT NULL COMMENT '回复的父评论ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0未删除 1已删除',
    INDEX idx_book_id (book_id),
    INDEX idx_user_id (user_id),
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图书评论表';

-- ============================================================
-- 支付流水表
-- ============================================================
DROP TABLE IF EXISTS t_payment;
CREATE TABLE t_payment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    payment_no VARCHAR(40) NOT NULL COMMENT '平台支付单号',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    order_no VARCHAR(32) NOT NULL COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    channel VARCHAR(20) NOT NULL DEFAULT 'MOCK' COMMENT '支付渠道',
    amount DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0待支付 1支付成功 2支付失败 3已关闭 4已退款',
    trade_no VARCHAR(64) DEFAULT NULL COMMENT '网关交易号',
    notify_time DATETIME DEFAULT NULL COMMENT '回调时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_payment_no (payment_no),
    UNIQUE KEY uk_trade_no (trade_no),
    INDEX idx_order_id (order_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付流水表';

-- ============================================================
-- 退款流水表
-- ============================================================
DROP TABLE IF EXISTS t_refund;
CREATE TABLE t_refund (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    refund_no VARCHAR(40) NOT NULL COMMENT '平台退款单号',
    payment_no VARCHAR(40) NOT NULL COMMENT '支付单号',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    amount DECIMAL(10,2) NOT NULL COMMENT '退款金额',
    reason VARCHAR(255) DEFAULT NULL COMMENT '退款原因',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0处理中 1成功 2失败',
    refund_trade_no VARCHAR(64) DEFAULT NULL COMMENT '网关退款单号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_refund_no (refund_no),
    INDEX idx_order_id (order_id),
    INDEX idx_payment_no (payment_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款流水表';

-- ============================================================
-- 网关对账单表 (模拟第三方账单, 用于对账)
-- ============================================================
DROP TABLE IF EXISTS t_gateway_bill;
CREATE TABLE t_gateway_bill (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    payment_no VARCHAR(40) NOT NULL COMMENT '平台支付单号',
    trade_no VARCHAR(64) DEFAULT NULL COMMENT '网关交易号',
    amount DECIMAL(10,2) NOT NULL COMMENT '金额',
    status VARCHAR(16) NOT NULL COMMENT '网关状态: SUCCESS/REFUND',
    bill_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_payment_no (payment_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网关对账单表';

-- ============================================================
-- 本地消息表 (Outbox) —— 保证"业务写库 + 发消息"原子性
-- ============================================================
DROP TABLE IF EXISTS t_outbox;
CREATE TABLE t_outbox (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_type VARCHAR(50) NOT NULL COMMENT '事件类型',
    aggregate_type VARCHAR(50) NOT NULL COMMENT '聚合类型',
    aggregate_id BIGINT NOT NULL COMMENT '聚合ID',
    payload TEXT COMMENT '事件负载(JSON)',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0待投递 1已投递 2失败',
    retry_count INT NOT NULL DEFAULT 0,
    last_error VARCHAR(255) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='本地消息表(Outbox)';

-- ============================================================
-- 初始化数据
-- ============================================================

-- 管理员 (密码: admin123)
INSERT INTO t_user (username, password, role) VALUES
('admin', '$2a$10$pWXOFUqNpT1hBh.sove/NOwix9iFhzvXFfFEJmUUYAgfi/GCPAiQO', 'ADMIN');

-- 分类
INSERT INTO t_category (id, parent_id, name, sort) VALUES
(1, 0, '计算机', 1),
(2, 0, '文学小说', 2),
(3, 0, '经济管理', 3),
(4, 0, '教育考试', 4),
(5, 1, '编程语言', 11),
(6, 1, '人工智能', 12),
(7, 1, '计算机网络', 13),
(8, 2, '中国文学', 21),
(9, 2, '外国文学', 22);

-- 图书
INSERT INTO t_book (title, author, isbn, publisher, category_id, price, rate, description, sales) VALUES
('深入理解Java虚拟机(第3版)', '周志明', '9787111641247', '机械工业出版社', 5, 129.00, 9.2, 'Java开发者必读的JVM经典著作，全面讲解JVM原理与调优', 5800),
('Spring实战(第6版)', 'Craig Walls', '9787115551283', '人民邮电出版社', 5, 139.00, 8.8, 'Spring框架权威指南，覆盖Spring Boot与Spring Cloud', 3200),
('高性能MySQL(第4版)', 'Silvia Botros', '9787121445774', '电子工业出版社', 5, 149.00, 9.0, 'MySQL性能优化圣经，涵盖查询优化、索引设计、高可用架构', 4100),
('深度学习', 'Ian Goodfellow', '9787115541475', '人民邮电出版社', 6, 168.00, 9.5, '深度学习领域奠基性著作，AI从业者必读', 2600),
('TCP/IP详解(卷1)', 'Kevin Fall', '9787111453840', '机械工业出版社', 7, 129.00, 8.5, '网络协议经典教材，面试高频参考书', 1800),
('活着', '余华', '9787530215319', '北京十月文艺出版社', 8, 45.00, 9.7, '余华代表作，讲述人在极端环境下的生存意志', 12000),
('百年孤独', '加西亚·马尔克斯', '9787544253994', '南海出版公司', 9, 55.00, 9.3, '魔幻现实主义文学代表作，诺贝尔文学奖获奖作品', 9800),
('三体', '刘慈欣', '9787536692930', '重庆出版社', 8, 93.00, 9.6, '雨果奖获奖作品，中国科幻文学的里程碑', 15000),
('原则', 'Ray Dalio', '9787521701807', '中信出版社', 3, 98.00, 8.3, '桥水基金创始人的人生经验与工作原则', 4300),
('经济学原理(第8版)', '曼昆', '9787302955960', '清华大学出版社', 3, 128.00, 8.7, '全球最受欢迎的经济学入门教材', 2100),
('Java编程思想(第4版)', 'Bruce Eckel', '9787111213826', '机械工业出版社', 5, 108.00, 9.1, 'Java学习必读经典，深入理解面向对象编程思想', 6700),
('算法导论(第3版)', 'Thomas Cormen', '9787111407010', '机械工业出版社', 5, 128.00, 9.4, '计算机算法领域的标准参考书，MIT经典教材', 4500),
('设计模式:可复用面向对象软件的基础', 'GoF', '9787111618339', '机械工业出版社', 5, 79.00, 9.0, '面向对象设计模式的开山之作，23种经典设计模式详解', 3800),
('Python编程:从入门到实践(第3版)', 'Eric Matthes', '9787115546081', '人民邮电出版社', 5, 109.00, 9.3, '零基础学Python的最佳入门书，项目驱动式学习', 8900),
('统计学习方法(第2版)', '李航', '9787302517276', '清华大学出版社', 6, 98.00, 9.2, '机器学习入门必读，深入浅出地介绍统计学习理论', 5600),
('机器学习', '周志华', '9787302423287', '清华大学出版社', 6, 88.00, 9.4, '中文机器学习领域最权威的教材，西瓜书', 7200),
('React设计原理', '卡颂', '9787121449987', '电子工业出版社', 5, 108.00, 9.0, '深入React内部原理，从Fiber架构到并发模式全解析', 2100),
('Vue.js设计与实现', '霍春阳', '9787115585233', '人民邮电出版社', 5, 119.00, 9.5, 'Vue.js核心团队成员力作，深入源码级别的Vue设计解析', 3400),
('计算机网络:自顶向下方法(第8版)', 'Kurose', '9787111589756', '机械工业出版社', 7, 99.00, 9.1, '全球最流行的计算机网络教材，以应用层为起点讲解', 2800),
('数据结构与算法分析(Java版)', 'Mark Weiss', '9787111527398', '机械工业出版社', 5, 89.00, 8.9, '经典数据结构教材，Java语言实现，适合面试准备', 3100),
('人月神话', 'Frederick Brooks', '9787302458555', '清华大学出版社', 5, 79.00, 8.8, '软件工程管理经典之作，没有银弹', 1900),
('重构:改善既有代码的设计(第2版)', 'Martin Fowler', '9787115546593', '人民邮电出版社', 5, 99.00, 9.3, '代码重构圣经，提升代码质量的必读之作', 4200),
('Effective Java(第3版)', 'Joshua Bloch', '9787111619251', '机械工业出版社', 5, 119.00, 9.6, 'Java程序员必读进阶书籍，90个高效Java编程建议', 5400),
('编码:隐匿在计算机软硬件背后的语言', 'Charles Petzold', '9787121397691', '电子工业出版社', 7, 79.00, 9.4, '从摩尔斯电码到计算机原理，讲述计算机底层运作的经典', 1600),
('黑客与画家', 'Paul Graham', '9787115249494', '人民邮电出版社', 5, 69.00, 8.6, '硅谷创业教父教你如何思考，程序员必读的非技术书', 2500),
('围城', '钱钟书', '9787020098095', '人民文学出版社', 8, 39.00, 9.2, '钱钟书唯一的长篇小说，中国现代文学经典', 8900),
('小王子', '圣埃克苏佩里', '9787547025727', '接力出版社', 9, 32.00, 9.4, '全球发行量最大的图书之一，写给大人的童话', 11000),
('人类简史', '尤瓦尔·赫拉利', '9787508647357', '中信出版社', 3, 68.00, 9.1, '从认知革命到科学革命，重新理解人类历史', 6700),
('思考快与慢', 'Daniel Kahneman', '9787508633558', '中信出版社', 3, 79.00, 8.9, '诺贝尔经济学奖得主教你如何做决策', 3900);

-- 库存
INSERT INTO t_inventory (book_id, stock, version) VALUES
(1, 500, 0),
(2, 300, 0),
(3, 400, 0),
(4, 250, 0),
(5, 180, 0),
(6, 800, 0),
(7, 600, 0),
(8, 1000, 0),
(9, 350, 0),
(10, 200, 0),
(11, 400, 0),
(12, 350, 0),
(13, 300, 0),
(14, 500, 0),
(15, 280, 0),
(16, 320, 0),
(17, 200, 0),
(18, 250, 0),
(19, 350, 0),
(20, 300, 0),
(21, 400, 0),
(22, 320, 0),
(23, 280, 0),
(24, 450, 0),
(25, 200, 0),
(26, 350, 0),
(27, 500, 0),
(28, 400, 0),
(29, 380, 0),
(30, 300, 0);

-- 优惠券模板
INSERT INTO t_coupon_template (name, type, threshold_amount, discount_amount, discount_rate, total_count, remaining_count, per_user_limit, start_time, end_time) VALUES
('新人满100减20', 1, 100.00, 20.00, 1.00, 1000, 1000, 1, '2024-01-01 00:00:00', '2027-12-31 23:59:59'),
('全场满200减50', 1, 200.00, 50.00, 1.00, 500, 500, 1, '2024-01-01 00:00:00', '2027-12-31 23:59:59'),
('编程图书8折', 2, 0.00, 0.00, 0.80, 300, 300, 1, '2024-01-01 00:00:00', '2027-12-31 23:59:59'),
('无门槛直减10元', 3, 0.00, 10.00, 1.00, 2000, 2000, 1, '2024-01-01 00:00:00', '2027-12-31 23:59:59');
