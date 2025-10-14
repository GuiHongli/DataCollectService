# Excel公式处理修复说明

## 问题描述

导入用例集Excel文件后，用例名称、用例编号、操作步骤等字段显示为`CONCATENATE()`开头的公式文本，而不是公式计算后的实际值。

### 问题原因

在 `ExcelParser.java` 的 `getCellValue()` 方法中，当遇到公式类型的单元格时，代码直接返回了公式文本而不是计算后的值：

```java
case FORMULA: {
    return cell.getCellFormula();  // ❌ 返回公式文本，如 "CONCATENATE(...)"
}
```

## 修复方案

使用 Apache POI 提供的 `FormulaEvaluator` 来计算公式并获取结果值。

### 代码改动

#### 1. 添加导入

```java
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
```

#### 2. 在 `parseTestCaseExcel()` 方法中创建公式计算器

```java
Sheet sheet = workbook.getSheetAt(0);
FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
```

#### 3. 修改方法签名，传递 `evaluator` 参数

- `parseRow(Row row, Long testCaseSetId, FormulaEvaluator evaluator)`
- `buildTestCaseFromRow(Row row, Long testCaseSetId, FormulaEvaluator evaluator)`
- `getCellValue(Cell cell, FormulaEvaluator evaluator)`
- `isEmptyRow(Row row, FormulaEvaluator evaluator)`

#### 4. 实现公式值获取逻辑

```java
case FORMULA: {
    // 计算公式并获取结果值
    return getFormulaValue(cell, evaluator);
}
```

新增 `getFormulaValue()` 方法：

```java
private String getFormulaValue(Cell cell, FormulaEvaluator evaluator) {
    try {
        CellType resultType = evaluator.evaluateFormulaCell(cell);
        switch (resultType) {
            case STRING: {
                return cell.getStringCellValue().trim();
            }
            case NUMERIC: {
                return formatNumericCellValue(cell);
            }
            case BOOLEAN: {
                return String.valueOf(cell.getBooleanCellValue());
            }
            default: {
                return null;
            }
        }
    } catch (Exception e) {
        log.warn("Failed to evaluate formula in cell {}: {}, using formula text instead", 
            cell.getAddress(), e.getMessage());
        return cell.getCellFormula();
    }
}
```

## 修复效果

- ✅ 正确解析 `CONCATENATE()` 等Excel公式
- ✅ 返回公式计算后的实际值
- ✅ 支持字符串、数值、布尔等各种类型的公式结果
- ✅ 公式计算失败时降级使用公式文本并记录警告日志

## 测试建议

1. 准备包含以下公式的测试Excel文件：
   - `CONCATENATE()` 函数
   - `CONCAT()` 函数
   - 字符串拼接公式（如 `A1&B1`）
   - 数值计算公式
   
2. 上传用例集文件并验证：
   - 用例名称正确显示
   - 用例编号正确显示
   - 操作步骤正确显示
   - 其他字段正确显示

## 修改文件

- `DataCollectService/src/main/java/com/datacollect/util/ExcelParser.java`

## 修改时间

2024-10-14

