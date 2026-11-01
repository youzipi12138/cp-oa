package com.cpoa.security;

import com.cpoa.entity.SysUser;
import com.cpoa.service.SysUserService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.util.StringUtils;

@Slf4j
public class JwtRealm extends AuthorizingRealm {

    private final JwtUtil jwtUtil;
    private final SysUserService userService;

    public JwtRealm(JwtUtil jwtUtil, SysUserService userService, CredentialsMatcher credentialsMatcher) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        setCredentialsMatcher(credentialsMatcher);
    }

    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof JwtToken;
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        SysUser user = (SysUser) principals.getPrimaryPrincipal();
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        if (StringUtils.hasText(user.getRole())) {
            info.addRole(user.getRole());
        }
        if ("admin".equals(user.getRole())) {
            info.addStringPermission("*:*");
        }
        return info;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        String jwt = String.valueOf(token.getCredentials());

        String username;
        try {
            Claims claims = jwtUtil.parseToken(jwt);
            username = claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT 校验失败: {}", e.getMessage());
            throw new IncorrectCredentialsException("Token 无效或已过期");
        }

        SysUser user = userService.getByUsername(username);
        if (user == null) {
            throw new UnknownAccountException("用户不存在");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new LockedAccountException("账号已被禁用");
        }
        return new SimpleAuthenticationInfo(user, jwt, getName());
    }
}
