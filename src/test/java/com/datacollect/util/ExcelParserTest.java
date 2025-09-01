package com.datacollect.util;

import com.datacollect.entity.TestCase;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({FileInputStream.class, XSSFWorkbook.class})
public class ExcelParserTest {

    private ExcelParser excelParser;
    private XSSFWorkbook mockWorkbook;
    private XSSFSheet mockSheet;

    @Before
    public void setUp() {
        excelParser = new ExcelParser();
        mockWorkbook = PowerMockito.mock(XSSFWorkbook.class);
        mockSheet = PowerMockito.mock(XSSFSheet.class);
    }

    @Test
    public void testParseTestCaseExcel_Success() throws Exception {
        // 准备测试数据
        String filePath = "test.xlsx";
        Long testCaseSetId = 1L;

        // Mock FileInputStream
        FileInputStream mockFis = PowerMockito.mock(FileInputStream.class);
        PowerMockito.whenNew(FileInputStream.class).withArguments(filePath).thenReturn(mockFis);

        // Mock XSSFWorkbook
        PowerMockito.whenNew(XSSFWorkbook.class).withArguments(mockFis).thenReturn(mockWorkbook);

        // Mock Sheet
        when(mockWorkbook.getSheetAt(0)).thenReturn(mockSheet);
        when(mockSheet.getLastRowNum()).thenReturn(2); // 3行数据（包括表头）

        // Mock Rows
        XSSFRow mockRow1 = createMockRow("测试用例1", "TC001", "逻辑组网1", "业务大类1", "App1", "测试步骤1", "预期结果1");
        XSSFRow mockRow2 = createMockRow("测试用例2", "TC002", "逻辑组网2", "业务大类2", "App2", "测试步骤2", "预期结果2");

        when(mockSheet.getRow(1)).thenReturn(mockRow1);
        when(mockSheet.getRow(2)).thenReturn(mockRow2);

        // 执行测试
        List<TestCase> result = excelParser.parseTestCaseExcel(filePath, testCaseSetId);

        // 验证结果
        assertNotNull(result);
        assertEquals(2, result.size());

        TestCase testCase1 = result.get(0);
        assertEquals("测试用例1", testCase1.getName());
        assertEquals("TC001", testCase1.getNumber());
        assertEquals("逻辑组网1", testCase1.getLogicNetwork());
        assertEquals("业务大类1", testCase1.getBusinessCategory());
        assertEquals("App1", testCase1.getApp());
        assertEquals("测试步骤1", testCase1.getTestSteps());
        assertEquals("预期结果1", testCase1.getExpectedResult());
        assertEquals(testCaseSetId, testCase1.getTestCaseSetId());

        TestCase testCase2 = result.get(1);
        assertEquals("测试用例2", testCase2.getName());
        assertEquals("TC002", testCase2.getNumber());
    }

    @Test
    public void testParseTestCaseExcel_EmptyFile() throws Exception {
        // 准备测试数据
        String filePath = "empty.xlsx";
        Long testCaseSetId = 1L;

        // Mock FileInputStream
        FileInputStream mockFis = PowerMockito.mock(FileInputStream.class);
        PowerMockito.whenNew(FileInputStream.class).withArguments(filePath).thenReturn(mockFis);

        // Mock XSSFWorkbook
        PowerMockito.whenNew(XSSFWorkbook.class).withArguments(mockFis).thenReturn(mockWorkbook);

        // Mock Sheet - 只有表头
        when(mockWorkbook.getSheetAt(0)).thenReturn(mockSheet);
        when(mockSheet.getLastRowNum()).thenReturn(0);

        // 执行测试
        List<TestCase> result = excelParser.parseTestCaseExcel(filePath, testCaseSetId);

        // 验证结果
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testParseTestCaseExcel_WithEmptyRows() throws Exception {
        // 准备测试数据
        String filePath = "with_empty_rows.xlsx";
        Long testCaseSetId = 1L;

        // Mock FileInputStream
        FileInputStream mockFis = PowerMockito.mock(FileInputStream.class);
        PowerMockito.whenNew(FileInputStream.class).withArguments(filePath).thenReturn(mockFis);

        // Mock XSSFWorkbook
        PowerMockito.whenNew(XSSFWorkbook.class).withArguments(mockFis).thenReturn(mockWorkbook);

        // Mock Sheet
        when(mockWorkbook.getSheetAt(0)).thenReturn(mockSheet);
        when(mockSheet.getLastRowNum()).thenReturn(3);

        // Mock Rows - 包含空行
        XSSFRow mockRow1 = createMockRow("测试用例1", "TC001", "逻辑组网1", "业务大类1", "App1", "测试步骤1", "预期结果1");
        XSSFRow mockRow2 = createEmptyMockRow(); // 空行
        XSSFRow mockRow3 = createMockRow("测试用例3", "TC003", "逻辑组网3", "业务大类3", "App3", "测试步骤3", "预期结果3");

        when(mockSheet.getRow(1)).thenReturn(mockRow1);
        when(mockSheet.getRow(2)).thenReturn(mockRow2);
        when(mockSheet.getRow(3)).thenReturn(mockRow3);

        // 执行测试
        List<TestCase> result = excelParser.parseTestCaseExcel(filePath, testCaseSetId);

        // 验证结果 - 应该跳过空行
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("测试用例1", result.get(0).getName());
        assertEquals("测试用例3", result.get(1).getName());
    }

    @Test
    public void testParseTestCaseExcel_WithInvalidRows() throws Exception {
        // 准备测试数据
        String filePath = "with_invalid_rows.xlsx";
        Long testCaseSetId = 1L;

        // Mock FileInputStream
        FileInputStream mockFis = PowerMockito.mock(FileInputStream.class);
        PowerMockito.whenNew(FileInputStream.class).withArguments(filePath).thenReturn(mockFis);

        // Mock XSSFWorkbook
        PowerMockito.whenNew(XSSFWorkbook.class).withArguments(mockFis).thenReturn(mockWorkbook);

        // Mock Sheet
        when(mockWorkbook.getSheetAt(0)).thenReturn(mockSheet);
        when(mockSheet.getLastRowNum()).thenReturn(2);

        // Mock Rows - 包含无效行（缺少必填字段）
        XSSFRow mockRow1 = createMockRow("测试用例1", "TC001", "逻辑组网1", "业务大类1", "App1", "测试步骤1", "预期结果1");
        XSSFRow mockRow2 = createMockRow("", "TC002", "逻辑组网2", "业务大类2", "App2", "测试步骤2", "预期结果2"); // 名称为空

        when(mockSheet.getRow(1)).thenReturn(mockRow1);
        when(mockSheet.getRow(2)).thenReturn(mockRow2);

        // 执行测试
        List<TestCase> result = excelParser.parseTestCaseExcel(filePath, testCaseSetId);

        // 验证结果 - 应该跳过无效行
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("测试用例1", result.get(0).getName());
    }

    @Test(expected = IOException.class)
    public void testParseTestCaseExcel_FileNotFound() throws Exception {
        // 准备测试数据
        String filePath = "nonexistent.xlsx";
        Long testCaseSetId = 1L;

        // Mock FileInputStream 抛出异常
        PowerMockito.whenNew(FileInputStream.class).withArguments(filePath).thenThrow(new IOException("File not found"));

        // 执行测试 - 应该抛出 IOException
        excelParser.parseTestCaseExcel(filePath, testCaseSetId);
    }

    // 辅助方法：创建包含数据的 Mock Row
    private XSSFRow createMockRow(String name, String number, String logicNetwork, String businessCategory, 
                                 String app, String testSteps, String expectedResult) {
        XSSFRow mockRow = PowerMockito.mock(XSSFRow.class);
        
        // Mock 单元格
        XSSFCell mockCell0 = createMockCell(name);
        XSSFCell mockCell1 = createMockCell(number);
        XSSFCell mockCell2 = createMockCell(logicNetwork);
        XSSFCell mockCell3 = createMockCell(businessCategory);
        XSSFCell mockCell4 = createMockCell(app);
        XSSFCell mockCell5 = createMockCell(testSteps);
        XSSFCell mockCell6 = createMockCell(expectedResult);

        when(mockRow.getCell(0)).thenReturn(mockCell0);
        when(mockRow.getCell(1)).thenReturn(mockCell1);
        when(mockRow.getCell(2)).thenReturn(mockCell2);
        when(mockRow.getCell(3)).thenReturn(mockCell3);
        when(mockRow.getCell(4)).thenReturn(mockCell4);
        when(mockRow.getCell(5)).thenReturn(mockCell5);
        when(mockRow.getCell(6)).thenReturn(mockCell6);

        return mockRow;
    }

    // 辅助方法：创建空数据的 Mock Row
    private XSSFRow createEmptyMockRow() {
        XSSFRow mockRow = PowerMockito.mock(XSSFRow.class);
        
        // Mock 空单元格
        for (int i = 0; i < 7; i++) {
            when(mockRow.getCell(i)).thenReturn(null);
        }

        return mockRow;
    }

    // 辅助方法：创建 Mock Cell
    private XSSFCell createMockCell(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        
        XSSFCell mockCell = PowerMockito.mock(XSSFCell.class);
        when(mockCell.getCellType()).thenReturn(CellType.STRING);
        when(mockCell.getStringCellValue()).thenReturn(value);
        
        return mockCell;
    }
}
