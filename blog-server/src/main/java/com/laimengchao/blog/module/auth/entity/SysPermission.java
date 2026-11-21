package com.laimengchao.blog.module.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_sys_permission")
public class SysPermission {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    /** MENU / BUTTON */
    private String type;
    private Integer sortNo;
}
