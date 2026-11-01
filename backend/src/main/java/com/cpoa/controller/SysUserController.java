package com.cpoa.controller;

import com.cpoa.common.PageResult;
import com.cpoa.common.Result;
import com.cpoa.dto.UserCreateRequest;
import com.cpoa.dto.UserUpdateRequest;
import com.cpoa.service.SysUserService;
import com.cpoa.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.SecurityUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService userService;

    @GetMapping
    public Result<PageResult<UserVO>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword) {
        return Result.ok(userService.page(current, size, keyword));
    }

    @PostMapping
    public Result<UserVO> create(@RequestBody @Valid UserCreateRequest request) {
        requireAdmin();
        return Result.ok(userService.create(request));
    }

    @PutMapping("/{id}")
    public Result<UserVO> update(@PathVariable Long id,
                                 @RequestBody @Valid UserUpdateRequest request) {
        requireAdmin();
        return Result.ok(userService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        requireAdmin();
        userService.delete(id);
        return Result.ok();
    }

    /**
     * 基于 Shiro 的运行时角色校验，未通过会抛出 AuthorizationException 并由全局异常处理器转为 403。
     */
    private void requireAdmin() {
        SecurityUtils.getSubject().checkRole("admin");
    }
}
