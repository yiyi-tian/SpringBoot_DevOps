#!/bin/bash
# ==========================================
# 一键建表脚本（MySQL + ClickHouse）
# 使用前请确认本文件的配置正确
# ==========================================

# ---------- MySQL 配置 ----------
MYSQL_HOST="localhost"
MYSQL_PORT="3306"
MYSQL_USER="root"
MYSQL_PASS="12345"

# ---------- ClickHouse 配置 ----------
CH_HOST="localhost"
CH_PORT="9000"
CH_USER="default"
CH_PASS=""
# ---------------------------------

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "========================================="
echo "  一键建表脚本"
echo "  MySQL: ${MYSQL_HOST}:${MYSQL_PORT}"
echo "========================================="

# ---------- 获取 SQL 文件 ----------
MYSQL_FILES=$(ls "${SCRIPT_DIR}"/*.sql 2>/dev/null | grep -v -i clickhouse | sort -t'_' -k1,1n)
CH_FILES=$(ls "${SCRIPT_DIR}"/*clickhouse*.sql 2>/dev/null | sort)

if [ -z "$MYSQL_FILES" ] && [ -z "$CH_FILES" ]; then
    echo "未找到 SQL 文件"
    exit 1
fi

# ==================== MySQL ====================
if [ -n "$MYSQL_FILES" ]; then
    echo ""
    echo "--- MySQL SQL 文件 ---"
    for f in $MYSQL_FILES; do
        echo "  $(basename $f)"
    done

    echo ""
    echo "[MySQL] 创建数据库..."
    mysql -h${MYSQL_HOST} -P${MYSQL_PORT} -u${MYSQL_USER} -p${MYSQL_PASS} -e "
CREATE DATABASE IF NOT EXISTS devops_user DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS devops_message DEFAULT CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS devops_log DEFAULT CHARACTER SET utf8mb4;
"

    for f in $MYSQL_FILES; do
        FILENAME=$(basename "$f")
        echo "[MySQL] 执行 ${FILENAME}..."

        case "$FILENAME" in
            *user*)   DB="devops_user" ;;
            *message*) DB="devops_message" ;;
            *log*)    DB="devops_log" ;;
            *)        DB="" ;;
        esac

        if [ -n "$DB" ]; then
            mysql -h${MYSQL_HOST} -P${MYSQL_PORT} -u${MYSQL_USER} -p${MYSQL_PASS} "$DB" < "$f"
            echo "    -> 已导入 ${DB}"
        else
            mysql -h${MYSQL_HOST} -P${MYSQL_PORT} -u${MYSQL_USER} -p${MYSQL_PASS} < "$f"
            echo "    -> 已执行"
        fi
    done

    echo ""
    echo "--- MySQL 表列表 ---"
    for DB in devops_user devops_message devops_log; do
        echo "  [${DB}]"
        mysql -h${MYSQL_HOST} -P${MYSQL_PORT} -u${MYSQL_USER} -p${MYSQL_PASS} "$DB" -e "SHOW TABLES;" 2>/dev/null
    done
else
    echo "未找到 MySQL SQL 文件，跳过"
fi

# ==================== ClickHouse ====================
if [ -n "$CH_FILES" ]; then
    echo ""
    echo "--- ClickHouse SQL 文件 ---"
    for f in $CH_FILES; do
        echo "  $(basename $f)"
    done

    # 检查 clickhouse-client 是否可用
    if command -v clickhouse-client &>/dev/null; then
        echo ""
        for f in $CH_FILES; do
            echo "[ClickHouse] 执行 $(basename $f)..."
            clickhouse-client -h ${CH_HOST} --port ${CH_PORT} -u ${CH_USER} --password "${CH_PASS}" --multiquery < "$f"
        done

        echo ""
        echo "--- ClickHouse 表列表 ---"
        clickhouse-client -h ${CH_HOST} --port ${CH_PORT} -u ${CH_USER} --password "${CH_PASS}" -q "SHOW TABLES FROM devops;"
    else
        echo "[ClickHouse] clickhouse-client 未安装，跳过"
        echo "  手动执行："
        for f in $CH_FILES; do
            echo "    clickhouse-client --multiquery < $f"
        done
        echo "  或用 HTTP 方式："
        for f in $CH_FILES; do
            echo "    curl -X POST 'http://${CH_HOST}:8123/' --data-binary @$f"
        done
    fi
else
    echo "未找到 ClickHouse SQL 文件，跳过"
fi

echo ""
echo "========================================="
echo "  建表完成"
echo "========================================="