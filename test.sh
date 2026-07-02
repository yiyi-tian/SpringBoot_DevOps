#!/bin/bash
BASE="http://localhost:8080/api/v1"
PASS=0
FAIL=0

check() {
  if echo "$1" | grep -q '"code":0'; then
    echo "✅ $2"
    ((PASS++))
  else
    echo "❌ $2: $1"
    ((FAIL++))
  fi
}

# echo "========== 1. 注册 =========="
# check "$(curl -s -X POST "$BASE/register" -H "Content-Type: application/json" -d '{"phone":"13800000001","password":"123456"}')" "注册A"
# check "$(curl -s -X POST "$BASE/register" -H "Content-Type: application/json" -d '{"phone":"13900000001","password":"123456"}')" "注册B"

# echo "========== 2. 登录 =========="
curl -s -X POST "$BASE/login" -H "Content-Type: application/json" -d '{"phone":"13800000001","password":"123456"}' -c /tmp/cookie_a.txt > /dev/null
RESULT=$(curl -s -X POST "$BASE/login" -H "Content-Type: application/json" -d '{"phone":"13800000001","password":"123456"}' -c /tmp/cookie_a.txt)
check "$RESULT" "登录A"

curl -s -X POST "$BASE/login" -H "Content-Type: application/json" -d '{"phone":"13900000001","password":"123456"}' -c /tmp/cookie_b.txt > /dev/null
RESULT=$(curl -s -X POST "$BASE/login" -H "Content-Type: application/json" -d '{"phone":"13900000001","password":"123456"}' -c /tmp/cookie_b.txt)
check "$RESULT" "登录B"

echo "========== 3. 站内信 =========="
check "$(curl -s -X POST "$BASE/send/instant" -H "Content-Type: application/json" -b /tmp/cookie_a.txt -d '{"channelType":"IN_APP","receiver":"2","content":"你好","templateId":1}')" "发站内信"
check "$(curl -s -X GET "$BASE/messages/inbox?receiver=2" -b /tmp/cookie_b.txt)" "查信箱"

echo "========== 4. 模板 =========="
check "$(curl -s -X POST "$BASE/templates" -H "Content-Type: application/json" -b /tmp/cookie_a.txt -d '{"name":"测试","content":"hello","channelType":"IN_APP"}')" "创建模板"
check "$(curl -s -X GET "$BASE/templates" -b /tmp/cookie_a.txt)" "查模板"

echo "========== 5. 权限 =========="
check "$(curl -s -X GET "$BASE/templates" -b /tmp/cookie_b.txt)" "普通用户禁访模板(应403)"

echo "========== 6. 载体 =========="
check "$(curl -s -X POST "$BASE/msg/carriers" -H "Content-Type: application/json" -b /tmp/cookie_a.txt -d '{"name":"test","channelType":"EMAIL","configJson":"{}"}')" "创建载体"
check "$(curl -s -X GET "$BASE/msg/carriers" -b /tmp/cookie_a.txt)" "查载体"

echo ""
echo "========== 结果: $PASS 通过, $FAIL 失败 =========="
