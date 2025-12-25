package com.datacollect.service;

import com.datacollect.dto.TaskInfoDTO;
import com.datacollect.entity.LostData;
import com.datacollect.entity.NetworkData;
import com.datacollect.entity.RttData;
import com.datacollect.entity.SpeedData;
import com.datacollect.entity.VideoData;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Autowired
    private LostDataService lostDataService;

    @Autowired
    private VideoDataService videoDataService;

    @Autowired
    private NetworkDataService networkDataService;

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
                isCompressedFile(fileName) ? taskInfoHolder : null,
                false  // 端侧FTP
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
                ftpConfig.getCheckMd5() != null && ftpConfig.getCheckMd5() == 1,
                null,
                true  // 网络侧FTP
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
        return processFtpFile(serverAddress, account, password, directory, fileName, checkMd5, null, false);
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
     * @param isNetworkFtp 是否为网络侧FTP
     * @return 上传后的文件URL
     * @throws IOException IO异常
     */
    private String processFtpFile(String serverAddress, String account, String password,
                                 String directory, String fileName, boolean checkMd5, 
                                 List<TaskInfoDTO> taskInfoHolder, boolean isNetworkFtp) throws IOException {
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

            // 3. 如果是网络侧压缩包，解析CSV文件
            if (isNetworkFtp && isCompressedFile(fileName)) {
                try {
                    List<NetworkData> networkDataList = ClientFileProcessor.extractAndParseNetworkCsv(localFilePath);
                    if (networkDataList != null && !networkDataList.isEmpty()) {
                        boolean saved = networkDataService.batchSaveNetworkData(networkDataList, fileName);
                        if (saved) {
                            log.info("NetworkData saved to database successfully - fileName: {}, count: {}", 
                                    fileName, networkDataList.size());
                        } else {
                            log.warn("Failed to save NetworkData to database - fileName: {}", fileName);
                        }
                    }
                } catch (Exception e) {
                    log.error("Error parsing or saving NetworkData - fileName: {}, error: {}", 
                            fileName, e.getMessage(), e);
                    // 不抛出异常，继续处理文件上传
                }
            }

            // 4. 如果是端侧压缩包，统一解析所有数据（只解压一次）
            if (taskInfoHolder != null && isCompressedFile(fileName)) {
                try {
                    // 统一解压并解析所有数据
                    ClientFileProcessor.ClientDataResult clientDataResult = ClientFileProcessor.extractAndParseAllClientData(localFilePath);
                    
                    TaskInfoDTO taskInfo = clientDataResult.getTaskInfo();
                    if (taskInfo != null) {
                        taskInfoHolder.add(taskInfo);
                        log.info("解析taskinfo.json成功: taskId={}, app={}, service={}",
                                taskInfo.getTaskId(), taskInfo.getApp(), taskInfo.getService());
                        
                        // 保存speed数据
                        try {
                            List<SpeedData> speedDataList = clientDataResult.getSpeedDataList();
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
                            log.error("Error saving SpeedData - taskId: {}, error: {}", 
                                    taskInfo.getTaskId(), e.getMessage(), e);
                        }

                        // 保存vmos数据
                        try {
                            List<VmosData> vmosDataList = clientDataResult.getVmosDataList();
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
                            log.error("Error saving VmosData - taskId: {}, error: {}", 
                                    taskInfo.getTaskId(), e.getMessage(), e);
                        }

                        // 保存rtt数据
                        try {
                            List<RttData> rttDataList = clientDataResult.getRttDataList();
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
                            log.error("Error saving RttData - taskId: {}, error: {}", 
                                    taskInfo.getTaskId(), e.getMessage(), e);
                        }

                        // 保存lost数据
                        try {
                            List<LostData> lostDataList = clientDataResult.getLostDataList();
                            if (lostDataList != null && !lostDataList.isEmpty()) {
                                boolean saved = lostDataService.batchSaveLostData(lostDataList, taskInfo.getTaskId());
                                if (saved) {
                                    log.info("LostData saved to database successfully - taskId: {}, count: {}", 
                                            taskInfo.getTaskId(), lostDataList.size());
                                } else {
                                    log.warn("Failed to save LostData to database - taskId: {}", taskInfo.getTaskId());
                                }
                            }
                        } catch (Exception e) {
                            log.error("Error saving LostData - taskId: {}, error: {}", 
                                    taskInfo.getTaskId(), e.getMessage(), e);
                        }

                        // 保存video数据
                        try {
                            List<VideoData> videoDataList = clientDataResult.getVideoDataList();
                            if (videoDataList != null && !videoDataList.isEmpty()) {
                                boolean saved = videoDataService.batchSaveVideoData(videoDataList, taskInfo.getTaskId());
                                if (saved) {
                                    log.info("VideoData saved to database successfully - taskId: {}, count: {}", 
                                            taskInfo.getTaskId(), videoDataList.size());
                                } else {
                                    log.warn("Failed to save VideoData to database - taskId: {}", taskInfo.getTaskId());
                                }
                            }
                        } catch (Exception e) {
                            log.error("Error saving VideoData - taskId: {}, error: {}", 
                                    taskInfo.getTaskId(), e.getMessage(), e);
                        }
                    } else {
                        log.warn("未能解析taskinfo.json");
                    }
                } catch (Exception e) {
                    log.error("解析端侧文件失败: {}", e.getMessage(), e);
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
                            fileTaskInfoHolder,
                            false  // 端侧FTP
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
                ftpConfig.getCheckMd5() != null && ftpConfig.getCheckMd5() == 1,
                true  // 网络侧FTP
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
        return processFtpFilesByDate(serverAddress, account, password, dateDirectory, checkMd5, false);
    }

    /**
     * 处理指定日期目录下的所有FTP文件
     *
     * @param serverAddress FTP服务器地址
     * @param account 账户
     * @param password 密码
     * @param dateDirectory 日期目录路径
     * @param checkMd5 是否校验MD5
     * @param isNetworkFtp 是否为网络侧FTP
     * @return 上传后的文件URL列表
     * @throws IOException IO异常
     */
    private List<String> processFtpFilesByDate(String serverAddress, String account, String password,
                                               String dateDirectory, boolean checkMd5, boolean isNetworkFtp) throws IOException {
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
                String fileUrl = processFtpFile(serverAddress, account, password, dateDirectory, fileName, checkMd5, null, isNetworkFtp);
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

    // ========== 本地文件处理（用于测试验证） ==========

    /**
     * 处理本地端侧文件（用于测试验证，不依赖FTP服务器）
     * 如果是压缩包，会解压并解析taskinfo.json、speed-10s.xlsx、vmos-10s.xlsx、rtt-10s.csv、lost-10s.csv、video-10s.csv
     *
     * @param filePath 本地文件路径
     * @return 处理结果信息
     * @throws IOException IO异常
     */
    public Map<String, Object> processLocalClientFile(String filePath) throws IOException {
        log.info("Processing local client file: {}", filePath);

        Path localPath = Paths.get(filePath);
        if (!Files.exists(localPath)) {
            throw new IOException("文件不存在: " + filePath);
        }

        String fileName = localPath.getFileName().toString();
        Map<String, Object> result = new HashMap<>();
        result.put("fileName", fileName);
        result.put("filePath", filePath);

        // 如果是压缩包，解析文件内容
        if (isCompressedFile(fileName)) {
            List<TaskInfoDTO> taskInfoList = new ArrayList<>();

            try {
                // 统一解压并解析所有数据（只解压一次）
                ClientFileProcessor.ClientDataResult clientDataResult = ClientFileProcessor.extractAndParseAllClientData(filePath);
                
                TaskInfoDTO taskInfo = clientDataResult.getTaskInfo();
                if (taskInfo != null) {
                    taskInfoList.add(taskInfo);
                    log.info("解析taskinfo.json成功: taskId={}, app={}, service={}",
                            taskInfo.getTaskId(), taskInfo.getApp(), taskInfo.getService());

                    // 保存taskinfo到数据库
                    boolean saved = taskInfoService.saveTaskInfo(taskInfo);
                    if (saved) {
                        log.info("TaskInfo saved to database successfully - taskId: {}", taskInfo.getTaskId());
                    } else {
                        log.warn("Failed to save TaskInfo to database - taskId: {}", taskInfo.getTaskId());
                    }

                    // 保存speed数据
                    try {
                        List<SpeedData> speedDataList = clientDataResult.getSpeedDataList();
                        if (speedDataList != null && !speedDataList.isEmpty()) {
                            boolean savedSpeed = speedDataService.batchSaveSpeedData(speedDataList, taskInfo.getTaskId());
                            if (savedSpeed) {
                                log.info("SpeedData saved to database successfully - taskId: {}, count: {}",
                                        taskInfo.getTaskId(), speedDataList.size());
                                result.put("speedDataCount", speedDataList.size());
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error saving SpeedData - taskId: {}, error: {}",
                                taskInfo.getTaskId(), e.getMessage(), e);
                    }

                    // 保存vmos数据
                    try {
                        List<VmosData> vmosDataList = clientDataResult.getVmosDataList();
                        if (vmosDataList != null && !vmosDataList.isEmpty()) {
                            boolean savedVmos = vmosDataService.batchSaveVmosData(vmosDataList, taskInfo.getTaskId());
                            if (savedVmos) {
                                log.info("VmosData saved to database successfully - taskId: {}, count: {}",
                                        taskInfo.getTaskId(), vmosDataList.size());
                                result.put("vmosDataCount", vmosDataList.size());
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error saving VmosData - taskId: {}, error: {}",
                                taskInfo.getTaskId(), e.getMessage(), e);
                    }

                    // 保存rtt数据
                    try {
                        List<RttData> rttDataList = clientDataResult.getRttDataList();
                        if (rttDataList != null && !rttDataList.isEmpty()) {
                            boolean savedRtt = rttDataService.batchSaveRttData(rttDataList, taskInfo.getTaskId());
                            if (savedRtt) {
                                log.info("RttData saved to database successfully - taskId: {}, count: {}",
                                        taskInfo.getTaskId(), rttDataList.size());
                                result.put("rttDataCount", rttDataList.size());
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error saving RttData - taskId: {}, error: {}",
                                taskInfo.getTaskId(), e.getMessage(), e);
                    }

                    // 保存lost数据
                    try {
                        List<LostData> lostDataList = clientDataResult.getLostDataList();
                        if (lostDataList != null && !lostDataList.isEmpty()) {
                            boolean savedLost = lostDataService.batchSaveLostData(lostDataList, taskInfo.getTaskId());
                            if (savedLost) {
                                log.info("LostData saved to database successfully - taskId: {}, count: {}",
                                        taskInfo.getTaskId(), lostDataList.size());
                                result.put("lostDataCount", lostDataList.size());
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error saving LostData - taskId: {}, error: {}",
                                taskInfo.getTaskId(), e.getMessage(), e);
                    }

                    // 保存video数据
                    try {
                        List<VideoData> videoDataList = clientDataResult.getVideoDataList();
                        if (videoDataList != null && !videoDataList.isEmpty()) {
                            boolean savedVideo = videoDataService.batchSaveVideoData(videoDataList, taskInfo.getTaskId());
                            if (savedVideo) {
                                log.info("VideoData saved to database successfully - taskId: {}, count: {}",
                                        taskInfo.getTaskId(), videoDataList.size());
                                result.put("videoDataCount", videoDataList.size());
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error saving VideoData - taskId: {}, error: {}",
                                taskInfo.getTaskId(), e.getMessage(), e);
                    }

                    result.put("taskInfo", taskInfo);
                    result.put("taskId", taskInfo.getTaskId());
                } else {
                    log.warn("未解析到taskinfo.json");
                }
            } catch (Exception e) {
                log.error("Error parsing taskinfo.json: {}", e.getMessage(), e);
                result.put("error", "解析taskinfo.json失败: " + e.getMessage());
            }

            result.put("taskInfoList", taskInfoList);
        } else {
            log.info("文件不是压缩包，跳过解析");
            result.put("message", "文件不是压缩包，跳过解析");
        }

        result.put("success", true);
        return result;
    }

    /**
     * 处理本地网络侧文件（用于测试验证，不依赖FTP服务器）
     * 如果是压缩包，会解压并解析CSV文件
     *
     * @param filePath 本地文件路径
     * @return 处理结果信息
     * @throws IOException IO异常
     */
    public Map<String, Object> processLocalNetworkFile(String filePath) throws IOException {
        log.info("Processing local network file: {}", filePath);

        Path localPath = Paths.get(filePath);
        if (!Files.exists(localPath)) {
            throw new IOException("文件不存在: " + filePath);
        }

        String fileName = localPath.getFileName().toString();
        Map<String, Object> result = new HashMap<>();
        result.put("fileName", fileName);
        result.put("filePath", filePath);

        // 如果是压缩包，解析CSV文件
        if (isCompressedFile(fileName)) {
            try {
                List<NetworkData> networkDataList = ClientFileProcessor.extractAndParseNetworkCsv(filePath);
                if (networkDataList != null && !networkDataList.isEmpty()) {
                    boolean saved = networkDataService.batchSaveNetworkData(networkDataList, fileName);
                    if (saved) {
                        log.info("NetworkData saved to database successfully - fileName: {}, count: {}",
                                fileName, networkDataList.size());
                        result.put("networkDataCount", networkDataList.size());
                        result.put("success", true);
                    } else {
                        log.warn("Failed to save NetworkData to database - fileName: {}", fileName);
                        result.put("success", false);
                        result.put("error", "保存到数据库失败");
                    }
                } else {
                    log.warn("未解析到网络侧数据");
                    result.put("success", false);
                    result.put("error", "未解析到网络侧数据");
                }
            } catch (Exception e) {
                log.error("Error parsing or saving NetworkData - fileName: {}, error: {}",
                        fileName, e.getMessage(), e);
                result.put("success", false);
                result.put("error", "解析或保存失败: " + e.getMessage());
            }
        } else {
            log.info("文件不是压缩包，跳过解析");
            result.put("success", false);
            result.put("error", "文件不是压缩包，跳过解析");
        }

        return result;
    }
}

