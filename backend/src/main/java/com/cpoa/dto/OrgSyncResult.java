package com.cpoa.dto;

/**
 * 一次组织架构同步的结果统计。
 */
public record OrgSyncResult(
        int departments,
        int departmentsRemoved,
        int usersCreated,
        int usersUpdated,
        int usersDisabled,
        int relations,
        long durationMs
) {
}
