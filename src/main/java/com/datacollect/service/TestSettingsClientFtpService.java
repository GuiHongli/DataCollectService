package com.datacollect.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datacollect.entity.TestSettingsClientFtp;

public interface TestSettingsClientFtpService extends IService<TestSettingsClientFtp> {
    
    /**
     * 获取端侧FTP服务器配置（只允许一条记录）
     */
    TestSettingsClientFtp getClientFtpConfig();
    
    /**
     * 保存或更新端侧FTP服务器配置（只允许一条记录）
     */
    boolean saveOrUpdateClientFtpConfig(TestSettingsClientFtp config);
}






