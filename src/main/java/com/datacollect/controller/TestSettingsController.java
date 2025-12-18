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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/test-settings")
@Validated
public class TestSettingsController {

    @Autowired
    private TestSettingsClientFtpService clientFtpService;

    @Autowired
    private TestSettingsNetworkFtpService networkFtpService;

    @Autowired
    private TestSettingsDeviceImsiMappingService deviceImsiMappingService;

    @Autowired
    private FtpFileProcessService ftpFileProcessService;

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
            log.error("Failed to process client FTP file: {}", e.getMessage(), e);
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
            log.error("Failed to process network FTP file: {}", e.getMessage(), e);
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
            log.error("Failed to process client FTP files: {}", e.getMessage(), e);
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
            log.error("Failed to process network FTP files: {}", e.getMessage(), e);
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
            log.error("Failed to process client FTP files by date: {}", e.getMessage(), e);
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
            log.error("Failed to process network FTP files by date: {}", e.getMessage(), e);
            return Result.error("处理文件失败: " + e.getMessage());
        }
    }

    // ========== 本地文件处理（用于测试验证） ==========

    /**
     * 处理本地端侧文件（用于测试验证，不依赖FTP服务器）
     * 如果是压缩包，会自动解压并解析taskinfo.json、speed-10s.csv、vmos-10s.xlsx、rtt-10s.csv、lost-10s.csv、video-10s.csv
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
            log.error("Failed to process local client file: {}", e.getMessage(), e);
            return Result.error("处理文件失败: " + e.getMessage());
        }
    }

    /**
     * 处理本地网络侧文件（用于测试验证，不依赖FTP服务器）
     * 如果是压缩包，会自动解压并解析CSV文件
     *
     * @param filePath 本地文件路径（绝对路径或相对路径）
     * @return 处理结果信息
     */
    @PostMapping("/local/network-file/process")
    public Result<Map<String, Object>> processLocalNetworkFile(@RequestParam @NotNull String filePath) {
        try {
            Map<String, Object> result = ftpFileProcessService.processLocalNetworkFile(filePath);
            return Result.success(result);
        } catch (Exception e) {
            log.error("Failed to process local network file: {}", e.getMessage(), e);
            return Result.error("处理文件失败: " + e.getMessage());
        }
    }
}




