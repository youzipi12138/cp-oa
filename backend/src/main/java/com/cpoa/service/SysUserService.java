package com.cpoa.service;

import com.cpoa.common.PageResult;
import com.cpoa.dto.UserCreateRequest;
import com.cpoa.dto.UserUpdateRequest;
import com.cpoa.entity.SysUser;
import com.cpoa.vo.UserVO;

public interface SysUserService {

    SysUser getByUsername(String username);

    PageResult<UserVO> page(long current, long size, String keyword);

    UserVO create(UserCreateRequest request);

    UserVO update(Long id, UserUpdateRequest request);

    void delete(Long id);

    UserVO toVO(SysUser user);
}
