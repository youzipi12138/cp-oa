package com.cpoa.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@Getter
@Setter
@ConfigurationProperties(prefix = "cp-oa.feishu")
public class FeishuProperties {

    /** 是否启用飞书组织架构同步 */
    private boolean enabled = true;

    private String appId;

    private String appSecret;

    private String baseUrl = "https://open.feishu.cn";

    /** 定时同步 cron，默认每天 02:30 */
    private String syncCron = "0 30 2 * * *";

    /** 事件订阅 Verification Token，用于校验回调来源 */
    private String verificationToken;

    /** 事件订阅 Encrypt Key，配置后校验签名并解密事件内容 */
    private String encryptKey;

    /** 全量同步时是否清理飞书侧已删除的部门和已离职/移除的用户 */
    private boolean cleanupMissing = true;

    /** 事件触发同步的合并窗口（毫秒），窗口内的多次事件只触发一次同步 */
    private long syncDebounceMs = 3000;

    public boolean isConfigured() {
        return StringUtils.hasText(appId) && StringUtils.hasText(appSecret);
    }

    public boolean isWebhookConfigured() {
        return StringUtils.hasText(verificationToken);
    }
}
