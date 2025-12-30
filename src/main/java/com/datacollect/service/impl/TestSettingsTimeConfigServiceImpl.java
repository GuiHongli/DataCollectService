package com.datacollect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.entity.TestSettingsTimeConfig;
import com.datacollect.mapper.TestSettingsTimeConfigMapper;
import com.datacollect.service.TestSettingsTimeConfigService;
import org.springframework.stereotype.Service;

/**
 * 端侧和网络侧时间配置服务实现类
 * 
 * @author system
 * @since 2024-01-01
 */
@Service
public class TestSettingsTimeConfigServiceImpl extends ServiceImpl<TestSettingsTimeConfigMapper, TestSettingsTimeConfig> implements TestSettingsTimeConfigService {

  @Override
  public TestSettingsTimeConfig getTimeConfig() {
    QueryWrapper<TestSettingsTimeConfig> queryWrapper = new QueryWrapper<>();
    queryWrapper.last("LIMIT 1");
    return getOne(queryWrapper);
  }

  @Override
  public boolean saveOrUpdateTimeConfig(TestSettingsTimeConfig config) {
    // 先查询是否已存在记录
    TestSettingsTimeConfig existing = getTimeConfig();
    if (existing != null) {
      // 更新现有记录
      config.setId(existing.getId());
      return updateById(config);
    } else {
      // 创建新记录
      return save(config);
    }
  }
}

