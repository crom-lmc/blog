package com.laimengchao.blog.module.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_sys_role_permission")
public class SysRolePermission {

    @TableId(type = IdType.INPUT)
    private Long roleId;
    private String permissionCode;
}
