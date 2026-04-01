package com.datacollect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.entity.TestSettingsClientFtp;
import com.datacollect.mapper.TestSettingsClientFtpMapper;
import com.datacollect.service.TestSettingsClientFtpService;
import org.springframework.stereotype.Service;

@Service
public class TestSettingsClientFtpServiceImpl extends ServiceImpl<TestSettingsClientFtpMapper, TestSettingsClientFtp> implements TestSettingsClientFtpService {

    @Override
    public TestSettingsClientFtp getClientFtpConfig() {
        QueryWrapper<TestSettingsClientFtp> queryWrapper = new QueryWrapper<>();
        queryWrapper.last("LIMIT 1");
        return getOne(queryWrapper);
    }

    @Override
    public boolean saveOrUpdateClientFtpConfig(TestSettingsClientFtp config) {
        // 先查询是否已存在记录
        TestSettingsClientFtp existing = getClientFtpConfig();
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






