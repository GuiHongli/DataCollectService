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

    private static final int COL_NAME = 0;
    private static final int COL_NUMBER = 1;
    private static final int COL_LOGIC_NETWORK = 2;
    private static final int COL_BUSINESS_CATEGORY = 3;
    private static final int COL_APP = 4;
    private static final int COL_TEST_STEPS = 5;
    private static final int COL_EXPECTED_RESULT = 6;
    private static final int EMPTY_ROW_CHECK_COLUMNS = 7; // 与解析列保持一致

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
                if (row == null) {
                    continue;
                }
                
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
            if (isEmptyRow(row)) {
                return null;
            }
            
            TestCase testCase = buildTestCaseFromRow(row, testCaseSetId);
            if (!validateRequiredFields(testCase, row)) {
                return null;
            }
            
            return testCase;
            
        } catch (Exception e) {
            log.error("解析第{}行数据时发生错误: {}", row.getRowNum() + 1, e.getMessage());
            return null;
        }
    }

    /**
     * 根据行构建测试用例实体
     */
    private TestCase buildTestCaseFromRow(Row row, Long testCaseSetId) {
        TestCase testCase = new TestCase();
        testCase.setTestCaseSetId(testCaseSetId);
        // 列顺序：用例_名称 用例_编号 用例_逻辑组网 用例_业务大类 用例_App 用例_测试步骤 用例_预期结果
        testCase.setName(getCellValue(row.getCell(COL_NAME)));
        testCase.setNumber(getCellValue(row.getCell(COL_NUMBER)));
        testCase.setLogicNetwork(getCellValue(row.getCell(COL_LOGIC_NETWORK)));
        testCase.setBusinessCategory(getCellValue(row.getCell(COL_BUSINESS_CATEGORY)));
        testCase.setApp(getCellValue(row.getCell(COL_APP)));
        testCase.setTestSteps(getCellValue(row.getCell(COL_TEST_STEPS)));
        testCase.setExpectedResult(getCellValue(row.getCell(COL_EXPECTED_RESULT)));
        return testCase;
    }

    /**
     * 验证必填字段
     */
    private boolean validateRequiredFields(TestCase testCase, Row row) {
        if (testCase.getName() == null || testCase.getName().trim().isEmpty()) {
            log.warn("第{}行用例名称为空，跳过该行", row.getRowNum() + 1);
            return false;
        }
        if (testCase.getNumber() == null || testCase.getNumber().trim().isEmpty()) {
            log.warn("第{}行用例编号为空，跳过该行", row.getRowNum() + 1);
            return false;
        }
        return true;
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
            case STRING: {
                return cell.getStringCellValue().trim();
            }
            case NUMERIC: {
                return formatNumericCellValue(cell);
            }
            case BOOLEAN: {
                return String.valueOf(cell.getBooleanCellValue());
            }
            case FORMULA: {
                return cell.getCellFormula();
            }
            default: {
                return null;
            }
        }
    }

    /**
     * 数值单元格转字符串，避免科学计数法
     */
    private String formatNumericCellValue(Cell cell) {
        if (DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue().toString();
        }
        double numericValue = cell.getNumericCellValue();
        if (numericValue == (long) numericValue) {
            return String.valueOf((long) numericValue);
        } else {
            return String.valueOf(numericValue);
        }
    }
    
    /**
     * 检查是否为空行
     * @param row 行数据
     * @return 是否为空行
     */
    private boolean isEmptyRow(Row row) {
        if (row == null) {
            return true;
        }
        
        for (int i = 0; i < EMPTY_ROW_CHECK_COLUMNS; i++) {
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
