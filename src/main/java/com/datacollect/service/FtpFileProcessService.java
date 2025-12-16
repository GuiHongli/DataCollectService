package com.datacollect.service;

import com.datacollect.dto.TaskInfoDTO;
import com.datacollect.entity.RttData;
import com.datacollect.entity.SpeedData;
import com.datacollect.entity.VmosData;
import com.datacollect.entity.TestSettingsClientFtp;
import com.datacollect.entity.TestSettingsNetworkFtp;
import com.datacollect.util.ClientFileProcessor;
import com.datacollect.util.FtpClientUtil;
import com.datacollect.util.GoHttpServerClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * FTP文件处理服务
 * 负责从FTP服务器下载文件、MD5校验、上传到gohttpserver
 */
@Slf4j
@Service
public class FtpFileProcessService {

    @Autowired
    private GoHttpServerClient goHttpServerClient;

    @Autowired
    private TestSettingsClientFtpService clientFtpService;

    @Autowired
    private TestSettingsNetworkFtpService networkFtpService;

    @Autowired
    private TaskInfoService taskInfoService;

    @Autowired
    private SpeedDataService speedDataService;

    @Autowired
    private VmosDataService vmosDataService;

    @Autowired
    private RttDataService rttDataService;

    /**
     * 从端侧FTP服务器下载文件并上传到gohttpserver
     * 如果是压缩包，会解压并解析taskinfo.json
     *
     * @param fileName 文件名
     * @return 上传后的文件URL
     * @throws IOException IO异常
     */
    public String processClientFtpFile(String fileName) throws IOException {
        log.info("Processing client FTP file: {}", fileName);

        // 获取端侧FTP服务器配置
        TestSettingsClientFtp ftpConfig = clientFtpService.getClientFtpConfig();
        if (ftpConfig == null) {
            throw new IOException("Client FTP server configuration not found");
        }

        // 用于存储解析的taskinfo信息
        List<TaskInfoDTO> taskInfoHolder = new ArrayList<>();
        
        String fileUrl = processFtpFile(
                ftpConfig.getServerAddress(),
                ftpConfig.getAccount(),
                ftpConfig.getPassword(),
                ftpConfig.getDirectory(),
                fileName,
                ftpConfig.getCheckMd5() != null && ftpConfig.getCheckMd5() == 1,
                isCompressedFile(fileName) ? taskInfoHolder : null
        );

        // 如果解析到了taskinfo，保存到数据库
        if (!taskInfoHolder.isEmpty()) {
            TaskInfoDTO taskInfo = taskInfoHolder.get(0);
            log.info("端侧文件taskinfo解析完成: taskId={}, app={}, service={}, nation={}, operator={}, deviceId={}",
                    taskInfo.getTaskId(), taskInfo.getApp(), taskInfo.getService(),
                    taskInfo.getNation(), taskInfo.getOperator(), taskInfo.getDeviceId());
            log.info("端侧文件数据报告: stunNumber={}, stunRate={}, avgUplinkRtt={}, avgDownlinkRtt={}, avgUplinkSpeed={}, avgDownlinkSpeed={}",
                    taskInfo.getSummary() != null ? taskInfo.getSummary().get("stunNumber") : null,
                    taskInfo.getSummary() != null ? taskInfo.getSummary().get("stunRate") : null,
                    taskInfo.getSummary() != null ? taskInfo.getSummary().get("avgUplinkRtt") : null,
                    taskInfo.getSummary() != null ? taskInfo.getSummary().get("avgDownlinkRtt") : null,
                    taskInfo.getSummary() != null ? taskInfo.getSummary().get("avgUplinkSpeed") : null,
                    taskInfo.getSummary() != null ? taskInfo.getSummary().get("avgDownlinkSpeed") : null);
            // 保存taskInfo到数据库
            try {
                boolean saved = taskInfoService.saveTaskInfo(taskInfo);
                if (saved) {
                    log.info("TaskInfo saved to database successfully - taskId: {}", taskInfo.getTaskId());
                } else {
                    log.warn("Failed to save TaskInfo to database - taskId: {}", taskInfo.getTaskId());
                }
            } catch (Exception e) {
                log.error("Error saving TaskInfo to database - taskId: {}, error: {}", taskInfo.getTaskId(), e.getMessage(), e);
            }

        }

        return fileUrl;
    }

    /**
     * 判断文件是否为压缩包
     *
     * @param fileName 文件名
     * @return 是否为压缩包
     */
    private boolean isCompressedFile(String fileName) {
        if (fileName == null) {
            return false;
        }
        String lowerName = fileName.toLowerCase();
        return lowerName.endsWith(".zip") ||
               lowerName.endsWith(".rar") ||
               lowerName.endsWith(".7z") ||
               lowerName.endsWith(".tar") ||
               lowerName.endsWith(".gz");
    }

    /**
     * 从网络侧FTP服务器下载文件并上传到gohttpserver
     *
     * @param fileName 文件名
     * @return 上传后的文件URL
     * @throws IOException IO异常
     */
    public String processNetworkFtpFile(String fileName) throws IOException {
        log.info("Processing network FTP file: {}", fileName);

        // 获取网络侧FTP服务器配置
        TestSettingsNetworkFtp ftpConfig = networkFtpService.getNetworkFtpConfig();
        if (ftpConfig == null) {
            throw new IOException("Network FTP server configuration not found");
        }

        return processFtpFile(
                ftpConfig.getServerAddress(),
                ftpConfig.getAccount(),
                ftpConfig.getPassword(),
                ftpConfig.getDirectory(),
                fileName,
                ftpConfig.getCheckMd5() != null && ftpConfig.getCheckMd5() == 1
        );
    }

    /**
     * 处理FTP文件的完整流程：下载、MD5校验、上传到gohttpserver
     *
     * @param serverAddress FTP服务器地址
     * @param account 账户
     * @param password 密码
     * @param directory 目录
     * @param fileName 文件名
     * @param checkMd5 是否校验MD5
     * @return 上传后的文件URL
     * @throws IOException IO异常
     */
    private String processFtpFile(String serverAddress, String account, String password,
                                 String directory, String fileName, boolean checkMd5) throws IOException {
        return processFtpFile(serverAddress, account, password, directory, fileName, checkMd5, null);
    }

    /**
     * 处理FTP文件的完整流程：下载、MD5校验、上传到gohttpserver
     *
     * @param serverAddress FTP服务器地址
     * @param account 账户
     * @param password 密码
     * @param directory 目录
     * @param fileName 文件名
     * @param checkMd5 是否校验MD5
     * @param taskInfoHolder 用于返回解析的taskinfo信息（仅端侧文件使用）
     * @return 上传后的文件URL
     * @throws IOException IO异常
     */
    private String processFtpFile(String serverAddress, String account, String password,
                                 String directory, String fileName, boolean checkMd5, 
                                 List<TaskInfoDTO> taskInfoHolder) throws IOException {
        // 创建临时目录
        Path tempDir = Files.createTempDirectory("ftp_download_");
        String localFilePath = tempDir.resolve(fileName).toString();

        try {
            // 1. 从FTP服务器下载文件
            log.info("Downloading file from FTP server: {}", fileName);
            boolean downloadSuccess = FtpClientUtil.downloadFile(
                    serverAddress, account, password, directory, fileName, localFilePath
            );

            if (!downloadSuccess) {
                throw new IOException("Failed to download file from FTP server: " + fileName);
            }

            log.info("File downloaded successfully: {}", localFilePath);

            // 2. 如果开启MD5校验，进行校验
            if (checkMd5) {
                log.info("MD5 check enabled, verifying file integrity");
                String md5FileName = fileName + ".md5";
                String md5Content = null;

                try {
                    // 从FTP服务器读取MD5文件
                    md5Content = FtpClientUtil.readFileContent(
                            serverAddress, account, password, directory, md5FileName
                    );
                    log.info("MD5 file content: {}", md5Content);
                } catch (IOException e) {
                    log.warn("Failed to read MD5 file: {}. Error: {}", md5FileName, e.getMessage());
                    throw new IOException("MD5 file not found or cannot be read: " + md5FileName, e);
                }

                // 计算下载文件的MD5值
                String calculatedMd5 = calculateFileMd5(localFilePath);
                log.info("Calculated MD5: {}", calculatedMd5);

                // 从MD5文件内容中提取MD5值（格式可能是：MD5值 文件名 或 只有MD5值）
                String expectedMd5 = extractMd5FromContent(md5Content, fileName);
                log.info("Expected MD5: {}", expectedMd5);

                // 比较MD5值
                if (!calculatedMd5.equalsIgnoreCase(expectedMd5)) {
                    throw new IOException(String.format(
                            "MD5 verification failed. Expected: %s, Calculated: %s",
                            expectedMd5, calculatedMd5
                    ));
                }

                log.info("MD5 verification passed");
            }

            // 3. 如果是端侧压缩包，解析taskinfo.json和speed-10s.csv
            if (taskInfoHolder != null && isCompressedFile(fileName)) {
                try {
                    TaskInfoDTO taskInfo = ClientFileProcessor.extractAndParseTaskInfo(localFilePath);
                    if (taskInfo != null) {
                        taskInfoHolder.add(taskInfo);
                        log.info("解析taskinfo.json成功: taskId={}, app={}, service={}",
                                taskInfo.getTaskId(), taskInfo.getApp(), taskInfo.getService());
                        
                        // 解析并保存speed-10s.csv
                        try {
                            List<SpeedData> speedDataList = ClientFileProcessor.extractAndParseSpeedCsv(localFilePath);
                            if (speedDataList != null && !speedDataList.isEmpty()) {
                                boolean saved = speedDataService.batchSaveSpeedData(speedDataList, taskInfo.getTaskId());
                                if (saved) {
                                    log.info("SpeedData saved to database successfully - taskId: {}, count: {}", 
                                            taskInfo.getTaskId(), speedDataList.size());
                                } else {
                                    log.warn("Failed to save SpeedData to database - taskId: {}", taskInfo.getTaskId());
                                }
                            }
                        } catch (Exception e) {
                            log.error("Error parsing or saving SpeedData - taskId: {}, error: {}", 
                                    taskInfo.getTaskId(), e.getMessage(), e);
                            // 不抛出异常，继续处理
                        }

                        // 解析并保存vmos-10s.xlsx
                        try {
                            List<VmosData> vmosDataList = ClientFileProcessor.extractAndParseVmosExcel(localFilePath);
                            if (vmosDataList != null && !vmosDataList.isEmpty()) {
                                boolean saved = vmosDataService.batchSaveVmosData(vmosDataList, taskInfo.getTaskId());
                                if (saved) {
                                    log.info("VmosData saved to database successfully - taskId: {}, count: {}", 
                                            taskInfo.getTaskId(), vmosDataList.size());
                                } else {
                                    log.warn("Failed to save VmosData to database - taskId: {}", taskInfo.getTaskId());
                                }
                            }
                        } catch (Exception e) {
                            log.error("Error parsing or saving VmosData - taskId: {}, error: {}", 
                                    taskInfo.getTaskId(), e.getMessage(), e);
                            // 不抛出异常，继续处理
                        }

                        // 解析并保存rtt-10s.csv
                        try {
                            List<RttData> rttDataList = ClientFileProcessor.extractAndParseRttCsv(localFilePath);
                            if (rttDataList != null && !rttDataList.isEmpty()) {
                                boolean saved = rttDataService.batchSaveRttData(rttDataList, taskInfo.getTaskId());
                                if (saved) {
                                    log.info("RttData saved to database successfully - taskId: {}, count: {}", 
                                            taskInfo.getTaskId(), rttDataList.size());
                                } else {
                                    log.warn("Failed to save RttData to database - taskId: {}", taskInfo.getTaskId());
                                }
                            }
                        } catch (Exception e) {
                            log.error("Error parsing or saving RttData - taskId: {}, error: {}", 
                                    taskInfo.getTaskId(), e.getMessage(), e);
                            // 不抛出异常，继续处理
                        }
                    } else {
                        log.warn("未能解析taskinfo.json");
                    }
                } catch (Exception e) {
                    log.error("解析端侧文件taskinfo.json失败: {}", e.getMessage(), e);
                    // 不抛出异常，继续处理文件上传
                }
            }

            // 4. 上传文件到gohttpserver
            log.info("Uploading file to gohttpserver: {}", fileName);
            String fileUrl = goHttpServerClient.uploadLocalFile(localFilePath, fileName);
            log.info("File uploaded to gohttpserver successfully: {}", fileUrl);

            return fileUrl;

        } finally {
            // 清理临时文件
            try {
                Files.deleteIfExists(Paths.get(localFilePath));
                Files.deleteIfExists(tempDir);
                log.info("Temporary files cleaned up");
            } catch (IOException e) {
                log.warn("Failed to clean up temporary files: {}", e.getMessage());
            }
        }
    }

    /**
     * 计算文件的MD5值
     *
     * @param filePath 文件路径
     * @return MD5值（小写）
     * @throws IOException IO异常
     */
    private String calculateFileMd5(String filePath) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            Path path = Paths.get(filePath);
            byte[] fileBytes = Files.readAllBytes(path);

            byte[] digest = md.digest(fileBytes);

            // 转换为十六进制字符串
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();
        } catch (Exception e) {
            throw new IOException("Failed to calculate MD5: " + e.getMessage(), e);
        }
    }

    /**
     * 从MD5文件内容中提取MD5值
     * MD5文件格式可能是：
     * - "md5值 文件名"
     * - "md5值"
     * - "MD5 (文件名) = md5值"
     *
     * @param md5Content MD5文件内容
     * @param fileName 文件名
     * @return MD5值
     */
    private String extractMd5FromContent(String md5Content, String fileName) {
        if (md5Content == null || md5Content.trim().isEmpty()) {
            throw new IllegalArgumentException("MD5 content is empty");
        }

        // 移除所有空白字符
        String content = md5Content.trim().replaceAll("\\s+", " ");

        // 格式1: "md5值 文件名"
        if (content.contains(" ")) {
            String[] parts = content.split("\\s+", 2);
            if (parts.length >= 1) {
                String md5 = parts[0].trim();
                // 验证MD5格式（32个十六进制字符）
                if (md5.matches("^[0-9a-fA-F]{32}$")) {
                    return md5.toLowerCase();
                }
            }
        }

        // 格式2: "MD5 (文件名) = md5值"
        if (content.contains("=")) {
            String[] parts = content.split("=");
            if (parts.length >= 2) {
                String md5 = parts[parts.length - 1].trim();
                if (md5.matches("^[0-9a-fA-F]{32}$")) {
                    return md5.toLowerCase();
                }
            }
        }

        // 格式3: 直接是MD5值
        String md5 = content.trim();
        if (md5.matches("^[0-9a-fA-F]{32}$")) {
            return md5.toLowerCase();
        }

        throw new IllegalArgumentException("Cannot extract MD5 from content: " + md5Content);
    }

    /**
     * 批量处理FTP文件
     *
     * @param isClientFtp 是否为端侧FTP
     * @param fileNames 文件名列表
     * @return 上传后的文件URL列表
     */
    public List<String> processFtpFiles(boolean isClientFtp, List<String> fileNames) throws IOException {
        List<String> fileUrls = new ArrayList<>();
        List<String> failedFiles = new ArrayList<>();

        for (String fileName : fileNames) {
            try {
                String fileUrl;
                if (isClientFtp) {
                    fileUrl = processClientFtpFile(fileName);
                } else {
                    fileUrl = processNetworkFtpFile(fileName);
                }
                fileUrls.add(fileUrl);
                log.info("File processed successfully: {} -> {}", fileName, fileUrl);
            } catch (Exception e) {
                log.error("Failed to process file: {}. Error: {}", fileName, e.getMessage(), e);
                failedFiles.add(fileName);
            }
        }

        if (!failedFiles.isEmpty()) {
            throw new IOException("Some files failed to process: " + String.join(", ", failedFiles));
        }

        return fileUrls;
    }

    /**
     * 从端侧FTP服务器指定日期目录下获取所有文件并上传到gohttpserver
     * 如果是压缩包，会解压并解析taskinfo.json
     *
     * @param dateStr 日期字符串，格式：YYYY-MM-DD，如：2025-12-05
     * @return 上传后的文件URL列表
     * @throws IOException IO异常
     */
    public List<String> processClientFtpFilesByDate(String dateStr) throws IOException {
        return processClientFtpFilesByDate(dateStr, null);
    }

    /**
     * 从端侧FTP服务器指定日期目录下获取所有文件并上传到gohttpserver
     * 如果是压缩包，会解压并解析taskinfo.json
     *
     * @param dateStr 日期字符串，格式：YYYY-MM-DD，如：2025-12-05
     * @param taskInfoList 用于返回解析的taskinfo信息列表
     * @return 上传后的文件URL列表
     * @throws IOException IO异常
     */
    public List<String> processClientFtpFilesByDate(String dateStr, List<TaskInfoDTO> taskInfoList) throws IOException {
        log.info("Processing client FTP files for date: {}", dateStr);

        // 获取端侧FTP服务器配置
        TestSettingsClientFtp ftpConfig = clientFtpService.getClientFtpConfig();
        if (ftpConfig == null) {
            throw new IOException("Client FTP server configuration not found");
        }

        // 构建日期目录路径：固定目录/日期目录
        String baseDirectory = ftpConfig.getDirectory();
        String dateDirectory = buildDateDirectoryPath(baseDirectory, dateStr);

        // 列出日期目录下的所有文件
        List<String> fileNames = FtpClientUtil.listFiles(
                ftpConfig.getServerAddress(),
                ftpConfig.getAccount(),
                ftpConfig.getPassword(),
                dateDirectory
        );

        if (fileNames.isEmpty()) {
            log.warn("No files found in date directory: {}", dateDirectory);
            return new ArrayList<>();
        }

        log.info("Found {} files in date directory: {}", fileNames.size(), dateDirectory);

        // 处理每个文件
        List<String> fileUrls = new ArrayList<>();
        List<String> failedFiles = new ArrayList<>();
        if (taskInfoList == null) {
            taskInfoList = new ArrayList<>();
        }

        for (String fileName : fileNames) {
            try {
                // 如果是压缩包，需要收集taskinfo信息
                if (isCompressedFile(fileName)) {
                    List<TaskInfoDTO> fileTaskInfoHolder = new ArrayList<>();
                    String fileUrl = processFtpFile(
                            ftpConfig.getServerAddress(),
                            ftpConfig.getAccount(),
                            ftpConfig.getPassword(),
                            dateDirectory,
                            fileName,
                            ftpConfig.getCheckMd5() != null && ftpConfig.getCheckMd5() == 1,
                            fileTaskInfoHolder
                    );
                    fileUrls.add(fileUrl);
                    // 收集taskinfo信息
                    if (!fileTaskInfoHolder.isEmpty()) {
                        taskInfoList.addAll(fileTaskInfoHolder);
                    }
                    log.info("File processed successfully: {} -> {}", fileName, fileUrl);
                } else {
                    // 非压缩包，直接处理
                    String fileUrl = processClientFtpFile(fileName);
                    fileUrls.add(fileUrl);
                    log.info("File processed successfully: {} -> {}", fileName, fileUrl);
                }
            } catch (Exception e) {
                log.error("Failed to process file: {}. Error: {}", fileName, e.getMessage(), e);
                failedFiles.add(fileName);
            }
        }

        if (!failedFiles.isEmpty()) {
            log.warn("Some files failed to process: {}", String.join(", ", failedFiles));
        }

        // 批量保存taskinfo到数据库
        if (taskInfoList != null && !taskInfoList.isEmpty()) {
            int savedCount = 0;
            int failedCount = 0;
            for (TaskInfoDTO taskInfo : taskInfoList) {
                try {
                    boolean saved = taskInfoService.saveTaskInfo(taskInfo);
                    if (saved) {
                        savedCount++;
                        log.debug("TaskInfo saved to database - taskId: {}", taskInfo.getTaskId());
                    } else {
                        failedCount++;
                        log.warn("Failed to save TaskInfo to database - taskId: {}", taskInfo.getTaskId());
                    }
                } catch (Exception e) {
                    failedCount++;
                    log.error("Error saving TaskInfo to database - taskId: {}, error: {}", 
                            taskInfo.getTaskId(), e.getMessage(), e);
                }
            }
            log.info("Batch saved TaskInfo: {} succeeded, {} failed, total: {}", 
                    savedCount, failedCount, taskInfoList.size());
        }

        log.info("Processed {} files successfully, {} files failed, {} taskinfo parsed", 
                fileUrls.size(), failedFiles.size(), taskInfoList != null ? taskInfoList.size() : 0);
        return fileUrls;
    }

    /**
     * 从网络侧FTP服务器指定日期目录下获取所有文件并上传到gohttpserver
     *
     * @param dateStr 日期字符串，格式：YYYY-MM-DD，如：2025-12-05
     * @return 上传后的文件URL列表
     * @throws IOException IO异常
     */
    public List<String> processNetworkFtpFilesByDate(String dateStr) throws IOException {
        log.info("Processing network FTP files for date: {}", dateStr);

        // 获取网络侧FTP服务器配置
        TestSettingsNetworkFtp ftpConfig = networkFtpService.getNetworkFtpConfig();
        if (ftpConfig == null) {
            throw new IOException("Network FTP server configuration not found");
        }

        // 构建日期目录路径：固定目录/日期目录
        String baseDirectory = ftpConfig.getDirectory();
        String dateDirectory = buildDateDirectoryPath(baseDirectory, dateStr);

        return processFtpFilesByDate(
                ftpConfig.getServerAddress(),
                ftpConfig.getAccount(),
                ftpConfig.getPassword(),
                dateDirectory,
                ftpConfig.getCheckMd5() != null && ftpConfig.getCheckMd5() == 1
        );
    }

    /**
     * 构建日期目录路径
     *
     * @param baseDirectory 基础目录
     * @param dateStr 日期字符串，格式：YYYY-MM-DD
     * @return 完整的日期目录路径
     */
    private String buildDateDirectoryPath(String baseDirectory, String dateStr) {
        // 验证日期格式
        if (!dateStr.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            throw new IllegalArgumentException("Invalid date format. Expected YYYY-MM-DD, got: " + dateStr);
        }

        if (baseDirectory == null || baseDirectory.trim().isEmpty()) {
            return dateStr;
        }

        // 确保目录路径格式正确（使用/分隔符）
        String normalizedBase = baseDirectory.trim().replaceAll("[/\\\\]+", "/");
        if (normalizedBase.endsWith("/")) {
            normalizedBase = normalizedBase.substring(0, normalizedBase.length() - 1);
        }

        return normalizedBase + "/" + dateStr;
    }

    /**
     * 处理指定日期目录下的所有FTP文件
     *
     * @param serverAddress FTP服务器地址
     * @param account 账户
     * @param password 密码
     * @param dateDirectory 日期目录路径
     * @param checkMd5 是否校验MD5
     * @return 上传后的文件URL列表
     * @throws IOException IO异常
     */
    private List<String> processFtpFilesByDate(String serverAddress, String account, String password,
                                               String dateDirectory, boolean checkMd5) throws IOException {
        log.info("Processing FTP files from date directory: {}", dateDirectory);

        // 1. 列出日期目录下的所有文件
        List<String> fileNames = FtpClientUtil.listFiles(serverAddress, account, password, dateDirectory);
        
        if (fileNames.isEmpty()) {
            log.warn("No files found in date directory: {}", dateDirectory);
            return new ArrayList<>();
        }

        log.info("Found {} files in date directory: {}", fileNames.size(), dateDirectory);

        // 2. 处理每个文件
        List<String> fileUrls = new ArrayList<>();
        List<String> failedFiles = new ArrayList<>();

        for (String fileName : fileNames) {
            try {
                String fileUrl = processFtpFile(serverAddress, account, password, dateDirectory, fileName, checkMd5);
                fileUrls.add(fileUrl);
                log.info("File processed successfully: {} -> {}", fileName, fileUrl);
            } catch (Exception e) {
                log.error("Failed to process file: {}. Error: {}", fileName, e.getMessage(), e);
                failedFiles.add(fileName);
            }
        }

        if (!failedFiles.isEmpty()) {
            log.warn("Some files failed to process: {}", String.join(", ", failedFiles));
        }

        log.info("Processed {} files successfully, {} files failed", fileUrls.size(), failedFiles.size());
        return fileUrls;
    }
}

