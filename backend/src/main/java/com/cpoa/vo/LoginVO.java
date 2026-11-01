package com.cpoa.vo;

/**
 * 登录成功响应。
 */
public record LoginVO(String token, UserVO user) {
}
