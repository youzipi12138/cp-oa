package com.cpoa.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cpoa.common.BusinessException;
import com.cpoa.config.FeishuProperties;
import com.cpoa.dto.OrgSyncResult;
import com.cpoa.entity.SysDepartment;
import com.cpoa.entity.SysUser;
import com.cpoa.entity.SysUserDepartment;
import com.cpoa.feishu.FeishuClient;
import com.cpoa.mapper.SysDepartmentMapper;
import com.cpoa.mapper.SysUserDepartmentMapper;
import com.cpoa.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 飞书组织架构 -> 本地部门/用户/关系同步。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrgSyncService {

    private static final String ROOT_FEISHU_ID = "0";

    private final FeishuClient feishuClient;
    private final FeishuProperties feishuProperties;
    private final SysDepartmentMapper departmentMapper;
    private final SysUserMapper userMapper;
    private final SysUserDepartmentMapper userDepartmentMapper;
    private final PasswordEncoder passwordEncoder;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public OrgSyncResult syncAll() {
        if (!feishuProperties.isEnabled()) {
            throw new BusinessException("飞书同步未启用");
        }
        if (!feishuClient.isConfigured()) {
            throw new BusinessException("未配置飞书应用凭证，请设置 FEISHU_APP_ID / FEISHU_APP_SECRET");
        }
        if (!running.compareAndSet(false, true)) {
            throw new BusinessException("同步任务正在执行中，请稍后再试");
        }

        long start = System.currentTimeMillis();
        try {
            List<JsonNode> departmentNodes = feishuClient.listAllDepartments();
            if (departmentNodes.isEmpty()) {
                throw new BusinessException("未拉取到任何部门，请检查应用通讯录权限范围是否为全员");
            }

            Map<String, SysDepartment> departments = upsertDepartments(departmentNodes);
            Map<String, SysUser> syncedUsers = new LinkedHashMap<>();
            Map<String, Set<Long>> userDepartments = new HashMap<>();
            int usersCreated = 0;
            int usersUpdated = 0;

            for (SysDepartment department : departments.values()) {
                if (department.getStatus() == null || department.getStatus() == 0) {
                    continue;
                }

                List<JsonNode> users;
                try {
                    users = feishuClient.listDepartmentUsers(department.getFeishuDepartmentId());
                } catch (BusinessException e) {
                    // 根部门或部分部门不在权限范围内时跳过，不影响其它部门
                    log.warn("拉取部门[{}]成员失败: {}", department.getName(), e.getMessage());
                    continue;
                }

                for (JsonNode node : users) {
                    String openId = text(node, "open_id");
                    if (!StringUtils.hasText(openId)) {
                        continue;
                    }

                    SysUser user = syncedUsers.get(openId);
                    if (user == null) {
                        UserUpsertResult result = upsertUser(node);
                        user = result.user();
                        syncedUsers.put(openId, user);
                        if (result.created()) {
                            usersCreated++;
                        } else {
                            usersUpdated++;
                        }
                    }
                    userDepartments.computeIfAbsent(openId, key -> new LinkedHashSet<>())
                            .add(department.getId());
                }
            }

            int relations = replaceUserDepartments(syncedUsers, userDepartments);
            long duration = System.currentTimeMillis() - start;
            OrgSyncResult result = new OrgSyncResult(
                    departments.size(), usersCreated, usersUpdated, relations, duration);
            log.info("飞书组织架构同步完成: {}", result);
            return result;
        } finally {
            running.set(false);
        }
    }

    private Map<String, SysDepartment> upsertDepartments(List<JsonNode> nodes) {
        Map<String, SysDepartment> departments = new LinkedHashMap<>();

        SysDepartment root = ensureRootDepartment();
        departments.put(ROOT_FEISHU_ID, root);

        for (JsonNode node : nodes) {
            String feishuId = firstNonBlank(
                    text(node, "open_department_id"),
                    text(node, "department_id"));
            if (!StringUtils.hasText(feishuId) || ROOT_FEISHU_ID.equals(feishuId)) {
                continue;
            }

            SysDepartment department = departmentMapper.selectOne(
                    new LambdaQueryWrapper<SysDepartment>()
                            .eq(SysDepartment::getFeishuDepartmentId, feishuId));
            boolean created = department == null;
            if (created) {
                department = new SysDepartment();
                department.setFeishuDepartmentId(feishuId);
                department.setDeleted(0);
            }

            String name = text(node, "name");
            department.setName(StringUtils.hasText(name)
                    ? name
                    : (StringUtils.hasText(department.getName()) ? department.getName() : "未命名部门"));
            department.setParentFeishuId(text(node, "parent_department_id"));
            department.setLeaderOpenId(text(node, "leader_user_id"));
            department.setOrderNum(parseInt(text(node, "order"), 0));
            department.setMemberCount(node.path("member_count").asInt(0));
            department.setStatus(node.path("status").path("is_deleted").asBoolean(false) ? 0 : 1);

            if (created) {
                departmentMapper.insert(department);
            } else {
                departmentMapper.updateById(department);
            }
            departments.put(feishuId, department);
        }

        // 第一轮拿到本地 ID 后，再回填本地父部门 ID
        for (SysDepartment department : departments.values()) {
            SysDepartment parent = departments.get(department.getParentFeishuId());
            Long parentId = parent == null ? null : parent.getId();
            if (!Objects.equals(parentId, department.getParentId())) {
                department.setParentId(parentId);
                departmentMapper.updateById(department);
            }
        }
        return departments;
    }

    private SysDepartment ensureRootDepartment() {
        SysDepartment root = departmentMapper.selectOne(
                new LambdaQueryWrapper<SysDepartment>()
                        .eq(SysDepartment::getFeishuDepartmentId, ROOT_FEISHU_ID));
        if (root == null) {
            root = new SysDepartment();
            root.setFeishuDepartmentId(ROOT_FEISHU_ID);
            root.setParentFeishuId(null);
            root.setName("全员");
            root.setOrderNum(0);
            root.setMemberCount(0);
            root.setStatus(1);
            root.setDeleted(0);
            departmentMapper.insert(root);
        }
        return root;
    }

    private UserUpsertResult upsertUser(JsonNode node) {
        String openId = text(node, "open_id");
        String email = trimToNull(text(node, "email"));
        String mobile = normalizeMobile(text(node, "mobile"));
        String employeeNo = trimToNull(text(node, "employee_no"));

        SysUser user = userMapper.selectByFeishuOpenIdIncludingDeleted(openId);
        boolean created = false;

        if (user != null && Integer.valueOf(1).equals(user.getDeleted())) {
            userMapper.restoreById(user.getId());
            user.setDeleted(0);
        }

        if (user == null) {
            user = findByContact(mobile, email);
            if (user == null) {
                user = new SysUser();
                user.setUsername(nextUsername(node, openId, mobile));
                // 飞书用户使用随机本地密码，只能通过飞书登录（后续接入）
                user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                user.setRole("user");
                user.setDeleted(0);
                created = true;
            }
        }

        user.setFeishuOpenId(openId);
        user.setFeishuUnionId(trimToNull(text(node, "union_id")));
        user.setEmployeeNo(employeeNo);

        // 早期同步生成过 fs_ 前缀的账号，拿到手机号后自动升级为手机号用户名
        if (!created && StringUtils.hasText(mobile)
                && user.getUsername() != null && user.getUsername().startsWith("fs_")) {
            user.setUsername(nextUsername(node, openId, mobile));
        }

        String name = text(node, "name");
        if (StringUtils.hasText(name)) {
            user.setRealName(name);
        } else if (!StringUtils.hasText(user.getRealName())) {
            user.setRealName("飞书用户");
        }
        if (StringUtils.hasText(email)) {
            user.setEmail(email);
        }
        if (StringUtils.hasText(mobile)) {
            user.setPhone(mobile);
        }
        user.setStatus(isActive(node) ? 1 : 0);

        if (created) {
            userMapper.insert(user);
        } else {
            userMapper.updateById(user);
        }
        return new UserUpsertResult(user, created);
    }

    private int replaceUserDepartments(Map<String, SysUser> users, Map<String, Set<Long>> userDepartments) {
        if (users.isEmpty()) {
            return 0;
        }

        List<Long> userIds = users.values().stream().map(SysUser::getId).toList();
        userDepartmentMapper.delete(
                new LambdaQueryWrapper<SysUserDepartment>()
                        .in(SysUserDepartment::getUserId, userIds));

        int relations = 0;
        for (Map.Entry<String, Set<Long>> entry : userDepartments.entrySet()) {
            SysUser user = users.get(entry.getKey());
            if (user == null) {
                continue;
            }
            for (Long departmentId : entry.getValue()) {
                SysUserDepartment relation = new SysUserDepartment();
                relation.setUserId(user.getId());
                relation.setDepartmentId(departmentId);
                relation.setIsPrimary(0);
                userDepartmentMapper.insert(relation);
                relations++;
            }
        }
        return relations;
    }

    private SysUser findByContact(String mobile, String email) {
        if (StringUtils.hasText(mobile)) {
            List<SysUser> users = userMapper.selectList(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhone, mobile).last("LIMIT 1"));
            if (!users.isEmpty()) {
                return users.get(0);
            }
        }
        if (StringUtils.hasText(email)) {
            List<SysUser> users = userMapper.selectList(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, email).last("LIMIT 1"));
            if (!users.isEmpty()) {
                return users.get(0);
            }
        }
        return null;
    }

    private String nextUsername(JsonNode node, String openId, String mobile) {
        String employeeNo = trimToNull(text(node, "employee_no"));
        String base = StringUtils.hasText(employeeNo)
                ? employeeNo
                : StringUtils.hasText(mobile)
                ? mobile
                : "fs_" + openId.replaceAll("[^A-Za-z0-9]", "");
        if (base.length() > 40) {
            base = base.substring(0, 40);
        }

        String candidate = base;
        int suffix = 2;
        while (userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, candidate)) > 0) {
            candidate = base + "_" + suffix++;
        }
        return candidate;
    }

    private boolean isActive(JsonNode node) {
        JsonNode status = node.path("status");
        if (status.isMissingNode() || status.isNull()) {
            return true;
        }
        return !status.path("is_resigned").asBoolean(false)
                && !status.path("is_exited").asBoolean(false)
                && !status.path("is_frozen").asBoolean(false);
    }

    private static String normalizeMobile(String mobile) {
        if (!StringUtils.hasText(mobile)) {
            return null;
        }
        String value = mobile.replaceAll("[\\s-]", "");
        if (value.startsWith("+86")) {
            value = value.substring(3);
        } else if (value.startsWith("86") && value.length() == 13) {
            value = value.substring(2);
        }
        return value.isEmpty() ? null : value;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asString();
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String firstNonBlank(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private static int parseInt(String value, int defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private record UserUpsertResult(SysUser user, boolean created) {
    }

    /** 供测试或其它场景复用 */
    public List<SysDepartment> listDepartments() {
        return new ArrayList<>(departmentMapper.selectList(
                new LambdaQueryWrapper<SysDepartment>()
                        .orderByAsc(SysDepartment::getOrderNum)
                        .orderByAsc(SysDepartment::getId)));
    }
}
