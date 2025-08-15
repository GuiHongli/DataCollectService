# 可用逻辑环境匹配功能测试

## 功能概述

在采集任务创建页面，用户选择采集策略和环境编排筛选条件后，系统会自动展示可用的逻辑环境列表。

## 匹配逻辑

1. **解析采集策略中关联用例集的测试用例**，提取出涉及的多个环境组网列表，命名为环境组网列表A
2. **基于环境编排中选择的筛选条件**，获得到一批可用的执行机，再获取到执行机关联的一批逻辑环境，命名为逻辑环境列表B
3. **筛选逻辑环境列表B**，针对每一个逻辑环境，其对应的多个环境组网，只要存在于环境组网列表A中，则认为该逻辑环境可用

## 测试步骤

### 1. 准备测试数据

确保数据库中有以下测试数据：

#### 测试用例数据
```sql
-- 插入测试用例，包含环境组网信息
INSERT INTO `test_case` (`test_case_set_id`, `name`, `code`, `logic_network`, `test_steps`, `expected_result`) VALUES
(1, '4G网络连接测试', 'TC001', '4G标准网络;4G弱网环境', '测试步骤', '预期结果'),
(1, '5G网络性能测试', 'TC002', '5G高速网络;5G低延迟网络', '测试步骤', '预期结果'),
(1, 'WiFi连接稳定性测试', 'TC003', 'WiFi标准网络;WiFi弱网环境', '测试步骤', '预期结果');
```

#### 逻辑组网数据
```sql
-- 插入逻辑组网数据
INSERT INTO `logic_network` (`name`, `description`) VALUES
('4G标准网络', '4G标准网络环境'),
('4G弱网环境', '4G弱网环境'),
('5G高速网络', '5G高速网络环境'),
('5G低延迟网络', '5G低延迟网络环境'),
('WiFi标准网络', 'WiFi标准网络环境'),
('WiFi弱网环境', 'WiFi弱网环境');
```

#### 逻辑环境数据
```sql
-- 插入逻辑环境数据
INSERT INTO `logic_environment` (`name`, `executor_id`, `description`, `status`) VALUES
('北京-Android环境', 1, '北京地区Android测试环境', 1),
('北京-iOS环境', 1, '北京地区iOS测试环境', 1),
('上海-Android环境', 3, '上海地区Android测试环境', 1);
```

#### 逻辑环境组网关联数据
```sql
-- 关联逻辑环境和组网
INSERT INTO `logic_environment_network` (`logic_environment_id`, `logic_network_id`) VALUES
(1, 1), -- 北京-Android环境关联4G标准网络
(1, 2), -- 北京-Android环境关联4G弱网环境
(2, 5), -- 北京-iOS环境关联WiFi标准网络
(2, 6), -- 北京-iOS环境关联WiFi弱网环境
(3, 3), -- 上海-Android环境关联5G高速网络
(3, 4); -- 上海-Android环境关联5G低延迟网络
```

### 2. 前端测试

1. **打开采集任务创建页面**
   - 访问 `/collect-task` 页面
   - 点击"新增任务"按钮

2. **选择采集策略**
   - 在步骤2中选择一个采集策略
   - 验证策略详情是否正确显示

3. **配置环境筛选条件**
   - 在步骤3中选择地域筛选条件
   - 可以只选择地域，或者进一步选择国家、省份、城市

4. **验证可用逻辑环境列表**
   - 检查是否显示了可用的逻辑环境列表
   - 验证列表中的逻辑环境是否包含匹配的环境组网
   - 检查每个逻辑环境卡片是否显示了完整信息：
     - 逻辑环境名称
     - 执行机信息
     - UE设备列表
     - 环境组网列表

### 3. API测试

#### 测试API接口
```bash
# 测试获取可用逻辑环境列表
curl -X GET "http://localhost:8080/collect-task/available-logic-environments?strategyId=1&regionId=1" \
  -H "Content-Type: application/json"
```

#### 预期响应
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "北京-Android环境",
      "executorId": 1,
      "executorName": "执行机-北京-01",
      "executorIpAddress": "192.168.1.100",
      "executorRegionName": "中国 / 北京市",
      "description": "北京地区Android测试环境",
      "status": 1,
      "ueList": [
        {
          "id": 1,
          "ueId": "UE001",
          "name": "UE-Android-01",
          "purpose": "短视频测试",
          "networkTypeName": "正常网络"
        }
      ],
      "networkList": [
        {
          "id": 1,
          "name": "4G标准网络",
          "description": "4G标准网络环境"
        },
        {
          "id": 2,
          "name": "4G弱网环境",
          "description": "4G弱网环境"
        }
      ]
    }
  ]
}
```

## 测试场景

### 场景1：完全匹配
- **策略用例集**：包含"4G标准网络"和"4G弱网环境"
- **逻辑环境**：关联"4G标准网络"和"4G弱网环境"
- **预期结果**：该逻辑环境应该出现在可用列表中

### 场景2：部分匹配
- **策略用例集**：包含"4G标准网络"、"5G高速网络"、"WiFi标准网络"
- **逻辑环境**：只关联"4G标准网络"
- **预期结果**：该逻辑环境应该出现在可用列表中

### 场景3：无匹配
- **策略用例集**：包含"4G标准网络"和"5G高速网络"
- **逻辑环境**：只关联"WiFi标准网络"
- **预期结果**：该逻辑环境不应该出现在可用列表中

### 场景4：地域筛选
- **策略**：选择包含多种环境组网的策略
- **地域筛选**：选择特定地域
- **预期结果**：只显示该地域下执行机关联的逻辑环境

## 错误处理测试

1. **策略不存在**
   - 传入不存在的strategyId
   - 预期返回错误信息

2. **网络异常**
   - 模拟网络请求失败
   - 预期显示错误提示

3. **数据为空**
   - 没有匹配的逻辑环境
   - 预期显示空状态提示

## 性能测试

1. **大量数据测试**
   - 创建大量测试用例和逻辑环境
   - 验证响应时间是否在可接受范围内

2. **并发请求测试**
   - 同时发起多个请求
   - 验证系统稳定性

## 日志验证

### 1. 启用详细日志
在`application.yml`中配置日志级别为DEBUG：
```yaml
logging:
  level:
    com.datacollect.controller.CollectTaskController: DEBUG
    com.datacollect.service.impl.ExecutorServiceImpl: DEBUG
    com.datacollect.service.impl.LogicEnvironmentServiceImpl: DEBUG
    com.datacollect.service.impl.LogicEnvironmentNetworkServiceImpl: DEBUG
```

### 2. 验证日志输出
执行测试后，检查日志中是否包含以下关键信息：

#### 环境组网需求提取
```
提取的环境组网需求列表A: [4G标准网络, 4G弱网环境, 5G高速网络, 5G低延迟网络, WiFi标准网络, WiFi弱网环境]
```

#### 执行机匹配
```
获取到符合条件的执行机数量: 2
匹配的执行机: 执行机-北京-01 (ID: 1, IP: 192.168.1.100)
匹配的执行机: 执行机-北京-02 (ID: 2, IP: 192.168.1.101)
```

#### 逻辑环境匹配
```
逻辑环境 北京-Android环境 的环境组网: [4G标准网络, 4G弱网环境]
找到匹配的环境组网: 4G标准网络 (逻辑环境: 北京-Android环境)
逻辑环境 北京-Android环境 匹配成功，添加到可用列表
```

#### 最终结果
```
匹配完成 - 可用逻辑环境数量: 1
可用逻辑环境: 北京-Android环境 (ID: 1)
```

### 3. 日志分析命令
```bash
# 查看完整的匹配过程
grep -A 30 "开始获取可用逻辑环境列表" logs/data-collect-service.log

# 查看环境组网需求
grep "环境组网需求列表A" logs/data-collect-service.log

# 查看执行机匹配结果
grep "获取到符合条件的执行机数量" logs/data-collect-service.log

# 查看逻辑环境匹配结果
grep "匹配完成" logs/data-collect-service.log
```

## 注意事项

1. 确保数据库中的逻辑删除字段正确设置
2. 验证环境组网名称的匹配是否区分大小写
3. 检查地域筛选的级联逻辑是否正确
4. 确认前端显示的逻辑环境信息是否完整准确
5. 检查日志输出是否符合预期，便于问题排查
