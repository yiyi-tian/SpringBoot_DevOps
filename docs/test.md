3. 公共前置：造数（所有用例依赖）
TC-PRE-01 注册 admin 用户
@'
{"credentialType":"PHONE","credential":"13800000001","password":"123456"}
'@ | Set-Content -Encoding utf8 register.json
curl.exe -s -X POST "http://localhost:8080/api/v1/register" -H "Content-Type: application/json" -d "@register.json"
检查项	期望
HTTP 200
code: 0
副作用
MySQL audit_log 将有 USER_REGISTER（若 TopBiz 编排成功）
TC-PRE-02 登录并保存 Cookie
curl.exe -s -c cookies.txt -X POST "http://localhost:8080/api/v1/login" -H "Content-Type: application/json" -d "@register.json"
检查项	期望
code
0
cookies.txt
含 JSESSIONID
TC-PRE-03 制造访问日志（ClickHouse 有数据）
# 多打几次 log 相关接口
curl.exe -s -b cookies.txt "http://localhost:8080/api/v1/log/ops/query?page=1&size=5&time_range=24h"
curl.exe -s -b cookies.txt "http://localhost:8080/api/v1/log/metrics?metric=pv&time_range=24h"
curl.exe -s -b cookies.txt "http://localhost:8080/api/v1/register" -H "Content-Type: application/json" -d "@register.json"
timeout /t 20
检查项	期望
文件
shared-logs/access/topbiz/access-*.jsonl 有新增
稍后 ops/query
total > 0（Vector 采集后）
4. 业务审计
TC-AUD-01 审计写入（内部，直连 8083）
@'
{
  "trace_id": "test-audit-001",
  "user_id": 1,
  "operation": "USER_LOGIN",
  "success": true,
  "target_id": "1",
  "detail": "{\"note\":\"manual test\"}"
}
'@ | Set-Content -Encoding utf8 audit-record.json
curl.exe -s -X POST "http://localhost:8083/internal/log/record" -H "Content-Type: application/json" -d "@audit-record.json"
检查项	期望
code
0
data.log_id
正整数
TC-AUD-02 审计写入 — operation 为空
curl.exe -s -X POST "http://localhost:8083/internal/log/record" -H "Content-Type: application/json" -d "{\"user_id\":1}"
| 期望 | code: 400，operation 不能为空 |

TC-AUD-03 审计写入 — 非法 operation
curl.exe -s -X POST "http://localhost:8083/internal/log/record" -H "Content-Type: application/json" -d "{\"operation\":\"FOO_BAR\",\"user_id\":1}"
| 期望 | code: 400，不支持的 operation |

合法 operation：USER_REGISTER, USER_LOGIN, USER_LOGOUT, USER_DEREGISTER, ADMIN_USER_CREATE, ADMIN_USER_DELETE, ADMIN_USER_UPDATE

TC-AUD-04 用户审计查询（对外）
curl.exe -s -b cookies.txt "http://localhost:8080/api/v1/log?page=1&size=10"
检查项	期望
code
0
data.list
含 USER_REGISTER、USER_LOGIN 等
data.total
≥ 1
字段
logId, userId, operation, success, createdAt
TC-AUD-05 按 operation 筛选
curl.exe -s -b cookies.txt "http://localhost:8080/api/v1/log?page=1&size=10&operation=USER_LOGIN"
| 期望 | 列表中 operation 均为 USER_LOGIN |

TC-AUD-06 未登录查审计
curl.exe -s "http://localhost:8080/api/v1/log?page=1&size=10"
| 期望 | code: 403 或 401 |

TC-AUD-07 内部审计查询（8083）
curl.exe -s "http://localhost:8083/internal/log/1/query?page=1&size=10"
| 期望 | code: 0，结构与 TC-AUD-04 的 data 一致（无 TopBiz 外层再包一层时 internal 自带 code） |

5. 运维日志查询 GET
TC-OPS-GET-01 默认分页（24h）
curl.exe -s -b cookies.txt "http://localhost:8080/api/v1/log/ops/query?page=1&size=10"
| 期望 | code:0；data.list 数组；data.total；data.page=1；data.size=10 |

TC-OPS-GET-02 单服务 + 耗时
curl.exe -s -b cookies.txt "http://localhost:8080/api/v1/log/ops/query?service_name=topbiz&cost_ms_min=0&page=1&size=10&time_range=24h"
| 期望 | list[].service_name 均为 topbiz |

TC-OPS-GET-03 多条件复合
curl.exe -s -b cookies.txt "http://localhost:8080/api/v1/log/ops/query?service_name=topbiz&method=POST&has_error=false&sort_by=cost_ms&sort_order=desc&page=1&size=10&time_range=7d"
TC-OPS-GET-04 仅慢请求
curl.exe -s -b cookies.txt "http://localhost:8080/api/v1/log/ops/query?slow_only=true&time_range=24h&page=1&size=10"
| 期望 | list[].cost_ms > 3000（默认阈值） |

TC-OPS-GET-05 仅错误
curl.exe -s -b cookies.txt "http://localhost:8080/api/v1/log/ops/query?has_error=true&time_range=24h&page=1&size=10"
TC-OPS-GET-06 URI 前缀
curl.exe -s -b cookies.txt "http://localhost:8080/api/v1/log/ops/query?api=/api/v1/log&time_range=24h&page=1&size=10"
TC-OPS-GET-07 关键字搜索
curl.exe -s -b cookies.txt "http://localhost:8080/api/v1/log/ops/query?keyword=register&time_range=24h&page=1&size=10"
TC-OPS-GET-08 无 Cookie（鉴权）
curl.exe -s "http://localhost:8080/api/v1/log/ops/query?page=1&size=5"
| 期望 | code: 403，权限不足 |

TC-OPS-GET-09 内部直连（8083）
curl.exe -s "http://localhost:8083/internal/log/ops/query?service_name=log&page=1&size=5&time_range=24h"
6. 运维日志查询 POST
与 GET 同一套查询引擎；适合多服务列表、嵌套 filters。

TC-OPS-POST-01 基础 filters
@'
{
  "time_range": "24h",
  "filters": {
    "service_names": ["topbiz", "log"],
    "cost_ms_min": 0
  },
  "page": 1,
  "size": 20
}
'@ | Set-Content -Encoding utf8 ops-query.json
curl.exe -s -b cookies.txt -X POST "http://localhost:8080/api/v1/log/ops/query" -H "Content-Type: application/json" -d "@ops-query.json"
TC-OPS-POST-02 复杂筛选 + 排序
@'
{
  "time_range": "7d",
  "filters": {
    "service_name": "topbiz",
    "method": "POST",
    "cost_ms_min": 50,
    "has_error": false,
    "api": "/api/v1"
  },
  "sort": { "field": "cost_ms", "order": "desc" },
  "page": 1,
  "size": 10
}
'@ | Set-Content -Encoding utf8 ops-query-complex.json
curl.exe -s -b cookies.txt -X POST "http://localhost:8080/api/v1/log/ops/query" -H "Content-Type: application/json" -d "@ops-query-complex.json"
TC-OPS-POST-03 等价于简单 GET（证明 POST 可简单查）
curl.exe -s -b cookies.txt -X POST "http://localhost:8080/api/v1/log/ops/query" -H "Content-Type: application/json" -d "{\"service_name\":\"topbiz\",\"page\":1,\"size\":10,\"time_range\":\"24h\"}"
| 期望 | 与 TC-OPS-GET-02 结果条数/结构一致 |

TC-OPS-POST-04 内部直连
curl.exe -s -X POST "http://localhost:8083/internal/log/ops/query" -H "Content-Type: application/json" -d "@ops-query.json"
7. 指标查询 GET /api/v1/log/metrics
TC-MET-01 PV
curl.exe -s -b cookies.txt "http://localhost:8080/api/v1/log/metrics?metric=pv&time_range=24h"
| 期望 | data.metric=pv，data.value 为数字，data.source=raw |

TC-MET-02 指定服务错误率
curl.exe -s -b cookies.txt "http://localhost:8080/api/v1/log/metrics?metric=error_rate&service_name=topbiz&time_range=24h"
TC-MET-03 P99
curl.exe -s -b cookies.txt "http://localhost:8080/api/v1/log/metrics?metric=p99&time_range=24h"
TC-MET-04 QPS
curl.exe -s -b cookies.txt "http://localhost:8080/api/v1/log/metrics?metric=qps&time_range=1h&interval=60"
TC-MET-05 成功率
curl.exe -s -b cookies.txt "http://localhost:8080/api/v1/log/metrics?metric=success_rate&time_range=24h"
TC-MET-06 最慢 API TopN
curl.exe -s -b cookies.txt "http://localhost:8080/api/v1/log/metrics?metric=slowest_api&time_range=24h&top_n=5"
| 期望 | data.value 为数组，含 uri、max_cost_ms |

TC-MET-07 IP 请求 TopN
curl.exe -s -b cookies.txt "http://localhost:8080/api/v1/log/metrics?metric=ip_request_topn&time_range=24h&top_n=5"
TC-MET-08 缺少 metric（异常）
curl.exe -s -b cookies.txt "http://localhost:8080/api/v1/log/metrics?time_range=24h"
| 期望 | code: 400，metric 参数不能为空 |

TC-MET-09 非法 metric（异常）
curl.exe -s -b cookies.txt "http://localhost:8080/api/v1/log/metrics?metric=not_exist&time_range=24h"
| 期望 | code: 400，无效的 metric |

TC-MET-10 全量 metric 冒烟（可选）
依次请求：api_calls, slow_count, error_count, http_5xx, http_4xx, avg, p95, max, ip_error_topn
每个均应 code: 0（无数据时 value 可为 0）。

TC-MET-11 内部直连
curl.exe -s "http://localhost:8083/internal/log/metrics?metric=pv&time_range=24h"
8. 日志导出 POST /api/v1/log/ops/export
TC-EXP-01 导出 CSV
@'
{
  "format": "csv",
  "time_range": "24h",
  "service_name": "topbiz",
  "page": 1,
  "size": 100
}
'@ | Set-Content -Encoding utf8 export-csv.json
curl.exe -s -b cookies.txt -X POST "http://localhost:8080/api/v1/log/ops/export" -H "Content-Type: application/json" -d "@export-csv.json"
| 期望 | data.format=csv；data.content 含表头；data.count ≥ 0 |

TC-EXP-02 导出 JSON
# format 改为 json
curl.exe -s -b cookies.txt -X POST "http://localhost:8080/api/v1/log/ops/export" -H "Content-Type: application/json" -d "{\"format\":\"json\",\"time_range\":\"24h\",\"page\":1,\"size\":50}"
TC-EXP-03 导出 TXT
curl.exe -s -b cookies.txt -X POST "http://localhost:8080/api/v1/log/ops/export" -H "Content-Type: application/json" -d "{\"format\":\"txt\",\"time_range\":\"24h\",\"page\":1,\"size\":50}"
TC-EXP-04 默认 format（不传）
curl.exe -s -b cookies.txt -X POST "http://localhost:8080/api/v1/log/ops/export" -H "Content-Type: application/json" -d "{\"time_range\":\"24h\",\"page\":1,\"size\":10}"
| 期望 | data.format=csv |

TC-EXP-05 非法 format
curl.exe -s -b cookies.txt -X POST "http://localhost:8080/api/v1/log/ops/export" -H "Content-Type: application/json" -d "{\"format\":\"xlsx\",\"time_range\":\"24h\"}"
| 期望 | code: 400，不支持 xlsx |

TC-EXP-06 带筛选条件导出
curl.exe -s -b cookies.txt -X POST "http://localhost:8080/api/v1/log/ops/export" -H "Content-Type: application/json" -d "@ops-query-complex.json"
将 ops-query-complex.json 加上 "format": "csv" 后请求。

9. 指标阈值配置
TC-CFG-01 读取配置
curl.exe -s -b cookies.txt "http://localhost:8080/api/v1/log/metrics/config"
| 期望 | data.error_rate_max、p99_max、success_rate_min、slow_count_max 有值；data.configs 为数组 |

TC-CFG-02 更新 error_rate_max
@'
{
  "config_key": "error_rate_max",
  "threshold_value": 0.08,
  "severity": "WARN"
}
'@ | Set-Content -Encoding utf8 metrics-config.json
curl.exe -s -b cookies.txt -X PUT "http://localhost:8080/api/v1/log/metrics/config" -H "Content-Type: application/json" -d "@metrics-config.json"
| 期望 | code: 0 |

TC-CFG-03 更新 p99_max
curl.exe -s -b cookies.txt -X PUT "http://localhost:8080/api/v1/log/metrics/config" -H "Content-Type: application/json" -d "{\"config_key\":\"p99_max\",\"threshold_value\":2500,\"severity\":\"HIGH\"}"
TC-CFG-04 更新 success_rate_min
curl.exe -s -b cookies.txt -X PUT "http://localhost:8080/api/v1/log/metrics/config" -H "Content-Type: application/json" -d "{\"config_key\":\"success_rate_min\",\"threshold_value\":0.92}"
TC-CFG-05 更新 slow_count_max
curl.exe -s -b cookies.txt -X PUT "http://localhost:8080/api/v1/log/metrics/config" -H "Content-Type: application/json" -d "{\"config_key\":\"slow_count_max\",\"threshold_value\":50}"
TC-CFG-06 读回验证
curl.exe -s -b cookies.txt "http://localhost:8080/api/v1/log/metrics/config"
| 期望 | 阈值与 TC-CFG-02~05 一致 |

TC-CFG-07 非法 config_key
curl.exe -s -b cookies.txt -X PUT "http://localhost:8080/api/v1/log/metrics/config" -H "Content-Type: application/json" -d "{\"config_key\":\"foo\",\"threshold_value\":1}"
| 期望 | code: 400，无效的配置项 |

TC-CFG-08 内部直连
curl.exe -s "http://localhost:8083/internal/log/metrics/config"
curl.exe -s -X PUT "http://localhost:8083/internal/log/metrics/config" -H "Content-Type: application/json" -d "@metrics-config.json"
