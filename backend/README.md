# flashback-backend (MVP user-side)

## 技术栈
- Spring Boot 3
- MyBatis
- MySQL 8
- Redis
- JWT

## 已实现能力（用户侧 MVP）
- 分层目录：controller / service / mapper / domain / dto / vo
- 统一返回体：`ApiResponse { code, message, data }`
- 全局异常处理：`GlobalExceptionHandler`
- JWT 登录拦截：`JwtAuthenticationInterceptor`
- 路由前缀约束：用户端 `/api/**`，管理端 `/admin/**`
- 多环境配置：`application-dev.yml` / `application-prod.yml`
- 用户侧认证链路：注册、登录、获取当前用户、更新个人信息
- 记录主链路：草稿创建/编辑/删除、封存、列表/详情查询、已解锁列表
- 回信主链路：仅 `UNLOCKED` 记录可回信，单记录仅一条回信
- 标签与时间轴：标签查询、记录标签绑定、按年/标签筛选时间轴
- AI 最小能力：写作提示、内容整理（默认 mock，可替换 provider）
- 解锁调度：定时扫描到期记录并写入解锁日志

## 说明
- 当前已完成用户侧 MVP 主链路，管理端接口仍在规划中（`/admin/**` 暂未完整落地）。
- 可在现有分层结构上继续扩展管理端与生产级运维能力。
- 当前 `application.yml` 已默认激活 `dev`，线上仍建议显式指定 `--spring.profiles.active=prod`。
- 本地如需覆盖数据库或 Redis 连接，可设置环境变量：`DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`REDIS_HOST`、`REDIS_PORT`、`REDIS_DATABASE`、`REDIS_TIMEOUT`
- 生产环境请显式注入 `APP_CORS_ALLOWED_ORIGIN_PATTERNS`，未配置时浏览器跨域请求默认不放行
- MySQL 建表参考见 `backend/sql/mysql/schema.mysql.sql`

## 本地联调最小启动步骤
1. 创建 MySQL 数据库 `flashback`
2. 执行 `backend/sql/mysql/schema.mysql.sql` 完成建表
3. 按本机情况配置数据库连接
4. 启动后端
5. 先通过用户端注册接口创建一个普通用户，再使用 `/api/auth/login` 登录

## PowerShell 一键启动（本地）
- 在 `backend` 目录执行：`./start-dev.ps1`
- 默认会注入：`DB_USERNAME=root`、`DB_PASSWORD=123456`，并以 `dev` profile 启动
- 如需临时覆盖：`./start-dev.ps1 -DbUsername root -DbPassword "你的密码" -Profile dev`

## 常见启动问题
- `Access denied for user 'root'@'localhost'`：数据库用户名或密码不正确，请检查 `DB_USERNAME` / `DB_PASSWORD`
- `Unknown database 'flashback'`：数据库未创建
- `Table 'flashback.user' doesn't exist`：建表脚本未执行
- 当前仓库前端登录页调用的是用户端接口 `/api/auth/login`，不是后台接口 `/admin/auth/login`
