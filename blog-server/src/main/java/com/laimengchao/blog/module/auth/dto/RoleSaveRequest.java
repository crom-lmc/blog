package com.laimengchao.blog.module.auth.dto;

import lombok.Data;

import java.util.List;

@Data
public class RoleSaveRequest {

    private Long id;
    private String code;
    private String name;
    private String description;
    private Integer sortNo;
    private List<String> permissions;
}
