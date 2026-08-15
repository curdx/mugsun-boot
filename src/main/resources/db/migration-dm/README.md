# 达梦（DM）Flyway 迁移脚本

本目录存放**达梦 / Oracle 语法**的 Flyway 迁移脚本，与 `classpath:db/migration`（PostgreSQL）**同版本号、同业务语义**，语法不可混用。

> **目录故意与 PG 并列**（`db/migration-dm`，而非 `db/migration/dm`）：Flyway 默认会递归扫描 `db/migration/**`，子目录同版本号会与 PG 脚本冲突导致启动失败。

## 为何独立目录

PG 脚本含 `INT4` / `TEXT` / 部分索引 `WHERE is_deleted = 0` / `ON CONFLICT` / `RETURNING` 等，达梦（Oracle 系）无法直接执行。须在此维护同版本号的 `NUMBER` / `VARCHAR2` / `CLOB` / `MERGE` 等 Oracle 语法脚本，并引入 `flyway-database-oracle`（或达梦官方 Flyway 扩展）。

## 如何启用

```yaml
spring:
  datasource:
    url: jdbc:dm://<host>:5236?schema=MUGSUN
    username: <user>
    password: <pwd>
  flyway:
    locations: classpath:db/migration-dm
```

打包时激活达梦驱动 profile：

```bash
mvn -Pdameng clean package -DskipTests
```

（`DmJdbcDriver18` 为厂商私有构件，需先安装到本地/私服。）

## 当前交付边界

| 项 | 状态 |
| --- | --- |
| 运行时 Java 方言（§5：MERGE / SYSTIMESTAMP / ALTER SESSION / 分页等） | ✅ 已按 `SqlDialect` 分支 |
| 本目录完整 V1–V68 与 PG 对齐 | ⏳ **待达梦实例联调后逐脚本转换** |
| 占位 `V1__auth.sql` | 仅示意 Oracle 系类型与注释写法，**不足以启动全量业务** |

> 完整 V1–V68 转换依赖真实达梦实例校验（标识符大小写、注释语法、唯一约束、Warm-Flow 表等），当前环境无厂商镜像授权，故不硬转全部 68 个 SQL。具备实例后：按版本号逐个从 `db/migration/V*.sql` 改写并 `flyway migrate` 验证。

## 编写约定

- 版本号与 PG 目录一一对应（如 `V22__serial_number.sql`）
- 主键/整型用 `NUMBER(19)` / `NUMBER(10)`；时间戳 `TIMESTAMP`；大文本 `CLOB`；变长 `VARCHAR2(n)`
- 列默认值顺序：`DEFAULT … NOT NULL`（勿写 `NOT NULL DEFAULT`）
- 勿使用 `DROP … IF EXISTS`、`ON CONFLICT`、`RETURNING`、`xmax`、部分索引
