package com.laimengchao.blog.module.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_sys_role")
public class SysRole {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private String description;
    private Integer sortNo;
    private LocalDateTime createdAt;

    /** 该角色已授予的权限码，列表用 */
    @TableField(exist = false)
    private java.util.List<String> permissions;
}
