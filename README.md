# CP OA

一个前后端分离的办公自动化（OA）项目骨架。

```
cp-oa/
├── frontend/   React 19 + Vite 7 + TypeScript + Ant Design 5 + Pro Components
└── backend/    Spring Boot 4 + Shiro 3 + MyBatis-Plus 3.5 + MySQL + Redis
```

## 技术栈

### 前端

- React 19、Vite 7、TypeScript 5.9
- Ant Design 5 + `@ant-design/pro-components`（ProLayout、ProTable、LoginForm）
- React Router 7、Axios
- 已实现：登录、ProLayout 主框架、工作台、组织架构（部门树/成员/手动同步）、
  用户管理（分页/搜索/新建/编辑/删除）

### 后端

- JDK 21、Spring Boot 4.1、Maven
- Apache Shiro 3.0（Jakarta Servlet，适配 Spring Boot 3/4）
- MyBatis-Plus 3.5.18（分页、逻辑删除、字段自动填充）
- MySQL 8、Redis 6+
- JWT（jjwt）+ Redis Token 白名单，BCrypt 密码加密
- 飞书通讯录同步：全量部门/成员 → 本地 `sys_department`、`sys_user`、
  `sys_user_department`，支持定时、手动与事件订阅实时同步

## 快速开始

环境要求：JDK 21、Maven 3.9+、Node 20.19+（或 22.12+）、pnpm 10+、MySQL 8+、Redis 6+。

### 1. 初始化数据库

```bash
mysql -uroot -p < backend/sql/schema.sql
```

已有数据库升级组织架构相关表：

```bash
mysql -uroot -p < backend/sql/upgrade_org.sql
```

### 2. 启动后端

```bash
cd backend
FEISHU_APP_SECRET=你的飞书AppSecret mvn spring-boot:run
```

后端地址 <http://localhost:8080>。数据库、Redis、飞书应用的连接信息均通过环境变量覆盖，见
[`backend/README.md`](backend/README.md#配置项)。

飞书组织架构同步需要先在飞书开放平台开通应用身份通讯录权限，并**创建版本发布**后生效。
如需通讯录变更实时同步，还需配置事件订阅（请求地址 `/api/org/feishu/events`、
`FEISHU_VERIFICATION_TOKEN` 等），详见
[`backend/README.md`](backend/README.md#事件订阅实时同步)。

### 3. 启动前端

```bash
cd frontend
pnpm install
pnpm dev
```

访问 <http://localhost:5173>，开发服务器会把 `/api` 代理到 `http://localhost:8080`。

### 默认账号

| 用户名 | 密码 | 角色 |
| --- | --- | --- |
| `admin` | `123456` | 管理员 |
| `zhangsan` | `123456` | 普通用户 |

## 端口约定

| 服务 | 端口 |
| --- | --- |
| 前端（Vite） | 5173 |
| 后端（Spring Boot） | 8080 |
| MySQL | 3306 |
| Redis | 6379 |

## CI/CD 自动部署

推送到 `main` 或手动触发 `CI/CD` workflow 后，GitHub Actions 会：

1. 构建后端：`mvn -DskipTests package` → `cp-oa-backend-0.1.0.jar`；
2. 构建前端：`pnpm install --frozen-lockfile && pnpm build`（`VITE_BASE_PATH=/cp-oa/`）；
3. 通过 SSH 把产物上传到 `ubuntu@49.233.89.175`，执行 `deploy/deploy.sh`：
   - 备份旧 jar → 覆盖挂载文件 → `docker restart cp-oa-backend` → 轮询
     `http://127.0.0.1:8081/api/health`，失败自动回滚上一版；
   - 前端 dist 先写入 `/var/www/cp-oa.new`，校验通过后原子切换到
     `/var/www/cp-oa`，旧目录保留为 `/var/www/cp-oa.prev`。

workflow 定义见 [`.github/workflows/ci-cd.yml`](.github/workflows/ci-cd.yml)，
服务器端脚本见 [`deploy/deploy.sh`](deploy/deploy.sh)。

### GitHub Secrets

在仓库 **Settings → Secrets and variables → Actions** 中配置：

| Secret | 说明 |
| --- | --- |
| `DEPLOY_HOST` | `49.233.89.175` |
| `DEPLOY_USER` | `ubuntu` |
| `DEPLOY_SSH_KEY` | 部署专用 ed25519 私钥（公钥已加入服务器 `~/.ssh/authorized_keys`，注释 `cp-oa-github-actions`） |
| `DEPLOY_PORT` | 可选，默认 `22` |

### 服务器目录与端口

| 路径 / 端口 | 用途 |
| --- | --- |
| `/opt/cp-oa/incoming/` | Actions 上传的待部署产物 |
| `/opt/cp-oa/backups/` | 后端 jar 备份，保留最近 5 份 |
| `/opt/cp-oa/backend/target/cp-oa-backend-0.1.0.jar` | 容器 bind mount 的 jar |
| `/opt/cp-oa/RELEASE` | 最近一次部署的 commit 与时间 |
| `/var/www/cp-oa` | 前端静态文件（nginx `root /var/www`） |
| `127.0.0.1:8081` | 后端容器 `cp-oa-backend`（复用 `juejin_default` 网络里的 MySQL/Redis） |
| `127.0.0.1:8082` | nginx：`/cp-oa/` 静态资源 + `/cp-oa/api/` 反代 8081 |

> 前端目前通过 cloudflared 快速隧道（`cp-oa-tunnel`）对外暴露，隧道 URL 重启会变化，
> 不属于 CI/CD 管理范围；若换正式域名，只需改 cloudflared/nginx，无需动流水线。

回滚方式：`/opt/cp-oa/backups/` 里选一份 jar，用 `cat` 覆盖回
`backend/target/cp-oa-backend-0.1.0.jar` 后 `docker restart cp-oa-backend`
（必须保持 inode 不变，不能 `mv`），前端则把 `/var/www/cp-oa.prev` 换回。

## 说明

- 后端统一响应为 `{ code, message, data }`，`code === 0` 成功；401 用于未登录/登录失效，403 用于权限不足。
- 登录态为无状态 JWT：后端将 Token 写入 Redis 白名单，退出登录即时失效。
- 用户管理的写操作要求 `admin` 角色。
- `admin` 为内置账号，后端禁止删除。
