package com.cpoa.service;

import com.cpoa.common.BusinessException;
import com.cpoa.config.FeishuProperties;
import com.cpoa.dto.OrgSyncResult;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 飞书事件触发的同步调度器。
 *
 * <p>飞书可能在一小段时间内推送多条事件，这里用去抖窗口把它们合并成一次全量同步；
 * 同步正在执行时收到的事件会在当前同步结束后再补一次，保证最终一致。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeishuSyncTrigger {

    private final OrgSyncService orgSyncService;
    private final FeishuProperties feishuProperties;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "feishu-sync-trigger");
        thread.setDaemon(true);
        return thread;
    });

    private final AtomicBoolean scheduled = new AtomicBoolean(false);

    /**
     * 安排一次去抖后的全量同步。
     *
     * @return true 表示本次调用安排了新的同步；false 表示已有待执行的同步或飞书同步未启用
     */
    public boolean trigger(String reason) {
        if (!feishuProperties.isEnabled() || !feishuProperties.isConfigured()) {
            log.debug("飞书同步未启用或未配置，忽略事件: {}", reason);
            return false;
        }
        if (!scheduled.compareAndSet(false, true)) {
            log.debug("已有待执行的飞书同步，事件被合并: {}", reason);
            return false;
        }

        long delay = Math.max(0, feishuProperties.getSyncDebounceMs());
        executor.schedule(() -> runSync(reason), delay, TimeUnit.MILLISECONDS);
        return true;
    }

    private void runSync(String reason) {
        scheduled.set(false);
        try {
            OrgSyncResult result = orgSyncService.syncAll();
            log.info("飞书事件触发同步完成({}): {}", reason, result);
        } catch (BusinessException e) {
            if (orgSyncService.isRunning()) {
                log.info("已有同步任务在执行，稍后重试({})", reason);
                trigger(reason + " 重试");
            } else {
                log.error("飞书事件触发同步失败({}): {}", reason, e.getMessage(), e);
            }
        } catch (Exception e) {
            log.error("飞书事件触发同步失败({})", reason, e);
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}
