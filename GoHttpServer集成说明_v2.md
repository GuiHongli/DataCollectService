# GoHttpServer集成说明 v2.0

## 概述

本系统已升级gohttpserver集成方式，使用官方提供的HTTP上传接口，将所有测试用例集ZIP文件统一存储在`test_case_set`目录下。

## 功能特性

### 1. 官方接口集成
- 使用gohttpserver官方提供的HTTP上传接口
- 支持multipart/form-data格式的文件上传
- 自动创建`test_case_set`目录结构

### 2. 统一文件管理
- 所有测试用例集ZIP文件存储在`upload`目录
- 文件访问URL格式：`http://localhost:8000/upload/文件名.zip`
- 支持文件删除和健康检查

### 3. 自动上传
- 用例集上传时自动同步到gohttpserver
- 生成唯一的文件访问URL
- 支持上传失败重试和错误处理

## 配置说明

### 后端配置 (application.yml)

```yaml
# gohttpserver配置
gohttpserver:
  url: http://localhost:8000
```

### 前端显示

前端页面新增"GoHttpServer地址"列，支持：
- 显示文件访问链接
- 点击跳转到文件
- 复制URL到剪贴板
- 未上传状态显示

## API接口

### 1. 获取gohttpserver配置
```
GET /api/test-case-set/gohttpserver/config
```

### 2. 上传用例集（自动上传到gohttpserver）
```
POST /api/test-case-set/upload
Content-Type: multipart/form-data

参数：
- file: ZIP文件
- description: 描述信息
```

### 3. 手动上传到gohttpserver
```
POST /api/test-case-set/gohttpserver/upload
Content-Type: multipart/form-data

参数：
- file: ZIP文件
```

## 文件结构

```
gohttpserver根目录/
└── upload/
    ├── 用例集1_v1.0.zip
    ├── 用例集2_v2.0.zip
    └── ...
```

## 技术实现

### 1. HTTP客户端
- 使用Java 11+的HttpClient
- 支持连接超时和请求超时配置
- 自动处理multipart请求构建

### 2. 错误处理
- 网络连接异常处理
- HTTP状态码检查
- 文件上传失败重试机制

### 3. 日志记录
- 详细的上传过程日志
- 错误信息记录
- 性能监控

## 使用流程

### 1. 启动gohttpserver
```bash
# 启动gohttpserver服务
gohttpserver -p 8000 -r ./storage
```

### 2. 配置后端
确保`application.yml`中gohttpserver配置正确：
```yaml
gohttpserver:
  url: http://localhost:8081
```

### 3. 上传用例集
- 通过前端页面上传ZIP文件
- 系统自动解析文件并上传到gohttpserver
- 生成文件访问URL并保存到数据库

### 4. 访问文件
- 通过前端页面点击链接访问文件
- 或直接访问：`http://localhost:8000/upload/文件名.zip`

## 测试验证

### 1. 健康检查
```bash
curl http://localhost:8000/
```

### 2. 目录访问
```bash
curl http://localhost:8000/upload/
```

### 3. 文件上传测试
```bash
# 使用Python测试脚本
python3 test_gohttpserver_upload.py
```

## 故障排除

### 1. gohttpserver连接失败
- 检查gohttpserver是否启动
- 验证端口8081是否被占用
- 检查防火墙设置

### 2. 文件上传失败
- 检查网络连接
- 验证gohttpserver权限
- 查看后端日志错误信息

### 3. 文件访问异常
- 确认文件已成功上传
- 检查文件路径是否正确
- 验证gohttpserver配置

## 升级说明

### 从v1.0升级到v2.0
1. 更新后端代码
2. 修改配置文件
3. 重启后端服务
4. 测试上传功能

### 数据迁移
- 现有用例集数据保持不变
- 新上传的文件将使用新的存储方式
- 旧文件可通过手动重新上传迁移

## 注意事项

1. **文件大小限制**：确保gohttpserver支持上传文件大小
2. **并发处理**：大量并发上传时注意性能影响
3. **存储空间**：定期清理不需要的文件
4. **备份策略**：建议定期备份test_case_set目录

## 相关文件

- `GoHttpServerClient.java`: gohttpserver客户端实现
- `TestCaseSetController.java`: 用例集控制器
- `test_gohttpserver_upload.py`: 测试脚本
- `application.yml`: 配置文件
