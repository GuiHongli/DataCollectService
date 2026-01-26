package com.datacollect.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datacollect.common.Result;
import com.datacollect.entity.TestSettingsClientFtp;
import com.datacollect.entity.TestSettingsDeviceImsiMapping;
import com.datacollect.entity.TestSettingsNetworkFtp;
import com.datacollect.dto.TaskInfoDTO;
import com.datacollect.service.FtpFileProcessService;
import com.datacollect.service.TestSettingsClientFtpService;
import com.datacollect.service.TestSettingsDeviceImsiMappingService;
import com.datacollect.service.TestSettingsNetworkFtpService;
import com.datacollect.service.TestSettingsTimeConfigService;
import com.datacollect.entity.TestSettingsTimeConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@RestController
@RequestMapping("/test-settings")
@Validated
public class TestSettingsController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestSettingsController.class);

    @Autowired
    private TestSettingsClientFtpService clientFtpService;

    @Autowired
    private TestSettingsNetworkFtpService networkFtpService;

    @Autowired
    private TestSettingsDeviceImsiMappingService deviceImsiMappingService;

    @Autowired
    private FtpFileProcessService ftpFileProcessService;

    @Autowired
    private TestSettingsTimeConfigService timeConfigService;

    // ========== 端侧和网络侧时间配置 ==========
    
    @GetMapping("/time-config")
    public Result<TestSettingsTimeConfig> getTimeConfig() {
        TestSettingsTimeConfig config = timeConfigService.getTimeConfig();
        return Result.success(config);
    }

    @PostMapping("/time-config")
    public Result<TestSettingsTimeConfig> saveOrUpdateTimeConfig(@Valid @RequestBody TestSettingsTimeConfig config) {
        boolean success = timeConfigService.saveOrUpdateTimeConfig(config);
        if (success) {
            TestSettingsTimeConfig saved = timeConfigService.getTimeConfig();
            return Result.success(saved);
        } else {
            return Result.error("保存失败");
        }
    }

    // ========== 端侧FTP服务器配置 ==========
    
    @GetMapping("/client-ftp")
    public Result<TestSettingsClientFtp> getClientFtpConfig() {
        TestSettingsClientFtp config = clientFtpService.getClientFtpConfig();
        return Result.success(config);
    }

    @PostMapping("/client-ftp")
    public Result<TestSettingsClientFtp> saveOrUpdateClientFtpConfig(@Valid @RequestBody TestSettingsClientFtp config) {
        boolean success = clientFtpService.saveOrUpdateClientFtpConfig(config);
        if (success) {
            TestSettingsClientFtp saved = clientFtpService.getClientFtpConfig();
            return Result.success(saved);
        } else {
            return Result.error("保存失败");
        }
    }

    // ========== 网络侧FTP服务器配置 ==========
    
    @GetMapping("/network-ftp")
    public Result<TestSettingsNetworkFtp> getNetworkFtpConfig() {
        TestSettingsNetworkFtp config = networkFtpService.getNetworkFtpConfig();
        return Result.success(config);
    }

    @PostMapping("/network-ftp")
    public Result<TestSettingsNetworkFtp> saveOrUpdateNetworkFtpConfig(@Valid @RequestBody TestSettingsNetworkFtp config) {
        boolean success = networkFtpService.saveOrUpdateNetworkFtpConfig(config);
        if (success) {
            TestSettingsNetworkFtp saved = networkFtpService.getNetworkFtpConfig();
            return Result.success(saved);
        } else {
            return Result.error("保存失败");
        }
    }

    // ========== deviceid和IMSI对应关系 ==========
    
    @GetMapping("/device-imsi-mapping")
    public Result<List<TestSettingsDeviceImsiMapping>> getDeviceImsiMappings() {
        QueryWrapper<TestSettingsDeviceImsiMapping> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("create_time");
        List<TestSettingsDeviceImsiMapping> list = deviceImsiMappingService.list(queryWrapper);
        return Result.success(list);
    }

    @GetMapping("/device-imsi-mapping/page")
    public Result<Page<TestSettingsDeviceImsiMapping>> getDeviceImsiMappingPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String imsi) {
        
        Page<TestSettingsDeviceImsiMapping> page = new Page<>(current, size);
        QueryWrapper<TestSettingsDeviceImsiMapping> queryWrapper = new QueryWrapper<>();
        
        if (deviceId != null && !deviceId.isEmpty()) {
            queryWrapper.like("device_id", deviceId);
        }
        if (imsi != null && !imsi.isEmpty()) {
            queryWrapper.like("imsi", imsi);
        }
        
        queryWrapper.orderByDesc("create_time");
        Page<TestSettingsDeviceImsiMapping> result = deviceImsiMappingService.page(page, queryWrapper);
        return Result.success(result);
    }

    @PostMapping("/device-imsi-mapping")
    public Result<TestSettingsDeviceImsiMapping> createDeviceImsiMapping(@Valid @RequestBody TestSettingsDeviceImsiMapping mapping) {
        deviceImsiMappingService.save(mapping);
        return Result.success(mapping);
    }

    @PutMapping("/device-imsi-mapping/{id}")
    public Result<TestSettingsDeviceImsiMapping> updateDeviceImsiMapping(
            @PathVariable @NotNull Long id,
            @Valid @RequestBody TestSettingsDeviceImsiMapping mapping) {
        mapping.setId(id);
        deviceImsiMappingService.updateById(mapping);
        return Result.success(mapping);
    }

    @DeleteMapping("/device-imsi-mapping/{id}")
    public Result<Boolean> deleteDeviceImsiMapping(@PathVariable @NotNull Long id) {
        boolean result = deviceImsiMappingService.removeById(id);
        return Result.success(result);
    }

    // ========== FTP文件处理 ==========

    /**
     * 从端侧FTP服务器下载文件并上传到gohttpserver
     * 如果是压缩包，会自动解压并解析taskinfo.json
     *
     * @param fileName 文件名
     * @return 上传后的文件URL和解析的taskinfo信息
     */
    @PostMapping("/client-ftp/process-file")
    public Result<Map<String, Object>> processClientFtpFile(@RequestParam @NotNull String fileName) {
        try {
            String fileUrl = ftpFileProcessService.processClientFtpFile(fileName);
            Map<String, Object> result = new HashMap<>();
            result.put("fileUrl", fileUrl);
            result.put("fileName", fileName);
            return Result.success(result);
        } catch (Exception e) {
            LOGGER.error("Failed to process client FTP file: {}", e.getMessage(), e);
            return Result.error("处理文件失败: " + e.getMessage());
        }
    }

    /**
     * 从网络侧FTP服务器下载文件并上传到gohttpserver
     *
     * @param fileName 文件名
     * @return 上传后的文件URL
     */
    @PostMapping("/network-ftp/process-file")
    public Result<String> processNetworkFtpFile(@RequestParam @NotNull String fileName) {
        try {
            String fileUrl = ftpFileProcessService.processNetworkFtpFile(fileName);
            return Result.success(fileUrl);
        } catch (Exception e) {
            LOGGER.error("Failed to process network FTP file: {}", e.getMessage(), e);
            return Result.error("处理文件失败: " + e.getMessage());
        }
    }

    /**
     * 批量处理端侧FTP文件
     *
     * @param fileNames 文件名列表
     * @return 上传后的文件URL列表
     */
    @PostMapping("/client-ftp/process-files")
    public Result<List<String>> processClientFtpFiles(@RequestBody List<String> fileNames) {
        try {
            List<String> fileUrls = ftpFileProcessService.processFtpFiles(true, fileNames);
            return Result.success(fileUrls);
        } catch (Exception e) {
            LOGGER.error("Failed to process client FTP files: {}", e.getMessage(), e);
            return Result.error("处理文件失败: " + e.getMessage());
        }
    }

    /**
     * 批量处理网络侧FTP文件
     *
     * @param fileNames 文件名列表
     * @return 上传后的文件URL列表
     */
    @PostMapping("/network-ftp/process-files")
    public Result<List<String>> processNetworkFtpFiles(@RequestBody List<String> fileNames) {
        try {
            List<String> fileUrls = ftpFileProcessService.processFtpFiles(false, fileNames);
            return Result.success(fileUrls);
        } catch (Exception e) {
            LOGGER.error("Failed to process network FTP files: {}", e.getMessage(), e);
            return Result.error("处理文件失败: " + e.getMessage());
        }
    }

    /**
     * 从端侧FTP服务器指定日期目录下获取所有文件并上传到gohttpserver
     * 如果是压缩包，会自动解压并解析taskinfo.json
     *
     * @param dateStr 日期字符串，格式：YYYY-MM-DD，如：2025-12-05
     * @return 上传后的文件URL列表和解析的taskinfo信息列表
     */
    @PostMapping("/client-ftp/process-files-by-date")
    public Result<Map<String, Object>> processClientFtpFilesByDate(@RequestParam @NotNull String dateStr) {
        try {
            List<TaskInfoDTO> taskInfoList = new ArrayList<>();
            List<String> fileUrls = ftpFileProcessService.processClientFtpFilesByDate(dateStr, taskInfoList);
            
            Map<String, Object> result = new HashMap<>();
            result.put("fileUrls", fileUrls);
            result.put("taskInfoList", taskInfoList);
            result.put("date", dateStr);
            result.put("fileCount", fileUrls.size());
            result.put("taskInfoCount", taskInfoList.size());
            
            return Result.success(result);
        } catch (Exception e) {
            LOGGER.error("Failed to process client FTP files by date: {}", e.getMessage(), e);
            return Result.error("处理文件失败: " + e.getMessage());
        }
    }

    /**
     * 从网络侧FTP服务器指定日期目录下获取所有文件并上传到gohttpserver
     *
     * @param dateStr 日期字符串，格式：YYYY-MM-DD，如：2025-12-05
     * @return 上传后的文件URL列表
     */
    @PostMapping("/network-ftp/process-files-by-date")
    public Result<List<String>> processNetworkFtpFilesByDate(@RequestParam @NotNull String dateStr) {
        try {
            List<String> fileUrls = ftpFileProcessService.processNetworkFtpFilesByDate(dateStr);
            return Result.success(fileUrls);
        } catch (Exception e) {
            LOGGER.error("Failed to process network FTP files by date: {}", e.getMessage(), e);
            return Result.error("处理文件失败: " + e.getMessage());
        }
    }

    // ========== 本地文件处理（用于测试验证） ==========

    /**
     * 处理本地端侧文件（用于测试验证，不依赖FTP服务器）
     * 如果是压缩包，会自动解压并解析taskinfo.json、speed-10s.xlsx、vmos-10s.xlsx、rtt-10s.csv、lost-10s.csv、video-10s.csv
     *
     * @param filePath 本地文件路径（绝对路径或相对路径）
     * @return 处理结果信息
     */
    @PostMapping("/local/client-file/process")
    public Result<Map<String, Object>> processLocalClientFile(@RequestParam @NotNull String filePath) {
        try {
            Map<String, Object> result = ftpFileProcessService.processLocalClientFile(filePath);
            return Result.success(result);
        } catch (Exception e) {
            LOGGER.error("Failed to process local client file: {}", e.getMessage(), e);
            return Result.error("处理文件失败: " + e.getMessage());
        }
    }

    /**
     * 上传端侧文件并解析（用于端侧数据管理页面）
     * 接收文件上传，保存到临时目录，然后解析并保存到数据库
     *
     * @param file 上传的文件（ZIP压缩包）
     * @return 处理结果信息
     */
    @PostMapping("/client-data/upload")
    public Result<Map<String, Object>> uploadClientDataFile(@RequestParam("file") MultipartFile file) {
        Path tempFilePath = null;
        try {
            // 验证文件
            if (file == null || file.isEmpty()) {
                return Result.error("文件不能为空");
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                return Result.error("文件名不能为空");
            }

            // 验证文件类型（只接受ZIP、GZ等压缩文件）
            String lowerFilename = originalFilename.toLowerCase();
            if (!lowerFilename.endsWith(".zip") && !lowerFilename.endsWith(".gz") && 
                !lowerFilename.endsWith(".tar.gz") && !lowerFilename.endsWith(".rar")) {
                return Result.error("只支持ZIP、GZ、TAR.GZ、RAR格式的压缩文件");
            }

            // 保存文件到临时目录
            Path tempDir = Files.createTempDirectory("client_data_upload_");
            tempFilePath = tempDir.resolve(originalFilename);
            File tempFile = tempFilePath.toFile();
            if (tempFile != null) {
                file.transferTo(tempFile);
            } else {
                throw new IOException("Failed to create temporary file");
            }
            LOGGER.info("File saved to temporary directory: {}", tempFilePath);

            // process文件并解析
            Map<String, Object> result = ftpFileProcessService.processLocalClientFile(tempFilePath.toString());
            result.put("fileName", originalFilename);
            result.put("fileSize", file.getSize());

            // 检查解析结果，如果有错误信息，返回错误
            if (result.containsKey("error") && result.get("error") != null) {
                String errorMsg = (String) result.get("error");
                LOGGER.error("Client data file parsing failed: {}, error: {}", originalFilename, errorMsg);
                return Result.error(errorMsg);
            }

            LOGGER.info("Client data file processed successfully: {}, result: {}", originalFilename, result);
            return Result.success(result);

        } catch (IOException e) {
            LOGGER.error("Failed to upload and process client data file: {}", e.getMessage(), e);
            return Result.error("文件上传或处理失败: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Error occurred while processing client data file: {}", e.getMessage(), e);
            return Result.error("处理文件时发生错误: " + e.getMessage());
        } finally {
            // 清理临时文件
            if (tempFilePath != null && Files.exists(tempFilePath)) {
                try {
                    Files.deleteIfExists(tempFilePath);
                    // 尝试删除临时目录
                    Path tempDir = tempFilePath.getParent();
                    if (tempDir != null && Files.exists(tempDir)) {
                        Files.deleteIfExists(tempDir);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to cleanup temporary file: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * process本地网络侧文件（用于测试validate，不依赖FTP服务器）
     * 如果是压缩包，会自动Extracting 并解析CSV file
     *
     * @param filePath 本地文件路径（绝对路径或相对路径）
     * @return process结果信息
     */
    @PostMapping("/local/network-file/process")
    public Result<Map<String, Object>> processLocalNetworkFile(@RequestParam @NotNull String filePath) {
        try {
            Map<String, Object> result = ftpFileProcessService.processLocalNetworkFile(filePath);
            return Result.success(result);
        } catch (Exception e) {
            LOGGER.error("Failed to process local network file: {}", e.getMessage(), e);
            return Result.error("处理文件失败: " + e.getMessage());
        }
    }

    /**
     * 上传网络侧文件并解析（用于网络侧数据管理页面）
     * 接收文件上传，保存到临时目录，然后解析并保存到数据库
     * 支持多文件上传
     *
     * @param files 上传的文件数组（ZIP、GZ、TAR.GZ、RAR压缩包）
     * @return 处理结果信息
     */
    @PostMapping("/network-data/upload")
    public Result<Map<String, Object>> uploadNetworkDataFile(@RequestParam("files") MultipartFile[] files) {
        List<Path> tempFilePaths = new ArrayList<>();
        int successCount = 0;
        int failCount = 0;
        long totalNetworkDataCount = 0;
        List<String> successFiles = new ArrayList<>();
        List<String> failFiles = new ArrayList<>();
        
        try {
            // 验证文件数组
            if (files == null || files.length == 0) {
                return Result.error("文件不能为空");
            }

            // 处理每个文件
            for (MultipartFile file : files) {
                Path tempFilePath = null;
                try {
                    // 验证文件
                    if (file == null || file.isEmpty()) {
                        failCount++;
                        failFiles.add("空文件");
                        continue;
                    }

                    String originalFilename = file.getOriginalFilename();
                    if (originalFilename == null || originalFilename.isEmpty()) {
                        failCount++;
                        failFiles.add("未命名文件");
                        continue;
                    }

                    // 验证文件类型（只接受ZIP、GZ等压缩文件）
                    String lowerFilename = originalFilename.toLowerCase();
                    if (!lowerFilename.endsWith(".zip") && !lowerFilename.endsWith(".gz") && 
                        !lowerFilename.endsWith(".tar.gz") && !lowerFilename.endsWith(".rar")) {
                        failCount++;
                        failFiles.add(originalFilename + " (不支持的文件格式)");
                        continue;
                    }

                    // 保存文件到临时目录
                    Path tempDir = Files.createTempDirectory("network_data_upload_");
                    tempFilePath = tempDir.resolve(originalFilename);
                    File tempFile = tempFilePath.toFile();
                    if (tempFile != null) {
                        file.transferTo(tempFile);
                    } else {
                        throw new IOException("Failed to create temporary file");
                    }
                    tempFilePaths.add(tempFilePath);
                    LOGGER.info("File saved to temporary directory: {}", tempFilePath);

                    // process文件并解析
                    Map<String, Object> fileResult = ftpFileProcessService.processLocalNetworkFile(tempFilePath.toString());
                    Object networkDataCountObj = fileResult.get("networkDataCount");
                    if (networkDataCountObj != null) {
                        if (networkDataCountObj instanceof Number) {
                            totalNetworkDataCount += ((Number) networkDataCountObj).longValue();
                        }
                    }
                    
                    successCount++;
                    successFiles.add(originalFilename);
                    LOGGER.info("Network data file processed successfully: {}, result: {}", originalFilename, fileResult);

                } catch (IOException e) {
                    LOGGER.error("Failed to upload and process network data file: {}", e.getMessage(), e);
                    failCount++;
                    if (file != null && file.getOriginalFilename() != null) {
                        failFiles.add(file.getOriginalFilename() + " (上传或处理失败: " + e.getMessage() + ")");
                    } else {
                        failFiles.add("未知文件 (上传或处理失败: " + e.getMessage() + ")");
                    }
                } catch (Exception e) {
                    LOGGER.error("Error occurred while processing network data file: {}", e.getMessage(), e);
                    failCount++;
                    if (file != null && file.getOriginalFilename() != null) {
                        failFiles.add(file.getOriginalFilename() + " (处理错误: " + e.getMessage() + ")");
                    } else {
                        failFiles.add("未知文件 (处理错误: " + e.getMessage() + ")");
                    }
                }
            }

            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("totalNetworkDataCount", totalNetworkDataCount);
            result.put("successCount", successCount);
            result.put("failCount", failCount);
            result.put("totalFiles", files.length);
            result.put("successFiles", successFiles);
            if (failCount > 0) {
                result.put("failFiles", failFiles);
            }

            if (successCount > 0) {
                LOGGER.info("Network data files processed: success={}, fail={}, totalNetworkDataCount={}", 
                    successCount, failCount, totalNetworkDataCount);
                return Result.success(result);
            } else {
                return Result.error("所有文件处理失败");
            }

        } catch (Exception e) {
            LOGGER.error("Error occurred while processing network data files: {}", e.getMessage(), e);
            return Result.error("处理文件时发生错误: " + e.getMessage());
        } finally {
            // 清理临时文件
            for (Path tempFilePath : tempFilePaths) {
                if (tempFilePath != null && Files.exists(tempFilePath)) {
                    try {
                        Files.deleteIfExists(tempFilePath);
                        // 尝试删除临时目录
                        Path tempDir = tempFilePath.getParent();
                        if (tempDir != null && Files.exists(tempDir)) {
                            Files.deleteIfExists(tempDir);
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Failed to cleanup temporary file: {}", e.getMessage());
                    }
                }
            }
        }
    }
}




