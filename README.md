# 时光回序 Flashback V2.0

《时光回序》是一款面向个人表达与延迟回看的微信小程序。它帮助用户在升学、毕业、求职、实习、关系变化或普通生活片段中写下当下的情绪、困惑、期待与选择，并在未来某个时间重新回看这一刻。

项目的核心理念不是效率管理、社交流或情绪打分，而是给用户一个安静、私密、低负担的记录空间。记录本身先成立，未来回看只是把理解权交还给时间。

## 核心能力概览

### 用户侧主流程

```text
注册 / 登录
  -> 首页写下此刻
  -> 创建或编辑草稿
  -> 添加正文、标签、位置、图片、语音和可选封面
  -> 可选使用 AI 辅助整理
  -> 保存草稿或交给时间
  -> 时光轴浏览和筛选记录
  -> 到期后进入时间回看
  -> 查看那时的我、现在的我、位置与媒体
```

### 已覆盖的核心模块

- 认证：用户名密码注册登录、JWT 鉴权、当前用户信息。
- 记录：草稿创建/编辑/删除、封存、详情、列表、已解锁记录、回信。
- 标签：标签列表、记录标签绑定、时光轴单标签筛选。
- 时间轴：按 `createdAt` 年/月/日筛选，支持单标签与日期 AND 组合，按 `created_at DESC, id DESC` 稳定分页。
- 位置：当前定位、地图选点、手动输入；草稿可改，封存/解锁后不可变。
- 附件：图片和语音上传、后端对象校验、私有桶短时访问 URL、草稿删除/重录。
- 封面：只能从当前记录的图片附件中选择。
- AI：后端侧配置 DeepSeek 或 OpenAI-compatible Provider；真实路径缺配置或调用失败时显式失败，不伪造成功。
- Preview：可保留演示数据，但必须与登录后的真实用户路径隔离。

## 目录结构

```text
Flashback/
├── backend/        # Spring Boot 后端
│   ├── src/
│   ├── sql/mysql/schema.mysql.sql
│   ├── start-dev.ps1
│   └── OBJECT_STORAGE_CONFIG.md
├── frontend/       # Uniapp 微信小程序
│   ├── src/
│   ├── package.json
│   ├── .env.development
│   └── .env.preview
├── openspec/       # 当前事实源与变更说明
├── Docs/           # 历史设计/开发文档，仅在不冲突时参考
├── .ai/            # Agent 当前任务与工作记录
└── README.md
```

## 环境要求

| 工具 | 建议版本 | 用途 |
| --- | --- | --- |
| JDK | 17+ | 后端编译运行 |
| Maven | 3.8+ | 后端依赖与测试 |
| MySQL | 8.0+ | 业务数据库 |
| Redis | 6.0+ | 缓存与运行依赖 |
| Node.js | 18+ | 前端依赖和构建 |
| pnpm | 8+ | 前端包管理，npm 也可用 |
| 微信开发者工具 | 当前稳定版 | 小程序调试 |

检查命令：

```powershell
java -version
mvn -version
mysql --version
redis-cli --version
node --version
pnpm --version
```

## 本地快速启动

以下步骤默认在 Windows PowerShell 中执行。

### 1. 克隆并进入项目

```powershell
git clone <your-repo-url>
cd Flashback
```

### 2. 初始化 MySQL

创建数据库：

```powershell
mysql -u root -p
```

进入 MySQL 后执行：

```sql
CREATE DATABASE flashback DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
EXIT;
```

导入表结构：

```powershell
mysql -u root -p flashback < backend/sql/mysql/schema.mysql.sql
```

当前 schema 包含用户、记录、位置、附件、标签、回信、提醒和解锁通知日志等表。

### 3. 启动 Redis

如果 Redis 已作为 Windows 服务安装：

```powershell
redis-cli ping
```

返回 `PONG` 即可。未启动时按你的本机安装方式启动 Redis，例如：

```powershell
redis-server
```

### 4. 启动后端

进入后端目录：

```powershell
cd backend
```

使用默认脚本启动：

```powershell
.\start-dev.ps1
```

脚本默认注入：

- `DB_USERNAME=root`
- `DB_PASSWORD=123456`
- `spring.profiles.active=dev`

如果你的 MySQL 密码不同：

```powershell
.\start-dev.ps1 -DbUsername root -DbPassword "你的密码"
```

启动成功后应看到 Tomcat 运行在 `8080`。可在另一个终端验证：

```powershell
curl http://127.0.0.1:8080/actuator/health
```

返回 `{"status":"UP"}` 表示后端可用。

### 5. 启动前端

新开一个 PowerShell，进入前端目录：

```powershell
cd frontend
pnpm install
pnpm dev:mp-weixin
```

如果不用 pnpm，也可以使用：

```powershell
npm install
npm run dev:mp-weixin
```

默认开发环境读取 `frontend/.env.development`：

```env
VITE_API_BASE_URL=http://127.0.0.1:8080
VITE_PREVIEW_MODE=false
```

### 6. 用微信开发者工具打开

1. 打开微信开发者工具。
2. 选择“小程序项目”。
3. 项目目录选择 `frontend`。
4. AppID 使用 `frontend/src/manifest.json` 中的 AppID，或使用你自己的测试号。
5. 本地联调时，在“详情/本地设置”中勾选“不校验合法域名、web-view、TLS 版本以及 HTTPS 证书”。
6. 勾选“允许发起本地网络请求”。

进入小程序后，先注册一个测试账号，再登录并创建记录。

## 配置说明

### 后端基础配置

后端默认 profile 是 `dev`，配置文件在：

- `backend/src/main/resources/application.yml`
- `backend/src/main/resources/application-dev.yml`
- `backend/src/main/resources/application-prod.yml`

常用环境变量：

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `DB_URL` | `jdbc:mysql://127.0.0.1:3306/flashback?...` | MySQL 连接地址 |
| `DB_USERNAME` | `root` | MySQL 用户名 |
| `DB_PASSWORD` | 空，启动脚本默认 `123456` | MySQL 密码 |
| `REDIS_HOST` | `127.0.0.1` | Redis 主机 |
| `REDIS_PORT` | `6379` | Redis 端口 |
| `REDIS_DATABASE` | `0` | Redis database |
| `REDIS_TIMEOUT` | `3000ms` | Redis 超时 |
| `JWT_SECRET` | dev 有本地示例值 | 生产/独立环境应显式注入 |
| `APP_TIME_ZONE_ID` | `Asia/Shanghai` | 业务时区 |

临时覆盖示例：

```powershell
$env:DB_URL = "jdbc:mysql://127.0.0.1:3306/flashback?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your_password"
$env:REDIS_HOST = "127.0.0.1"
$env:REDIS_PORT = "6379"

cd backend
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
```

### AI Provider 配置

AI 密钥只能放在后端环境变量、本地忽略提交脚本或部署侧 secret 中，不能写进前端或 tracked file。

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `AI_PROVIDER` | `mock` | `mock`、`deepseek`、`openai-compatible` |
| `AI_BASE_URL` | `https://api.deepseek.com` | OpenAI-compatible base URL |
| `AI_API_KEY` | 空 | Provider API Key |
| `AI_MODEL` | `deepseek-v4-pro` | 模型名 |
| `AI_TIMEOUT_MILLIS` | `10000` | 请求超时 |
| `AI_REAL_MODE_MOCK_ENABLED` | `false` | 是否允许真实路径 mock，默认禁止 |

DeepSeek 示例：

```powershell
$env:AI_PROVIDER = "deepseek"
$env:AI_BASE_URL = "https://api.deepseek.com"
$env:AI_API_KEY = "<your-api-key>"
$env:AI_MODEL = "deepseek-v4-pro"
```

如果没有配置真实 `AI_API_KEY`，真实用户路径应看到明确的不可用/失败状态，而不是 mock 成功。

### 对象存储与媒体配置

M4 附件使用私有对象存储，前端向后端请求上传授权，再直传到 Provider，最后由后端校验对象存在后才写入附件元数据。

支持的 `STORAGE_PROVIDER`：

| 值 | 后端 Provider | 说明 |
| --- | --- | --- |
| `qiniu` | `QINIU` | 七牛云 Kodo 私有空间 |
| `s3-compatible` / `aws-s3` | `S3_COMPATIBLE` | AWS S3 或通用 SigV4 服务 |
| `aliyun-oss` | `S3_COMPATIBLE` | 阿里云 OSS S3 兼容模式 |
| `tencent-cos` | `S3_COMPATIBLE` | 腾讯云 COS S3 兼容模式 |
| `minio` | `S3_COMPATIBLE` | MinIO |

媒体限制：

| 配置 | 默认值 |
| --- | --- |
| 每条记录图片数 | 9 |
| 每条记录语音数 | 9 |
| 单文件大小 | 40 MB |
| 单条记录附件总大小 | 300 MB |
| 上传授权 TTL | 600 秒 |
| 下载访问 URL TTL | 600 秒 |

七牛云示例：

```powershell
$env:STORAGE_PROVIDER = "qiniu"
$env:QINIU_ACCESS_KEY = "<AK>"
$env:QINIU_SECRET_KEY = "<SK>"
$env:QINIU_BUCKET = "<private-bucket>"
$env:QINIU_REGION = "z0"
$env:QINIU_PRIVATE_DOMAIN = "https://<private-media-domain>"
$env:QINIU_KEY_PREFIX = "flashback"
```

S3 兼容示例：

```powershell
$env:STORAGE_PROVIDER = "s3-compatible"
$env:S3_ENDPOINT = "https://<provider-endpoint>"
$env:S3_REGION = "<provider-region>"
$env:S3_ACCESS_KEY = "<AK>"
$env:S3_SECRET_KEY = "<SK>"
$env:S3_BUCKET = "<private-bucket>"
$env:S3_PATH_STYLE_ACCESS = "false"
$env:S3_KEY_PREFIX = "flashback"
```

更多服务商说明见 `backend/OBJECT_STORAGE_CONFIG.md`。

### 微信能力配置

微信登录和订阅消息相关变量：

| 变量 | 说明 |
| --- | --- |
| `WECHAT_MINI_PROGRAM_APP_ID` | 小程序 AppID |
| `WECHAT_MINI_PROGRAM_SECRET` | 小程序 AppSecret |
| `WECHAT_UNLOCK_REMINDER_TEMPLATE_ID` | 解锁提醒订阅消息模板 ID |
| `WECHAT_UNLOCK_REMINDER_PAGE` | 订阅消息跳转页面，默认 `pages/record-detail/index` |
| `WECHAT_UNLOCK_REMINDER_THING_KEY` | 模板 thing 字段 key |
| `WECHAT_UNLOCK_REMINDER_TIME_KEY` | 模板 time 字段 key |

本地真实微信联调时，可把这些 secret 放在忽略提交的本地启动脚本、PowerShell 会话环境变量或系统环境变量中。不要提交真实 AppSecret。

小程序端权限在 `frontend/src/manifest.json` 中声明：

- `scope.userLocation`：用于保存用户主动选择的位置。
- `scope.record`：用于添加用户主动录制的语音。
- `requiredPrivateInfos`：`getLocation`、`chooseLocation`。

### 前端模式配置

前端环境文件：

- `frontend/.env.development`：真实后端联调，`VITE_PREVIEW_MODE=false`。
- `frontend/.env.preview`：preview 演示模式，`VITE_PREVIEW_MODE=true`。

常用命令：

```powershell
cd frontend

# 真实后端联调
pnpm dev:mp-weixin

# preview 演示模式
pnpm dev:mp-weixin:preview

# 构建微信小程序
pnpm build:mp-weixin

# 构建 preview 小程序
pnpm build:mp-weixin:preview

# 类型检查
pnpm type-check
```

## 联调验收建议

### 最小可用链路

1. 后端 `/actuator/health` 返回 `UP`。
2. 小程序注册新账号。
3. 登录后进入首页。
4. 创建一条草稿记录。
5. 保存草稿后在“我的记录”或“时光轴”中看到它。
6. 封存记录后确认正文、位置、附件和封面不可再修改。
7. 到期后进入“时间回看”查看原记录和回信入口。

### M4 能力验收

- AI：配置真实 Provider 后调用写作提示/内容整理；缺配置时显示明确不可用。
- 位置：分别验证当前定位、地图选点、手动输入；封存后禁止修改。
- 图片：选择、压缩、上传、后端 commit 校验、预览、草稿删除。
- 语音：录制、上传、播放、草稿重录/删除。
- 封面：只能从当前记录图片附件选择；删除当前封面图片时封面被清理。
- 时光轴：单标签、年、月、日、组合筛选、重置、空结果、加载更多。
- Preview：显式 preview 模式可用，但登录真实路径不使用 preview/mock 数据。

## 开发与验证命令

### 后端

```powershell
cd backend

# 启动开发服务
.\start-dev.ps1

# 手动启动
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"

# 运行测试
mvn test

# 打包
mvn clean package -DskipTests
```

### 前端

```powershell
cd frontend

# 安装依赖
pnpm install

# 类型检查
pnpm type-check

# 微信小程序构建
pnpm build:mp-weixin
```

### 数据库

```powershell
mysql -u root -p flashback
```

常用 SQL：

```sql
SHOW TABLES;
DESCRIBE record;
DESCRIBE record_location;
DESCRIBE record_attachment;
SELECT id, username, nickname FROM user LIMIT 10;
SELECT id, user_id, status, title, created_at FROM record ORDER BY id DESC LIMIT 10;
```

## 常见问题

### 后端启动报 `Unknown database 'flashback'`

数据库未创建。执行：

```sql
CREATE DATABASE flashback DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

然后重新导入 `backend/sql/mysql/schema.mysql.sql`。

### 后端启动报 `Access denied for user`

MySQL 用户名或密码不对。用下面命令确认可登录：

```powershell
mysql -u root -p
```

再用正确密码启动：

```powershell
cd backend
.\start-dev.ps1 -DbUsername root -DbPassword "你的密码"
```

### 后端报表不存在

未导入 schema。执行：

```powershell
mysql -u root -p flashback < backend/sql/mysql/schema.mysql.sql
```

### Redis 连接失败

确认 Redis 正在运行：

```powershell
redis-cli ping
```

如果没有返回 `PONG`，先启动 Redis，再重启后端。

### 小程序请求后端失败

检查：

- 后端是否运行在 `http://127.0.0.1:8080`。
- `frontend/.env.development` 中 `VITE_API_BASE_URL` 是否正确。
- 微信开发者工具是否允许本地网络请求。
- 是否勾选“不校验合法域名、web-view、TLS 版本以及 HTTPS 证书”。

### AI 一直不可用

检查：

- `AI_PROVIDER` 是否为 `deepseek` 或 `openai-compatible`。
- `AI_API_KEY` 是否只注入到了后端进程。
- `AI_BASE_URL` 与 `AI_MODEL` 是否匹配当前 Provider。
- 后端日志是否出现超时、鉴权失败或响应格式错误。

### 图片/语音上传失败

检查：

- `STORAGE_PROVIDER` 是否与凭据变量匹配。
- bucket 是否为私有桶。
- Provider 是否支持上传、HEAD/stat、GET 签名访问和 DELETE。
- 单文件是否超过 40 MB，单记录附件总量是否超过 300 MB。
- 后端是否返回对象验证失败；验证失败不会写入附件元数据。

### 微信定位或录音没有弹窗

检查 `frontend/src/manifest.json` 中的权限声明，以及微信开发者工具的权限模拟设置。真机上还需要用户授权定位和录音。
