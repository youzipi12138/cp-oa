# CP OA Frontend

React 19 + Vite 7 + TypeScript + Ant Design 5 + Ant Design Pro Components。

## 快速开始

```bash
pnpm install
pnpm dev
```

默认地址：<http://localhost:5173>

开发环境下 `/api` 会被代理到 `http://localhost:8080`，配置见 `vite.config.ts`。

## 常用命令

| 命令 | 说明 |
| --- | --- |
| `pnpm dev` | 启动开发服务器 |
| `pnpm build` | 类型检查并构建到 `dist/` |
| `pnpm preview` | 预览构建产物 |
| `pnpm typecheck` | 仅做 TypeScript 类型检查 |

## 目录结构

```
src/
├── layouts/          # ProLayout 主布局
├── pages/            # 页面（登录、工作台、用户管理）
├── router/           # 路由守卫
├── services/         # 接口请求
├── utils/            # axios 封装、登录态存储
├── types/            # 公共类型
├── styles/           # 全局样式
├── App.tsx           # 路由与主题配置
└── main.tsx          # 入口
```

## 对接后端

- 接口前缀 `/api`，登录后通过 `Authorization: Bearer <token>` 传递 JWT。
- 后端统一返回 `{ code, message, data }`，`code === 0` 表示成功。
- 401 自动清理本地登录态并跳转登录页。
