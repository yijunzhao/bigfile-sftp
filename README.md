# Bigfile SFTP

Bigfile SFTP 是一个面向大文件同步场景的 Web 工具，前端基于 Vue 3，后端基于 Java 17、Spring Boot 3.5、Apache Camel、H2 和 AWS S3 SDK 实现。项目支持从源 SFTP 服务器同步文件到本地目录、远程 SFTP 服务器或 S3/MinIO 对象存储。

## 模块说明

```text
bigfile-sftp
├── bigfile-sftp-frontend   # Vue 3 前端页面
├── bigfile-sftp-backend    # Spring Boot 后端接口与同步逻辑
└── README.md               # 项目说明文档
```

## 主要功能

- 源端支持 SFTP 服务器绝对路径扫描与同步。
- 目标端支持三种类型：
  - 本地文件同步路径
  - 远程 SFTP 服务
  - S3/MinIO 对象存储
- 支持文件级任务队列和可配置并发文件数。
- 支持全局总传输限速，多个并发文件共享同一个限速值。
- 支持大文件流式传输，避免将完整文件加载到 JVM 内存。
- 本地/SFTP 目标支持 `.part` 临时文件写入和断点续传。
- S3/MinIO 目标支持 Multipart Upload 断点续传。
- 支持 H2 文件数据库保存页面配置和 S3 Multipart 状态。
- 前端页面展示远端目录扫描结果、同步结果和失败文件列表。

## 技术栈

### 前端

- Vue 3
- Vite
- 原生 Fetch 调用后端接口

### 后端

- Java 17
- Spring Boot 3.5
- Spring Web
- Spring Data JPA
- H2 文件数据库
- Apache Camel SFTP/File
- AWS SDK for Java v2 S3，用于 MinIO/S3 Multipart Upload

## 环境要求

- JDK 17+
- Maven 3.8+
- Node.js 22.18+ 或满足前端 `package.json` 中的 engines 要求
- 可访问的源 SFTP 服务器
- 可选：目标 SFTP 服务器或 MinIO/S3 服务

## 数据库说明

后端默认使用 H2 文件数据库，数据库文件默认位于：

```text
bigfile-sftp-backend/db/bigfile-sftp.mv.db
```

如果需要自定义数据库文件位置，可以设置环境变量或 JVM 系统属性：

```text
BIGFILE_SFTP_DB_PATH
```

例如：

```powershell
$env:BIGFILE_SFTP_DB_PATH='D:\data\bigfile-sftp'
mvn spring-boot:run
```

H2 控制台地址：

```text
http://localhost:8080/h2-console
```

默认连接信息：

```text
JDBC URL: jdbc:h2:file:实际数据库路径
User Name: sa
Password: 空
```

## 后端运行

进入后端模块：

```powershell
cd bigfile-sftp-backend
```

构建：

```powershell
mvn -DskipTests package
```

启动：

```powershell
mvn spring-boot:run
```

默认后端地址：

```text
http://localhost:8080
```

## 前端运行

进入前端模块：

```powershell
cd bigfile-sftp-frontend
```

安装依赖：

```powershell
npm install
```

启动开发服务：

```powershell
npm run dev
```

默认前端地址：

```text
http://localhost:5173
```

前端开发环境已通过 Vite 代理将 `/api` 转发到：

```text
http://localhost:8080
```

## 页面配置说明

### 源 SFTP 配置

- `SFTP服务器地址`：源 SFTP 服务器 IP 或域名。
- `SFTP服务器端口`：源 SFTP 端口，通常为 `22`。
- `用户名`：源 SFTP 用户名。
- `密码`：源 SFTP 密码。
- `SFTP文件路径`：源 SFTP 服务器上的绝对路径，例如 `/tzdata/bigfile/`。

### 同步控制配置

- `总传输限速（MB/s）`：所有并发文件共享的总限速，支持整数或小数，例如 `5`、`0.3`；留空表示不限速。
- `并发文件数`：同时同步的文件数量，范围 `1-16`。

限速换算规则：

```text
1 MB/s = 1024 KB/s = 1048576 B/s
```

例如：

```text
5 MB/s = 5120 KB/s = 5242880 B/s
0.3 MB/s = 307.2 KB/s ≈ 314573 B/s
```

### 目标类型：本地文件同步路径

选择 `本地文件同步路径` 后填写：

- `文件同步路径`：后端服务所在机器可访问的本地目录，例如 `D:\sftp-sync` 或 `/data/sftp-sync`。

同步链路：

```text
源 SFTP -> 后端服务本地目录
```

### 目标类型：远程 SFTP 服务

选择 `远程SFTP服务` 后填写：

- `目标SFTP服务器地址`
- `目标SFTP服务器端口`
- `目标用户名`
- `目标密码`
- `目标SFTP文件路径`：目标服务器绝对路径，例如 `/data/bigfile/`。

同步链路：

```text
源 SFTP -> 后端流式转发 -> 目标 SFTP
```

### 目标类型：S3/MinIO 对象存储

选择 `S3/MinIO对象存储` 后填写：

- `S3 Endpoint`：MinIO 或 S3 兼容服务地址，例如 `http://192.168.2.150:9000`。
- `S3 Access Key`
- `S3 Secret Key`
- `S3 Bucket`
- `S3 Region`：MinIO 通常可填写 `us-east-1`。
- `S3对象前缀`：对象 key 前缀，例如 `backup/bigfile/`，可留空。
- `MinIO Path Style Access`：MinIO 通常需要勾选。

同步链路：

```text
源 SFTP -> 后端分片读取 -> S3/MinIO Multipart Upload
```

## 操作步骤

1. 启动后端服务。
2. 启动前端页面。
3. 在页面填写源 SFTP 配置。
4. 填写总传输限速和并发文件数。
5. 选择目标类型并填写目标配置。
6. 点击 `保存`，将配置写入 H2 数据库。
7. 点击 `数据同步`，开始同步文件。
8. 在页面查看远端目录扫描、已同步文件和失败文件。

## 大文件与断点续传说明

### 本地/SFTP 目标

本地和远程 SFTP 目标使用 `.part` 临时文件机制：

```text
filename.part  # 同步中或中断后的临时文件
filename       # 完整同步成功后的正式文件
```

如果同步中断，再次同步时会：

1. 检查目标端正式文件大小。
2. 检查目标端 `.part` 文件大小。
3. 如果 `.part` 小于源文件大小，则从源 SFTP 对应 offset 继续读取。
4. 将剩余内容追加写入 `.part`。
5. `.part` 大小达到源文件大小后重命名为正式文件。

### S3/MinIO 目标

S3/MinIO 使用标准 Multipart Upload：

1. 后端创建 Multipart Upload，获得 `uploadId`。
2. 按分片上传文件内容。
3. 每个分片上传成功后，将 `partNumber`、`offset`、`size`、`eTag` 写入 H2。
4. 服务中断后再次同步时，复用未完成的 `uploadId`，跳过已上传分片。
5. 所有分片完成后调用 Complete Multipart Upload，生成最终对象。

相关状态表：

```text
s3_multipart_upload
s3_multipart_part
```

## 注意事项

- 源 SFTP 路径和目标 SFTP 路径均建议填写绝对路径。
- 如果使用 root 用户，后端会自动将绝对路径换算为 Camel 可消费的相对目录，避免错误落到 `/root` 下。
- `并发文件数` 控制的是同时传输的文件数量，不限制目标目录中 `.part` 文件的数量。
- `.part` 文件表示未完成或待续传文件，不建议手动删除，除非确认要重新传输。
- S3/MinIO 目标不使用 `.part` 对象，而是使用 Multipart Upload 状态表续传。
- 总传输限速是全局限速，所有并发文件共享该速度。
- 大文件同步时，请确保目标磁盘或对象存储空间充足。

## 常见问题

### H2 数据库文件在哪里？

默认在：

```text
bigfile-sftp-backend/db/bigfile-sftp.mv.db
```

### `.part` 文件为什么没有立刻变成正式文件？

通常是文件还没有完整同步。后端会比较 `.part` 文件大小和源文件大小，只有大小达到源文件大小时才会重命名。

### 并发文件数为 2，为什么目录里有多个 `.part` 文件？

并发文件数限制的是同时传输的文件数量，不是 `.part` 文件总数。多个 `.part` 表示多个文件曾经开始同步但尚未完成。

### S3/MinIO 同步中断后会重新上传吗？

不会从头上传已完成分片。后端会从 H2 表中读取已上传分片，继续上传未完成分片。

### 控制台出现 known_hosts 日志正常吗？

正常。表示 Camel/JSch 使用当前用户的 SSH known hosts 文件或自动记录目标主机指纹。

## 构建命令汇总

后端：

```powershell
cd bigfile-sftp-backend
mvn -DskipTests package
mvn spring-boot:run
```

前端：

```powershell
cd bigfile-sftp-frontend
npm install
npm run dev
```

生产构建前端：

```powershell
cd bigfile-sftp-frontend
npm run build
```
