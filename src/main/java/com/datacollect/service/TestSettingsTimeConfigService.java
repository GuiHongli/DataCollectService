package com.datacollect.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datacollect.entity.TestSettingsTimeConfig;

/**
 * 端侧和网络侧时间配置服务接口
 * 
 * @author system
 * @since 2024-01-01
 */
public interface TestSettingsTimeConfigService extends IService<TestSettingsTimeConfig> {
  
  /**
   * 获取时间配置（只允许一条记录）
   */
  TestSettingsTimeConfig getTimeConfig();
  
  /**
   * 保存或更新时间配置（只允许一条记录）
   */
  boolean saveOrUpdateTimeConfig(TestSettingsTimeConfig config);
}




