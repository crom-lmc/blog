package com.laimengchao.blog.module.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 博客本地账号，沿用原有 t_user 表，不新增用户主表。 */
@Data
@TableName("t_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String email;
    private String passwordHash;
    /** 兼容旧字段：admin / author */
    private String role;
    private Integer status;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private java.util.List<String> roleCodes;
}
