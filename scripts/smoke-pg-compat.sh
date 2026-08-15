#!/usr/bin/env bash
# 金仓 / openGauss（PG 兼容系）冒烟清单——需可连通的实例与（金仓）厂商 JDBC。
# 本机 Docker Desktop 上 enmotech/opengauss 常因 cgroup 无法拉起；有 Linux/K8s 实例时跑本脚本。
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

: "${DB_HOST:?set DB_HOST}"
: "${DB_PORT:=54321}"
: "${DB_NAME:=mugsun}"
: "${DB_USER:?set DB_USER}"
: "${DB_PASSWORD:?set DB_PASSWORD}"
: "${JDBC_URL:=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}}"

echo "==> 1) 方言单测（无实例）"
mvn -q -Dtest=SqlDialectTest test

echo "==> 2) 打包（可选 -Pkingbase 需本地已 install 厂商 jar）"
if [[ "${USE_KINGBASE_PROFILE:-0}" == "1" ]]; then
  mvn -q -Pkingbase -DskipTests package
else
  mvn -q -DskipTests package
fi

echo "==> 3) 请用下列配置启动应用后手工验收："
cat <<EOF
spring.datasource.url=${JDBC_URL}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
spring.flyway.locations=classpath:db/migration

验收：启动无报错 → 登录 → /system/user/page → /system/gen DDL 建表出型为 PG 族。
EOF
