package com.cpoa.controller;

import com.cpoa.common.BusinessException;
import com.cpoa.common.RedisKeys;
import com.cpoa.common.Result;
import com.cpoa.dto.LoginRequest;
import com.cpoa.entity.SysUser;
import com.cpoa.security.JwtUtil;
import com.cpoa.service.SysUserService;
import com.cpoa.vo.LoginVO;
import com.cpoa.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.shiro.SecurityUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Valid LoginRequest request) {
        SysUser user = userService.getByUsername(request.getUsername());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }

        String token = jwtUtil.createToken(user);
        redisTemplate.opsForValue().set(
                RedisKeys.authToken(token),
                String.valueOf(user.getId()),
                Duration.ofMinutes(jwtUtil.getExpireMinutes())
        );
        return Result.ok(new LoginVO(token, userService.toVO(user)));
    }

    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.ok(userService.toVO(currentUser()));
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String token = jwtUtil.resolveToken(request);
        if (token != null) {
            redisTemplate.delete(RedisKeys.authToken(token));
        }
        return Result.ok();
    }

    private SysUser currentUser() {
        Object principal = SecurityUtils.getSubject().getPrincipal();
        if (principal instanceof SysUser user) {
            return user;
        }
        throw new BusinessException(401, "未登录或登录已失效");
    }
}
