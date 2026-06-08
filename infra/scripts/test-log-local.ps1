# 本地 ClickHouse + Vector 测试脚本（MySQL 仍用云服务器）
# 用法：在仓库根目录执行  powershell -File infra/scripts/test-log-local.ps1

$ErrorActionPreference = "Stop"
$Root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
if (-not (Test-Path "$Root\pom.xml")) { $Root = Split-Path $PSScriptRoot -Parent }

Write-Host "==> 1. 启动 ClickHouse + Vector (Docker)"
Set-Location "$Root\infra"
docker compose -f docker-compose.local.yml up -d
if ($LASTEXITCODE -ne 0) {
    Write-Warning "Docker 启动失败。请检查 Docker Desktop 是否运行，并修复镜像源（Settings -> Docker Engine，删除失效的 mirrors.ustc.edu.cn）"
}

Write-Host "==> 2. 等待 ClickHouse 就绪"
Start-Sleep -Seconds 8
docker exec devops-clickhouse clickhouse-client --query "SHOW TABLES FROM devops" 2>$null

Write-Host "==> 3. 调用 log-service record（需 log-service 已在 8083 运行）"
$bodyFile = "$Root\infra\test-record.json"
curl.exe -s -X POST "http://localhost:8083/internal/log/record" `
  -H "Content-Type: application/json" `
  -d "@$bodyFile"
Write-Host ""

Write-Host "==> 4. 查看 JSONL（shared-logs/access）"
Get-ChildItem "$Root\shared-logs\access" -Recurse -Filter "*.jsonl" | ForEach-Object {
    Write-Host "--- $($_.FullName) ---"
    Get-Content $_.FullName -Tail 2
}

Write-Host "==> 5. 等待 Vector 入库后查 ClickHouse"
Start-Sleep -Seconds 8
docker exec devops-clickhouse clickhouse-client --query "SELECT trace_id, service_name, uri, http_status FROM devops.access_log ORDER BY timestamp DESC LIMIT 5" 2>$null

Write-Host "==> 完成"
