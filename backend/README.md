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
| `REDIS_PASSWORD` | 空 | Redis 密码 |
| `REDIS_DATABASE` | `0` | Redis 库 |
| `JWT_SECRET` | 内置开发密钥 | 至少 32 字节，生产必须覆盖 |
| `JWT_EXPIRE_MINUTES` | `720` | Token 有效期（分钟） |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | 允许的前端来源，逗号分隔 |

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

统一响应：`{ "code": 0, "message": "ok", "data": ... }`，`code != 0` 表示失败。

## 目录结构

```
src/main/java/com/cpoa/
├── common/      # 统一响应、异常、Redis Key
├── config/      # Shiro、CORS、MyBatis-Plus、JWT 配置
├── controller/  # 登录、用户、健康检查
├── dto/         # 请求参数
├── entity/      # 数据库实体
├── mapper/      # MyBatis-Plus Mapper
├── security/    # JWT 工具、Realm、Filter、凭证匹配器
├── service/     # 业务逻辑
└── vo/          # 响应对象
```

## 演示账号

- `admin` / `123456`（管理员）
- `zhangsan` / `123456`（普通用户）
