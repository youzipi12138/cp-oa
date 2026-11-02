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

    public boolean isConfigured() {
        return StringUtils.hasText(appId) && StringUtils.hasText(appSecret);
    }
}
