# Flashback
《时光回序》一款面向大学生与年轻人的**节点记录与延迟回看产品**。产品聚焦用户在升学、毕业、求职、实习等重要人生节点中的迷茫、焦虑、期待与自我表达需求，提供一个私密、低负担的记录空间，并通过“延迟回看”机制帮助用户重新理解过去的自己，增强对当下问题的整理与行动感。

## 项目结构
- 后端：Spring Boot 3 + MyBatis + MySQL/Redis + JWT，用户侧 MVP 主链路已完成，详见 [backend/README.md](backend/README.md)
- 前端：Uniapp(Vue 3) + Vite，小程序端开发规范见 [Docs/前端文档/flashback_frontend_dev_spec.md](Docs/前端文档/flashback_frontend_dev_spec.md)
- 文档：需求/设计/接口与数据库等资料汇总在 [Docs/开发文档/接口清单文档.md](Docs/开发文档/接口清单文档.md)

## 快速开始

### 系统环境要求

| 工具 | 版本 | 说明 |
|-----|------|------|
| JDK | 17+ | 后端编译运行必需 |
| Maven | 3.8+ | Java 项目依赖管理 |
| MySQL | 8.0+ | 业务数据库 |
| Redis | 6.0+ | 缓存服务 |
| Node.js | 16+ | 前端包管理 |
| Git | 任意 | 版本管理 |

**验证命令：**
```powershell
java -version
mvn -version
mysql --version
redis-cli --version
node --version
```

### 后端启动（完整步骤）

#### 1. 环境准备检查

```powershell
# 检查 JDK
java -version

# 检查 Maven
mvn -version

# 检查 MySQL 是否正在运行（Windows 需先启动 MySQL 服务）
# 进入 MySQL 命令行验证连接
mysql -u root -p
# 输入你的数据库密码
```

#### 2. 数据库初始化

```powershell
# 使用 MySQL 命令行创建数据库
mysql -u root -p

# 在 MySQL 提示符下执行：
CREATE DATABASE flashback DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
EXIT;

# 导入建表脚本
mysql -u root -p flashback < backend/sql/mysql/schema.mysql.sql
```

或使用 MySQL 可视化工具（如 Navicat）：
1. 新建数据库 `flashback`
2. 打开 `backend/sql/mysql/schema.mysql.sql` 脚本并执行

#### 3. Redis 启动

```powershell
# 如果已安装 Redis，启动服务
redis-server

# 或在另一个终端验证连接
redis-cli ping
# 应返回 PONG
```

#### 4. 启动后端服务

进入 `backend` 目录，使用一键启动脚本：

```powershell
cd backend
./start-dev.ps1
```

**脚本说明：**
- 默认数据库用户名：`root`，密码：`123456`
- 如需修改，运行：`./start-dev.ps1 -DbUsername your_user -DbPassword your_password`

或手动启动：

```powershell
cd backend
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
```

**启动成功标志：**
```
Tomcat started on port(s): 8080 (http) with context path ''
Started FlashbackApplication in X.XXX seconds
```

#### 5. 环境变量覆盖（可选）

如需修改连接信息，可设置环境变量（如使用其他数据库或 Redis 实例）：

```powershell
# 数据库连接
$env:DB_URL = "jdbc:mysql://127.0.0.1:3306/flashback?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your_password"

# Redis 连接
$env:REDIS_HOST = "127.0.0.1"
$env:REDIS_PORT = "6379"
$env:REDIS_DATABASE = "0"
$env:REDIS_TIMEOUT = "3000ms"

# 然后启动后端
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
```

### 前端启动（完整步骤）

#### 1. 安装依赖

首先进入前端目录：

```powershell
cd frontend
```

安装依赖（推荐使用 pnpm，速度较快）：

```powershell
# 如已安装 pnpm
pnpm install

# 如未安装 pnpm，使用 npm（Node.js 自带）
npm install
```

**第一次安装可能需要 1-3 分钟，请耐心等待。**

#### 2. 启动开发服务

```powershell
pnpm dev:mp-weixin
```

开发服务启动后，将看到类似输出：
```
  VITE ⚡ [vite] v5.2.8 is ready

  ➜  Local: http://localhost:5173
  ➜  press h to show help
```

#### 3. 使用微信开发者工具调试

1. 打开微信开发者工具
2. 选择"小程序项目"
3. 项目路径指向 `frontend` 目录
4. AppID 可填写测试号或留空
5. 勾选"不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书"
6. 勾选"允许发起本地网络请求"（重要！否则无法调试后端接口）
7. 点击"打开"

#### 4. 类型检查（可选）

检查 TypeScript 类型是否有错误：

```powershell
npm run type-check
```

### 联调验证

启动后端和前端后，进行以下测试：

#### 1. 后端接口测试

```powershell
# 使用 curl 测试健康检查（需在另一个终端）
curl http://localhost:8080/actuator/health

# 应返回: {"status":"UP"}
```

#### 2. 前端注册测试

1. 在小程序中打开登录页
2. 填写用户名、密码、昵称
3. 点击"注册"
4. 成功注册后应跳转到首页

#### 3. 前端登录测试

1. 使用刚注册的账号登录
2. 登录成功后应能看到首页内容
3. 检查浏览器控制台是否有错误信息

## 开发规范与架构

### 后端技术栈
- **框架：** Spring Boot 3（Java 17+）
- **ORM：** MyBatis（SQL 手写）
- **数据库：** MySQL 8
- **缓存：** Redis 6+
- **认证：** JWT（2 小时过期）

### 项目分层结构

后端采用标准三层架构，位置在 `backend/src/main/java/com/flashback/`：

```
controller/          # 控制层（接收请求）
├── api/             # 用户端接口 /api/**
└── admin/           # 管理端接口 /admin/**

service/             # 业务层（业务逻辑）
├── impl/            # 业务实现
└── 接口定义

mapper/              # 数据访问层（SQL 映射）
└── XML 文件位于 resources/mapper/

domain/              # 实体类（数据库表映射）

dto/                 # 数据传输对象（请求/响应）

vo/                  # 视图对象（前端展示）
```

### 认证与授权

**核心原则：**
1. 用户端 `/api/**` 需要登录，管理端 `/admin/**` 需要管理员权限
2. 白名单接口：`/api/auth/**`、`/admin/auth/**`、`/error`、`/actuator/health`
3. 其他接口必须通过 `JwtAuthenticationInterceptor` 检查 token

**使用登录用户信息：**

```java
// 控制器中直接注入当前用户
@PostMapping("/records")
public ApiResponse<RecordVO> createRecord(
    @CurrentUser AuthUser authUser,  // 自动注入当前登录用户
    @RequestBody CreateRecordRequest request
) {
    Long userId = authUser.getUserId();
    String role = authUser.getRole();
    // ... 业务逻辑
}
```

### 统一响应格式

所有接口都返回统一的 `ApiResponse` 结构：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    // 实际业务数据
  }
}
```

**异常处理：** 业务异常统一抛 `BizException` 或其子类：

```java
// 示例：找不到记录
if (record == null) {
    throw new BizException(ErrorCode.RECORD_NOT_FOUND, "记录不存在");
}
```

### 前端技术栈
- **框架：** Uniapp 3.0（Vue 3）
- **构建：** Vite 5
- **包管理：** pnpm 或 npm
- **平台：** 微信小程序

### 前端页面规范

**一级导航（底部 Tab）固定 3 个：**
1. 首页 - 草稿入口 + 最近解锁
2. 时间轴 - 记录列表 + 筛选
3. 个人中心 - 用户信息 + 设置

**二级页面（无底部导航）：**
- 我的档案 - 全部记录管理
- 新建页 - 沉浸式书写
- 解锁页 - 查看已解锁回忆
- 详情页 - 记录详情 + 回信

### 关键业务流程

#### 1. 用户认证流程
```
注册 (username, password, nickname)
  ↓
登录 (username, password)
  ↓
获取 JWT Token（2 小时有效期）
  ↓
后续请求携带 Authorization header
  ↓
Token 过期需重新登录
```

#### 2. 记录生命周期
```
草稿 (Draft)
  ↓
封存 (Archived) - 不可编辑，可查看
  ↓
到期解锁 (Unlocked) - 定时任务自动解锁，可回信
  ↓
单记录仅一条回信
```

#### 3. 标签绑定
```
创建记录时指定 tagIds
  ↓
标签用于时间轴筛选
  ↓
按标签、按年份分类显示
```

### 开发注意事项

**记录编辑陷阱：**
- 编辑记录必须显式传递 `tagIds`、`aiSummary`、`aiPromptResults`
- 遗漏的字段会被更新为 NULL（不是保留原值）

**测试环境特殊性：**
- 测试 profile 默认禁用 DataSource、MyBatis、Redis 自动配置
- 新增数据库依赖的测试需额外处理配置

**前端事件类型：**
- `<input @input>` 在 TypeScript 中按 DOM Event 类型推断
- 需要先接收 Event，再从 `event.detail?.value` 取值

**多语言支持：**
- 中文作为主要表达，英文仅作弱辅助
- 文档和代码注释优先使用中文

## 常见问题与排查

### 后端启动问题

#### ❌ `Access denied for user 'root'@'localhost'`

**问题原因：** 数据库用户名或密码错误

**排查步骤：**
1. 验证 MySQL 是否正在运行：`mysql -u root -p`
2. 确认你的数据库密码，使用正确密码重新连接
3. 如要修改启动脚本中的密码：
   ```powershell
   cd backend
   ./start-dev.ps1 -DbUsername root -DbPassword 你的正确密码
   ```

#### ❌ `Unknown database 'flashback'`

**问题原因：** 数据库不存在

**解决方案：**
1. 登录 MySQL：`mysql -u root -p`
2. 创建数据库：
   ```sql
   CREATE DATABASE flashback DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
3. 查看是否创建成功：`SHOW DATABASES;`

#### ❌ `Table 'flashback.user' doesn't exist`

**问题原因：** 没有执行建表脚本

**解决方案：**
```powershell
# 在项目根目录执行
mysql -u root -p flashback < backend/sql/mysql/schema.mysql.sql
```

验证建表成功：
```sql
mysql -u root -p flashback
SHOW TABLES;  # 应看到 user, record, tag, reply 等表
```

#### ❌ `Connection refused` 或 `Communications link failure`

**问题原因：** MySQL 或 Redis 未启动，或端口不通

**排查步骤：**
1. **检查 MySQL 状态：**
   ```powershell
   # Windows：检查 MySQL 服务是否运行
   Get-Service MySQL80  # 或你的 MySQL 版本
   
   # 如未运行，启动服务
   net start MySQL80
   ```

2. **检查 Redis 状态：**
   ```powershell
   redis-cli ping
   # 应返回 PONG
   ```

3. **检查端口占用：**
   ```powershell
   netstat -ano | findstr ":3306"  # MySQL
   netstat -ano | findstr ":6379"  # Redis
   ```

#### ❌ `TestEngine failed to discover tests` 伴随 `NoClassDefFoundError`

**问题原因：** Maven 编译产物陈旧，构建缓存冲突

**解决方案：**
```powershell
cd backend
mvn clean test
```

### 前端启动问题

#### ❌ `uni is not a function`

**问题原因：** Uniapp 依赖版本不一致

**解决方案：**
1. 删除 node_modules 和 pnpm-lock.yaml：
   ```powershell
   cd frontend
   Remove-Item node_modules -Recurse -Force
   Remove-Item pnpm-lock.yaml -Force
   ```

2. 重新安装（确保版本一致）：
   ```powershell
   pnpm install
   ```

3. 验证依赖版本：
   ```powershell
   pnpm ls @dcloudio/uni-app
   # 应显示 3.0.0-5000720260410001
   ```

#### ❌ 找不到模块或 `Cannot find module`

**问题原因：** 依赖未安装或安装不完整

**解决方案：**
```powershell
cd frontend

# 清理旧的安装
pnpm store prune
Remove-Item node_modules -Recurse -Force
Remove-Item pnpm-lock.yaml -Force

# 重新安装
pnpm install
```

#### ❌ 类型检查失败 `vue-tsc` 报错

**问题原因：** TypeScript 类型不匹配

**排查步骤：**
```powershell
# 运行详细类型检查
npm run type-check

# 查看具体错误信息，根据行号定位问题
# 常见原因：事件类型不匹配、变量未声明等
```

**快速修复：**
- 组件 `@input` 事件：接收 `Event` 类型，再取 `event.detail?.value`
- 注意区分 DOM 事件和组件事件

### 联调问题

#### ❌ 小程序无法请求后端

**问题原因：** 微信开发者工具未允许本地网络请求

**解决方案：**
1. 打开微信开发者工具
2. 点击"详情"标签
3. 勾选"不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书"
4. 勾选"允许发起本地网络请求"
5. 重启开发服务

#### ❌ 登录返回 `401 Unauthorized`

**问题原因：** Token 过期或未携带

**排查步骤：**
1. 确认已成功登录（有 token 返回）
2. 检查后续请求是否在 Header 中携带 token：
   ```
   Authorization: Bearer <your_token_here>
   ```
3. 检查 token 是否过期（默认 2 小时过期）

#### ❌ 注册时返回 `400 nickname不能为空`

**问题原因：** 前端表单字段不完整

**排查步骤：**
1. 检查注册表单是否填写了所有必填项：`username`、`password`、`nickname`
2. 检查前端代码中注册请求是否包含所有字段
3. 查看浏览器控制台的请求详情（Network 标签）

#### ❌ 登录后无法获取用户信息

**问题原因：** 数据库未初始化或测试账号不存在

**排查步骤：**
1. 确认数据库已建表：`SHOW TABLES;`
2. 使用已注册的账号登录（不要使用默认账号）
3. 检查后端日志是否有数据库查询错误
4. 验证 Redis 连接是否正常

### 验证完整联调流程

1. **后端服务已启动：**
   ```powershell
   curl http://localhost:8080/actuator/health
   # 应返回 {"status":"UP"}
   ```

2. **前端服务已启动：**
   - 微信开发者工具可看到编译结果

3. **注册测试账号：**
   - 用户名：`testuser123`
   - 密码：`password123`
   - 昵称：`测试用户`

4. **登录测试：**
   - 使用测试账号登录
   - 能成功进入首页

5. **创建记录：**
   - 在首页点击"新建"
   - 输入内容并点击"封存这一刻"
   - 成功保存后回到首页

**如果以上步骤全部成功，说明本地联调环境配置完成！**

## 推荐工具与IDE

### 后端开发
| 工具 | 用途 | 说明 |
|-----|------|------|
| IntelliJ IDEA | IDE | 推荐使用 Community 版本（免费）或 Ultimate 版本 |
| MySQL Workbench | 数据库管理 | 官方工具，可视化管理数据库 |
| Navicat | 数据库管理 | 商业工具，功能更完整 |
| Postman / Insomnia | API 调试 | 测试接口，导出测试集 |
| DBeaver | 数据库工具 | 免费开源，支持多数据库 |

### 前端开发
| 工具 | 用途 | 说明 |
|-----|------|------|
| VS Code | 编辑器 | 轻量、快速、扩展丰富 |
| 微信开发者工具 | 小程序调试 | 必需工具，官方提供 |
| Vue DevTools | 浏览器扩展 | 调试 Vue 组件状态 |

### VS Code 推荐扩展
```
Vue - Official                          (官方 Vue 3 支持)
Vite                                    (Vite 构建工具)
Better Comments                         (代码注释高亮)
Chinese (Simplified) Language Pack      (中文界面)
```

## 常用命令速查

### 后端常用命令

```powershell
# 启动后端（开发模式，自动刷新）
cd backend
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"

# 只编译不运行
mvn clean compile

# 运行单元测试
mvn test

# 运行特定测试类
mvn test -Dtest=UserServiceImplTest

# 打包为可执行 JAR
mvn clean package -DskipTests

# 生产环境启动（需显式指定 prod profile）
java -jar target/flashback-backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod

# 清理所有编译产物
mvn clean
```

### 前端常用命令

```powershell
# 安装依赖
pnpm install  # 或 npm install

# 启动开发服务
pnpm dev:mp-weixin

# 构建生产版本
pnpm build:mp-weixin

# 类型检查
npm run type-check

# 清理缓存重新安装
pnpm store prune
Remove-Item node_modules -Recurse -Force
Remove-Item pnpm-lock.yaml -Force
pnpm install
```

### 数据库常用命令

```powershell
# 连接 MySQL
mysql -u root -p

# 在 MySQL 提示符内
SHOW DATABASES;                 # 查看所有数据库
USE flashback;                  # 切换到 flashback 数据库
SHOW TABLES;                    # 查看表列表
DESCRIBE user;                  # 查看 user 表结构
SELECT COUNT(*) FROM user;      # 查看 user 表记录数
SELECT * FROM user LIMIT 10;    # 查看前 10 条记录

# 备份数据库
mysqldump -u root -p flashback > backup.sql

# 恢复数据库
mysql -u root -p flashback < backup.sql
```

## 项目代码统计

```
后端代码结构：
├── controller/    # HTTP 请求处理层
├── service/       # 业务逻辑层  
├── mapper/        # 数据访问层 + XML SQL
├── domain/        # 实体类
├── dto/           # 数据传输对象
├── vo/            # 视图对象
└── 其他工具类

前端代码结构：
├── pages/         # 页面组件
├── components/    # 可复用组件
├── services/      # API 调用
├── stores/        # Pinia 状态管理
├── utils/         # 工具函数
├── types/         # TypeScript 类型定义
└── styles/        # 全局样式
```

## 资源链接

### 官方文档
- [Spring Boot 官方文档](https://spring.io/projects/spring-boot)
- [Vue 3 官方文档](https://vuejs.org/)
- [Uniapp 官方文档](https://uniapp.dcloud.net.cn/)
- [MySQL 官方文档](https://dev.mysql.com/doc/)

### 相关教程
- [Spring Boot 快速开始](https://spring.io/guides/gs/spring-boot/)
- [Vue 3 入门教程](https://vuejs.org/guide/introduction.html)
- [Uniapp 快速上手](https://uniapp.dcloud.net.cn/quickstart-hx.html)
- [MyBatis 中文文档](https://mybatis.org/mybatis-3/zh/index.html)

### 本地文档
- [后端使用说明](backend/README.md)
- [接口文档](Docs/开发文档/接口清单文档.md)
- [数据库设计](Docs/开发文档/数据库设计文档.md)
- [前端开发规范](Docs/前端文档/flashback_frontend_dev_spec.md)
- [联调清单](Docs/开发文档/联调清单.md)

## 快速问题排查检查清单

遇到问题时，按以下顺序排查：

- [ ] 确认 JDK、Maven、MySQL、Redis 都已安装
- [ ] 确认 MySQL 已启动且可连接
- [ ] 确认 Redis 已启动
- [ ] 确认 MySQL 数据库 `flashback` 已创建
- [ ] 确认建表脚本已执行：`backend/sql/mysql/schema.mysql.sql`
- [ ] 确认后端启动没有错误信息
- [ ] 确认前端依赖已安装：`pnpm install`
- [ ] 确认前端开发服务已启动
- [ ] 确认微信开发者工具已勾选"允许发起本地网络请求"
- [ ] 尝试注册新账号并登录测试
- [ ] 查看浏览器控制台 Network 标签检查 API 响应
- [ ] 查看后端服务日志查看错误栈

## 获取帮助

如遇到问题：

1. **查阅本文档** - 常见问题栏已覆盖大部分场景
2. **查阅项目文档** - [Docs 目录](Docs/)下的详细文档
3. **查看后端日志** - 后端控制台输出通常包含问题线索
4. **查看前端日志** - 微信开发者工具的调试器和控制台
5. **查看数据库** - 用 SQL 直接验证数据是否正确保存
