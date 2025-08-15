# 可用逻辑环境匹配功能实现

## 功能概述

在采集任务创建页面，用户选择采集策略和环境编排筛选条件后，系统会自动展示可用的逻辑环境列表。该功能实现了智能匹配逻辑，确保用户能够选择到最适合的逻辑环境来执行采集任务。

## 核心匹配逻辑

### 1. 环境组网列表A的提取
- 解析采集策略中关联用例集的所有测试用例
- 提取每个测试用例的`logic_network`字段
- 将多个环境组网名称（用分号分隔）拆分为独立的组网名称
- 去重后形成环境组网列表A

### 2. 逻辑环境列表B的获取
- 基于用户选择的地域筛选条件（地域、国家、省份、城市）
- 获取符合条件的执行机列表
- 获取这些执行机关联的所有逻辑环境
- 形成逻辑环境列表B

### 3. 匹配筛选
- 遍历逻辑环境列表B中的每个逻辑环境
- 获取该逻辑环境关联的所有环境组网
- 检查是否有任何环境组网存在于环境组网列表A中
- 如果存在匹配，则将该逻辑环境标记为可用

## 技术实现

### 后端实现

#### 1. API接口
```java
@GetMapping("/available-logic-environments")
public Result<List<LogicEnvironmentDTO>> getAvailableLogicEnvironments(
    @RequestParam @NotNull Long strategyId,
    @RequestParam(required = false) Long regionId,
    @RequestParam(required = false) Long countryId,
    @RequestParam(required = false) Long provinceId,
    @RequestParam(required = false) Long cityId)
```

#### 2. 核心服务方法
- `ExecutorService.getExecutorsByRegion()` - 根据地域条件获取执行机
- `LogicEnvironmentService.getByExecutorId()` - 根据执行机ID获取逻辑环境
- `LogicEnvironmentNetworkService.getByLogicEnvironmentId()` - 获取逻辑环境关联的网络
- `LogicEnvironmentService.getLogicEnvironmentDTO()` - 获取逻辑环境详细信息

#### 3. 数据模型
- `CollectStrategy` - 采集策略，关联用例集
- `TestCase` - 测试用例，包含环境组网需求
- `LogicEnvironment` - 逻辑环境，关联执行机
- `LogicEnvironmentNetwork` - 逻辑环境与网络组网的关联
- `LogicNetwork` - 逻辑组网定义

### 前端实现

#### 1. 页面结构
- 步骤2：采集策略选择
- 步骤3：环境编排筛选
- 可用逻辑环境列表展示

#### 2. 交互逻辑
- 策略选择后自动触发环境匹配
- 地域筛选条件变化时重新匹配
- 实时显示匹配结果

#### 3. 展示组件
- 逻辑环境卡片展示
- 执行机信息显示
- UE设备列表
- 环境组网标签

## 数据库设计

### 核心表结构

#### 1. 测试用例表 (test_case)
```sql
CREATE TABLE `test_case` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `test_case_set_id` bigint(20) NOT NULL,
  `name` varchar(200) NOT NULL,
  `code` varchar(100) NOT NULL,
  `logic_network` varchar(100) DEFAULT NULL, -- 环境组网需求
  `test_steps` text,
  `expected_result` text,
  PRIMARY KEY (`id`)
);
```

#### 2. 逻辑环境表 (logic_environment)
```sql
CREATE TABLE `logic_environment` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `executor_id` bigint(20) NOT NULL,
  `description` text,
  `status` tinyint(4) DEFAULT '1',
  PRIMARY KEY (`id`)
);
```

#### 3. 逻辑组网表 (logic_network)
```sql
CREATE TABLE `logic_network` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `description` text,
  PRIMARY KEY (`id`)
);
```

#### 4. 逻辑环境组网关联表 (logic_environment_network)
```sql
CREATE TABLE `logic_environment_network` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `logic_environment_id` bigint(20) NOT NULL,
  `logic_network_id` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_logic_environment_network` (`logic_environment_id`, `logic_network_id`)
);
```

## 使用流程

### 1. 用户操作步骤
1. 打开采集任务创建页面
2. 填写基本信息（任务名称、描述）
3. 选择采集策略
4. 配置环境筛选条件（地域、国家、省份、城市）
5. 查看可用的逻辑环境列表
6. 确认任务配置并创建

### 2. 系统处理流程
1. 用户选择策略后，系统解析策略关联的用例集
2. 提取用例集中所有测试用例的环境组网需求
3. 用户配置地域筛选条件后，系统获取符合条件的执行机
4. 获取执行机关联的逻辑环境
5. 匹配逻辑环境的环境组网与测试用例需求
6. 返回匹配的逻辑环境列表

## 配置说明

### 1. 环境组网配置
- 在`logic_network`表中定义各种环境组网
- 常见的组网类型：4G标准网络、4G弱网环境、5G高速网络、5G低延迟网络、WiFi标准网络、WiFi弱网环境

### 2. 测试用例配置
- 在测试用例的`logic_network`字段中指定所需的环境组网
- 多个组网用分号(;)分隔，如："4G标准网络;4G弱网环境"

### 3. 逻辑环境配置
- 创建逻辑环境并关联到执行机
- 通过`logic_environment_network`表关联环境组网

## 测试验证

### 1. 功能测试
- 执行`test_data_for_available_environments.sql`插入测试数据
- 按照`test_available_environments.md`中的步骤进行测试

### 2. 测试场景
- 完全匹配：逻辑环境的环境组网完全满足测试用例需求
- 部分匹配：逻辑环境的环境组网部分满足测试用例需求
- 无匹配：逻辑环境的环境组网不满足测试用例需求
- 地域筛选：验证地域筛选条件是否正确应用

### 3. 性能测试
- 大量数据场景下的响应时间
- 并发请求的处理能力

## 注意事项

### 1. 数据一致性
- 确保测试用例的环境组网名称与逻辑组网表中的名称完全一致
- 注意大小写敏感性

### 2. 性能优化
- 对于大量数据，考虑添加适当的索引
- 可以考虑缓存常用的匹配结果

### 3. 错误处理
- 处理策略不存在的情况
- 处理网络请求异常
- 处理数据为空的情况

### 4. 扩展性
- 支持新增环境组网类型
- 支持更复杂的匹配规则
- 支持多策略组合匹配

## 相关文件

- `CollectTaskController.java` - API接口实现
- `ExecutorService.java` - 执行机服务接口
- `LogicEnvironmentService.java` - 逻辑环境服务接口
- `LogicEnvironmentNetworkService.java` - 逻辑环境网络关联服务接口
- `collect-task/index.vue` - 前端页面实现
- `test_data_for_available_environments.sql` - 测试数据脚本
- `test_available_environments.md` - 测试文档
