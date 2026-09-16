# 在线书城系统（Online Bookstore）

一个前后端分离、面向 C 端的图书商城，覆盖商品浏览/搜索、购物车、下单、支付、优惠券秒杀、退款与后台管理，并在**高并发、分布式一致性、可观测性**方面做了工程化落地。

> 后端 Spring Boot 3 + MySQL + Redis + Elasticsearch；前端 Vue 3 + Vite + Element Plus。

## 功能特性

- **用户与安全**：注册/登录（图形验证码）、手机号+验证码找回密码、JWT + 登出黑名单、登录失败锁定、RBAC 与方法级鉴权、接口防重提交、敏感信息脱敏。
- **商品与搜索**：分类树、图书列表/详情；Elasticsearch 全文检索（IK 中文分词 + 拼音/首字母）、书名/作者字段检索、分面导航、搜索建议、作者主页；ES 异常自动降级 MySQL。
- **购物车**：Redis 主存储 + Redis Stream 可靠落库（DB 备份可恢复）。
- **订单**：下单幂等、订单状态机、库存预占、超时自动关单（Redisson 延时队列）。
- **支付**：模拟网关 RSA 验签回调、金额三重核对、幂等记账、退款、对账补偿。
- **优惠券**：领取/核销/退还、秒杀（Redis Lua 原子预扣 + Stream 可靠落库 + 对账）。
- **一致性**：库存预占 + 本地消息表 Outbox + 延时关单 + 支付/库存/优惠券三处对账。
- **可观测性**：Micrometer + Prometheus + Grafana + Alertmanager + Zipkin，指标/日志(traceId)/链路/告警。
- **后台管理**：图书、分类、库存、订单、优惠券管理。

## 技术栈

| 层次 | 技术 |
|---|---|
| 后端 | Java 17、Spring Boot 3.3、MyBatis-Plus、Spring Security、JJWT |
| 存储 | MySQL 8、Redis 7、Elasticsearch 8.11（IK + analysis-pinyin 插件） |
| 分布式 | Redisson（分布式锁/延时队列）、Micrometer（指标/链路） |
| 前端 | Vue 3、Vite、TypeScript、Element Plus、Pinia、Vue Router |
| 可观测 | Prometheus、Grafana、Alertmanager、Zipkin |
| 部署 | Docker Compose、Nginx |

## 目录结构

```
market/
├── market-server/   # Spring Boot 后端
│   └── src/main/java/com/market/{common,module}
├── market-web/      # Vue 3 前端
├── docker/          # docker-compose、ES 自定义镜像、nginx、init.sql
├── loadtest/        # 秒杀压测脚本
└── docs/            # 架构与技术设计文档
```

## 快速开始

### 1. 准备环境变量（不提交真实密钥）

```bash
# 后端：复制本地私密配置
cp market-server/application-local.example.yml market-server/application-local.yml
# 编辑 application-local.yml 填入 DB/Redis 密码与 JWT 密钥

# 基础设施：复制 compose 变量
cp docker/.env.example docker/.env
# 编辑 docker/.env 填入 MySQL/Redis 密码
```
> 也支持用环境变量注入：`DB_PASSWORD`、`REDIS_PASSWORD`、`JWT_SECRET`。
> `application-local.yml`、`docker/.env` 已在 `.gitignore` 中，不会被提交。

### 2. 启动基础设施

```bash
cd docker
docker compose up -d mysql redis elasticsearch
# 可选：可观测栈
docker compose -f docker-compose.observability.yml up -d
```
首次启动 `mysql` 会自动执行 `docker/mysql/init.sql`（建表 + 种子数据 + 管理员 `admin / admin123`）。

### 3. 启动后端

```bash
cd market-server
./mvnw spring-boot:run     # Windows: mvnw.cmd spring-boot:run
```

### 4. 启动前端

```bash
cd market-web
npm install
npm run dev                # http://localhost:5173
```

## 默认端口

| 服务 | 端口 |
|---|---|
| 前端 | 5173 |
| 后端 | 8080 |
| MySQL | 3307 |
| Redis | 6379 |
| Elasticsearch | 9200 |
| Prometheus / Grafana / Alertmanager / Zipkin | 9090 / 3000 / 9093 / 9411 |

## 压测

```bash
# 用管理员在后台创建秒杀券并预热后
TEMPLATE_ID=<id> USERS=1000 CONCURRENCY=500 \
  node loadtest/seckill-loadtest.mjs
```

## 文档

详细架构与实现见 [`docs/`](docs/README.md)：
总体架构、认证与安全、商品与搜索(ES)、购物车与缓存、订单与库存预占、支付与对账、秒杀、分布式一致性与 Outbox、可观测性、部署与压测、存储设计、面试讲解版。

## 安全说明

- 仓库中**不含任何真实密钥**，均通过环境变量或本地未跟踪文件注入。
- 演示用管理员账号与数据库口令仅为本地开发默认值，生产环境请务必替换。
