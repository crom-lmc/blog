package com.laimengchao.blog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** 独立博客服务入口：不依赖 Nacos、网关、账号中心，自带登录与 RBAC。 */
@ConfigurationPropertiesScan("com.laimengchao.blog")
@SpringBootApplication(scanBasePackages = "com.laimengchao.blog")
public class BlogAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlogAdminApplication.class, args);
    }
}
