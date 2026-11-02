package com.cpoa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cpoa.entity.SysDepartment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SysDepartmentMapper extends BaseMapper<SysDepartment> {

    /** 包含逻辑删除数据，避免飞书部门被删除后重新出现时触发唯一键冲突 */
    @Select("SELECT * FROM sys_department WHERE feishu_department_id = #{feishuId} LIMIT 1")
    SysDepartment selectByFeishuIdIncludingDeleted(@Param("feishuId") String feishuId);

    @Update("UPDATE sys_department SET deleted = 0, status = 1 WHERE id = #{id}")
    int restoreById(@Param("id") Long id);
}
