package org.example.topbiz.service;

import org.example.topbiz.feign.LogServiceClient;
import org.example.topbiz.feign.UserServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 自驱任务调度服务：认证过期检测、不活跃用户治理、注销用户物理删除、缓存清理、过期数据清理
 *
 * 设计原则：
 * - Topbiz 作为调度中心，定期发起普查/清理请求
 * - 具体数据操作由 user-service 执行
 * - 每次执行记录审计日志
 */
@Service
public class SchedulerService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerService.class);

    @Autowired
    private UserServiceClient userServiceClient;

    @Autowired
    private LogServiceClient logServiceClient;

    /**
     * 认证过期检测 — 每日凌晨 3:00 执行
     * 扫描 user_auth.expired_at 超过 14 天的用户，标记为 EXPIRED
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void expireStaleAuths() {
        String traceId = UUID.randomUUID().toString();
        MDC.put("traceId", traceId);
        log.info("[Scheduler] 开始执行：认证过期检测");

        try {
            Map<String, Object> request = new HashMap<>();
            request.put("days", 14);
            Map<String, Object> result = userServiceClient.expireStaleAuths(request);

            if (result != null && "0".equals(String.valueOf(result.get("code")))) {
                Map<String, Object> data = (Map<String, Object>) result.get("data");
                log.info("[Scheduler] 认证过期检测完成：受影响用户数 = {}", data.get("affectedCount"));
                recordSchedulerAudit("SCHEDULER_EXPIRE_AUTHS", true,
                        "affectedCount=" + data.get("affectedCount"));
            } else {
                log.warn("[Scheduler] 认证过期检测异常：{}", result);
                recordSchedulerAudit("SCHEDULER_EXPIRE_AUTHS", false, "unexpected result");
            }
        } catch (Exception e) {
            log.error("[Scheduler] 认证过期检测失败", e);
            recordSchedulerAudit("SCHEDULER_EXPIRE_AUTHS", false, e.getMessage());
        } finally {
            MDC.remove("traceId");
        }
    }

    /**
     * 长期未活跃用户识别 — 每周日凌晨 4:00 执行
     * 扫描 last_login_at 超过 30 天的活跃用户，标记为 INACTIVE
     */
    @Scheduled(cron = "0 0 4 ? * SUN")
    public void deactivateInactiveUsers() {
        String traceId = UUID.randomUUID().toString();
        MDC.put("traceId", traceId);
        log.info("[Scheduler] 开始执行：不活跃用户识别");

        try {
            Map<String, Object> request = new HashMap<>();
            request.put("days", 30);
            Map<String, Object> result = userServiceClient.deactivateInactiveUsers(request);

            if (result != null && "0".equals(String.valueOf(result.get("code")))) {
                Map<String, Object> data = (Map<String, Object>) result.get("data");
                log.info("[Scheduler] 不活跃用户识别完成：受影响用户数 = {}", data.get("affectedCount"));
                recordSchedulerAudit("SCHEDULER_DEACTIVATE_INACTIVE", true,
                        "affectedCount=" + data.get("affectedCount"));
            } else {
                log.warn("[Scheduler] 不活跃用户识别异常：{}", result);
                recordSchedulerAudit("SCHEDULER_DEACTIVATE_INACTIVE", false, "unexpected result");
            }
        } catch (Exception e) {
            log.error("[Scheduler] 不活跃用户识别失败", e);
            recordSchedulerAudit("SCHEDULER_DEACTIVATE_INACTIVE", false, e.getMessage());
        } finally {
            MDC.remove("traceId");
        }
    }

    /**
     * 注销用户物理删除 — 每周日凌晨 5:00 执行
     * 扫描 DEREGISTERED 状态超过 30 天的用户，彻底删除 PII 数据
     */
    @Scheduled(cron = "0 0 5 ? * SUN")
    public void purgeDeregisteredUsers() {
        String traceId = UUID.randomUUID().toString();
        MDC.put("traceId", traceId);
        log.info("[Scheduler] 开始执行：注销用户物理删除");

        try {
            Map<String, Object> request = new HashMap<>();
            request.put("days", 30);
            Map<String, Object> result = userServiceClient.purgeDeregisteredUsers(request);

            if (result != null && "0".equals(String.valueOf(result.get("code")))) {
                Map<String, Object> data = (Map<String, Object>) result.get("data");
                log.info("[Scheduler] 注销用户物理删除完成：删除用户数 = {}", data.get("deletedCount"));
                recordSchedulerAudit("SCHEDULER_PURGE_DEREGISTERED", true,
                        "deletedCount=" + data.get("deletedCount"));
            } else {
                log.warn("[Scheduler] 注销用户物理删除异常：{}", result);
                recordSchedulerAudit("SCHEDULER_PURGE_DEREGISTERED", false, "unexpected result");
            }
        } catch (Exception e) {
            log.error("[Scheduler] 注销用户物理删除失败", e);
            recordSchedulerAudit("SCHEDULER_PURGE_DEREGISTERED", false, e.getMessage());
        } finally {
            MDC.remove("traceId");
        }
    }

    /**
     * 清理用户过期缓存 — 每月 1 日凌晨 2:00 执行（1.2.3.1）
     * 建立缓存键命名规范与版本管理策略，清理旧版本缓存。
     * 当前 Shiro 会话由 Redis TTL 自动管理，此任务预留为应用级缓存版本升级入口。
     */
    @Scheduled(cron = "0 0 2 1 * ?")
    public void cleanExpiredCaches() {
        String traceId = UUID.randomUUID().toString();
        MDC.put("traceId", traceId);
        log.info("[Scheduler] 开始执行：过期缓存清理");

        try {
            Map<String, Object> result = userServiceClient.cleanExpiredCaches();

            if (result != null && "0".equals(String.valueOf(result.get("code")))) {
                log.info("[Scheduler] 过期缓存清理完成（Redis TTL 自动管理，无需额外操作）");
                recordSchedulerAudit("SCHEDULER_CLEAN_CACHES", true, "ok");
            } else {
                log.warn("[Scheduler] 过期缓存清理异常：{}", result);
                recordSchedulerAudit("SCHEDULER_CLEAN_CACHES", false, "unexpected result");
            }
        } catch (Exception e) {
            log.error("[Scheduler] 过期缓存清理失败", e);
            recordSchedulerAudit("SCHEDULER_CLEAN_CACHES", false, e.getMessage());
        } finally {
            MDC.remove("traceId");
        }
    }

    /**
     * 清理用户过期数据 — 每月 1 日凌晨 2:30 执行（1.2.3.3）
     * 物理删除超过 90 天的 REJECTED 权限申请记录，防止 junction 表无限增长。
     */
    @Scheduled(cron = "0 30 2 1 * ?")
    public void cleanExpiredData() {
        String traceId = UUID.randomUUID().toString();
        MDC.put("traceId", traceId);
        log.info("[Scheduler] 开始执行：过期数据清理");

        try {
            Map<String, Object> request = new HashMap<>();
            request.put("days", 90);
            Map<String, Object> result = userServiceClient.cleanExpiredData(request);

            if (result != null && "0".equals(String.valueOf(result.get("code")))) {
                Map<String, Object> data = (Map<String, Object>) result.get("data");
                log.info("[Scheduler] 过期数据清理完成：删除用户权限申请 {} 条，组权限申请 {} 条",
                        data.get("deletedUserPermissions"), data.get("deletedGroupPermissions"));
                recordSchedulerAudit("SCHEDULER_CLEAN_EXPIRED_DATA", true,
                        "upDeleted=" + data.get("deletedUserPermissions")
                                + ", gpDeleted=" + data.get("deletedGroupPermissions"));
            } else {
                log.warn("[Scheduler] 过期数据清理异常：{}", result);
                recordSchedulerAudit("SCHEDULER_CLEAN_EXPIRED_DATA", false, "unexpected result");
            }
        } catch (Exception e) {
            log.error("[Scheduler] 过期数据清理失败", e);
            recordSchedulerAudit("SCHEDULER_CLEAN_EXPIRED_DATA", false, e.getMessage());
        } finally {
            MDC.remove("traceId");
        }
    }

    /**
     * v0.4.0: IP 突变扫描 — 每日凌晨 3:30 执行（1.2.2.2）
     * 扫描 login_history，对 24h 内从多个 /16 子网登录的用户标记 RISKY/LOCKED。
     */
    @Scheduled(cron = "0 30 3 * * ?")
    public void scanIpMutations() {
        String traceId = UUID.randomUUID().toString();
        MDC.put("traceId", traceId);
        log.info("[Scheduler] 开始执行：IP 突变扫描");

        try {
            Map<String, Object> request = new HashMap<>();
            request.put("hours", 24);
            Map<String, Object> result = userServiceClient.scanIpMutations(request);

            if (result != null && "0".equals(String.valueOf(result.get("code")))) {
                Map<String, Object> data = (Map<String, Object>) result.get("data");
                log.info("[Scheduler] IP 突变扫描完成：RISKY {} 人，LOCKED {} 人，扫描 {} 人",
                        data.get("riskyCount"), data.get("lockedCount"), data.get("scannedUsers"));
                recordSchedulerAudit("SCHEDULER_SCAN_IP_MUTATIONS", true,
                        "risky=" + data.get("riskyCount")
                                + ", locked=" + data.get("lockedCount")
                                + ", scanned=" + data.get("scannedUsers"));
            } else {
                log.warn("[Scheduler] IP 突变扫描异常：{}", result);
                recordSchedulerAudit("SCHEDULER_SCAN_IP_MUTATIONS", false, "unexpected result");
            }
        } catch (Exception e) {
            log.error("[Scheduler] IP 突变扫描失败", e);
            recordSchedulerAudit("SCHEDULER_SCAN_IP_MUTATIONS", false, e.getMessage());
        } finally {
            MDC.remove("traceId");
        }
    }

    /**
     * v0.4.0: 过期会话清理 — 每小时整点执行（2.1.5）
     * 将创建超过 30 分钟且仍为 ACTIVE 的 user_session 记录标记为 EXPIRED。
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void cleanStaleSessions() {
        String traceId = UUID.randomUUID().toString();
        MDC.put("traceId", traceId);
        log.info("[Scheduler] 开始执行：过期会话清理");

        try {
            Map<String, Object> request = new HashMap<>();
            request.put("ttlMinutes", 30);
            Map<String, Object> result = userServiceClient.cleanStaleSessions(request);

            if (result != null && "0".equals(String.valueOf(result.get("code")))) {
                Map<String, Object> data = (Map<String, Object>) result.get("data");
                log.info("[Scheduler] 过期会话清理完成：过期 {} 个会话", data.get("expiredCount"));
                recordSchedulerAudit("SCHEDULER_CLEAN_STALE_SESSIONS", true,
                        "expiredCount=" + data.get("expiredCount"));
            } else {
                log.warn("[Scheduler] 过期会话清理异常：{}", result);
                recordSchedulerAudit("SCHEDULER_CLEAN_STALE_SESSIONS", false, "unexpected result");
            }
        } catch (Exception e) {
            log.error("[Scheduler] 过期会话清理失败", e);
            recordSchedulerAudit("SCHEDULER_CLEAN_STALE_SESSIONS", false, e.getMessage());
        } finally {
            MDC.remove("traceId");
        }
    }

    /**
     * 记录调度器审计日志
     */
    private void recordSchedulerAudit(String operation, boolean success, String detail) {
        try {
            Map<String, Object> logRequest = new HashMap<>();
            logRequest.put("trace_id", MDC.get("traceId"));
            logRequest.put("user_id", null);  // 系统操作，无用户
            logRequest.put("operation", operation);
            logRequest.put("success", success);
            logRequest.put("target_id", null);
            logRequest.put("detail", detail);
            logServiceClient.recordAudit(logRequest);
        } catch (Exception e) {
            log.error("[Scheduler] 记录审计日志失败: {}", e.getMessage());
        }
    }
}
