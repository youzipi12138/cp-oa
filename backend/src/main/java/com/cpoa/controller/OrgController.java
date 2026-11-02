package com.cpoa.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cpoa.common.Result;
import com.cpoa.dto.DepartmentNode;
import com.cpoa.dto.OrgSyncResult;
import com.cpoa.entity.SysDepartment;
import com.cpoa.entity.SysUser;
import com.cpoa.entity.SysUserDepartment;
import com.cpoa.mapper.SysDepartmentMapper;
import com.cpoa.mapper.SysUserDepartmentMapper;
import com.cpoa.mapper.SysUserMapper;
import com.cpoa.service.OrgSyncService;
import com.cpoa.service.SysUserService;
import com.cpoa.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.SecurityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/org")
@RequiredArgsConstructor
public class OrgController {

    private final SysDepartmentMapper departmentMapper;
    private final SysUserDepartmentMapper userDepartmentMapper;
    private final SysUserMapper userMapper;
    private final SysUserService userService;
    private final OrgSyncService orgSyncService;

    /**
     * 本地部门树（从飞书同步后的数据）。
     */
    @GetMapping("/tree")
    public Result<List<DepartmentNode>> tree() {
        List<SysDepartment> departments = departmentMapper.selectList(
                new LambdaQueryWrapper<SysDepartment>()
                        .orderByAsc(SysDepartment::getOrderNum)
                        .orderByAsc(SysDepartment::getId));

        Map<Long, DepartmentNode> nodes = new LinkedHashMap<>();
        for (SysDepartment department : departments) {
            nodes.put(department.getId(), new DepartmentNode(
                    department.getId(),
                    department.getName(),
                    department.getFeishuDepartmentId(),
                    department.getMemberCount(),
                    new ArrayList<>()));
        }

        List<DepartmentNode> roots = new ArrayList<>();
        for (SysDepartment department : departments) {
            DepartmentNode node = nodes.get(department.getId());
            DepartmentNode parent = department.getParentId() == null
                    ? null
                    : nodes.get(department.getParentId());
            if (parent == null) {
                roots.add(node);
            } else {
                parent.children().add(node);
            }
        }
        return Result.ok(roots);
    }

    @GetMapping("/departments/{id}/users")
    public Result<List<UserVO>> users(@PathVariable Long id) {
        List<SysUserDepartment> relations = userDepartmentMapper.selectList(
                new LambdaQueryWrapper<SysUserDepartment>()
                        .eq(SysUserDepartment::getDepartmentId, id));
        if (relations.isEmpty()) {
            return Result.ok(List.of());
        }

        List<Long> userIds = relations.stream()
                .map(SysUserDepartment::getUserId)
                .distinct()
                .toList();
        List<SysUser> users = userMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                        .in(SysUser::getId, userIds)
                        .orderByAsc(SysUser::getId));
        return Result.ok(users.stream().map(userService::toVO).toList());
    }

    /**
     * 手动触发全量同步，仅管理员可用。
     */
    @PostMapping("/sync")
    public Result<OrgSyncResult> sync() {
        SecurityUtils.getSubject().checkRole("admin");
        return Result.ok(orgSyncService.syncAll());
    }
}
