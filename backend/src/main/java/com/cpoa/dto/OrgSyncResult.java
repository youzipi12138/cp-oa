package com.cpoa.dto;

/**
 * 一次组织架构同步的结果统计。
 */
public record OrgSyncResult(
        int departments,
        int usersCreated,
        int usersUpdated,
        int relations,
        long durationMs
) {
}
