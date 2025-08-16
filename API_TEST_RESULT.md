# 用例执行结果接口文档

## 概述

DataCollectService 提供用例执行结果的接收和查询接口，用于接收CaseExecuteService上报的用例执行结果。

## 接口列表

### 1. 接收用例执行结果

#### 接口信息
- **URL**: `POST /test-result/report`
- **描述**: 接收用例执行结果并保存到数据库
- **Content-Type**: `application/json`

#### 请求参数

| 参数名 | 类型 | 必填 | 描述 |
|--------|------|------|------|
| taskId | String | 是 | 任务ID |
| testCaseId | Long | 是 | 用例ID |
| round | Integer | 是 | 轮次 |
| status | String | 是 | 执行状态 (SUCCESS/FAILED/TIMEOUT) |
| result | String | 否 | 执行结果描述 |
| executionTime | Long | 否 | 执行耗时（毫秒） |
| startTime | LocalDateTime | 否 | 开始时间 |
| endTime | LocalDateTime | 否 | 结束时间 |
| errorMessage | String | 否 | 错误信息 |
| executorIp | String | 是 | 执行机IP |
| testCaseSetId | Long | 是 | 用例集ID |

#### 请求示例

```bash
curl -X POST "http://localhost:8080/test-result/report" \
  -H "Content-Type: application/json" \
  -d @test_case_execution_result_example.json
```

#### 请求数据示例

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
  "errorMessage": null,
  "executorIp": "192.168.1.100",
  "testCaseSetId": 1
}
```

#### 响应示例

```json
{
  "code": 200,
  "message": "用例执行结果接收成功",
  "data": {
    "taskId": "TASK_20240101_001",
    "testCaseId": 1,
    "round": 1,
    "status": "SUCCESS",
    "message": "用例执行结果接收成功",
    "timestamp": 1704067200000
  },
  "timestamp": 1704067200000
}
```

### 2. 根据任务ID查询执行结果

#### 接口信息
- **URL**: `GET /test-result/task/{taskId}`
- **描述**: 根据任务ID查询所有执行结果
- **参数**: taskId (路径参数)

#### 请求示例

```bash
curl -X GET "http://localhost:8080/test-result/task/TASK_20240101_001"
```

#### 响应示例

```json
{
  "code": 200,
  "message": "查询成功",
  "data": [
    {
      "id": 1,
      "taskId": "TASK_20240101_001",
      "testCaseId": 1,
      "round": 1,
      "status": "SUCCESS",
      "result": "用例执行成功",
      "executionTime": 15000,
      "startTime": "2024-01-01T10:00:00",
      "endTime": "2024-01-01T10:00:15",
      "errorMessage": null,
      "executorIp": "192.168.1.100",
      "testCaseSetId": 1,
      "createTime": "2024-01-01T10:00:15",
      "updateTime": "2024-01-01T10:00:15"
    }
  ],
  "timestamp": 1704067200000
}
```

### 3. 根据用例ID查询执行结果

#### 接口信息
- **URL**: `GET /test-result/testcase/{testCaseId}`
- **描述**: 根据用例ID查询所有执行结果
- **参数**: testCaseId (路径参数)

#### 请求示例

```bash
curl -X GET "http://localhost:8080/test-result/testcase/1"
```

#### 响应示例

```json
{
  "code": 200,
  "message": "查询成功",
  "data": [
    {
      "id": 1,
      "taskId": "TASK_20240101_001",
      "testCaseId": 1,
      "round": 1,
      "status": "SUCCESS",
      "result": "用例执行成功",
      "executionTime": 15000,
      "startTime": "2024-01-01T10:00:00",
      "endTime": "2024-01-01T10:00:15",
      "errorMessage": null,
      "executorIp": "192.168.1.100",
      "testCaseSetId": 1,
      "createTime": "2024-01-01T10:00:15",
      "updateTime": "2024-01-01T10:00:15"
    },
    {
      "id": 2,
      "taskId": "TASK_20240101_001",
      "testCaseId": 1,
      "round": 2,
      "status": "FAILED",
      "result": "用例执行失败",
      "executionTime": 8000,
      "startTime": "2024-01-01T10:01:00",
      "endTime": "2024-01-01T10:01:08",
      "errorMessage": "网络连接超时",
      "executorIp": "192.168.1.100",
      "testCaseSetId": 1,
      "createTime": "2024-01-01T10:01:08",
      "updateTime": "2024-01-01T10:01:08"
    }
  ],
  "timestamp": 1704067200000
}
```

## 数据库表结构

### test_case_execution_result 表

| 字段名 | 类型 | 说明 |
|--------|------|------|
| id | bigint | 主键ID |
| task_id | varchar(100) | 任务ID |
| test_case_id | bigint | 用例ID |
| round | int | 轮次 |
| status | varchar(20) | 执行状态 |
| result | text | 执行结果描述 |
| execution_time | bigint | 执行耗时（毫秒） |
| start_time | datetime | 开始时间 |
| end_time | datetime | 结束时间 |
| error_message | text | 错误信息 |
| executor_ip | varchar(50) | 执行机IP |
| test_case_set_id | bigint | 用例集ID |
| create_time | datetime | 创建时间 |
| update_time | datetime | 更新时间 |

## 使用流程

### 1. 创建数据库表
```sql
-- 执行SQL脚本创建表
source create_test_case_execution_result_table.sql
```

### 2. 启动服务
```bash
mvn spring-boot:run
```

### 3. 测试接口
```bash
# 上报执行结果
curl -X POST "http://localhost:8080/test-result/report" \
  -H "Content-Type: application/json" \
  -d @test_case_execution_result_example.json

# 查询任务结果
curl -X GET "http://localhost:8080/test-result/task/TASK_20240101_001"

# 查询用例结果
curl -X GET "http://localhost:8080/test-result/testcase/1"
```

## 注意事项

1. **数据验证**: 接口会对必填字段进行验证
2. **时间格式**: 时间字段使用ISO 8601格式 (yyyy-MM-ddTHH:mm:ss)
3. **状态值**: status字段只能是 SUCCESS、FAILED、TIMEOUT 之一
4. **性能**: 查询接口支持分页，建议大量数据时使用分页查询
5. **日志**: 所有操作都会记录详细日志，便于问题排查
