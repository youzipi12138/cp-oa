package com.cpoa.dto;

import java.util.List;

/**
 * 前端组织架构树节点。
 */
public record DepartmentNode(
        Long id,
        String name,
        String feishuDepartmentId,
        Integer memberCount,
        List<DepartmentNode> children
) {
}
