package com.cpoa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cpoa.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /** 包含逻辑删除数据，避免已删除的飞书账号再次同步时触发唯一键冲突 */
    @Select("SELECT * FROM sys_user WHERE feishu_open_id = #{openId} LIMIT 1")
    SysUser selectByFeishuOpenIdIncludingDeleted(@Param("openId") String openId);

    @Update("UPDATE sys_user SET deleted = 0 WHERE id = #{id}")
    int restoreById(@Param("id") Long id);
}
