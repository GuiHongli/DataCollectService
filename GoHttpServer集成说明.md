# GoHttpServer集成说明

## 概述

本系统已集成gohttpserver功能，在用例集上传时会自动将文件同步上传到gohttpserver，提供文件访问服务。

## 功能特性

### 1. 自动上传
- 用例集上传时自动同步到gohttpserver
- 支持ZIP文件上传
- 生成唯一的文件访问URL

### 2. 文件管理
- 支持文件上传、删除
- 支持目录上传
- 文件访问URL管理

### 3. 配置管理
- 可配置gohttpserver地址
- 可配置存储目录
- 支持健康检查

## 配置说明

### application.yml配置
```yaml
# gohttpserver配置
gohttpserver:
  url: http://localhost:8081          # gohttpserver服务地址
  upload-path: /upload                # 上传路径
  storage-dir: ./storage              # 存储目录
```

### 环境变量配置
```bash
# 可以通过环境变量覆盖配置
export GOHTTPSERVER_URL=http://localhost:8081
export GOHTTPSERVER_STORAGE_DIR=./storage
```

## API接口

### 1. 获取gohttpserver配置
```
GET /api/test-case-set/gohttpserver/config
```

**响应示例：**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": "GoHttpServer配置 - URL: http://localhost:8081, 存储目录: ./storage, 状态: 可用"
}
```

### 2. 手动上传文件到gohttpserver
```
POST /api/test-case-set/gohttpserver/upload
Content-Type: multipart/form-data

参数：
- file: 要上传的文件
- targetFileName: 目标文件名
```

**响应示例：**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": "http://localhost:8081/test_file.zip"
}
```

### 3. 上传用例集（自动集成gohttpserver）
```
POST /api/test-case-set/upload
Content-Type: multipart/form-data

参数：
- file: ZIP文件
- description: 描述信息
```

**响应示例：**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 4,
    "name": "网络测试用例集",
    "version": "v1.0",
    "filePath": "uploads/testcase/uuid.zip",
    "gohttpserverUrl": "http://localhost:8081/uuid.zip",
    "fileSize": 8245,
    "description": "测试用例集，包含gohttpserver集成",
    "status": 1
  }
}
```

## 数据库结构

### test_case_set表新增字段
```sql
ALTER TABLE `test_case_set` 
ADD COLUMN `gohttpserver_url` varchar(500) DEFAULT NULL COMMENT 'gohttpserver文件访问URL' 
AFTER `file_path`;
```

## 使用流程

### 1. 启动gohttpserver
```bash
# 启动gohttpserver服务
gohttpserver -p 8081 -r ./storage
```

### 2. 配置应用
确保application.yml中的gohttpserver配置正确：
```yaml
gohttpserver:
  url: http://localhost:8081
  storage-dir: ./storage
```

### 3. 上传用例集
通过API上传用例集，系统会自动：
1. 保存文件到本地
2. 上传文件到gohttpserver
3. 保存gohttpserver URL到数据库
4. 解析Excel文件内容
5. 保存测试用例数据

### 4. 访问文件
通过gohttpserver URL访问文件：
```
http://localhost:8081/filename.zip
```

## 错误处理

### 1. gohttpserver不可用
- 系统会记录警告日志
- 不影响主要业务流程
- 用例集仍可正常上传和解析

### 2. 文件上传失败
- 记录错误日志
- 返回错误信息
- 不影响数据库操作

### 3. 配置错误
- 使用默认配置
- 记录配置错误日志

## 测试脚本

### 运行测试
```bash
cd DataCollectService
python3 test_gohttpserver_integration.py
```

### 测试内容
1. 获取gohttpserver配置
2. 直接上传文件到gohttpserver
3. 上传用例集（包含gohttpserver集成）
4. 获取用例集列表

## 监控和日志

### 日志级别
```yaml
logging:
  level:
    com.datacollect.util.GoHttpServerClient: debug
```

### 关键日志
- 文件上传开始/成功/失败
- gohttpserver状态检查
- 配置信息加载

## 扩展功能

### 1. 文件版本管理
- 支持文件版本控制
- 自动清理旧版本文件

### 2. 访问权限控制
- 支持文件访问权限设置
- 支持用户认证

### 3. 文件同步
- 支持多gohttpserver实例
- 支持文件同步机制

## 注意事项

1. **存储空间**：确保gohttpserver存储目录有足够空间
2. **网络连接**：确保应用能访问gohttpserver服务
3. **文件权限**：确保应用有读写存储目录的权限
4. **并发处理**：支持并发文件上传
5. **错误恢复**：gohttpserver故障不影响主要功能

## 故障排除

### 常见问题

1. **gohttpserver连接失败**
   - 检查gohttpserver服务是否启动
   - 检查网络连接
   - 检查防火墙设置

2. **文件上传失败**
   - 检查存储目录权限
   - 检查磁盘空间
   - 检查文件格式

3. **配置错误**
   - 检查application.yml配置
   - 检查环境变量
   - 查看启动日志

### 调试方法

1. **查看日志**
   ```bash
   tail -f logs/application.log
   ```

2. **检查配置**
   ```bash
   curl http://localhost:8080/api/test-case-set/gohttpserver/config
   ```

3. **测试连接**
   ```bash
   curl http://localhost:8081/
   ```
