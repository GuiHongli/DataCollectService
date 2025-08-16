# 用例集路径获取修复

## 问题描述

在调用CaseExecuteService时，`testCaseSetPath`字段被硬编码为`"http://localhost:8000/upload/testcase.zip"`，没有使用真实的用例集路径。

## 解决方案

### 1. 获取用例集信息
从采集任务中获取用例集ID，然后查询用例集的详细信息：

```java
// 获取用例集详细信息
TestCaseSet testCaseSet = testCaseSetService.getById(testCaseSetId);
if (testCaseSet != null) {
    // 优先使用gohttpserver URL，如果没有则使用本地文件路径
    testCaseSetPath = testCaseSet.getGohttpserverUrl();
    if (testCaseSetPath == null || testCaseSetPath.trim().isEmpty()) {
        testCaseSetPath = testCaseSet.getFilePath();
    }
    log.info("获取到用例集路径 - 用例集ID: {}, 路径: {}", testCaseSetId, testCaseSetPath);
}
```

### 2. 路径优先级策略
- **第一优先级**: `gohttpserver_url` - 如果存在且不为空，使用HTTP URL
- **第二优先级**: `file_path` - 如果gohttpserver URL不存在，使用本地文件路径

### 3. 错误处理
- 检查用例集是否存在
- 检查用例集路径是否为空
- 记录详细的日志信息

## 数据验证

根据数据库查询结果，用例集路径获取逻辑正确：

| 用例集ID | 名称 | file_path | gohttpserver_url | 最终使用的路径 |
|---------|------|-----------|------------------|---------------|
| 1 | 短视频采集 | /uploads/testcase/短视频采集_v0.1.zip | NULL | /uploads/testcase/短视频采集_v0.1.zip |
| 17 | 正确地址测试 | /Users/.../正确地址测试_v1.0.zip | http://localhost:8000/upload/正确地址测试_v1.0.zip | http://localhost:8000/upload/正确地址测试_v1.0.zip |
| 21 | 修复测试 | /Users/.../修复测试_v1.0.zip | http://localhost:8000/upload/修复测试_v1.0.zip | http://localhost:8000/upload/修复测试_v1.0.zip |

## 技术实现

### 1. 依赖注入
添加了`TestCaseSetService`的依赖注入：

```java
@Autowired
private TestCaseSetService testCaseSetService;
```

### 2. 导入语句
添加了必要的导入：

```java
import com.datacollect.entity.TestCaseSet;
import com.datacollect.service.TestCaseSetService;
```

### 3. 路径验证
在发送HTTP请求前验证路径的有效性：

```java
if (testCaseSetPath == null || testCaseSetPath.trim().isEmpty()) {
    log.error("无法获取用例集路径 - 用例集ID: {}, 执行机IP: {}", testCaseSetId, executorIp);
    return false;
}
```

## 优势

1. **动态路径获取**: 不再硬编码路径，而是从数据库动态获取
2. **灵活的策略**: 支持HTTP URL和本地文件路径两种方式
3. **错误处理**: 完善的错误检查和日志记录
4. **向后兼容**: 对于没有gohttpserver URL的用例集，仍然可以使用本地路径

## 注意事项

1. **网络访问**: 如果使用gohttpserver URL，需要确保执行机能够访问该URL
2. **文件权限**: 如果使用本地文件路径，需要确保执行机有访问权限
3. **路径格式**: 确保路径格式符合CaseExecuteService的要求
