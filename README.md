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

## 说明

- 后端统一响应为 `{ code, message, data }`，`code === 0` 成功；401 用于未登录/登录失效，403 用于权限不足。
- 登录态为无状态 JWT：后端将 Token 写入 Redis 白名单，退出登录即时失效。
- 用户管理的写操作要求 `admin` 角色。
- `admin` 为内置账号，后端禁止删除。
