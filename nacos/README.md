# Nacos 接入说明

`blog-server` 的敏感配置（数据库口令、Redis 口令、JWT 密钥、admin 初始密码）统一放 Nacos，
**仓库与镜像内均无明文**。本目录只保存配置模板和说明。

## 1. 环境隔离

两套环境靠 **namespace** 隔离，dataId / group 完全一致，切环境只改 namespace：

| 环境 | namespace | dataId | group |
|---|---|---|---|
| local | `blog-local` | `blog-server.yaml` | `BLOG` |
| prod  | `blog-prod`  | `blog-server.yaml` | `BLOG` |

配置集里只放敏感项（模板见 `blog-server.yaml.example`）；非敏感项继续用 jar 内
`application.yml` 的默认值。优先级：**Nacos > application.yml > 代码默认值**。

## 2. 配置如何生效

`application.yml` 里：

```yaml
spring:
  config:
    import:
      - "optional:nacos:blog-server.yaml?group=${BLOG_NACOS_GROUP:BLOG}&refreshEnabled=true"
  cloud:
    nacos:
      config:
        enabled: ${BLOG_NACOS_ENABLED:false}
        namespace: ${BLOG_NACOS_NAMESPACE:blog-local}
```

Nacos 拉回的 `blog-server.yaml` 里放 `BLOG_DB_PASSWORD: xxx` 这样的 key，
`application.yml` 的 `password: ${BLOG_DB_PASSWORD:}` 占位符会解析到它。

`optional:` 表示 Nacos 不可用时服务仍能用环境变量启动（本地开发很方便）：
把 `BLOG_NACOS_ENABLED` 设为 `false`，直接通过环境变量提供 `BLOG_DB_*` / `BLOG_JWT_SECRET` 等即可。

## 3. 连接 Nacos 的凭据（部署机 .env）

连 Nacos 自身的地址/账号/密码不能放进 Nacos，由部署机环境变量提供：

```bash
BLOG_NACOS_ENABLED=true
BLOG_NACOS_ADDR=127.0.0.1:8848      # 容器内访问宿主机用 172.17.0.1:8848
BLOG_NACOS_USER=nacos
BLOG_NACOS_PASSWORD=<NACOS_PASSWORD>
BLOG_NACOS_NAMESPACE=blog-prod      # 本地开发改 blog-local
BLOG_NACOS_GROUP=BLOG
```

> 服务器现有 `.env` 里已有 `SSO_NACOS_*` 系列，代码做了兼容回退：
> `BLOG_NACOS_*` 未设置时依次回退到 `SSO_NACOS_*`、默认值。因此服务端只补一个
> `BLOG_NACOS_NAMESPACE=blog-prod` 和 `BLOG_NACOS_ENABLED=true` 即可切换。

## 4. 导入配置

在 Nacos 控制台新建 namespace `blog-prod`，dataId `blog-server.yaml`、group `BLOG`，
把模板里的 `<...>` 占位符替换为真实值后粘贴发布。发布后重启服务生效。

> 若需脚本化导入，可复用 Polaris 主仓库的 `scripts/import-config.py`，把 namespace 换成
> `blog-prod`、group 换成 `BLOG` 即可。**切勿把填好真实值的 yaml 提交到本仓库。**
