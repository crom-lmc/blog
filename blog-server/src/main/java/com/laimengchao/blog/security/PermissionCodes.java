package com.laimengchao.blog.security;

/**
 * 博客自己的权限码常量。只服务本应用，不再依赖统一账号中心。
 *
 * <p>菜单码用于前端路由 meta.perm 与左侧菜单显隐；按钮码用于 {@link Permit}
 * 和前端 store.can(code)。新增码时按「后端常量 → 权限种子 → 前端引用」三处同步。
 */
public final class PermissionCodes {

    private PermissionCodes() {
    }

    /* 菜单 */
    public static final String MENU_DASHBOARD = "dashboard";
    public static final String MENU_ARTICLE = "article";
    public static final String MENU_CATEGORY = "category";
    public static final String MENU_TAG = "tag";
    public static final String MENU_COMMENT = "comment";
    public static final String MENU_MEDIA = "media";
    public static final String MENU_THEME = "theme";
    public static final String MENU_SETTING = "setting";
    public static final String MENU_LOG = "log";
    public static final String MENU_USER = "user";
    public static final String MENU_ROLE = "role";

    /* 文章 */
    public static final String ARTICLE_LIST = "article:list";
    public static final String ARTICLE_CREATE = "article:create";
    public static final String ARTICLE_EDIT = "article:edit";
    public static final String ARTICLE_PUBLISH = "article:publish";
    public static final String ARTICLE_DELETE = "article:delete";

    /* 分类 / 标签 */
    public static final String CATEGORY_LIST = "category:list";
    public static final String CATEGORY_EDIT = "category:edit";
    public static final String CATEGORY_DELETE = "category:delete";
    public static final String TAG_LIST = "tag:list";
    public static final String TAG_EDIT = "tag:edit";
    public static final String TAG_DELETE = "tag:delete";

    /* 评论 */
    public static final String COMMENT_LIST = "comment:list";
    public static final String COMMENT_AUDIT = "comment:audit";
    public static final String COMMENT_DELETE = "comment:delete";
    public static final String COMMENT_REPLY = "comment:reply";

    /* 媒体库 */
    public static final String MEDIA_LIST = "media:list";
    public static final String MEDIA_EDIT = "media:edit";
    public static final String MEDIA_DELETE = "media:delete";
    public static final String MEDIA_UPLOAD = "media:upload";

    /* 主题 / 设置 / 日志 / 统计 */
    public static final String THEME_LIST = "theme:list";
    public static final String THEME_EDIT = "theme:edit";
    public static final String THEME_DELETE = "theme:delete";
    public static final String THEME_PUBLISH = "theme:publish";
    public static final String SETTING_VIEW = "setting:view";
    public static final String SETTING_EDIT = "setting:edit";
    public static final String LOG_LIST = "log:list";
    public static final String LOG_CLEAN = "log:clean";
    public static final String STAT_VIEW = "stat:view";

    /* 自有账号与角色 */
    public static final String USER_LIST = "user:list";
    public static final String USER_CREATE = "user:create";
    public static final String USER_EDIT = "user:edit";
    public static final String USER_DELETE = "user:delete";
    public static final String USER_RESET_PWD = "user:resetPassword";
    public static final String ROLE_LIST = "role:list";
    public static final String ROLE_CREATE = "role:create";
    public static final String ROLE_EDIT = "role:edit";
    public static final String ROLE_DELETE = "role:delete";

    /** 全部权限码，供启动播种与「超级管理员」授权使用 */
    public static final String[] ALL = {
            MENU_DASHBOARD, MENU_ARTICLE, MENU_CATEGORY, MENU_TAG, MENU_COMMENT, MENU_MEDIA,
            MENU_THEME, MENU_SETTING, MENU_LOG, MENU_USER, MENU_ROLE,
            ARTICLE_LIST, ARTICLE_CREATE, ARTICLE_EDIT, ARTICLE_PUBLISH, ARTICLE_DELETE,
            CATEGORY_LIST, CATEGORY_EDIT, CATEGORY_DELETE,
            TAG_LIST, TAG_EDIT, TAG_DELETE,
            COMMENT_LIST, COMMENT_AUDIT, COMMENT_DELETE, COMMENT_REPLY,
            MEDIA_LIST, MEDIA_EDIT, MEDIA_DELETE, MEDIA_UPLOAD,
            THEME_LIST, THEME_EDIT, THEME_DELETE, THEME_PUBLISH,
            SETTING_VIEW, SETTING_EDIT, LOG_LIST, LOG_CLEAN, STAT_VIEW,
            USER_LIST, USER_CREATE, USER_EDIT, USER_DELETE, USER_RESET_PWD,
            ROLE_LIST, ROLE_CREATE, ROLE_EDIT, ROLE_DELETE
    };
}
