package com.datacollect.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datacollect.entity.TestSettingsNetworkFtp;

public interface TestSettingsNetworkFtpService extends IService<TestSettingsNetworkFtp> {
    
    /**
     * 获取网络侧FTP服务器配置（只允许一条记录）
     */
    TestSettingsNetworkFtp getNetworkFtpConfig();
    
    /**
     * 保存或更新网络侧FTP服务器配置（只允许一条记录）
     */
    boolean saveOrUpdateNetworkFtpConfig(TestSettingsNetworkFtp config);
}



