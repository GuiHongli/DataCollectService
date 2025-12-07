package com.datacollect.util;

import com.datacollect.dto.TaskInfoDTO;
import com.datacollect.entity.SpeedData;
import com.datacollect.entity.VmosData;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 端侧文件处理工具类
 * 负责解压端侧压缩包并解析taskinfo.json
 */
@Slf4j
public class ClientFileProcessor {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 解压端侧压缩包并解析taskinfo.json
     *
     * @param zipFilePath 压缩包文件路径
     * @return TaskInfoDTO对象，如果解析失败返回null
     * @throws IOException IO异常
     */
    public static TaskInfoDTO extractAndParseTaskInfo(String zipFilePath) throws IOException {
        log.info("开始解压端侧文件并解析taskinfo.json: {}", zipFilePath);

        Path zipPath = Paths.get(zipFilePath);
        if (!Files.exists(zipPath)) {
            throw new IOException("压缩包文件不存在: " + zipFilePath);
        }

        // 创建临时解压目录
        Path extractDir = Files.createTempDirectory("client_file_extract_");
        TaskInfoDTO taskInfo = null;

        try {
            // 解压文件
            extractZipFile(zipPath, extractDir);

            // 查找并解析taskinfo.json
            Path taskInfoPath = extractDir.resolve("taskinfo.json");
            if (!Files.exists(taskInfoPath)) {
                // 尝试在子目录中查找
                taskInfoPath = findTaskInfoFile(extractDir);
            }

            if (taskInfoPath != null && Files.exists(taskInfoPath)) {
                log.info("找到taskinfo.json文件: {}", taskInfoPath);
                taskInfo = parseTaskInfoJson(taskInfoPath);
                log.info("taskinfo.json解析成功: taskId={}", taskInfo != null ? taskInfo.getTaskId() : "null");
            } else {
                log.warn("未找到taskinfo.json文件");
            }

        } finally {
            // 清理临时目录
            try {
                deleteDirectory(extractDir);
                log.info("临时解压目录已清理: {}", extractDir);
            } catch (Exception e) {
                log.warn("清理临时目录失败: {}", e.getMessage());
            }
        }

        return taskInfo;
    }

    /**
     * 解压ZIP文件到指定目录
     *
     * @param zipPath ZIP文件路径
     * @param extractDir 解压目录
     * @throws IOException IO异常
     */
    private static void extractZipFile(Path zipPath, Path extractDir) throws IOException {
        log.info("解压ZIP文件: {} -> {}", zipPath, extractDir);

        try (FileInputStream fis = new FileInputStream(zipPath.toFile());
             ZipInputStream zis = new ZipInputStream(fis)) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path filePath = extractDir.resolve(entry.getName());

                if (entry.isDirectory()) {
                    Files.createDirectories(filePath);
                } else {
                    Files.createDirectories(filePath.getParent());
                    Files.copy(zis, filePath);
                }

                zis.closeEntry();
            }

            log.info("ZIP文件解压完成");
        }
    }

    /**
     * 递归查找taskinfo.json文件
     *
     * @param directory 搜索目录
     * @return taskinfo.json文件路径，如果未找到返回null
     */
    private static Path findTaskInfoFile(Path directory) throws IOException {
        return Files.walk(directory)
                .filter(path -> path.getFileName() != null && 
                        path.getFileName().toString().equalsIgnoreCase("taskinfo.json"))
                .findFirst()
                .orElse(null);
    }

    /**
     * 解析taskinfo.json文件
     *
     * @param taskInfoPath taskinfo.json文件路径
     * @return TaskInfoDTO对象
     * @throws IOException IO异常
     */
    private static TaskInfoDTO parseTaskInfoJson(Path taskInfoPath) throws IOException {
        try {
            String jsonContent = new String(Files.readAllBytes(taskInfoPath), "UTF-8");
            log.debug("taskinfo.json内容: {}", jsonContent);

            TaskInfoDTO taskInfo = objectMapper.readValue(jsonContent, TaskInfoDTO.class);
            return taskInfo;
        } catch (Exception e) {
            log.error("解析taskinfo.json失败: {}", e.getMessage(), e);
            throw new IOException("解析taskinfo.json失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解压端侧压缩包并解析speed-10s.csv
     *
     * @param zipFilePath 压缩包文件路径
     * @return SpeedData列表，如果解析失败返回空列表
     * @throws IOException IO异常
     */
    public static List<SpeedData> extractAndParseSpeedCsv(String zipFilePath) throws IOException {
        log.info("开始解压端侧文件并解析speed-10s.csv: {}", zipFilePath);

        Path zipPath = Paths.get(zipFilePath);
        if (!Files.exists(zipPath)) {
            throw new IOException("压缩包文件不存在: " + zipFilePath);
        }

        // 创建临时解压目录
        Path extractDir = Files.createTempDirectory("client_file_extract_");
        List<SpeedData> speedDataList = new ArrayList<>();

        try {
            // 解压文件
            extractZipFile(zipPath, extractDir);

            // 查找并解析speed-10s.csv
            Path speedCsvPath = extractDir.resolve("speed-10s.csv");
            if (!Files.exists(speedCsvPath)) {
                // 尝试在子目录中查找
                speedCsvPath = findSpeedCsvFile(extractDir);
            }

            if (speedCsvPath != null && Files.exists(speedCsvPath)) {
                log.info("找到speed-10s.csv文件: {}", speedCsvPath);
                speedDataList = parseSpeedCsv(speedCsvPath);
                log.info("speed-10s.csv解析成功: 共{}条记录", speedDataList.size());
            } else {
                log.warn("未找到speed-10s.csv文件");
            }

        } finally {
            // 清理临时目录
            try {
                deleteDirectory(extractDir);
                log.info("临时解压目录已清理: {}", extractDir);
            } catch (Exception e) {
                log.warn("清理临时目录失败: {}", e.getMessage());
            }
        }

        return speedDataList;
    }

    /**
     * 递归查找speed-10s.csv文件
     *
     * @param directory 搜索目录
     * @return speed-10s.csv文件路径，如果未找到返回null
     */
    private static Path findSpeedCsvFile(Path directory) throws IOException {
        return Files.walk(directory)
                .filter(path -> path.getFileName() != null && 
                        path.getFileName().toString().equalsIgnoreCase("speed-10s.csv"))
                .findFirst()
                .orElse(null);
    }

    /**
     * 解析speed-10s.csv文件
     *
     * @param csvPath CSV文件路径
     * @return SpeedData列表
     * @throws IOException IO异常
     */
    private static List<SpeedData> parseSpeedCsv(Path csvPath) throws IOException {
        List<SpeedData> speedDataList = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                // 跳过空行
                if (line.trim().isEmpty()) {
                    continue;
                }

                // 跳过表头
                if (isFirstLine) {
                    isFirstLine = false;
                    // 检查表头格式
                    if (line.toLowerCase().contains("dl_speed") || 
                        line.toLowerCase().contains("ul_speed") || 
                        line.toLowerCase().contains("total")) {
                        continue;
                    }
                }

                // 解析CSV行
                String[] values = parseCsvLine(line);
                if (values.length >= 3) {
                    SpeedData speedData = new SpeedData();
                    speedData.setDlSpeed(values[0].trim());
                    speedData.setUlSpeed(values[1].trim());
                    speedData.setTotal(values[2].trim());
                    speedDataList.add(speedData);
                } else {
                    log.warn("CSV行格式不正确，跳过: {}", line);
                }
            }

            log.info("解析speed-10s.csv完成，共{}条记录", speedDataList.size());
        } catch (Exception e) {
            log.error("解析speed-10s.csv失败: {}", e.getMessage(), e);
            throw new IOException("解析speed-10s.csv失败: " + e.getMessage(), e);
        }

        return speedDataList;
    }

    /**
     * 解压端侧压缩包并解析vmos-10s.xlsx
     *
     * @param zipFilePath 压缩包文件路径
     * @return VmosData列表，如果解析失败返回空列表
     * @throws IOException IO异常
     */
    public static List<VmosData> extractAndParseVmosExcel(String zipFilePath) throws IOException {
        log.info("开始解压端侧文件并解析vmos-10s.xlsx: {}", zipFilePath);

        Path zipPath = Paths.get(zipFilePath);
        if (!Files.exists(zipPath)) {
            throw new IOException("压缩包文件不存在: " + zipFilePath);
        }

        // 创建临时解压目录
        Path extractDir = Files.createTempDirectory("client_file_extract_");
        List<VmosData> vmosDataList = new ArrayList<>();

        try {
            // 解压文件
            extractZipFile(zipPath, extractDir);

            // 查找并解析vmos-10s.xlsx
            Path vmosExcelPath = extractDir.resolve("vmos-10s.xlsx");
            if (!Files.exists(vmosExcelPath)) {
                // 尝试在子目录中查找
                vmosExcelPath = findVmosExcelFile(extractDir);
            }

            if (vmosExcelPath != null && Files.exists(vmosExcelPath)) {
                log.info("找到vmos-10s.xlsx文件: {}", vmosExcelPath);
                vmosDataList = parseVmosExcel(vmosExcelPath);
                log.info("vmos-10s.xlsx解析成功: 共{}条记录", vmosDataList.size());
            } else {
                log.warn("未找到vmos-10s.xlsx文件");
            }

        } finally {
            // 清理临时目录
            try {
                deleteDirectory(extractDir);
                log.info("临时解压目录已清理: {}", extractDir);
            } catch (Exception e) {
                log.warn("清理临时目录失败: {}", e.getMessage());
            }
        }

        return vmosDataList;
    }

    /**
     * 递归查找vmos-10s.xlsx文件
     *
     * @param directory 搜索目录
     * @return vmos-10s.xlsx文件路径，如果未找到返回null
     */
    private static Path findVmosExcelFile(Path directory) throws IOException {
        return Files.walk(directory)
                .filter(path -> path.getFileName() != null && 
                        path.getFileName().toString().equalsIgnoreCase("vmos-10s.xlsx"))
                .findFirst()
                .orElse(null);
    }

    /**
     * 解析vmos-10s.xlsx文件
     *
     * @param excelPath Excel文件路径
     * @return VmosData列表
     * @throws IOException IO异常
     */
    private static List<VmosData> parseVmosExcel(Path excelPath) throws IOException {
        List<VmosData> vmosDataList = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(excelPath.toFile());
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0); // 获取第一个工作表
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            // 跳过表头行，从第二行开始读取数据
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                VmosData vmosData = parseVmosRow(row, evaluator);
                if (vmosData != null) {
                    vmosDataList.add(vmosData);
                }
            }

            log.info("解析vmos-10s.xlsx完成，共{}条记录", vmosDataList.size());
        } catch (Exception e) {
            log.error("解析vmos-10s.xlsx失败: {}", e.getMessage(), e);
            throw new IOException("解析vmos-10s.xlsx失败: " + e.getMessage(), e);
        }

        return vmosDataList;
    }

    /**
     * 解析vMOS数据行
     * 列顺序：序号、速率、分辨率、RTT、丢包率、卡顿占比、初缓时延、码率、视频体验、交互体验、呈现体验、α、β、vMOS、avgQOE
     *
     * @param row 行数据
     * @param evaluator 公式计算器
     * @return VmosData对象
     */
    private static VmosData parseVmosRow(Row row, FormulaEvaluator evaluator) {
        try {
            if (isEmptyVmosRow(row, evaluator)) {
                return null;
            }

            VmosData vmosData = new VmosData();
            vmosData.setSequenceNumber(getCellValue(row.getCell(0), evaluator));
            vmosData.setSpeed(getCellValue(row.getCell(1), evaluator));
            vmosData.setResolution(getCellValue(row.getCell(2), evaluator));
            vmosData.setRtt(getCellValue(row.getCell(3), evaluator));
            vmosData.setPacketLossRate(getCellValue(row.getCell(4), evaluator));
            vmosData.setStutterRatio(getCellValue(row.getCell(5), evaluator));
            vmosData.setInitialBufferingDelay(getCellValue(row.getCell(6), evaluator));
            vmosData.setBitrate(getCellValue(row.getCell(7), evaluator));
            vmosData.setVideoExperience(getCellValue(row.getCell(8), evaluator));
            vmosData.setInteractionExperience(getCellValue(row.getCell(9), evaluator));
            vmosData.setPresentationExperience(getCellValue(row.getCell(10), evaluator));
            vmosData.setAlpha(getCellValue(row.getCell(11), evaluator));
            vmosData.setBeta(getCellValue(row.getCell(12), evaluator));
            vmosData.setVmos(getCellValue(row.getCell(13), evaluator));
            vmosData.setAvgQoe(getCellValue(row.getCell(14), evaluator));

            return vmosData;
        } catch (Exception e) {
            log.error("Error parsing vMOS row {}: {}", row.getRowNum() + 1, e.getMessage());
            return null;
        }
    }

    /**
     * 检查是否为空行
     */
    private static boolean isEmptyVmosRow(Row row, FormulaEvaluator evaluator) {
        if (row == null) {
            return true;
        }

        // 检查前几列是否都为空
        for (int i = 0; i < 5; i++) {
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

    /**
     * 获取单元格值
     */
    private static String getCellValue(Cell cell, FormulaEvaluator evaluator) {
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
                return getFormulaValue(cell, evaluator);
            }
            default: {
                return null;
            }
        }
    }

    /**
     * 获取公式单元格的计算结果
     */
    private static String getFormulaValue(Cell cell, FormulaEvaluator evaluator) {
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
            log.warn("Failed to evaluate formula in cell {}: {}", cell.getAddress(), e.getMessage());
            return cell.getCellFormula();
        }
    }

    /**
     * 数值单元格转字符串，避免科学计数法
     */
    private static String formatNumericCellValue(Cell cell) {
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
     * 解析CSV行（简单实现，处理逗号分隔）
     *
     * @param line CSV行
     * @return 字段值数组
     */
    private static String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder currentValue = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(currentValue.toString());
                currentValue = new StringBuilder();
            } else {
                currentValue.append(c);
            }
        }

        // 添加最后一个值
        values.add(currentValue.toString());

        return values.toArray(new String[0]);
    }

    /**
     * 递归删除目录
     *
     * @param directory 要删除的目录
     * @throws IOException IO异常
     */
    private static void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            Files.walk(directory)
                    .sorted((a, b) -> b.compareTo(a)) // 先删除文件，再删除目录
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.warn("删除文件/目录失败: {}", path, e);
                        }
                    });
        }
    }
}

