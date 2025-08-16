# test_case表字段重命名总结

## 修改概述
将`test_case`表的`code`字段重命名为`number`字段，以更好地表达字段含义。

## 修改内容

### 1. 数据库表结构修改
- **表名**: `test_case`
- **字段名**: `code` → `number`
- **字段类型**: `varchar(100) NOT NULL COMMENT '用例编号'`
- **索引**: `idx_code` → `idx_number`

### 2. Java实体类修改
**文件**: `DataCollectService/src/main/java/com/datacollect/entity/TestCase.java`
- 字段名: `code` → `number`
- 注解: `@TableField("code")` → `@TableField("number")`

### 3. DTO类修改
**文件**: `DataCollectService/src/main/java/com/datacollect/dto/CollectStrategyDTO.java`
- `TestCaseInfo`类中的字段: `code` → `number`

### 4. 服务类修改
**文件**: `DataCollectService/src/main/java/com/datacollect/service/impl/CollectStrategyServiceImpl.java`
- 方法调用: `testCase.getCode()` → `testCase.getNumber()`

**文件**: `DataCollectService/src/main/java/com/datacollect/service/impl/TestCaseServiceImpl.java`
- 排序字段: `orderByAsc("code")` → `orderByAsc("number")`

### 5. 控制器修改
**文件**: `DataCollectService/src/main/java/com/datacollect/controller/CollectTaskController.java`
- 日志输出: `testCase.getCode()` → `testCase.getNumber()`

### 6. 工具类修改
**文件**: `DataCollectService/src/main/java/com/datacollect/util/ExcelParser.java`
- 字段设置: `testCase.setCode()` → `testCase.setNumber()`
- 字段验证: `testCase.getCode()` → `testCase.getNumber()`

### 7. 前端代码修改
**文件**: `DataCollectFront/src/views/collect-strategy/index.vue`
- 模板引用: `testCase.code` → `testCase.number`

**文件**: `DataCollectFront/src/views/test-case-set/detail.vue`
- 表格列: `prop="code"` → `prop="number"`

### 8. SQL文件修改
**文件**: `DataCollectService/src/main/resources/db/add_test_case.sql`
- 表结构定义中的字段名和索引名

## 验证结果
- 数据库字段重命名成功
- 索引更新成功
- 数据完整性保持
- 所有相关代码已更新

## 注意事项
1. 此修改为向后不兼容的变更
2. 如果有其他系统或脚本依赖`code`字段，需要相应更新
3. 建议在部署前进行完整的测试验证
