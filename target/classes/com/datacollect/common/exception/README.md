# 自定义异常类说明

本项目定义了自定义异常类来替代直接抛出RuntimeException，提供更好的异常分类和处理机制。

## 异常类列表

### 1. CollectTaskException - 采集任务异常
用于处理采集任务相关的业务异常。

**错误码示例：**
- `COLLECT_TASK_CREATE_FAILED` - 采集任务创建失败
- `COLLECT_STRATEGY_NOT_FOUND` - 采集策略不存在
- `TEST_CASE_INSTANCE_SAVE_FAILED` - 保存用例执行例次失败
- `LOGIC_ENVIRONMENT_EMPTY` - 逻辑环境列表为空
- `EXECUTOR_IP_NOT_FOUND` - 未找到可用的执行机IP
- `COLLECT_TASK_SAVE_FAILED` - 采集任务保存失败

**使用示例：**
```java
// 简单异常
throw new CollectTaskException("COLLECT_STRATEGY_NOT_FOUND", "采集策略不存在");

// 带原因的异常
throw new CollectTaskException("COLLECT_TASK_CREATE_FAILED", "处理采集任务创建失败: " + e.getMessage(), e);
```

### 2. ConfigurationException - 配置异常
用于处理配置相关的异常。

**错误码示例：**
- `FILE_UPLOAD_DIR_INIT_FAILED` - 初始化文件上传目录失败
- `TEMP_DIR_CREATE_FAILED` - 创建临时目录失败

**使用示例：**
```java
// 带原因的异常
throw new ConfigurationException("FILE_UPLOAD_DIR_INIT_FAILED", "初始化文件上传目录失败", e);
```

## 异常处理

所有自定义异常都会在`GlobalExceptionHandler`中被捕获并转换为统一的响应格式：

```java
@ExceptionHandler(CollectTaskException.class)
public Result<String> handleCollectTaskException(CollectTaskException e) {
    log.error("采集任务异常: {}", e.getMessage(), e);
    return Result.error("采集任务异常: " + e.getMessage());
}
```

## 最佳实践

1. **使用有意义的错误码**：错误码应该能够清楚地标识异常类型
2. **提供详细的错误消息**：错误消息应该包含足够的信息帮助调试
3. **保留原始异常**：使用带cause的构造函数来保留原始异常信息
4. **避免直接抛出RuntimeException**：始终使用自定义异常类

## 添加新的异常类

如果需要添加新的异常类，请：

1. 继承自RuntimeException
2. 提供多个构造函数支持不同的使用场景
3. 在GlobalExceptionHandler中添加对应的异常处理方法
4. 更新此文档
