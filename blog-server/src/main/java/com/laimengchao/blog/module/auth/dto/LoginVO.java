package com.laimengchao.blog.module.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Set;

@Data
@AllArgsConstructor
public class LoginVO {

    private String accessToken;
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private boolean superAdmin;
    private Set<String> permissions;
}
