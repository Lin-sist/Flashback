# flashback-backend (skeleton)

## 技术栈
- Spring Boot 3
- MyBatis
- MySQL 8
- Redis
- JWT

## 已包含骨架能力
- 分层目录：controller / service / mapper / domain / dto / vo
- 统一返回体：`ApiResponse { code, message, data }`
- 全局异常处理：`GlobalExceptionHandler`
- JWT 登录拦截：`JwtAuthenticationInterceptor`
- 路由前缀约束：用户端 `/api/**`，管理端 `/admin/**`
- 多环境配置：`application-dev.yml` / `application-prod.yml`

## 说明
- 当前仅为项目骨架，不包含任何业务代码。
- 可直接在对应分层目录补充具体模块实现。
- 默认不激活任何 Spring Profile，避免部署时误用开发配置。
- 本地启动请显式指定：`--spring.profiles.active=dev`
- 生产启动请显式指定：`--spring.profiles.active=prod`
- 生产环境请显式注入 `APP_CORS_ALLOWED_ORIGIN_PATTERNS`，未配置时浏览器跨域请求默认不放行
- MySQL 建表参考见 `backend/sql/mysql/schema.mysql.sql`
