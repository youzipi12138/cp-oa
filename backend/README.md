# CP OA Backend

Spring Boot 4 + Apache Shiro 3 + MyBatis-Plus + MySQL + Redis。

## 技术版本

| 组件 | 版本 |
| --- | --- |
| JDK | 21 |
| Spring Boot | 4.1.1 |
| Apache Shiro | 3.0.1（Jakarta Servlet） |
| MyBatis-Plus | 3.5.18（`spring-boot4` starter） |
| JWT | jjwt 0.13.0 |
| MySQL | 8+ |
| Redis | 6+ |

> 说明：Shiro 2.x 仍基于 `javax.servlet`，无法在 Spring Boot 3/4 上使用；
> 3.x 才是 Jakarta 版本，因此这里选用 Shiro 3.0.1。

## 快速开始

1. 初始化数据库（会创建 `cp_oa` 库并写入演示账号）：

   ```bash
   mysql -uroot -p < sql/schema.sql
   ```

   已有的 `cp_oa` 库升级组织架构表时执行一次：

   ```bash
   mysql -uroot -p < sql/upgrade_org.sql
   ```

2. 启动 Redis 与 MySQL（默认 `localhost:6379`、`localhost:3306`）。

3. 启动应用：

   ```bash
   mvn spring-boot:run
   ```

   默认地址：<http://localhost:8080>，健康检查：`GET /api/health`。

## 配置项

均支持环境变量覆盖，见 `src/main/resources/application.yml`：

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `DB_HOST` / `DB_PORT` | `localhost` / `3306` | MySQL 地址 |
| `DB_NAME` | `cp_oa` | 数据库名 |
| `DB_USERNAME` / `DB_PASSWORD` | `root` / `root` | 数据库账号 |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | Redis 地址 |
| `REDIS_USERNAME` | 空 | Redis ACL 用户名（如 `default`） |
| `REDIS_PASSWORD` | 空 | Redis 密码 |
| `REDIS_DATABASE` | `0` | Redis 库 |
| `JWT_SECRET` | 内置开发密钥 | 至少 32 字节，生产必须覆盖 |
| `JWT_EXPIRE_MINUTES` | `720` | Token 有效期（分钟） |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://127.0.0.1:5173` | 允许的前端来源，逗号分隔 |
| `FEISHU_ENABLED` | `true` | 是否启用飞书组织架构同步 |
| `FEISHU_APP_ID` | `cli_aa6da0336df8dc06` | 飞书应用 App ID |
| `FEISHU_APP_SECRET` | 空 | 飞书应用 App Secret，必须通过环境变量注入 |
| `FEISHU_BASE_URL` | `https://open.feishu.cn` | 飞书开放平台地址 |
| `FEISHU_SYNC_CRON` | `0 30 2 * * *` | 组织架构定时同步 cron |
| `FEISHU_VERIFICATION_TOKEN` | 空 | 事件订阅 Verification Token，配置后才接收事件回调 |
| `FEISHU_ENCRYPT_KEY` | 空 | 事件订阅 Encrypt Key，配置后启用签名校验与报文解密 |
| `FEISHU_CLEANUP_MISSING` | `true` | 同步时清理已删除部门、禁用已离职/移除的用户 |
| `FEISHU_SYNC_DEBOUNCE_MS` | `3000` | 事件触发的去抖窗口（毫秒） |

## 认证与授权流程

1. `POST /api/auth/login`：BCrypt 校验密码 → 签发 JWT → 写入 Redis 白名单
   `cp-oa:auth:token:{jwt}`（TTL 与 Token 一致）。
2. 后续请求携带 `Authorization: Bearer <token>`，`JwtFilter` 交给 Shiro
   `JwtRealm` 验证签名与用户状态，`JwtCredentialsMatcher` 校验 Redis 白名单。
3. Shiro 关闭 Session 存储，完全无状态；退出登录删除 Redis Key 即刻失效。
4. 用户增删改需要 `admin` 角色（`SecurityUtils.getSubject().checkRole("admin")`）。

> 默认关闭了 Shiro 注解鉴权（`shiro.annotations.enabled: false`）以避免启动时
> 大量 BeanPostProcessor 警告；如需使用 `@RequiresRoles`、`@RequiresPermissions`，
> 将该配置改为 `true` 即可。

## 接口

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| GET | `/api/health` | 健康检查 | 匿名 |
| POST | `/api/auth/login` | 登录 | 匿名 |
| GET | `/api/auth/me` | 当前用户 | 登录 |
| POST | `/api/auth/logout` | 退出登录 | 登录 |
| GET | `/api/users` | 分页查询（`current`、`size`、`keyword`） | 登录 |
| POST | `/api/users` | 新建用户 | admin |
| PUT | `/api/users/{id}` | 更新用户 | admin |
| DELETE | `/api/users/{id}` | 删除用户（逻辑删除） | admin |
| GET | `/api/org/tree` | 本地部门树 | 登录 |
| GET | `/api/org/departments/{id}/users` | 部门成员 | 登录 |
| POST | `/api/org/sync` | 手动全量同步飞书组织架构 | admin |
| POST | `/api/org/feishu/events` | 飞书事件订阅回调 | 匿名（验签） |

统一响应：`{ "code": 0, "message": "ok", "data": ... }`，`code != 0` 表示失败。

## 飞书组织架构同步

同步使用**应用身份** `tenant_access_token` 调用飞书通讯录接口，需要开通以下权限
（应用身份）：

- `contact:contact.base:readonly` 获取通讯录基本信息
- `contact:department.base:readonly` 获取部门基础信息
- `contact:department.organize:readonly` 获取通讯录部门组织架构信息
- `contact:user.department:readonly` 获取用户组织架构信息

应用在飞书后台的**通讯录权限范围**需覆盖要同步的部门；同步全员需选择全员范围。
权限与配置修改后，需要在飞书后台**创建版本并发布**才会生效。

同步逻辑：

1. `GET /open-apis/contact/v3/departments?parent_department_id=0&fetch_child=true`
   拉取全部部门，按 `open_department_id` upsert 到 `sys_department`，并回填本地父子关系。
2. 逐部门调用 `/open-apis/contact/v3/users/find_by_department` 拉取直属成员，
   按 `open_id` upsert 到 `sys_user`（匹配不到时按手机号/邮箱匹配已有账号，
   仍不存在则创建 `role=user` 的账号）。
3. 用本次同步的部门集合重建 `sys_user_department` 关系。
4. 飞书侧已不存在的部门会被逻辑删除并清理成员关系；本次未出现过的飞书用户
   会被禁用并清理部门关系（管理员账号不会被自动禁用）。
   只要有部门成员拉取失败，本次就跳过用户清理，避免误禁用。
5. 定时任务按 `FEISHU_SYNC_CRON` 兜底执行；管理员也可在组织架构页面点击「立即同步」；
   配置事件订阅后，通讯录变更会实时触发同步。

说明：

- 飞书同步创建的用户使用随机密码，只能通过飞书登录（飞书登录后续接入）。
- 已逻辑删除的飞书部门/用户再次同步时会被自动恢复，避免唯一键冲突。
- 用户被移出某部门后，其旧的部门关系会在下次同步时清理；离职用户状态会被置为禁用。
- 清理（`FEISHU_CLEANUP_MISSING=true`，默认开启）依赖同步能拿到全量通讯录，
  因此权限范围必须是**全员**；只授权了部分部门时请关闭该开关，否则范围外的部门和用户会被误清理。
- 管理员账号（`role=admin`）不会被自动禁用，避免同步异常导致无法登录后台。

### 事件订阅（实时同步）

在飞书开放平台「事件与回调 → 事件配置」中：

1. 请求地址填 `https://你的域名/api/org/feishu/events`（需公网可达）。
2. 将后台生成的 **Verification Token** 配置到 `FEISHU_VERIFICATION_TOKEN`；
   如启用了 **Encrypt Key**，同时配置 `FEISHU_ENCRYPT_KEY`。
3. 添加通讯录事件：`contact.department.created_v3`、`contact.department.updated_v3`、
   `contact.department.deleted_v3`、`contact.user.created_v3`、`contact.user.updated_v3`、
   `contact.user.deleted_v3` 等（所有 `contact.` 开头的事件都会被识别）。
4. 发布应用版本后生效。

回调处理逻辑：

- 配置 Encrypt Key 时按 `SHA256(timestamp + nonce + encrypt_key + body)` 校验签名，
  并按 `AES-256-CBC` 解密报文；随后校验 Verification Token。
- 飞书配置请求地址时的 `url_verification` 会原样返回 `challenge`。
- 按 `event_id` 在 Redis 做 10 分钟幂等去重（`cp-oa:feishu:event:{id}`）。
- 事件只作为「有变更」的信号，默认合并 3 秒内的多次事件后异步执行一次全量同步，
  不做逐条增量更新；同步失败会记录日志，定时任务仍会兜底。

## 目录结构

```
src/main/java/com/cpoa/
├── common/      # 统一响应、异常、Redis Key
├── config/      # Shiro、CORS、MyBatis-Plus、JWT 配置
├── controller/  # 登录、用户、组织架构、健康检查
├── dto/         # 请求参数 / 同步结果
├── entity/      # 数据库实体
├── feishu/      # 飞书开放平台客户端
├── mapper/      # MyBatis-Plus Mapper
├── scheduler/   # 定时同步任务
├── security/    # JWT 工具、Realm、Filter、凭证匹配器
├── service/     # 业务逻辑、组织架构同步
└── vo/          # 响应对象
```

## 演示账号

- `admin` / `123456`（管理员）
- `zhangsan` / `123456`（普通用户）
