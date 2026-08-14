#!/bin/bash
# Mugsun 数据库初始化（仅 PostgreSQL 容器首次启动、数据卷为空时执行）
# 复用仓库内已验证的 scripts/init-db.sql（幂等建账号/主库/埋点库），
# 账号密码由 MUGSUN_DB_PASSWORD 环境变量注入，不落明文进镜像与仓库。
set -euo pipefail

: "${MUGSUN_DB_PASSWORD:?必须设置 MUGSUN_DB_PASSWORD（应用账号 mugsun 的密码，仅限字母数字）}"

sed "s/WITH PASSWORD '[^']*'/WITH PASSWORD '${MUGSUN_DB_PASSWORD}'/" /initdb/init-db.sql \
  | psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB"

echo ">> mugsun / mugsun_track 数据库初始化完成"
