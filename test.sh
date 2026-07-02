#!/bin/bash
BASE="http://localhost:8080/api/v1"
PASS=0
FAIL=0

check() {
  local RESULT="$1"
  local MSG="$2"
  local EXPECT="${3:-0}"  # 期望 code，默认 0
  local ACTUAL=$(echo "$RESULT" | grep -o '"code":[0-9]*' | head -1 | grep -o '[0-9]*')
  
  if [ "$ACTUAL" = "$EXPECT" ]; then
    echo "✅ $MSG"
    echo "   $RESULT"
    ((PASS++))
  else
    echo "❌ $MSG (期望 code=$EXPECT, 实际 code=$ACTUAL)"
    echo "   $RESULT"
    ((FAIL++))
  fi
}

echo "========== 1. 注册 =========="
# 跳过（已注册过）

echo "========== 2. 登录 =========="
R=$(curl -s -X POST "$BASE/login" -H "Content-Type: application/json" -d '{"phone":"13800000001","password":"123456"}' -c /tmp/cookie_a.txt)
check "$R" "登录A"
R=$(curl -s -X POST "$BASE/login" -H "Content-Type: application/json" -d '{"phone":"13900000001","password":"123456"}' -c /tmp/cookie_b.txt)
check "$R" "登录B"

echo "========== 3. 站内信 =========="
R=$(curl -s -X POST "$BASE/send/instant" -H "Content-Type: application/json" -b /tmp/cookie_a.txt -d '{"channelType":"IN_APP","receiver":"2","content":"你好","templateId":1}')
check "$R" "发站内信"
R=$(curl -s -X GET "$BASE/messages/inbox?receiver=2" -b /tmp/cookie_b.txt)
check "$R" "查信箱"

echo "========== 4. 模板 =========="
R=$(curl -s -X POST "$BASE/templates" -H "Content-Type: application/json" -b /tmp/cookie_a.txt -d '{"name":"测试","content":"hello","channelType":"IN_APP"}')
check "$R" "创建模板"
R=$(curl -s -X GET "$BASE/templates" -b /tmp/cookie_a.txt)
check "$R" "查模板"

echo "========== 5. 权限 =========="
R=$(curl -s -X GET "$BASE/templates" -b /tmp/cookie_b.txt)
check "$R" "普通用户禁访模板(应403)" "403"

echo "========== 6. 载体 =========="
R=$(curl -s -X POST "$BASE/msg/carriers" -H "Content-Type: application/json" -b /tmp/cookie_a.txt -d '{"name":"test","channelType":"EMAIL","configJson":"{}"}')
check "$R" "创建载体"
R=$(curl -s -X GET "$BASE/msg/carriers" -b /tmp/cookie_a.txt)
check "$R" "查载体"

echo ""
echo "========== 结果: $PASS 通过, $FAIL 失败 =========="
