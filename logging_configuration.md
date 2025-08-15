# 可用逻辑环境匹配功能日志配置

## 概述

为了便于调试和监控可用逻辑环境匹配功能，系统添加了详细的过程日志。本文档说明如何配置和查看这些日志。

## 日志级别说明

### INFO级别日志
- 记录主要步骤的执行情况
- 显示关键数据统计信息
- 适合生产环境监控

### DEBUG级别日志
- 记录详细的处理过程
- 显示具体的数据内容
- 适合开发和调试环境

### WARN级别日志
- 记录警告信息
- 不影响功能但需要注意的情况

### ERROR级别日志
- 记录错误信息
- 功能执行失败的情况

## 日志内容

### 1. 控制器层日志 (CollectTaskController)

#### 开始处理
```
开始获取可用逻辑环境列表 - 策略ID: 1, 地域筛选: regionId=1, countryId=null, provinceId=null, cityId=null
```

#### 步骤1：获取采集策略
```
步骤1: 获取采集策略信息 - 策略ID: 1
获取到采集策略: 北京Android性能监控 (用例集ID: 1)
```

#### 步骤2：获取测试用例
```
步骤2: 获取策略关联的测试用例 - 用例集ID: 1
获取到测试用例数量: 5
```

#### 步骤3：提取环境组网需求
```
步骤3: 提取测试用例中的环境组网需求
处理测试用例: 4G网络连接测试 (编号: TC001)
测试用例 TC001 的环境组网需求: 4G标准网络;4G弱网环境
添加环境组网需求: 4G标准网络
添加环境组网需求: 4G弱网环境
提取的环境组网需求列表A: [4G标准网络, 4G弱网环境, 5G高速网络, 5G低延迟网络, WiFi标准网络, WiFi弱网环境]
```

#### 步骤4：获取执行机
```
步骤4: 根据地域筛选条件获取执行机 - regionId=1, countryId=null, provinceId=null, cityId=null
获取到符合条件的执行机数量: 2
匹配的执行机: 执行机-北京-01 (ID: 1, IP: 192.168.1.100)
匹配的执行机: 执行机-北京-02 (ID: 2, IP: 192.168.1.101)
```

#### 步骤5：获取逻辑环境
```
步骤5: 获取执行机关联的逻辑环境
获取执行机 执行机-北京-01 关联的逻辑环境
执行机 执行机-北京-01 关联的逻辑环境数量: 2
执行机 执行机-北京-01 关联的逻辑环境: 北京-Android环境 (ID: 1)
执行机 执行机-北京-01 关联的逻辑环境: 北京-iOS环境 (ID: 2)
获取到所有逻辑环境数量: 2
```

#### 步骤6：筛选匹配
```
步骤6: 筛选匹配的逻辑环境
检查逻辑环境: 北京-Android环境 (ID: 1)
逻辑环境 北京-Android环境 关联的环境组网数量: 2
找到匹配的环境组网: 4G标准网络 (逻辑环境: 北京-Android环境)
逻辑环境 北京-Android环境 的环境组网: [4G标准网络, 4G弱网环境]
逻辑环境 北京-Android环境 匹配成功，添加到可用列表
匹配完成 - 可用逻辑环境数量: 1
可用逻辑环境: 北京-Android环境 (ID: 1)
```

### 2. 执行机服务日志 (ExecutorServiceImpl)

#### 地域筛选过程
```
开始根据地域条件获取执行机 - regionId=1, countryId=null, provinceId=null, cityId=null
按地域筛选执行机 - regionId: 1
地域 1 下的国家数量: 2, 国家ID列表: [2, 3]
国家 2 下的省份数量: 1
省份 4 下的城市数量: 2, 城市ID列表: [5, 6]
国家 3 下的省份数量: 1
省份 7 下的城市数量: 2, 城市ID列表: [8, 9]
地域 1 下所有城市数量: 4, 城市ID列表: [5, 6, 8, 9]
根据地域条件获取到执行机数量: 2
匹配的执行机: 执行机-北京-01 (ID: 1, IP: 192.168.1.100, 地域ID: 2)
匹配的执行机: 执行机-北京-02 (ID: 2, IP: 192.168.1.101, 地域ID: 2)
```

### 3. 逻辑环境服务日志 (LogicEnvironmentServiceImpl)

#### 获取执行机关联的逻辑环境
```
获取执行机关联的逻辑环境 - 执行机ID: 1
执行机 1 关联的逻辑环境数量: 2
执行机 1 关联的逻辑环境: 北京-Android环境 (ID: 1)
执行机 1 关联的逻辑环境: 北京-iOS环境 (ID: 2)
```

#### 获取逻辑环境详细信息
```
获取逻辑环境详细信息 - 逻辑环境ID: 1
获取到逻辑环境详细信息: 北京-Android环境 (ID: 1)
```

### 4. 逻辑环境网络服务日志 (LogicEnvironmentNetworkServiceImpl)

#### 获取逻辑环境关联的网络组网
```
获取逻辑环境关联的网络组网 - 逻辑环境ID: 1
逻辑环境 1 关联的网络组网数量: 2
逻辑环境 1 关联的网络组网ID: 1
逻辑环境 1 关联的网络组网ID: 2
```

## 日志配置

### 1. application.yml配置

```yaml
logging:
  level:
    com.datacollect.controller.CollectTaskController: DEBUG
    com.datacollect.service.impl.ExecutorServiceImpl: DEBUG
    com.datacollect.service.impl.LogicEnvironmentServiceImpl: DEBUG
    com.datacollect.service.impl.LogicEnvironmentNetworkServiceImpl: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/data-collect-service.log
    max-size: 100MB
    max-history: 30
```

### 2. 开发环境配置

```yaml
logging:
  level:
    com.datacollect: DEBUG
  pattern:
    console: "%d{HH:mm:ss.SSS} %-5level %logger{36} - %msg%n"
```

### 3. 生产环境配置

```yaml
logging:
  level:
    com.datacollect.controller.CollectTaskController: INFO
    com.datacollect.service.impl.ExecutorServiceImpl: INFO
    com.datacollect.service.impl.LogicEnvironmentServiceImpl: INFO
    com.datacollect.service.impl.LogicEnvironmentNetworkServiceImpl: INFO
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
```

## 日志查看方法

### 1. 控制台查看
```bash
# 启动应用时查看实时日志
java -jar data-collect-service.jar

# 或者使用Spring Boot Maven插件
mvn spring-boot:run
```

### 2. 文件查看
```bash
# 查看日志文件
tail -f logs/data-collect-service.log

# 查看特定功能的日志
grep "可用逻辑环境" logs/data-collect-service.log

# 查看特定策略的日志
grep "策略ID: 1" logs/data-collect-service.log
```

### 3. 日志分析

#### 查找匹配过程
```bash
# 查看完整的匹配过程
grep -A 20 "开始获取可用逻辑环境列表" logs/data-collect-service.log

# 查看环境组网需求提取
grep "环境组网需求列表A" logs/data-collect-service.log

# 查看执行机匹配结果
grep "获取到符合条件的执行机数量" logs/data-collect-service.log

# 查看逻辑环境匹配结果
grep "匹配完成" logs/data-collect-service.log
```

#### 性能分析
```bash
# 统计匹配耗时
grep "开始获取可用逻辑环境列表\|匹配完成" logs/data-collect-service.log | awk '{print $1, $2, $NF}'
```

## 常见问题排查

### 1. 没有找到匹配的逻辑环境

检查日志中的以下信息：
- 环境组网需求列表A是否为空
- 执行机数量是否为0
- 逻辑环境是否有关联的环境组网
- 环境组网名称是否完全匹配

### 2. 执行机筛选结果异常

检查日志中的以下信息：
- 地域筛选条件是否正确
- 地域层级关系是否正确
- 执行机的地域ID是否正确

### 3. 性能问题

检查日志中的以下信息：
- 测试用例数量是否过多
- 执行机数量是否过多
- 逻辑环境数量是否过多

## 日志清理

### 1. 自动清理
```yaml
logging:
  file:
    max-size: 100MB
    max-history: 30
```

### 2. 手动清理
```bash
# 清理30天前的日志
find logs/ -name "*.log" -mtime +30 -delete

# 清理大于100MB的日志文件
find logs/ -name "*.log" -size +100M -delete
```

## 注意事项

1. **生产环境**：建议将日志级别设置为INFO，避免产生过多DEBUG日志影响性能
2. **开发环境**：可以设置为DEBUG级别，便于调试和问题排查
3. **日志文件**：定期清理日志文件，避免占用过多磁盘空间
4. **敏感信息**：日志中可能包含IP地址等敏感信息，注意日志文件的安全管理
