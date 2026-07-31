# 技术栈（Kiro Steering）

## Frontend

- **框架**：Uniapp + Vue 3 + Composition API（`<script setup>`）
- **状态管理**：Pinia
- **目标平台**：WeChat Mini Program（`mp-weixin`）
- **构建**：`@dcloudio/vite-plugin-uni`
- **请求**：`uni.request` 封装 → `httpClient.ts`
- **样式**：SCSS；组件 scoped style

## Backend

- **框架**：Spring Boot 3.x
- **持久化**：MyBatis + MySQL 8.0
- **对象存储**：私有 MinIO-compatible S3（signed URL）
- **AI provider**：DeepSeek / OpenAI-compatible（通过后端 `AiServiceImpl` 调用）
- **认证**：JWT（jjwt + 自研过滤器/拦截器；**未引入 Spring Security**，pom 无 security starter）
- **Agent runtime**：自研 `AgentStageMachine` + `AgentModelClient`（OpenAI-compatible HTTP）；**未引入 Spring AI**
- **构建**：Maven（Spring Boot 3.3.5 / Java 17）

## 约束

- AI API key、对象存储 AK/SK 等 secret **只能**存在于 backend-side config / local secret
- 不得进入 frontend 代码或 tracked files
- 不改 package / lockfile，除非任务明确要求并说明原因
- 不做大规模 backend rewrite 或 major frontend visual reconstruction
