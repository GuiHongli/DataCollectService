package com.datacollect.util;

import com.datacollect.dto.TaskInfoDTO;
import com.datacollect.entity.LostData;
import com.datacollect.entity.NetworkData;
import com.datacollect.entity.RttData;
import com.datacollect.entity.SpeedData;
import com.datacollect.entity.VideoData;
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

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
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
     * 文件后缀（时间间隔）
     */
    private static final String FILE_SUFFIX = "10s";

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
     * 解压压缩文件到指定目录（支持ZIP和GZ格式）
     *
     * @param filePath 压缩文件路径
     * @param extractDir 解压目录
     * @throws IOException IO异常
     */
    private static void extractZipFile(Path filePath, Path extractDir) throws IOException {
        String fileName = filePath.getFileName().toString().toLowerCase();
        
        if (fileName.endsWith(".gz")) {
            log.info("解压GZ文件: {} -> {}", filePath, extractDir);
            extractGzFile(filePath, extractDir);
        } else {
            log.info("解压ZIP文件: {} -> {}", filePath, extractDir);
            extractZipFileInternal(filePath, extractDir);
        }
    }

    /**
     * 解压ZIP文件到指定目录
     *
     * @param zipPath ZIP文件路径
     * @param extractDir 解压目录
     * @throws IOException IO异常
     */
    private static void extractZipFileInternal(Path zipPath, Path extractDir) throws IOException {
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
     * 解压GZ文件到指定目录
     * 如果是.tar.gz文件，会先解压gz，再解压tar
     * 如果是单独的.gz文件，直接解压
     *
     * @param gzPath GZ文件路径
     * @param extractDir 解压目录
     * @throws IOException IO异常
     */
    private static void extractGzFile(Path gzPath, Path extractDir) throws IOException {
        String fileName = gzPath.getFileName().toString().toLowerCase();
        
        // 创建临时文件用于存储解压后的内容
        Path tempFile = Files.createTempFile("gz_extract_", fileName.replace(".gz", ""));
        
        try {
            // 解压GZ文件
            try (FileInputStream fis = new FileInputStream(gzPath.toFile());
                 GZIPInputStream gzis = new GZIPInputStream(fis);
                 FileOutputStream fos = new FileOutputStream(tempFile.toFile())) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = gzis.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }
            
            log.info("GZ文件解压完成，临时文件: {}", tempFile);
            
            // 检查是否是tar.gz文件
            if (fileName.endsWith(".tar.gz")) {
                // 解压TAR文件
                extractTarFile(tempFile, extractDir);
            } else {
                // 单独的gz文件，直接复制到解压目录
                String originalFileName = fileName.replace(".gz", "");
                Path targetFile = extractDir.resolve(originalFileName);
                Files.createDirectories(targetFile.getParent());
                Files.copy(tempFile, targetFile);
                log.info("GZ文件内容已复制到: {}", targetFile);
            }
            
        } finally {
            // 清理临时文件
            try {
                Files.deleteIfExists(tempFile);
            } catch (Exception e) {
                log.warn("清理临时文件失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 解压TAR文件到指定目录
     * 使用Apache Commons Compress库处理TAR文件
     *
     * @param tarPath TAR文件路径
     * @param extractDir 解压目录
     * @throws IOException IO异常
     */
    private static void extractTarFile(Path tarPath, Path extractDir) throws IOException {
        log.info("解压TAR文件: {} -> {}", tarPath, extractDir);
        
        try (FileInputStream fis = new FileInputStream(tarPath.toFile());
             TarArchiveInputStream tis = new TarArchiveInputStream(fis)) {
            
            TarArchiveEntry entry;
            while ((entry = tis.getNextTarEntry()) != null) {
                Path filePath = extractDir.resolve(entry.getName());
                
                if (entry.isDirectory()) {
                    Files.createDirectories(filePath);
                } else {
                    Files.createDirectories(filePath.getParent());
                    try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = tis.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }
                    }
                }
            }
            
            log.info("TAR文件解压完成");
        } catch (Exception e) {
            // 如果TAR解压失败，尝试直接读取文件内容（可能是单个文件被压缩成gz）
            log.warn("TAR解压失败，尝试直接读取文件内容: {}", e.getMessage());
            String fileName = tarPath.getFileName().toString().replace(".tar", "");
            Path targetFile = extractDir.resolve(fileName);
            Files.createDirectories(targetFile.getParent());
            Files.copy(tarPath, targetFile);
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
     * 解压端侧压缩包并解析speed-10s.xlsx
     *
     * @param zipFilePath 压缩包文件路径
     * @return SpeedData列表，如果解析失败返回空列表
     * @throws IOException IO异常
     */
    public static List<SpeedData> extractAndParseSpeedExcel(String zipFilePath) throws IOException {
        log.info("开始解压端侧文件并解析speed-{}文件: {}", FILE_SUFFIX, zipFilePath);

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

            // 查找并解析speed文件
            Path speedExcelPath = extractDir.resolve("speed-" + FILE_SUFFIX + ".xlsx");
            if (!Files.exists(speedExcelPath)) {
                // 尝试在子目录中查找
                speedExcelPath = findSpeedExcelFile(extractDir);
            }

            if (speedExcelPath != null && Files.exists(speedExcelPath)) {
                log.info("找到speed-{}文件: {}", FILE_SUFFIX, speedExcelPath);
                speedDataList = parseSpeedExcel(speedExcelPath);
                log.info("speed-{}解析成功: 共{}条记录", FILE_SUFFIX, speedDataList.size());
            } else {
                log.warn("未找到speed-{}文件", FILE_SUFFIX);
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
     * 递归查找speed-10s.xlsx文件
     *
     * @param directory 搜索目录
     * @return speed-10s.xlsx文件路径，如果未找到返回null
     */
    private static Path findSpeedExcelFile(Path directory) throws IOException {
        return Files.walk(directory)
                .filter(path -> path.getFileName() != null && 
                        path.getFileName().toString().equalsIgnoreCase("speed-" + FILE_SUFFIX + ".xlsx"))
                .findFirst()
                .orElse(null);
    }

    /**
     * 解析speed-10s.xlsx文件
     * 列顺序：dl_speed(bps)、ul_speed(bps)、total
     *
     * @param excelPath Excel文件路径
     * @return SpeedData列表
     * @throws IOException IO异常
     */
    private static List<SpeedData> parseSpeedExcel(Path excelPath) throws IOException {
        List<SpeedData> speedDataList = new ArrayList<>();

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

                SpeedData speedData = parseSpeedRow(row, evaluator);
                if (speedData != null) {
                    speedDataList.add(speedData);
                }
            }

            log.info("解析speed-{}完成，共{}条记录", FILE_SUFFIX, speedDataList.size());
        } catch (Exception e) {
            log.error("解析speed-{}失败: {}", FILE_SUFFIX, e.getMessage(), e);
            throw new IOException("解析speed-" + FILE_SUFFIX + "失败: " + e.getMessage(), e);
        }

        return speedDataList;
    }

    /**
     * 解析速率数据行
     * 列顺序：dl_speed(bps)、ul_speed(bps)、total
     *
     * @param row 行数据
     * @param evaluator 公式计算器
     * @return SpeedData对象
     */
    private static SpeedData parseSpeedRow(Row row, FormulaEvaluator evaluator) {
        try {
            if (isEmptySpeedRow(row, evaluator)) {
                return null;
            }

            SpeedData speedData = new SpeedData();
            speedData.setDlSpeed(getCellValue(row.getCell(0), evaluator));
            speedData.setUlSpeed(getCellValue(row.getCell(1), evaluator));
            speedData.setTotal(getCellValue(row.getCell(2), evaluator));

            return speedData;
        } catch (Exception e) {
            log.error("Error parsing speed row {}: {}", row.getRowNum() + 1, e.getMessage());
            return null;
        }
    }

    /**
     * 检查是否为空行
     */
    private static boolean isEmptySpeedRow(Row row, FormulaEvaluator evaluator) {
        if (row == null) {
            return true;
        }

        // 检查前几列是否都为空
        for (int i = 0; i < 3; i++) {
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
     * 解压端侧压缩包并解析vmos-10s.xlsx
     *
     * @param zipFilePath 压缩包文件路径
     * @return VmosData列表，如果解析失败返回空列表
     * @throws IOException IO异常
     */
    public static List<VmosData> extractAndParseVmosExcel(String zipFilePath) throws IOException {
        log.info("开始解压端侧文件并解析vmos-{}文件: {}", FILE_SUFFIX, zipFilePath);

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

            // 查找并解析vmos文件
            Path vmosExcelPath = extractDir.resolve("vmos-" + FILE_SUFFIX + ".xlsx");
            if (!Files.exists(vmosExcelPath)) {
                // 尝试在子目录中查找
                vmosExcelPath = findVmosExcelFile(extractDir);
            }

            if (vmosExcelPath != null && Files.exists(vmosExcelPath)) {
                log.info("找到vmos-{}文件: {}", FILE_SUFFIX, vmosExcelPath);
                vmosDataList = parseVmosExcel(vmosExcelPath);
                log.info("vmos-{}解析成功: 共{}条记录", FILE_SUFFIX, vmosDataList.size());
            } else {
                log.warn("未找到vmos-{}文件", FILE_SUFFIX);
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
                        path.getFileName().toString().equalsIgnoreCase("vmos-" + FILE_SUFFIX + ".xlsx"))
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

            log.info("解析vmos-{}完成，共{}条记录", FILE_SUFFIX, vmosDataList.size());
        } catch (Exception e) {
            log.error("解析vmos-{}失败: {}", FILE_SUFFIX, e.getMessage(), e);
            throw new IOException("解析vmos-" + FILE_SUFFIX + "失败: " + e.getMessage(), e);
        }

        return vmosDataList;
    }

    /**
     * 解析vMOS数据行
     * 列顺序：序号、速率、分辨率、RTT、丢包率、卡顿占比、初缓时延、码率、计算分辨率、视频体验、交互体验、呈现体验、丢包率（s_lost_packet_rate）、卡顿占比（s_stall_rate）、α、β、vMOS、avgQOE
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
            vmosData.setCalculatedResolution(getCellValue(row.getCell(8), evaluator));
            vmosData.setVideoExperience(getCellValue(row.getCell(9), evaluator));
            vmosData.setInteractionExperience(getCellValue(row.getCell(10), evaluator));
            vmosData.setPresentationExperience(getCellValue(row.getCell(11), evaluator));
            vmosData.setSLostPacketRate(getCellValue(row.getCell(12), evaluator));
            vmosData.setSStallRate(getCellValue(row.getCell(13), evaluator));
            vmosData.setAlpha(getCellValue(row.getCell(14), evaluator));
            vmosData.setBeta(getCellValue(row.getCell(15), evaluator));
            vmosData.setVmos(getCellValue(row.getCell(16), evaluator));
            vmosData.setAvgQoe(getCellValue(row.getCell(17), evaluator));

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
     * 解压端侧压缩包并解析rtt-10s.csv
     *
     * @param zipFilePath 压缩包文件路径
     * @return RttData列表，如果解析失败返回空列表
     * @throws IOException IO异常
     */
    public static List<RttData> extractAndParseRttCsv(String zipFilePath) throws IOException {
        log.info("开始解压端侧文件并解析rtt-{}文件: {}", FILE_SUFFIX, zipFilePath);

        Path zipPath = Paths.get(zipFilePath);
        if (!Files.exists(zipPath)) {
            throw new IOException("压缩包文件不存在: " + zipFilePath);
        }

        // 创建临时解压目录
        Path extractDir = Files.createTempDirectory("client_file_extract_");
        List<RttData> rttDataList = new ArrayList<>();

        try {
            // 解压文件
            extractZipFile(zipPath, extractDir);

            // 查找并解析rtt文件
            Path rttCsvPath = extractDir.resolve("rtt-" + FILE_SUFFIX + ".csv");
            if (!Files.exists(rttCsvPath)) {
                // 尝试在子目录中查找
                rttCsvPath = findRttCsvFile(extractDir);
            }

            if (rttCsvPath != null && Files.exists(rttCsvPath)) {
                log.info("找到rtt-{}文件: {}", FILE_SUFFIX, rttCsvPath);
                rttDataList = parseRttCsv(rttCsvPath);
                log.info("rtt-{}解析成功: 共{}条记录", FILE_SUFFIX, rttDataList.size());
            } else {
                log.warn("未找到rtt-{}文件", FILE_SUFFIX);
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

        return rttDataList;
    }

    /**
     * 递归查找rtt-10s.csv文件
     *
     * @param directory 搜索目录
     * @return rtt-10s.csv文件路径，如果未找到返回null
     * @throws IOException IO异常
     */
    private static Path findRttCsvFile(Path directory) throws IOException {
        return Files.walk(directory)
                .filter(path -> path.getFileName() != null && 
                        path.getFileName().toString().equalsIgnoreCase("rtt-" + FILE_SUFFIX + ".csv"))
                .findFirst()
                .orElse(null);
    }

    /**
     * 解析rtt-10s.csv文件
     *
     * @param csvPath CSV文件路径
     * @return RttData列表
     * @throws IOException IO异常
     */
    private static List<RttData> parseRttCsv(Path csvPath) throws IOException {
        List<RttData> rttDataList = new ArrayList<>();

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
                    if (line.toLowerCase().contains("index") || 
                        line.toLowerCase().contains("dl_delay") || 
                        line.toLowerCase().contains("ul_delay")) {
                        continue;
                    }
                }

                // 解析CSV行
                String[] values = parseCsvLine(line);
                if (values.length >= 3) {
                    RttData rttData = new RttData();
                    rttData.setIndexTime(values[0].trim());
                    rttData.setDlDelay(values[1].trim());
                    rttData.setUlDelay(values[2].trim());
                    rttDataList.add(rttData);
                } else {
                    log.warn("CSV行格式不正确，跳过: {}", line);
                }
            }

            log.info("解析rtt-{}完成，共{}条记录", FILE_SUFFIX, rttDataList.size());
        } catch (Exception e) {
            log.error("解析rtt-{}失败: {}", FILE_SUFFIX, e.getMessage(), e);
            throw new IOException("解析rtt-" + FILE_SUFFIX + "失败: " + e.getMessage(), e);
        }

        return rttDataList;
    }

    /**
     * 解压端侧压缩包并解析lost-10s.csv
     *
     * @param zipFilePath 压缩包文件路径
     * @return LostData列表，如果解析失败返回空列表
     * @throws IOException IO异常
     */
    public static List<LostData> extractAndParseLostCsv(String zipFilePath) throws IOException {
        log.info("开始解压端侧文件并解析lost-{}文件: {}", FILE_SUFFIX, zipFilePath);

        Path zipPath = Paths.get(zipFilePath);
        if (!Files.exists(zipPath)) {
            throw new IOException("压缩包文件不存在: " + zipFilePath);
        }

        // 创建临时解压目录
        Path extractDir = Files.createTempDirectory("client_file_extract_");
        List<LostData> lostDataList = new ArrayList<>();

        try {
            // 解压文件
            extractZipFile(zipPath, extractDir);

            // 查找并解析lost文件
            Path lostCsvPath = extractDir.resolve("lost-" + FILE_SUFFIX + ".csv");
            if (!Files.exists(lostCsvPath)) {
                // 尝试在子目录中查找
                lostCsvPath = findLostCsvFile(extractDir);
            }

            if (lostCsvPath != null && Files.exists(lostCsvPath)) {
                log.info("找到lost-{}文件: {}", FILE_SUFFIX, lostCsvPath);
                lostDataList = parseLostCsv(lostCsvPath);
                log.info("lost-{}解析成功: 共{}条记录", FILE_SUFFIX, lostDataList.size());
            } else {
                log.warn("未找到lost-{}文件", FILE_SUFFIX);
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

        return lostDataList;
    }

    /**
     * 递归查找lost-10s.csv文件
     *
     * @param directory 搜索目录
     * @return lost-10s.csv文件路径，如果未找到返回null
     * @throws IOException IO异常
     */
    private static Path findLostCsvFile(Path directory) throws IOException {
        return Files.walk(directory)
                .filter(path -> path.getFileName() != null && 
                        path.getFileName().toString().equalsIgnoreCase("lost-" + FILE_SUFFIX + ".csv"))
                .findFirst()
                .orElse(null);
    }

    /**
     * 解析lost-10s.csv文件
     *
     * @param csvPath CSV文件路径
     * @return LostData列表
     * @throws IOException IO异常
     */
    private static List<LostData> parseLostCsv(Path csvPath) throws IOException {
        List<LostData> lostDataList = new ArrayList<>();

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
                    if (line.toLowerCase().contains("index") || 
                        line.toLowerCase().contains("dl_loss") || 
                        line.toLowerCase().contains("ul_loss") ||
                        line.toLowerCase().contains("total_loss")) {
                        continue;
                    }
                }

                // 解析CSV行
                String[] values = parseCsvLine(line);
                if (values.length >= 4) {
                    LostData lostData = new LostData();
                    lostData.setIndexTime(values[0].trim());
                    lostData.setDlLoss(values[1].trim());
                    lostData.setUlLoss(values[2].trim());
                    lostData.setTotalLoss(values[3].trim());
                    lostDataList.add(lostData);
                } else {
                    log.warn("CSV行格式不正确，跳过: {}", line);
                }
            }

            log.info("解析lost-{}完成，共{}条记录", FILE_SUFFIX, lostDataList.size());
        } catch (Exception e) {
            log.error("解析lost-{}失败: {}", FILE_SUFFIX, e.getMessage(), e);
            throw new IOException("解析lost-" + FILE_SUFFIX + "失败: " + e.getMessage(), e);
        }

        return lostDataList;
    }

    /**
     * 解压端侧压缩包并解析video-10s.csv
     *
     * @param zipFilePath 压缩包文件路径
     * @return VideoData列表，如果解析失败返回空列表
     * @throws IOException IO异常
     */
    public static List<VideoData> extractAndParseVideoCsv(String zipFilePath) throws IOException {
        log.info("开始解压端侧文件并解析video-{}文件: {}", FILE_SUFFIX, zipFilePath);

        Path zipPath = Paths.get(zipFilePath);
        if (!Files.exists(zipPath)) {
            throw new IOException("压缩包文件不存在: " + zipFilePath);
        }

        // 创建临时解压目录
        Path extractDir = Files.createTempDirectory("client_file_extract_");
        List<VideoData> videoDataList = new ArrayList<>();

        try {
            // 解压文件
            extractZipFile(zipPath, extractDir);

            // 查找并解析video文件
            Path videoCsvPath = extractDir.resolve("video-" + FILE_SUFFIX + ".csv");
            if (!Files.exists(videoCsvPath)) {
                // 尝试在子目录中查找
                videoCsvPath = findVideoCsvFile(extractDir);
            }

            if (videoCsvPath != null && Files.exists(videoCsvPath)) {
                log.info("找到video-{}文件: {}", FILE_SUFFIX, videoCsvPath);
                videoDataList = parseVideoCsv(videoCsvPath);
                log.info("video-{}解析成功: 共{}条记录", FILE_SUFFIX, videoDataList.size());
            } else {
                log.warn("未找到video-{}文件", FILE_SUFFIX);
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

        return videoDataList;
    }

    /**
     * 递归查找video-10s.csv文件
     *
     * @param directory 搜索目录
     * @return video-10s.csv文件路径，如果未找到返回null
     * @throws IOException IO异常
     */
    private static Path findVideoCsvFile(Path directory) throws IOException {
        return Files.walk(directory)
                .filter(path -> path.getFileName() != null && 
                        path.getFileName().toString().equalsIgnoreCase("video-" + FILE_SUFFIX + ".csv"))
                .findFirst()
                .orElse(null);
    }

    /**
     * 解析video-10s.csv文件
     *
     * @param csvPath CSV文件路径
     * @return VideoData列表
     * @throws IOException IO异常
     */
    private static List<VideoData> parseVideoCsv(Path csvPath) throws IOException {
        List<VideoData> videoDataList = new ArrayList<>();

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
                    if (line.toLowerCase().contains("time") || 
                        line.toLowerCase().contains("caton_time")) {
                        continue;
                    }
                }

                // 解析CSV行
                String[] values = parseCsvLine(line);
                if (values.length >= 2) {
                    VideoData videoData = new VideoData();
                    videoData.setTime(values[0].trim());
                    videoData.setCatonTime(values[1].trim());
                    videoDataList.add(videoData);
                } else {
                    log.warn("CSV行格式不正确，跳过: {}", line);
                }
            }

            log.info("解析video-{}完成，共{}条记录", FILE_SUFFIX, videoDataList.size());
        } catch (Exception e) {
            log.error("解析video-{}失败: {}", FILE_SUFFIX, e.getMessage(), e);
            throw new IOException("解析video-" + FILE_SUFFIX + "失败: " + e.getMessage(), e);
        }

        return videoDataList;
    }

    /**
     * 解压网络侧压缩包并解析CSV文件
     *
     * @param zipFilePath 压缩包文件路径
     * @return NetworkData列表，如果解析失败返回空列表
     * @throws IOException IO异常
     */
    public static List<NetworkData> extractAndParseNetworkCsv(String zipFilePath) throws IOException {
        log.info("开始解压网络侧文件并解析CSV: {}", zipFilePath);

        Path zipPath = Paths.get(zipFilePath);
        if (!Files.exists(zipPath)) {
            throw new IOException("压缩包文件不存在: " + zipFilePath);
        }

        // 创建临时解压目录
        Path extractDir = Files.createTempDirectory("network_file_extract_");
        List<NetworkData> networkDataList = new ArrayList<>();

        try {
            // 解压文件
            extractZipFile(zipPath, extractDir);

            // 查找所有CSV文件
            List<Path> csvFiles = findCsvFiles(extractDir);
            
            if (csvFiles.isEmpty()) {
                log.warn("未找到CSV文件");
                return networkDataList;
            }

            // 解析所有CSV文件
            for (Path csvFile : csvFiles) {
                log.info("找到CSV文件: {}", csvFile);
                try {
                    List<NetworkData> dataList = parseNetworkCsv(csvFile);
                    networkDataList.addAll(dataList);
                    log.info("CSV文件解析成功: {}, 共{}条记录", csvFile.getFileName(), dataList.size());
                } catch (Exception e) {
                    log.error("解析CSV文件失败: {}, error: {}", csvFile, e.getMessage(), e);
                    // 继续解析其他文件
                }
            }

            log.info("网络侧CSV解析完成，共{}条记录", networkDataList.size());

        } finally {
            // 清理临时目录
            try {
                deleteDirectory(extractDir);
                log.info("临时解压目录已清理: {}", extractDir);
            } catch (Exception e) {
                log.warn("清理临时目录失败: {}", e.getMessage());
            }
        }

        return networkDataList;
    }

    /**
     * 递归查找所有CSV文件
     *
     * @param directory 搜索目录
     * @return CSV文件路径列表
     * @throws IOException IO异常
     */
    private static List<Path> findCsvFiles(Path directory) throws IOException {
        List<Path> csvFiles = new ArrayList<>();
        Files.walk(directory)
                .filter(path -> {
                    String fileName = path.getFileName() != null ? path.getFileName().toString().toLowerCase() : "";
                    return Files.isRegularFile(path) && fileName.endsWith(".csv");
                })
                .forEach(csvFiles::add);
        return csvFiles;
    }

    /**
     * 解析网络侧CSV文件
     *
     * @param csvPath CSV文件路径
     * @return NetworkData列表
     * @throws IOException IO异常
     */
    private static List<NetworkData> parseNetworkCsv(Path csvPath) throws IOException {
        List<NetworkData> networkDataList = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
            String line;
            boolean isFirstLine = true;
            int[] columnIndexMap = null; // 存储列索引映射

            while ((line = reader.readLine()) != null) {
                // 跳过空行
                if (line.trim().isEmpty()) {
                    continue;
                }

                // 解析表头，建立列索引映射
                if (isFirstLine) {
                    isFirstLine = false;
                    columnIndexMap = parseHeader(line);
                    if (columnIndexMap == null) {
                        log.warn("CSV表头格式不正确，跳过文件: {}", csvPath);
                        return networkDataList;
                    }
                    continue;
                }

                // 解析数据行
                String[] values = parseCsvLine(line);
                if (values.length > 0) {
                    NetworkData networkData = createNetworkDataFromRow(values, columnIndexMap);
                    if (networkData != null) {
                        networkDataList.add(networkData);
                    }
                }
            }

            log.info("解析网络侧CSV完成，共{}条记录", networkDataList.size());
        } catch (Exception e) {
            log.error("解析网络侧CSV失败: {}", e.getMessage(), e);
            throw new IOException("解析网络侧CSV失败: " + e.getMessage(), e);
        }

        return networkDataList;
    }

    /**
     * 解析CSV表头，建立列索引映射
     *
     * @param headerLine 表头行
     * @return 列索引映射数组，索引对应字段顺序
     */
    private static int[] parseHeader(String headerLine) {
        String[] headers = parseCsvLine(headerLine);
        int[] columnIndexMap = new int[58]; // 58个字段
        java.util.Arrays.fill(columnIndexMap, -1);

        // 定义字段名称映射
        String[] fieldNames = {
            "serialNo", "TimeStamp", "startTime", "Gpsi", "Dnn", "S_NSSAI", "RatType", "Qci",
            "ExpOptFlag", "ExpOptStartTime", "AppId", "SubAppId", "AppStatus", "AppQuality",
            "Tai", "CellId", "DelayAn", "DelayDn", "UplinkBandwidth", "DownlinkBandwidth",
            "UplinkPkg", "DownlinkPkg", "LostUplinkPkg", "LostDownLinkPkg", "SubAppStartTime",
            "InfoIndicate", "Upfld", "Pei", "SubAppEffDuration", "MaxDelayAn", "MaxDelayDn",
            "AvgBandwidthUI", "AvgBandwidthDI", "MaxBandwidthUI", "MaxBandwidthDI", "VolumeUI",
            "VolumeDI", "ExpOptEndTime", "SubAppEdrStartTime", "UserServiceLoadUIEcn",
            "UserServiceLoadDIEcn", "AssuranceFailedReason", "GnbQncNotifType", "AvgQoe",
            "MaxResolution", "MostResolution", "MaxBitRateUI", "AvgBitRateUI", "MaxBitRateDI",
            "AvgBitRateDI", "StallingDuration", "StallingNumber", "ServiceDelay", "ServiceInitialDuration"
        };

        // 建立列索引映射
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i].trim();
            for (int j = 0; j < fieldNames.length; j++) {
                if (header.equalsIgnoreCase(fieldNames[j]) || 
                    header.replaceAll("[_\\s-]", "").equalsIgnoreCase(fieldNames[j].replaceAll("[_\\s-]", ""))) {
                    columnIndexMap[j] = i;
                    break;
                }
            }
        }

        return columnIndexMap;
    }

    /**
     * 从CSV行数据创建NetworkData对象
     *
     * @param values CSV行数据
     * @param columnIndexMap 列索引映射
     * @return NetworkData对象
     */
    private static NetworkData createNetworkDataFromRow(String[] values, int[] columnIndexMap) {
        NetworkData networkData = new NetworkData();
        
        try {
            if (columnIndexMap[0] >= 0 && columnIndexMap[0] < values.length) {
                networkData.setSerialNo(values[columnIndexMap[0]].trim());
            }
            if (columnIndexMap[1] >= 0 && columnIndexMap[1] < values.length) {
                networkData.setTimeStamp(values[columnIndexMap[1]].trim());
            }
            if (columnIndexMap[2] >= 0 && columnIndexMap[2] < values.length) {
                networkData.setStartTime(values[columnIndexMap[2]].trim());
            }
            if (columnIndexMap[3] >= 0 && columnIndexMap[3] < values.length) {
                networkData.setGpsi(values[columnIndexMap[3]].trim());
            }
            if (columnIndexMap[4] >= 0 && columnIndexMap[4] < values.length) {
                networkData.setDnn(values[columnIndexMap[4]].trim());
            }
            if (columnIndexMap[5] >= 0 && columnIndexMap[5] < values.length) {
                networkData.setSNssai(values[columnIndexMap[5]].trim());
            }
            if (columnIndexMap[6] >= 0 && columnIndexMap[6] < values.length) {
                networkData.setRatType(values[columnIndexMap[6]].trim());
            }
            if (columnIndexMap[7] >= 0 && columnIndexMap[7] < values.length) {
                networkData.setQci(values[columnIndexMap[7]].trim());
            }
            if (columnIndexMap[8] >= 0 && columnIndexMap[8] < values.length) {
                networkData.setExpOptFlag(values[columnIndexMap[8]].trim());
            }
            if (columnIndexMap[9] >= 0 && columnIndexMap[9] < values.length) {
                networkData.setExpOptStartTime(values[columnIndexMap[9]].trim());
            }
            if (columnIndexMap[10] >= 0 && columnIndexMap[10] < values.length) {
                networkData.setAppId(values[columnIndexMap[10]].trim());
            }
            if (columnIndexMap[11] >= 0 && columnIndexMap[11] < values.length) {
                networkData.setSubAppId(values[columnIndexMap[11]].trim());
            }
            if (columnIndexMap[12] >= 0 && columnIndexMap[12] < values.length) {
                networkData.setAppStatus(values[columnIndexMap[12]].trim());
            }
            if (columnIndexMap[13] >= 0 && columnIndexMap[13] < values.length) {
                networkData.setAppQuality(values[columnIndexMap[13]].trim());
            }
            if (columnIndexMap[14] >= 0 && columnIndexMap[14] < values.length) {
                networkData.setTai(values[columnIndexMap[14]].trim());
            }
            if (columnIndexMap[15] >= 0 && columnIndexMap[15] < values.length) {
                networkData.setCellId(values[columnIndexMap[15]].trim());
            }
            if (columnIndexMap[16] >= 0 && columnIndexMap[16] < values.length) {
                networkData.setDelayAn(values[columnIndexMap[16]].trim());
            }
            if (columnIndexMap[17] >= 0 && columnIndexMap[17] < values.length) {
                networkData.setDelayDn(values[columnIndexMap[17]].trim());
            }
            if (columnIndexMap[18] >= 0 && columnIndexMap[18] < values.length) {
                networkData.setUplinkBandwidth(values[columnIndexMap[18]].trim());
            }
            if (columnIndexMap[19] >= 0 && columnIndexMap[19] < values.length) {
                networkData.setDownlinkBandwidth(values[columnIndexMap[19]].trim());
            }
            if (columnIndexMap[20] >= 0 && columnIndexMap[20] < values.length) {
                networkData.setUplinkPkg(values[columnIndexMap[20]].trim());
            }
            if (columnIndexMap[21] >= 0 && columnIndexMap[21] < values.length) {
                networkData.setDownlinkPkg(values[columnIndexMap[21]].trim());
            }
            if (columnIndexMap[22] >= 0 && columnIndexMap[22] < values.length) {
                networkData.setLostUplinkPkg(values[columnIndexMap[22]].trim());
            }
            if (columnIndexMap[23] >= 0 && columnIndexMap[23] < values.length) {
                networkData.setLostDownlinkPkg(values[columnIndexMap[23]].trim());
            }
            if (columnIndexMap[24] >= 0 && columnIndexMap[24] < values.length) {
                networkData.setSubAppStartTime(values[columnIndexMap[24]].trim());
            }
            if (columnIndexMap[25] >= 0 && columnIndexMap[25] < values.length) {
                networkData.setInfoIndicate(values[columnIndexMap[25]].trim());
            }
            if (columnIndexMap[26] >= 0 && columnIndexMap[26] < values.length) {
                networkData.setUpfld(values[columnIndexMap[26]].trim());
            }
            if (columnIndexMap[27] >= 0 && columnIndexMap[27] < values.length) {
                networkData.setPei(values[columnIndexMap[27]].trim());
            }
            if (columnIndexMap[28] >= 0 && columnIndexMap[28] < values.length) {
                networkData.setSubAppEffDuration(values[columnIndexMap[28]].trim());
            }
            if (columnIndexMap[29] >= 0 && columnIndexMap[29] < values.length) {
                networkData.setMaxDelayAn(values[columnIndexMap[29]].trim());
            }
            if (columnIndexMap[30] >= 0 && columnIndexMap[30] < values.length) {
                networkData.setMaxDelayDn(values[columnIndexMap[30]].trim());
            }
            if (columnIndexMap[31] >= 0 && columnIndexMap[31] < values.length) {
                networkData.setAvgBandwidthUI(values[columnIndexMap[31]].trim());
            }
            if (columnIndexMap[32] >= 0 && columnIndexMap[32] < values.length) {
                networkData.setAvgBandwidthDI(values[columnIndexMap[32]].trim());
            }
            if (columnIndexMap[33] >= 0 && columnIndexMap[33] < values.length) {
                networkData.setMaxBandwidthUI(values[columnIndexMap[33]].trim());
            }
            if (columnIndexMap[34] >= 0 && columnIndexMap[34] < values.length) {
                networkData.setMaxBandwidthDI(values[columnIndexMap[34]].trim());
            }
            if (columnIndexMap[35] >= 0 && columnIndexMap[35] < values.length) {
                networkData.setVolumeUI(values[columnIndexMap[35]].trim());
            }
            if (columnIndexMap[36] >= 0 && columnIndexMap[36] < values.length) {
                networkData.setVolumeDI(values[columnIndexMap[36]].trim());
            }
            if (columnIndexMap[37] >= 0 && columnIndexMap[37] < values.length) {
                networkData.setExpOptEndTime(values[columnIndexMap[37]].trim());
            }
            if (columnIndexMap[38] >= 0 && columnIndexMap[38] < values.length) {
                networkData.setSubAppEdrStartTime(values[columnIndexMap[38]].trim());
            }
            if (columnIndexMap[39] >= 0 && columnIndexMap[39] < values.length) {
                networkData.setUserServiceLoadUIEcn(values[columnIndexMap[39]].trim());
            }
            if (columnIndexMap[40] >= 0 && columnIndexMap[40] < values.length) {
                networkData.setUserServiceLoadDIEcn(values[columnIndexMap[40]].trim());
            }
            if (columnIndexMap[41] >= 0 && columnIndexMap[41] < values.length) {
                networkData.setAssuranceFailedReason(values[columnIndexMap[41]].trim());
            }
            if (columnIndexMap[42] >= 0 && columnIndexMap[42] < values.length) {
                networkData.setGnbQncNotifType(values[columnIndexMap[42]].trim());
            }
            if (columnIndexMap[43] >= 0 && columnIndexMap[43] < values.length) {
                networkData.setAvgQoe(values[columnIndexMap[43]].trim());
            }
            if (columnIndexMap[44] >= 0 && columnIndexMap[44] < values.length) {
                networkData.setMaxResolution(values[columnIndexMap[44]].trim());
            }
            if (columnIndexMap[45] >= 0 && columnIndexMap[45] < values.length) {
                networkData.setMostResolution(values[columnIndexMap[45]].trim());
            }
            if (columnIndexMap[46] >= 0 && columnIndexMap[46] < values.length) {
                networkData.setMaxBitRateUI(values[columnIndexMap[46]].trim());
            }
            if (columnIndexMap[47] >= 0 && columnIndexMap[47] < values.length) {
                networkData.setAvgBitRateUI(values[columnIndexMap[47]].trim());
            }
            if (columnIndexMap[48] >= 0 && columnIndexMap[48] < values.length) {
                networkData.setMaxBitRateDI(values[columnIndexMap[48]].trim());
            }
            if (columnIndexMap[49] >= 0 && columnIndexMap[49] < values.length) {
                networkData.setAvgBitRateDI(values[columnIndexMap[49]].trim());
            }
            if (columnIndexMap[50] >= 0 && columnIndexMap[50] < values.length) {
                networkData.setStallingDuration(values[columnIndexMap[50]].trim());
            }
            if (columnIndexMap[51] >= 0 && columnIndexMap[51] < values.length) {
                networkData.setStallingNumber(values[columnIndexMap[51]].trim());
            }
            if (columnIndexMap[52] >= 0 && columnIndexMap[52] < values.length) {
                networkData.setServiceDelay(values[columnIndexMap[52]].trim());
            }
            if (columnIndexMap[53] >= 0 && columnIndexMap[53] < values.length) {
                networkData.setServiceInitialDuration(values[columnIndexMap[53]].trim());
            }
            
            return networkData;
        } catch (Exception e) {
            log.warn("创建NetworkData对象失败: {}", e.getMessage());
            return null;
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
     * 客户端数据解析结果
     */
    public static class ClientDataResult {
        private TaskInfoDTO taskInfo;
        private List<SpeedData> speedDataList;
        private List<VmosData> vmosDataList;
        private List<RttData> rttDataList;
        private List<LostData> lostDataList;
        private List<VideoData> videoDataList;

        public TaskInfoDTO getTaskInfo() {
            return taskInfo;
        }

        public void setTaskInfo(TaskInfoDTO taskInfo) {
            this.taskInfo = taskInfo;
        }

        public List<SpeedData> getSpeedDataList() {
            return speedDataList;
        }

        public void setSpeedDataList(List<SpeedData> speedDataList) {
            this.speedDataList = speedDataList;
        }

        public List<VmosData> getVmosDataList() {
            return vmosDataList;
        }

        public void setVmosDataList(List<VmosData> vmosDataList) {
            this.vmosDataList = vmosDataList;
        }

        public List<RttData> getRttDataList() {
            return rttDataList;
        }

        public void setRttDataList(List<RttData> rttDataList) {
            this.rttDataList = rttDataList;
        }

        public List<LostData> getLostDataList() {
            return lostDataList;
        }

        public void setLostDataList(List<LostData> lostDataList) {
            this.lostDataList = lostDataList;
        }

        public List<VideoData> getVideoDataList() {
            return videoDataList;
        }

        public void setVideoDataList(List<VideoData> videoDataList) {
            this.videoDataList = videoDataList;
        }
    }

    /**
     * 解压端侧压缩包并统一解析所有数据（taskInfo、vmos、rtt、video、speed、lost）
     * 只解压一次，然后统一处理所有数据
     *
     * @param zipFilePath 压缩包文件路径
     * @return ClientDataResult对象，包含所有解析结果
     * @throws IOException IO异常
     */
    public static ClientDataResult extractAndParseAllClientData(String zipFilePath) throws IOException {
        log.info("开始解压端侧文件并统一解析所有数据: {}", zipFilePath);

        Path zipPath = Paths.get(zipFilePath);
        if (!Files.exists(zipPath)) {
            throw new IOException("压缩包文件不存在: " + zipFilePath);
        }

        // 创建临时解压目录
        Path extractDir = Files.createTempDirectory("client_file_extract_");
        ClientDataResult result = new ClientDataResult();

        try {
            // 解压文件（只解压一次）
            extractZipFile(zipPath, extractDir);
            log.info("文件解压完成，解压目录: {}", extractDir);

            // 解析taskinfo.json
            try {
                Path taskInfoPath = extractDir.resolve("taskinfo.json");
                if (!Files.exists(taskInfoPath)) {
                    taskInfoPath = findTaskInfoFile(extractDir);
                }
                if (taskInfoPath != null && Files.exists(taskInfoPath)) {
                    log.info("找到taskinfo.json文件: {}", taskInfoPath);
                    TaskInfoDTO taskInfo = parseTaskInfoJson(taskInfoPath);
                    result.setTaskInfo(taskInfo);
                    log.info("taskinfo.json解析成功: taskId={}", taskInfo != null ? taskInfo.getTaskId() : "null");
                } else {
                    log.warn("未找到taskinfo.json文件");
                }
            } catch (Exception e) {
                log.error("解析taskinfo.json失败: {}", e.getMessage(), e);
            }

            // 解析speed文件
            try {
                Path speedExcelPath = extractDir.resolve("speed-" + FILE_SUFFIX + ".xlsx");
                if (!Files.exists(speedExcelPath)) {
                    speedExcelPath = findSpeedExcelFile(extractDir);
                }
                if (speedExcelPath != null && Files.exists(speedExcelPath)) {
                    log.info("找到speed-{}文件: {}", FILE_SUFFIX, speedExcelPath);
                    List<SpeedData> speedDataList = parseSpeedExcel(speedExcelPath);
                    result.setSpeedDataList(speedDataList);
                    log.info("speed-{}解析成功: 共{}条记录", FILE_SUFFIX, speedDataList.size());
                } else {
                    log.warn("未找到speed-{}文件", FILE_SUFFIX);
                    result.setSpeedDataList(new ArrayList<>());
                }
            } catch (Exception e) {
                log.error("解析speed-{}失败: {}", FILE_SUFFIX, e.getMessage(), e);
                result.setSpeedDataList(new ArrayList<>());
            }

            // 解析vmos文件
            try {
                Path vmosExcelPath = extractDir.resolve("vmos-" + FILE_SUFFIX + ".xlsx");
                if (!Files.exists(vmosExcelPath)) {
                    vmosExcelPath = findVmosExcelFile(extractDir);
                }
                if (vmosExcelPath != null && Files.exists(vmosExcelPath)) {
                    log.info("找到vmos-{}文件: {}", FILE_SUFFIX, vmosExcelPath);
                    List<VmosData> vmosDataList = parseVmosExcel(vmosExcelPath);
                    result.setVmosDataList(vmosDataList);
                    log.info("vmos-{}解析成功: 共{}条记录", FILE_SUFFIX, vmosDataList.size());
                } else {
                    log.warn("未找到vmos-{}文件", FILE_SUFFIX);
                    result.setVmosDataList(new ArrayList<>());
                }
            } catch (Exception e) {
                log.error("解析vmos-{}失败: {}", FILE_SUFFIX, e.getMessage(), e);
                result.setVmosDataList(new ArrayList<>());
            }

            // 解析rtt文件
            try {
                Path rttCsvPath = extractDir.resolve("rtt-" + FILE_SUFFIX + ".csv");
                if (!Files.exists(rttCsvPath)) {
                    rttCsvPath = findRttCsvFile(extractDir);
                }
                if (rttCsvPath != null && Files.exists(rttCsvPath)) {
                    log.info("找到rtt-{}文件: {}", FILE_SUFFIX, rttCsvPath);
                    List<RttData> rttDataList = parseRttCsv(rttCsvPath);
                    result.setRttDataList(rttDataList);
                    log.info("rtt-{}解析成功: 共{}条记录", FILE_SUFFIX, rttDataList.size());
                } else {
                    log.warn("未找到rtt-{}文件", FILE_SUFFIX);
                    result.setRttDataList(new ArrayList<>());
                }
            } catch (Exception e) {
                log.error("解析rtt-{}失败: {}", FILE_SUFFIX, e.getMessage(), e);
                result.setRttDataList(new ArrayList<>());
            }

            // 解析lost文件
            try {
                Path lostCsvPath = extractDir.resolve("lost-" + FILE_SUFFIX + ".csv");
                if (!Files.exists(lostCsvPath)) {
                    lostCsvPath = findLostCsvFile(extractDir);
                }
                if (lostCsvPath != null && Files.exists(lostCsvPath)) {
                    log.info("找到lost-{}文件: {}", FILE_SUFFIX, lostCsvPath);
                    List<LostData> lostDataList = parseLostCsv(lostCsvPath);
                    result.setLostDataList(lostDataList);
                    log.info("lost-{}解析成功: 共{}条记录", FILE_SUFFIX, lostDataList.size());
                } else {
                    log.warn("未找到lost-{}文件", FILE_SUFFIX);
                    result.setLostDataList(new ArrayList<>());
                }
            } catch (Exception e) {
                log.error("解析lost-{}失败: {}", FILE_SUFFIX, e.getMessage(), e);
                result.setLostDataList(new ArrayList<>());
            }

            // 解析video文件
            try {
                Path videoCsvPath = extractDir.resolve("video-" + FILE_SUFFIX + ".csv");
                if (!Files.exists(videoCsvPath)) {
                    videoCsvPath = findVideoCsvFile(extractDir);
                }
                if (videoCsvPath != null && Files.exists(videoCsvPath)) {
                    log.info("找到video-{}文件: {}", FILE_SUFFIX, videoCsvPath);
                    List<VideoData> videoDataList = parseVideoCsv(videoCsvPath);
                    result.setVideoDataList(videoDataList);
                    log.info("video-{}解析成功: 共{}条记录", FILE_SUFFIX, videoDataList.size());
                } else {
                    log.warn("未找到video-{}文件", FILE_SUFFIX);
                    result.setVideoDataList(new ArrayList<>());
                }
            } catch (Exception e) {
                log.error("解析video-{}失败: {}", FILE_SUFFIX, e.getMessage(), e);
                result.setVideoDataList(new ArrayList<>());
            }

            log.info("所有数据解析完成");

        } finally {
            // 清理临时目录
            try {
                deleteDirectory(extractDir);
                log.info("临时解压目录已清理: {}", extractDir);
            } catch (Exception e) {
                log.warn("清理临时目录失败: {}", e.getMessage());
            }
        }

        return result;
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

