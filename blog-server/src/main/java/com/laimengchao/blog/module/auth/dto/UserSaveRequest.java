package com.laimengchao.blog.module.auth.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserSaveRequest {

    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String password;
    private Integer status;
    private List<Long> roleIds;
}
