# 对象存储切换配置

Flashback 后端通过 `app.storage.provider` 选择新附件的上传目标。前端不保存任何 AK/SK，也不需要因切换服务商改业务代码。

## 支持的 provider

| `STORAGE_PROVIDER` | 后端实现 | 适用场景 |
| --- | --- | --- |
| `qiniu` | `QINIU` | 七牛云 Kodo 私有空间 |
| `s3-compatible` / `aws-s3` | `S3_COMPATIBLE` | AWS S3 或通用 SigV4 服务 |
| `aliyun-oss` | `S3_COMPATIBLE` | 已启用 S3 兼容访问的阿里云 OSS |
| `tencent-cos` | `S3_COMPATIBLE` | 已启用 S3 兼容访问的腾讯云 COS |
| `minio` | `S3_COMPATIBLE` | MinIO |

别名只决定使用哪个后端适配器。实际 endpoint、region、bucket 和凭证始终由环境变量提供。

## 七牛云配置

```powershell
$env:STORAGE_PROVIDER = 'qiniu'
$env:QINIU_ACCESS_KEY = '<AK>'
$env:QINIU_SECRET_KEY = '<SK>'
$env:QINIU_BUCKET = '<private-bucket>'
$env:QINIU_REGION = 'z0'
$env:QINIU_PRIVATE_DOMAIN = 'https://<private-media-domain>'
$env:QINIU_KEY_PREFIX = 'flashback'
```

## S3 兼容配置

```powershell
$env:STORAGE_PROVIDER = 's3-compatible'
$env:S3_ENDPOINT = 'https://<provider-endpoint>'
$env:S3_REGION = '<provider-region>'
$env:S3_ACCESS_KEY = '<AK>'
$env:S3_SECRET_KEY = '<SK>'
$env:S3_SESSION_TOKEN = ''
$env:S3_BUCKET = '<private-bucket>'
$env:S3_PATH_STYLE_ACCESS = 'false'
$env:S3_KEY_PREFIX = 'flashback'
```

常见形态（以服务商控制台当前显示为准）：

- 阿里云 OSS：`STORAGE_PROVIDER=aliyun-oss`，Java/AWS SDK 2.x 的官方 S3 兼容 endpoint 形如 `https://s3.oss-<region>.aliyuncs.com`；必须使用虚拟托管风格，即 `S3_PATH_STYLE_ACCESS=false`。官方 Java 2.x 示例使用 `S3_REGION=aws-global`。
- 腾讯云 COS：`STORAGE_PROVIDER=tencent-cos`，endpoint 通常形如 `https://cos.<region>.myqcloud.com`；bucket 通常包含 APPID 后缀。
- MinIO：`STORAGE_PROVIDER=minio`，endpoint 是 MinIO 服务地址，通常设置 `S3_PATH_STYLE_ACCESS=true`。

S3 兼容实现要求服务端支持 Signature V4 的预签名 PUT、GET、HEAD 和 DELETE。若服务商未开启或未完整兼容这些能力，后端会返回明确的存储不可用/对象验证失败，不会写入假成功附件。

阿里云 OSS 示例（杭州地域）：

```powershell
$env:STORAGE_PROVIDER = 'aliyun-oss'
$env:S3_ENDPOINT = 'https://s3.oss-cn-hangzhou.aliyuncs.com'
$env:S3_REGION = 'aws-global'
$env:S3_ACCESS_KEY = '<RAM AccessKey ID>'
$env:S3_SECRET_KEY = '<RAM AccessKey Secret>'
$env:S3_BUCKET = '<private-bucket>'
$env:S3_PATH_STYLE_ACCESS = 'false'
```

建议使用单独 RAM 用户，不使用主账号 AccessKey；最小权限应限制到目标 bucket 和 `flashback/*` 前缀，并允许 PutObject、GetObject、HeadObject/读取元数据及 DeleteObject。

## 切换行为

1. 修改 `STORAGE_PROVIDER` 及对应 provider 环境变量。
2. 重启后端。
3. 新上传使用新的 active provider。
4. 已有附件继续根据数据库中的 `storage_provider` 路由。

若旧附件仍需访问或删除，必须保留旧 provider 的凭证配置；只删除旧凭证不会迁移远端对象。跨云对象搬迁不属于 M4 的自动切换范围。

## 安全要求

- 真实 AK/SK 只放在忽略提交的本地启动脚本、环境变量或部署侧 secret 中。
- 不要把真实密钥写入 `application.yml`、前端 `.env` 或任何 tracked file。
- bucket 必须保持私有；媒体访问由后端签发短时 URL。
- 切换后先用草稿上传一张小图片，完成上传、commit、访问 URL、删除四步验收，再迁移正式使用。
