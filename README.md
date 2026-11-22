# Blog

独立博客系统：Spring Boot 3 后端 + Vue 3 前台/管理端，前后端放在同一个仓库。
自带账号体系和按钮级 RBAC，不依赖统一认证中心。敏感配置由 Nacos 按环境（local / prod）下发，仓库内无任何明文口令。

## 在线体验

| 入口 | 地址 |
|---|---|
| 读者端（前台） | https://blog.laimengchao.site/ |
| 管理端 | https://blog.laimengchao.site/admin/ |

> 管理端需要账号登录，账号请向项目维护者索取（仓库公开，故不在 README 中放置口令）。

## 功能特性

### 读者端（前台，根路径 `/`）

- **内容浏览**：首页、文章分页列表、文章详情，支持点赞与浏览量统计
- **分类与标签**：分类列表及分类下文章、标签云及标签下文章
- **归档与搜索**：按时间归档、标题/正文全文搜索
- **独立页面**：可自定义 Page（如关于、友链等），通过 `/page/:slug` 访问
- **评论**：访客留言，可在站点设置中开关评论审核
- **SEO**：自动生成 `sitemap.xml`、站点标题/副标题/关键词/描述、页脚与 ICP 备案号
- **主题呈现**：前台按当前启用的主题动态渲染样式

### 管理端（`/admin/`）

- **仪表盘**：文章/草稿/评论/待审评论/分类/标签/账号/媒体数量统计，文章与评论发布趋势、热门文章、待审评论列表
- **文章管理**：新建/编辑/发布/草稿/置顶、批量删除、Markdown 编辑（md-editor-v3）、文章导入导出、重建搜索索引
- **分类管理**：增删改、排序、文章数重算
- **标签管理**：增删改、标签合并
- **评论管理**：审核通过/拒绝、批量通过/删除、博主回复、删除
- **媒体库**：文件夹管理、图片/文件上传（含大文件分片上传）、编辑与批量删除
- **主题编辑器**：在线可视化配置主题（配色、字体、布局、圆角与间距、首页区块、深色模式、自定义 CSS 等 7 个面板），实时预览、主题复制/激活/发布、内置预设、导入导出
- **站点设置**：站点标题/副标题/Logo/Favicon/页脚/ICP 备案/SEO 关键词与描述/友链/社交链接/评论审核开关/每页条数
- **账号与权限**：账号增删改与重置密码；角色管理，按权限码分配菜单权限与按钮/数据权限；内置 ADMIN、AUTHOR 两种角色
- **操作日志**：记录后台关键操作，支持查询与清理
- **个人功能**：修改密码、管理端深色模式

### 系统能力

- **认证鉴权**：自带账号体系，JWT（HS512）无状态鉴权，密码加密存储；按钮级 RBAC（40+ 权限码），路由与接口双重校验，不依赖统一认证中心
- **配置隔离**：DB / Redis 口令、JWT 密钥、管理员初始密码等敏感项只存 Nacos，按 `blog-local` / `blog-prod` 隔离，仓库内零明文
- **基础设施**：MyBatis-Plus + MySQL、Redis 缓存、接口限流、操作日志切面、springdoc-openapi 接口文档
- **主题机制**：主题以配置 + CSS 形式存储，可导出导入，支持多主题切换与在线发布

## 目录结构

```
blog/
├── blog-server/          # 后端服务（Spring Boot 3.2.5 / Java 17）
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/laimengchao/blog/   # 按 module 组织：article(含分类/标签)/auth(含账号/角色)/comment/media/theme/setting/stat/log
│       └── resources/
│           ├── application.yml          # 非敏感默认配置 + Nacos 接入
│           ├── schema.sql / data.sql    # 建表与种子数据
│           └── mapper/                  # MyBatis-Plus XML
├── blog-web/             # 前端（npm workspaces，Vue 3 + Vite + TypeScript）
│   ├── packages/web/     # 读者端（前台）
│   └── packages/admin/   # 管理端（Ant Design Vue，部署在 /admin/）
└── nacos/                # Nacos 配置模板与说明（无真实值）
    ├── blog-server.yaml.example
    └── README.md
```

## 技术栈

**后端**：Java 17、Spring Boot 3.2.5、Spring Cloud Alibaba（Nacos Config）、MyBatis-Plus 3.5.7、
MySQL 5.7、Redis、Spring Security + JJWT（HS512）、springdoc-openapi。

**前端**：Vue 3、Vite 5、TypeScript、Pinia、Vue Router、Axios；管理端用 Ant Design Vue 4、ECharts、md-editor-v3。

## 本地开发

### 后端

需本机 MySQL（库 `blob`）和 Redis。`application.yml` 里的敏感项没有默认值，本地两种提供方式任选其一：

- **不用 Nacos**：保持 `BLOG_NACOS_ENABLED=false`，通过环境变量提供
  `BLOG_DB_URL` / `BLOG_DB_USER` / `BLOG_DB_PASSWORD` / `BLOG_REDIS_*` / `BLOG_JWT_SECRET`（≥32 字节）
  / `BLOG_ADMIN_DEFAULT_PASSWORD`，再启动：
  ```bash
  cd blog-server
  mvn spring-boot:run
  ```
  默认端口 `9003`。
- **用 Nacos**：设 `BLOG_NACOS_ENABLED=true`、`BLOG_NACOS_NAMESPACE=blog-local`，
  在 Nacos 的 `blog-local` namespace（group `BLOG`，dataId `blog-server.yaml`）配置敏感项，
  连接凭据见 `nacos/README.md`。

首次启动时 `AuthInitializer` 会幂等播种权限码、ADMIN/AUTHOR 角色和默认管理员 `admin`
（初始密码取 `BLOG_ADMIN_DEFAULT_PASSWORD`）；已存在的 admin 不会被覆盖。

### 前端

```bash
cd blog-web
npm install
npm run dev:web     # 读者端，默认 http://localhost:5175，/api 与 /uploads 代理到 127.0.0.1:9003
npm run dev:admin   # 管理端，默认 http://localhost:5174，base 为 /admin/
```

复制 `packages/web/.env.example`、`packages/admin/.env.example` 为 `.env.local` 可按需要改端口、后端地址。
`VITE_USE_MOCK=true` 时可启用内置 Mock，无需后端。

## 构建

```bash
cd blog-server && mvn -DskipTests package      # 产物 target/blog-server.jar
cd blog-web && npm run build                    # 分别产出 packages/web/dist、packages/admin/dist
```

## 配置与环境隔离

敏感配置（DB / Redis 口令、JWT 密钥、admin 初始密码、站点地址）只存 Nacos，按 namespace 隔离：

| 环境 | namespace | dataId | group |
|---|---|---|---|
| 本地 | `blog-local` | `blog-server.yaml` | `BLOG` |
| 生产 | `blog-prod`  | `blog-server.yaml` | `BLOG` |

Nacos 里用 `BLOG_DB_PASSWORD` 等环境变量名做 key，`application.yml` 通过 `${BLOG_XXX:}` 占位符读取；
连接 Nacos 自身的地址/账号/密码由部署环境变量提供。详见 `nacos/README.md` 和 `nacos/blog-server.yaml.example`。

> 部署机环境变量优先级高于 Nacos：若同名环境变量存在会覆盖 Nacos 的值，因此迁移后应从部署环境中删除已放入 Nacos 的项。

## 部署形态

线上前台与管理端共用同一域名：根路径 `/` 为前台，`/admin/` 为管理端（hash 路由 + Vite base），
`/api`、`/uploads` 反向代理到后端服务。后端监听 `9003`，前面挂 Nginx。
