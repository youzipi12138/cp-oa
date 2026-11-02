package com.cpoa.scheduler;

import com.cpoa.config.FeishuProperties;
import com.cpoa.service.OrgSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeishuSyncScheduler {

    private final OrgSyncService orgSyncService;
    private final FeishuProperties feishuProperties;

    @Scheduled(cron = "${cp-oa.feishu.sync-cron:0 30 2 * * *}")
    public void sync() {
        if (!feishuProperties.isEnabled()) {
            return;
        }
        if (!feishuProperties.isConfigured()) {
            log.info("未配置飞书应用凭证，跳过定时组织架构同步");
            return;
        }
        try {
            orgSyncService.syncAll();
        } catch (Exception e) {
            log.error("定时同步飞书组织架构失败", e);
        }
    }
}
