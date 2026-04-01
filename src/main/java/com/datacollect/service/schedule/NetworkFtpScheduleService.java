package com.datacollect.service.schedule;

import com.datacollect.service.FtpFileProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 网络侧FTP文件定时处理服务
 * 定时从网络侧FTP服务器获取当前日期文件夹下的文件并处理
 * 
 * @author system
 * @since 2024-01-01
 */
@Service
public class NetworkFtpScheduleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkFtpScheduleService.class);

    @Autowired
    private FtpFileProcessService ftpFileProcessService;

    /**
     * 日期格式化器
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 定时处理网络侧FTP文件
     * 每30分钟执行一次，获取当前日期文件夹下的文件
     * 执行频率可以通过修改cron表达式调整
     */
    @Scheduled(cron = "0 */30 * * * ?")
    public void scheduledProcessNetworkFtpFiles() {
        try {
            // 获取当前日期
            String currentDate = LocalDate.now().format(DATE_FORMATTER);
            LOGGER.info("Scheduled task: Starting to process network FTP files for date: {}", currentDate);
            
            // process当前日期文件夹下的所有文件
            // processNetworkFtpFilesByDate 方法内部会根据配置判断是否需要校验MD5
            List<String> fileUrls = ftpFileProcessService.processNetworkFtpFilesByDate(currentDate);
            
            LOGGER.info("Scheduled task: Network FTP files processing completed for date: {}, processed {} files", 
                    currentDate, fileUrls.size());
            
        } catch (Exception e) {
            LOGGER.error("Scheduled task: Failed to process network FTP files - error: {}", e.getMessage(), e);
            // 不抛出异常，避免影响定时任务continue执行
        }
    }

    /**
     * 手动触发process指定日期的网络侧FTP文件（用于测试或手动刷新）
     * 
     * @param dateStr 日期字符串，格式：YYYY-MM-DD
     * @return process后的文件URL列表
     */
    public List<String> manualProcessNetworkFtpFiles(String dateStr) {
        LOGGER.info("Manual trigger: Starting to process network FTP files for date: {}", dateStr);
        try {
            List<String> fileUrls = ftpFileProcessService.processNetworkFtpFilesByDate(dateStr);
            LOGGER.info("Manual trigger: Network FTP files processing completed for date: {}, processed {} files", 
                    dateStr, fileUrls.size());
            return fileUrls;
        } catch (Exception e) {
            LOGGER.error("Manual trigger: Failed to process network FTP files for date: {} - error: {}", 
                    dateStr, e.getMessage(), e);
            throw new RuntimeException("Failed to process network FTP files: " + e.getMessage(), e);
        }
    }

    /**
     * 手动触发处理当前日期的网络侧FTP文件
     * 
     * @return 处理后的文件URL列表
     */
    public List<String> manualProcessCurrentDateNetworkFtpFiles() {
        String currentDate = LocalDate.now().format(DATE_FORMATTER);
        return manualProcessNetworkFtpFiles(currentDate);
    }
}
