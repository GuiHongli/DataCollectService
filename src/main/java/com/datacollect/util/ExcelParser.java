package com.datacollect.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import com.datacollect.entity.TestCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Component
public class ExcelParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExcelParser.class);

    private static final int COL_NAME = 0;
    private static final int COL_NUMBER = 1;
    private static final int COL_LOGIC_NETWORK = 2;
    private static final int COL_BUSINESS_CATEGORY = 3;
    private static final int COL_APP = 4;
    private static final int COL_APP_EN = 5;
    private static final int COL_MODEL_SCENARIO = 6;
    private static final int COL_PHONE_OS_TYPE = 7;
    private static final int COL_TEST_STEPS = 8;
    private static final int COL_EXPECTED_RESULT = 9;
    private static final int EMPTY_ROW_CHECK_COLUMNS = 10; // 与解析列保持一致

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
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator(); // 创建公式计算器
            
            // 跳过表头行，从第二行开始读取数据
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                
                TestCase testCase = parseRow(row, testCaseSetId, evaluator);
                if (testCase != null) {
                    testCases.add(testCase);
                }
            }
        }
        
        LOGGER.info("Successfully parsed Excel file, found {} test cases", testCases.size());
        return testCases;
    }
    
    /**
     * 解析单行数据
     * @param row 行数据
     * @param testCaseSetId 用例集ID
     * @param evaluator 公式计算器
     * @return 测试用例对象
     */
    private TestCase parseRow(Row row, Long testCaseSetId, FormulaEvaluator evaluator) {
        try {
            if (isEmptyRow(row, evaluator)) {
                return null;
            }
            
            TestCase testCase = buildTestCaseFromRow(row, testCaseSetId, evaluator);
            if (!validateRequiredFields(testCase, row)) {
                return null;
            }
            
            return testCase;
            
        } catch (Exception e) {
            LOGGER.error("Error parsing row {}: {}", row.getRowNum() + 1, e.getMessage());
            return null;
        }
    }

    /**
     * 根据行构建测试用例实体
     */
    private TestCase buildTestCaseFromRow(Row row, Long testCaseSetId, FormulaEvaluator evaluator) {
        TestCase testCase = new TestCase();
        testCase.setTestCaseSetId(testCaseSetId);
        // 列顺序：用例_名称 用例_编号 用例_逻辑组网 用例_业务大类 用例_App 用例_AppEn 用例_模型场景 用例_手机OS类 用例_操作步骤 用例_预期结果
        testCase.setName(getCellValue(row.getCell(COL_NAME), evaluator));
        testCase.setNumber(getCellValue(row.getCell(COL_NUMBER), evaluator));
        testCase.setLogicNetwork(getCellValue(row.getCell(COL_LOGIC_NETWORK), evaluator));
        testCase.setBusinessCategory(getCellValue(row.getCell(COL_BUSINESS_CATEGORY), evaluator));
        testCase.setApp(getCellValue(row.getCell(COL_APP), evaluator));
        testCase.setAppEn(getCellValue(row.getCell(COL_APP_EN), evaluator));
        testCase.setModelScenario(getCellValue(row.getCell(COL_MODEL_SCENARIO), evaluator));
        testCase.setPhoneOsType(getCellValue(row.getCell(COL_PHONE_OS_TYPE), evaluator));
        testCase.setTestSteps(getCellValue(row.getCell(COL_TEST_STEPS), evaluator));
        testCase.setExpectedResult(getCellValue(row.getCell(COL_EXPECTED_RESULT), evaluator));
        return testCase;
    }

    /**
     * 验证必填字段
     */
    private boolean validateRequiredFields(TestCase testCase, Row row) {
        if (testCase.getName() == null || testCase.getName().trim().isEmpty()) {
            LOGGER.warn("Row {} test case name is empty, skipping this row", row.getRowNum() + 1);
            return false;
        }
        if (testCase.getNumber() == null || testCase.getNumber().trim().isEmpty()) {
            LOGGER.warn("Row {} test case number is empty, skipping this row", row.getRowNum() + 1);
            return false;
        }
        return true;
    }
    
    /**
     * get单元格值
     * @param cell 单元格
     * @param evaluator 公式计算器
     * @return 单元格值字符串
     */
    private String getCellValue(Cell cell, FormulaEvaluator evaluator) {
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
                // 计算公式并get结果值
                return getFormulaValue(cell, evaluator);
            }
            default: {
                return null;
            }
        }
    }
    
    /**
     * get公式单元格的计算结果
     * @param cell 公式单元格
     * @param evaluator 公式计算器
     * @return 公式计算结果字符串
     */
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
            LOGGER.warn("Failed to evaluate formula in cell {}: {}, using formula text instead", 
                cell.getAddress(), e.getMessage());
            return cell.getCellFormula();
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
        if (Math.abs(numericValue - (long) numericValue) < 1e-10) {
            return String.valueOf((long) numericValue);
        } else {
            return String.valueOf(numericValue);
        }
    }
    
    /**
     * 检查是否为空行
     * @param row 行数据
     * @param evaluator 公式计算器
     * @return 是否为空行
     */
    private boolean isEmptyRow(Row row, FormulaEvaluator evaluator) {
        if (row == null) {
            return true;
        }
        
        for (int i = 0; i < EMPTY_ROW_CHECK_COLUMNS; i++) {
            Cell cell = row.getCell(i);
            if (cell != null) {
                String value = getCellValue(cell, evaluator);
                if (value != null && !value.trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }
}
