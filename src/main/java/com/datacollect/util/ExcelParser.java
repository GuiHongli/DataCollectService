package com.datacollect.util;

import com.datacollect.entity.TestCase;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class ExcelParser {

    /**
     * 解析Excel文件中的测试用例数据
     * @param filePath Excel文件路径
     * @param testCaseSetId 用例集ID
     * @return 测试用例列表
     */
    public List<TestCase> parseTestCaseExcel(String filePath, Long testCaseSetId) throws IOException {
        List<TestCase> testCases = new ArrayList<>();
        
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0); // 获取第一个工作表
            
            // 跳过表头行，从第二行开始读取数据
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                TestCase testCase = parseRow(row, testCaseSetId);
                if (testCase != null) {
                    testCases.add(testCase);
                }
            }
        }
        
        log.info("成功解析Excel文件，共解析到 {} 条测试用例", testCases.size());
        return testCases;
    }
    
    /**
     * 解析单行数据
     * @param row 行数据
     * @param testCaseSetId 用例集ID
     * @return 测试用例对象
     */
    private TestCase parseRow(Row row, Long testCaseSetId) {
        try {
            // 检查是否为空行
            if (isEmptyRow(row)) {
                return null;
            }
            
            TestCase testCase = new TestCase();
            testCase.setTestCaseSetId(testCaseSetId);
            
            // 解析各列数据
            // 列顺序：用例_名称 用例_编号 用例_逻辑组网 用例_业务大类 用例_App 用例_测试步骤 用例_预期结果
            testCase.setName(getCellValue(row.getCell(0)));
            testCase.setNumber(getCellValue(row.getCell(1)));
            testCase.setLogicNetwork(getCellValue(row.getCell(2)));
            testCase.setBusinessCategory(getCellValue(row.getCell(3)));
            testCase.setApp(getCellValue(row.getCell(4)));
            testCase.setTestSteps(getCellValue(row.getCell(5)));
            testCase.setExpectedResult(getCellValue(row.getCell(6)));
            
            // 验证必填字段
            if (testCase.getName() == null || testCase.getName().trim().isEmpty()) {
                log.warn("第{}行用例名称为空，跳过该行", row.getRowNum() + 1);
                return null;
            }
            
            if (testCase.getNumber() == null || testCase.getNumber().trim().isEmpty()) {
                log.warn("第{}行用例编号为空，跳过该行", row.getRowNum() + 1);
                return null;
            }
            
            return testCase;
            
        } catch (Exception e) {
            log.error("解析第{}行数据时发生错误: {}", row.getRowNum() + 1, e.getMessage());
            return null;
        }
    }
    
    /**
     * 获取单元格值
     * @param cell 单元格
     * @return 单元格值字符串
     */
    private String getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    // 避免科学计数法，转为整数
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == (long) numericValue) {
                        return String.valueOf((long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }
    
    /**
     * 检查是否为空行
     * @param row 行数据
     * @return 是否为空行
     */
    private boolean isEmptyRow(Row row) {
        if (row == null) return true;
        
        for (int i = 0; i < 5; i++) { // 检查前5列
            Cell cell = row.getCell(i);
            if (cell != null) {
                String value = getCellValue(cell);
                if (value != null && !value.trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }
}
