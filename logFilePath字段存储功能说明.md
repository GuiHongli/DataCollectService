# logFilePath字段存储功能说明

## 功能概述

DataCollectService接收到用例执行结果时，会自动将`logFilePath`字段存储到数据库中，该字段包含日志文件的路径或HTTP链接。

## 功能特性

### 1. 字段存储
- 在`test_case_execution_result`表中存储`log_file_path`字段
- 在`test_case_execution_instance`表中存储`log_file_path`字段
- 支持存储本地文件路径或HTTP链接

### 2. 自动更新
- 接收到用例执行结果时自动更新相关字段
- 同时更新执行结果表和执行例次表
- 包含完整的日志记录

## 技术实现

### 1. 数据库表结构修改

#### test_case_execution_result表
```sql
-- 添加日志文件路径字段
ALTER TABLE test_case_execution_result 
ADD COLUMN log_file_path VARCHAR(500) COMMENT '日志文件路径或HTTP链接' AFTER failure_reason;

-- 添加索引
ALTER TABLE test_case_execution_result 
ADD INDEX idx_log_file_path (log_file_path);
```

#### test_case_execution_instance表
```sql
-- 添加日志文件路径字段
ALTER TABLE test_case_execution_instance 
ADD COLUMN log_file_path VARCHAR(500) COMMENT '日志文件路径或HTTP链接' AFTER failure_reason;

-- 添加索引
ALTER TABLE test_case_execution_instance 
ADD INDEX idx_log_file_path (log_file_path);
```

### 2. 实体类修改

#### TestCaseExecutionResult实体类
```java
/**
 * 日志文件路径或HTTP链接
 */
private String logFilePath;
```

#### TestCaseExecutionInstance实体类
```java
/**
 * 日志文件路径或HTTP链接
 */
private String logFilePath;
```

### 3. DTO类修改

#### TestCaseExecutionResult DTO类
```java
/**
 * 日志文件路径或HTTP链接
 */
private String logFilePath;
```

### 4. 服务层修改

#### TestCaseExecutionResultController
```java
// 记录日志文件信息
if (result.getLogFilePath() != null && !result.getLogFilePath().trim().isEmpty()) {
    log.info("用例执行结果包含日志文件 - 任务ID: {}, 用例ID: {}, 轮次: {}, 日志文件: {}", 
            result.getTaskId(), result.getTestCaseId(), result.getRound(), result.getLogFilePath());
}

// 返回结果中包含日志文件路径
data.put("logFilePath", result.getLogFilePath());
```

#### TestCaseExecutionInstanceService
```java
/**
 * 根据用例ID和轮次更新执行状态、结果、失败原因和日志文件路径
 */
boolean updateExecutionStatusAndResultAndFailureReasonAndLogFilePathByTestCaseAndRound(
    Long collectTaskId, Long testCaseId, Integer round, String status, 
    String result, String failureReason, String logFilePath);
```

### 5. CaseExecuteService修改

#### PythonExecutionResult类
```java
// 字段名从logFileName改为logFilePath
private String logFilePath;

// Builder方法
public Builder logFilePath(String logFilePath) {
    executionResult.logFilePath = logFilePath;
    return this;
}

// Getter方法
public String getLogFilePath() { return logFilePath; }
```

#### PythonExecutorUtil
```java
// 返回结果时使用logFilePath
return PythonExecutionResult.builder()
        .status(status)
        .result(result)
        .executionTime(executionTime)
        .startTime(startTime)
        .endTime(endTime)
        .logContent(logContent)
        .logFilePath(uploadedLogUrl != null ? uploadedLogUrl : logFileName)
        .failureReason(failureReason)
        .build();
```

## 数据流程

### 1. 用例执行阶段
1. CaseExecuteService执行Python脚本
2. 生成日志文件并保存到本地
3. 上传日志文件到gohttpserver（如果配置了地址）
4. 生成包含日志文件路径的执行结果

### 2. 结果上报阶段
1. CaseExecuteService上报执行结果到DataCollectService
2. 结果中包含`logFilePath`字段（本地路径或HTTP链接）

### 3. 数据存储阶段
1. DataCollectService接收执行结果
2. 保存到`test_case_execution_result`表
3. 同时更新`test_case_execution_instance`表
4. 记录详细的日志信息

## 字段内容说明

### logFilePath字段可能包含的内容

#### 1. 本地文件路径
```
/Users/zhengtengsong/projects/ghl/cursor/DataCollectService/CaseExecuteService/logs/1_1.log
```

#### 2. HTTP链接（上传到gohttpserver后）
```
http://localhost:8000/upload/1_1.log
```

#### 3. 相对路径
```
logs/1_1.log
```

## 使用示例

### 1. 接收执行结果
```json
{
  "taskId": "TASK_20240101_001",
  "testCaseId": 1,
  "round": 1,
  "status": "SUCCESS",
  "result": "用例执行成功",
  "executionTime": 15000,
  "startTime": "2024-01-01T10:00:00",
  "endTime": "2024-01-01T10:00:15",
  "failureReason": null,
  "logFilePath": "http://localhost:8000/upload/1_1.log",
  "executorIp": "192.168.1.100",
  "testCaseSetId": 1
}
```

### 2. 响应结果
```json
{
  "code": 200,
  "message": "用例执行结果接收成功",
  "data": {
    "taskId": "TASK_20240101_001",
    "testCaseId": 1,
    "round": 1,
    "status": "SUCCESS",
    "logFilePath": "http://localhost:8000/upload/1_1.log",
    "message": "用例执行结果接收成功",
    "timestamp": 1704067200000
  }
}
```

## 注意事项

1. **字段长度**: `log_file_path`字段设置为VARCHAR(500)，足够存储大多数路径和URL
2. **索引优化**: 为`log_file_path`字段添加了索引，提高查询性能
3. **空值处理**: 如果`logFilePath`为空或null，不会影响其他字段的存储
4. **日志记录**: 所有操作都有详细的日志记录，便于问题排查

## 相关文件

### SQL脚本
- `add_log_file_name.sql`: 为test_case_execution_result表添加logFilePath字段
- `add_log_file_name_to_instance.sql`: 为test_case_execution_instance表添加logFilePath字段

### Java文件
- `TestCaseExecutionResult.java`: 执行结果实体类
- `TestCaseExecutionResult.java` (DTO): 执行结果DTO类
- `TestCaseExecutionInstance.java`: 执行例次实体类
- `TestCaseExecutionResultController.java`: 执行结果控制器
- `TestCaseExecutionResultServiceImpl.java`: 执行结果服务实现
- `TestCaseExecutionInstanceServiceImpl.java`: 执行例次服务实现

### CaseExecuteService文件
- `PythonExecutorUtil.java`: Python执行工具类
- `PythonExecutionResult.java`: Python执行结果类
