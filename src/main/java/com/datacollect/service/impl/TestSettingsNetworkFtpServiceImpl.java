package com.datacollect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.entity.TestSettingsNetworkFtp;
import com.datacollect.mapper.TestSettingsNetworkFtpMapper;
import com.datacollect.service.TestSettingsNetworkFtpService;
import org.springframework.stereotype.Service;

@Service
public class TestSettingsNetworkFtpServiceImpl extends ServiceImpl<TestSettingsNetworkFtpMapper, TestSettingsNetworkFtp> implements TestSettingsNetworkFtpService {

    @Override
    public TestSettingsNetworkFtp getNetworkFtpConfig() {
        QueryWrapper<TestSettingsNetworkFtp> queryWrapper = new QueryWrapper<>();
        queryWrapper.last("LIMIT 1");
        return getOne(queryWrapper);
    }

    @Override
    public boolean saveOrUpdateNetworkFtpConfig(TestSettingsNetworkFtp config) {
        // 先查询是否已存在记录
        TestSettingsNetworkFtp existing = getNetworkFtpConfig();
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



