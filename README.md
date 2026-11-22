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

## 技术栈与环境要求

**后端**：Java 17、Spring Boot 3.2.5、Spring Cloud Alibaba（Nacos Config）、MyBatis-Plus 3.5.7、
MySQL 5.7、Redis、Spring Security + JJWT（HS512）、springdoc-openapi。

**前端**：Vue 3、Vite 5、TypeScript、Pinia、Vue Router、Axios；管理端用 Ant Design Vue 4、ECharts、md-editor-v3。

部署前需准备：

| 依赖 | 版本要求 | 说明 |
|---|---|---|
| JDK | 17 | 编译与运行后端 |
| Maven | 3.9+ | compiler 插件 3.13 需要 3.9（3.6 会构建失败） |
| MySQL | 5.7+ | 博客数据库 |
| Redis | 任意稳定版 | 缓存、限流 |
| Nacos | 2.x | 可选；生产推荐，本地可不开 |
| Node.js + npm | Node 18+ | 构建前端 |

---

## 部署指南：从 clone 到启动

下面是一台新机器上线的完整步骤。示例约定：后端端口 `9003`，数据库名 `blob`，
前台挂域名根路径 `/`，管理端挂 `/admin/`，二者共用一个域名、前面放 Nginx。

### 第 1 步：拉取代码

```bash
git clone https://github.com/crom-lmc/blog.git
cd blog
```

### 第 2 步：初始化数据库

新建数据库 `blob`（字符集 `utf8mb4`），然后按顺序执行仓库里的两个 SQL 脚本：

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS blob DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p blob < blog-server/src/main/resources/schema.sql
mysql -u root -p blob < blog-server/src/main/resources/data.sql
```

- `schema.sql` 用 `CREATE TABLE IF NOT EXISTS`，`data.sql` 用 `INSERT IGNORE`，可安全重复执行
- `application.yml` 中 `spring.sql.init.mode` 默认为 `never`（不自动建表），所以**新库必须手工执行上面两步**
- `data.sql` 会写入权限码、ADMIN/AUTHOR 角色等基础数据

### 第 3 步：准备配置（关键）

后端敏感项在 `application.yml` 中**都没有默认值**，缺失会直接启动失败。有两种提供方式，任选其一：

**方式 A：环境变量（最简单，本地/小规模部署推荐）**

直接 export 或通过 systemd / docker 注入下列变量：

```bash
export BLOG_DB_URL='jdbc:mysql://127.0.0.1:3306/blob?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true'
export BLOG_DB_USER='your_db_user'
export BLOG_DB_PASSWORD='your_db_password'
export BLOG_REDIS_HOST='127.0.0.1'
export BLOG_REDIS_PORT=6379
export BLOG_REDIS_PASSWORD=''          # 无密码留空
export BLOG_REDIS_DB=7
export BLOG_JWT_SECRET='replace-with-at-least-32-bytes-random-string'
export BLOG_ADMIN_DEFAULT_PASSWORD='your_admin_initial_password'
export BLOG_SITE_BASE_URL='https://your-domain.com'
# 保持 Nacos 关闭（默认即 false），无需 BLOG_NACOS_* 变量
```

**方式 B：Nacos（生产推荐，敏感值不落在部署机环境变量里）**

1. 在 Nacos 新建 namespace `blog-prod`，dataId `blog-server.yaml`，group `BLOG`
2. 复制 `nacos/blog-server.yaml.example`，把 `<...>` 占位符替换为真实值后发布（**切勿把填好真实值的 yaml 提交到本仓库**）
3. 部署机只提供连接 Nacos 自身的变量：

```bash
export BLOG_NACOS_ENABLED=true
export BLOG_NACOS_ADDR='127.0.0.1:8848'   # 容器访问宿主机 Nacos 用 172.17.0.1:8848
export BLOG_NACOS_USER='nacos'
export BLOG_NACOS_PASSWORD='your_nacos_password'
export BLOG_NACOS_NAMESPACE='blog-prod'
export BLOG_NACOS_GROUP=BLOG
```

**必须修改 / 确认的配置项速查**：

| 配置项 | 是否必须 | 说明 |
|---|---|---|
| `BLOG_DB_URL` / `BLOG_DB_USER` / `BLOG_DB_PASSWORD` | 必须 | 数据库地址、账号、口令；URL 中库名为 `blob` |
| `BLOG_REDIS_HOST` / `PORT` / `PASSWORD` / `DB` | 必须 | Redis 连接；无密码时 `PASSWORD` 留空 |
| `BLOG_JWT_SECRET` | 必须 | JWT 签名密钥，≥32 字节；缺失启动失败 |
| `BLOG_ADMIN_DEFAULT_PASSWORD` | 必须* | 首次启动播种 admin 的初始密码；已存在的 admin 不会被覆盖，缺失时新建 admin 会报错 |
| `BLOG_SITE_BASE_URL` | 建议 | 站点对外地址，用于 sitemap；默认 `http://localhost:9003` |
| `BLOG_NACOS_*` | 用 Nacos 时必须 | Nacos 连接与 namespace，详见 `nacos/README.md` |
| `BLOG_SERVER_PORT` | 可选 | 后端端口，默认 `9003` |
| `BLOG_UPLOAD_ROOT` | 可选 | 上传文件落盘目录，默认 `./uploads`，需保证运行用户可写 |

> **优先级提醒**：部署机环境变量 > Nacos。若用了 Nacos，务必从环境变量里删掉已放进 Nacos 的项，否则 Nacos 配置会被覆盖（等于没配）。

### 第 4 步：构建并启动后端

```bash
cd blog-server
mvn -DskipTests package                 # 产物：target/blog-server.jar（约 1 个可执行 fat jar）
java -jar target/blog-server.jar        # 前台/管理端/API 监听 9003
```

生产建议用 systemd 托管（把第 3 步的变量写进 `/etc/default/blog-server`）：

```ini
# /etc/systemd/system/blog-server.service
[Unit]
Description=Blog Server
After=network.target

[Service]
User=www-data
EnvironmentFile=/etc/default/blog-server
WorkingDirectory=/opt/blog
ExecStart=/usr/bin/java -jar /opt/blog/blog-server.jar
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload && sudo systemctl enable --now blog-server
sudo journalctl -u blog-server -f       # 观察启动日志
```

启动成功的标志：日志出现 `Started BlogAdminApplication`，无 JWT/DB 相关异常。
首次启动 `AuthInitializer` 会幂等播种权限码、角色和默认管理员 `admin`（密码取 `BLOG_ADMIN_DEFAULT_PASSWORD`）。

排障时可临时打开接口文档：`SPRINGDOC_ENABLED=true` 后访问 `http://<host>:9003/swagger-ui.html`（生产默认关闭）。

### 第 5 步：构建并部署前端

```bash
cd blog-web
npm install                             # npm workspaces，一次装好 web + admin
npm run build                           # 产出 packages/web/dist 与 packages/admin/dist
```

> 生产构建**不需要**设置任何 `VITE_*` 环境变量：API 走相对路径 `/api/blog`，
> 管理端预览地址在构建产物里自动取当前域名（`window.location.origin`），不会跳到 localhost。

把两个 `dist` 放到 Nginx 静态目录，例如：

```
/var/www/blog/
├── index.html          # ← packages/web/dist/*  （前台，根路径）
├── assets/
├── admin/
│   ├── index.html      # ← packages/admin/dist/*（管理端，/admin/）
│   └── assets/
```

### 第 6 步：配置 Nginx

前台与管理端同域，根路径给前台、`/admin/` 给管理端，`/api` 与 `/uploads` 反代到后端 9003：

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 管理端：/admin/ 下用 try_files 命中 hash 路由的 index.html
    location /admin/ {
        alias /var/www/blog/admin/;
        try_files $uri $uri/ /admin/index.html;
    }

    # 后台接口 + 上传文件：反代到 Spring Boot
    location /api/     { proxy_pass http://127.0.0.1:9003; proxy_set_header Host $host;
                         proxy_set_header X-Real-IP $remote_addr; proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for; }
    location /uploads/ { proxy_pass http://127.0.0.1:9003; client_max_body_size 20m; }

    # 前台：history 路由，兜底到 index.html
    location / {
        root /var/www/blog;
        try_files $uri $uri/ /index.html;
    }
}
```

上传目录 `./uploads` 由后端自身以静态资源方式提供（见 `WebMvcConfig`），所以 Nginx 只需 `/uploads/` 反代即可；
但运行后端的用户必须对该目录有写权限。改完 `nginx -t && systemctl reload nginx`。

### 第 7 步：验证

```bash
curl -I http://127.0.0.1:9003/api/blog/public/bootstrap   # 后端接口有响应
curl -I https://your-domain.com/                          # 前台 200
curl -I https://your-domain.com/admin/                    # 管理端 200
```

浏览器打开管理端 `/admin/`，用 `admin` + `BLOG_ADMIN_DEFAULT_PASSWORD` 登录，
进「站点设置」改站点标题，再发一篇文章确认前台可见——全流程通即可。

---

## 本地开发（不开 Nacos）

本地用环境变量方式启动后端（见第 3 步方式 A），然后：

```bash
# 后端
cd blog-server && mvn spring-boot:run      # 或 java -jar target/blog-server.jar，端口 9003

# 前端（另开终端）
cd blog-web
npm install
npm run dev:web     # 读者端，默认 http://localhost:5175，/api 与 /uploads 代理到 127.0.0.1:9003
npm run dev:admin   # 管理端，默认 http://localhost:5174，base 为 /admin/
```

复制 `packages/web/.env.example`、`packages/admin/.env.example` 为 `.env.local` 可改端口与后端地址。
`VITE_USE_MOCK=true` 时启用内置 Mock，无需后端。

## 配置与环境隔离（Nacos 细节）

| 环境 | namespace | dataId | group |
|---|---|---|---|
| 本地 | `blog-local` | `blog-server.yaml` | `BLOG` |
| 生产 | `blog-prod`  | `blog-server.yaml` | `BLOG` |

Nacos 里用 `BLOG_DB_PASSWORD` 等环境变量名做 key，`application.yml` 通过 `${BLOG_XXX:}` 占位符读取；
连接 Nacos 自身的地址/账号/密码由部署环境变量提供。详见 `nacos/README.md` 和 `nacos/blog-server.yaml.example`。

`optional:nacos:` 表示 Nacos 不可用时服务仍可用环境变量启动（本地开发很方便）。
切换环境只需改 `BLOG_NACOS_NAMESPACE`，dataId / group 完全一致。
