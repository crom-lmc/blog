package com.laimengchao.blog.module.auth.dto;

import lombok.Data;

/** 当前登录人自助修改密码。 */
@Data
public class ChangePasswordRequest {

    private String oldPassword;
    private String newPassword;
}
