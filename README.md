# mugsun-boot

Mugsun 平台单体版：将全部功能模块打包为单个可执行应用，开箱即用的企业级快速开发平台。基于 [mugsun-core](https://github.com/curdx/mugsun-core) 构建，管理端为 [mugsun-pc](https://github.com/curdx/mugsun-pc)。

## 功能

- **组织与权限**：用户/角色/菜单/按钮权限、行级数据权限引擎、字段级权限与脱敏、后端菜单驱动（授权一刷新生效）
- **多租户 SaaS**：字段隔离 + 独立数据源/schema 可插拔、租户套餐与生命周期强制、全局配置平台收口
- **认证安全**：图形验证码、双因子、等保密码策略、忘记密码（邮件验证码）、第三方社交登录、OAuth2 开放平台（PKCE/刷新轮换/自省/撤销）、接口限流防重、XSS 全局过滤、国密 SM2/SM3/SM4 传输与存储加密
- **可观测与审计**：操作日志（自动织入+哈希链防篡改验签）、登录日志（UA 解析/IP 归属地/账号解锁）、访问日志与错误日志闭环、全站 traceId、Prometheus 指标、在线会话与强退
- **效率工具**：代码生成全栈（树懒加载/主子表级联/菜单 SQL）、在线表单与动态建表、Excel 导入导出（模板/覆盖/失败明细）、单号生成器、缓存管理
- **消息与调度**：站内信+邮件+短信多渠道统一调度（失败重试流水）、WebSocket 实时推送、Warm-Flow 工作流（分支/并行/会签/审批中心）、PowerJob 分布式任务（处理器注册表真实可执行）

## 技术栈

JDK 21（虚拟线程）· Spring Boot 3.5.x · MyBatis-Flex · Sa-Token · JetCache · Warm-Flow · PowerJob · Knife4j · x-file-storage · SMS4J · PostgreSQL（主推，多方言适配）

## 运行

1. 先在 mugsun-core 执行 `mvn clean install -DskipTests`
2. 准备 PostgreSQL 16、Redis 7（调度需 PowerJob Server）

```bash
mvn spring-boot:run          # 开发启动（默认 8080）
mvn clean package -DskipTests
```

## 测试

```bash
mvn test    # 69 个集成测试：Testcontainers 自动拉起 PostgreSQL/Redis，真实登录/权限/租户/工作流链路
```

## 许可

[Apache License 2.0](LICENSE)
