# TestCaseInfo增加testCaseNumber字段

## 修改概述

为`TestCaseExecutionRequest.TestCaseInfo`类增加了`testCaseNumber`字段，用于在执行机服务调用时传递用例编号信息。同时修改了DataCollectService和CaseExecuteService两个服务，确保DTO结构一致。

## 修改内容

### 1. DataCollectService DTO类修改

**文件**: `DataCollectService/src/main/java/com/datacollect/dto/TestCaseExecutionRequest.java`

在`TestCaseInfo`内部类中增加了`testCaseNumber`字段：

```java
@Data
public static class TestCaseInfo {
    
    /**
     * 用例ID
     */
    @NotNull(message = "用例ID不能为空")
    private Long testCaseId;
    
    /**
     * 用例编号
     */
    private String testCaseNumber;
    
    /**
     * 轮次
     */
    @NotNull(message = "轮次不能为空")
    private Integer round;
}
```

### 2. DataCollectService 服务层修改

**文件**: `DataCollectService/src/main/java/com/datacollect/service/impl/CollectTaskProcessServiceImpl.java`

在构建`TestCaseInfo`对象时，增加了获取和设置用例编号的逻辑：

```java
// 构建用例列表
List<TestCaseExecutionRequest.TestCaseInfo> testCaseList = new ArrayList<>();
for (TestCaseExecutionInstance instance : instances) {
    // 获取用例编号
    TestCase testCase = testCaseService.getById(instance.getTestCaseId());
    if (testCase != null) {
        TestCaseExecutionRequest.TestCaseInfo testCaseInfo = 
            new TestCaseExecutionRequest.TestCaseInfo();
        testCaseInfo.setTestCaseId(instance.getTestCaseId());
        testCaseInfo.setTestCaseNumber(testCase.getNumber());
        testCaseInfo.setRound(instance.getRound());
        testCaseList.add(testCaseInfo);
    } else {
        log.warn("用例不存在 - 用例ID: {}", instance.getTestCaseId());
    }
}
```

### 3. CaseExecuteService DTO类修改

**文件**: `CaseExecuteService/src/main/java/com/caseexecute/dto/TestCaseExecutionRequest.java`

在`TestCaseInfo`内部类中增加了`testCaseNumber`字段：

```java
@Data
public static class TestCaseInfo {
    
    /**
     * 用例ID
     */
    @NotNull(message = "用例ID不能为空")
    private Long testCaseId;
    
    /**
     * 用例编号
     */
    private String testCaseNumber;
    
    /**
     * 轮次
     */
    @NotNull(message = "轮次不能为空")
    private Integer round;
}
```

### 4. CaseExecuteService 服务层修改

**文件**: `CaseExecuteService/src/main/java/com/caseexecute/service/impl/TestCaseExecutionServiceImpl.java`

修改了用例执行逻辑，优先使用用例编号查找脚本文件：

```java
// 查找用例脚本文件 - 优先使用用例编号，如果没有则使用用例ID
String scriptFileName = testCase.getTestCaseNumber() != null && !testCase.getTestCaseNumber().trim().isEmpty() 
        ? testCase.getTestCaseNumber() + ".py" 
        : testCase.getTestCaseId() + ".py";
Path scriptPath = extractPath.resolve("cases").resolve(scriptFileName);
```

同时更新了日志记录，包含用例编号信息：

```java
log.info("开始执行用例 - 用例ID: {}, 用例编号: {}, 轮次: {}", 
        testCase.getTestCaseId(), testCase.getTestCaseNumber(), testCase.getRound());
```

## 功能说明

### 1. 用例编号获取
- 通过`testCaseService.getById()`获取用例详细信息
- 从用例实体中提取`number`字段作为用例编号
- 支持错误处理，当用例不存在时记录警告日志

### 2. 数据传递
- 用例编号会随同用例ID和轮次一起传递给CaseExecuteService
- 执行机可以根据用例编号进行更精确的用例识别和处理

### 3. 脚本文件查找
- CaseExecuteService优先使用用例编号查找脚本文件（如：TC001.py）
- 如果用例编号为空，则回退使用用例ID查找脚本文件
- 支持更灵活的脚本文件命名和映射

### 4. 数据验证
根据数据库查询结果，用例编号数据正确：

| 用例ID | 用例名称 | 用例编号 |
|--------|----------|----------|
| 41 | 4G网络连接测试 | TC001 |
| 42 | 5G网络性能测试 | TC002 |
| 43 | WiFi连接稳定性测试 | TC003 |
| 44 | 多网络环境切换测试 | TC004 |
| 45 | 弱网环境测试 | TC005 |

## 优势

1. **更精确的用例识别**: 执行机可以通过用例编号更准确地识别要执行的用例
2. **更好的日志记录**: 在日志中可以使用用例编号进行更清晰的标识
3. **支持用例编号映射**: 执行机可以根据用例编号找到对应的脚本文件
4. **向后兼容**: 新增字段为可选字段，不影响现有功能
5. **灵活的脚本文件查找**: 支持用例编号和用例ID两种方式查找脚本文件
6. **服务间一致性**: DataCollectService和CaseExecuteService的DTO结构保持一致

## 使用场景

1. **脚本文件命名**: 执行机可以根据用例编号命名脚本文件（如：TC001.py）
2. **日志标识**: 在日志中使用用例编号进行更清晰的标识
3. **结果关联**: 执行结果可以与用例编号进行关联
4. **报告生成**: 在生成执行报告时使用用例编号进行标识
5. **脚本文件映射**: 支持用例编号到脚本文件的直接映射
6. **调试和排查**: 通过用例编号更容易定位和排查问题

## 注意事项

1. **用例编号唯一性**: 确保用例编号在用例集中是唯一的
2. **脚本文件映射**: 执行机需要根据用例编号找到对应的脚本文件
3. **错误处理**: 当用例不存在时，会跳过该用例并记录警告日志
4. **性能考虑**: 每个用例执行例次都需要查询用例信息，可能影响性能
5. **脚本文件命名**: 脚本文件应该使用用例编号命名（如：TC001.py）
6. **服务同步**: 确保DataCollectService和CaseExecuteService的修改同步部署
