# 达梦（DM）Flyway 迁移脚本

本目录存放**达梦 / Oracle 语法**脚本，与 `classpath:db/migration`（PostgreSQL）同版本号、同业务语义。由 `scripts/pg_to_dm.py` 从 PG 脚本机械转换后经达梦实例校验。

> **目录故意与 PG 并列**（`db/migration-dm`，而非 `db/migration/dm`）：Flyway 默认会递归扫描 `db/migration/**`。

## 如何启用

```yaml
spring:
  datasource:
    url: jdbc:dm://<host>:5236?schema=MUGSUN
    username: MUGSUN
    password: <pwd>
  flyway:
    enabled: false          # 社区 Flyway 不识别 DM DBMS 8.1
    locations: classpath:db/migration-dm
```

灌库：`scripts/pg_to_dm.py` 生成后用 JDBC 按版本号逐语句执行（见联调记录）。打包：`mvn -Pdameng package -DskipTests`。

## 编写约定

- 主键/整型：`BIGINT` / `INT` / `SMALLINT`（达梦均支持，避免与 Flex 未加引号标识符大小写纠缠时再转 NUMBER）
- 大文本 `CLOB`；时间戳 `TIMESTAMP`；`now()` → `SYSDATE`
- `ON CONFLICT` / 部分索引 WHERE / `VALUES (...) AS v()` / `DO $$` 已改写或跳过
- 保留字列（如 `type`、`domain`）加双引号小写，对齐 Flex DmDialect
