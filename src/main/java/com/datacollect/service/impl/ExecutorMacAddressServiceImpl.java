package com.datacollect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.entity.ExecutorMacAddress;
import com.datacollect.mapper.ExecutorMacAddressMapper;
import com.datacollect.service.ExecutorMacAddressService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class ExecutorMacAddressServiceImpl extends ServiceImpl<ExecutorMacAddressMapper, ExecutorMacAddress> implements ExecutorMacAddressService {

    @Override
    public ExecutorMacAddress getByMacAddress(String macAddress) {
        QueryWrapper<ExecutorMacAddress> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("mac_address", macAddress);
        queryWrapper.eq("deleted", 0);
        return getOne(queryWrapper);
    }

    @Override
    public List<ExecutorMacAddress> getByExecutorId(Long executorId) {
        QueryWrapper<ExecutorMacAddress> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("executor_id", executorId);
        queryWrapper.eq("deleted", 0);
        queryWrapper.orderByDesc("create_time");
        return list(queryWrapper);
    }

    @Override
    public ExecutorMacAddress registerOrUpdateMacAddress(String macAddress, Long executorId, String ipAddress) {
        ExecutorMacAddress macAddressEntity = getByMacAddress(macAddress);
        
        if (macAddressEntity == null) {
            // 创建新记录
            macAddressEntity = new ExecutorMacAddress();
            macAddressEntity.setMacAddress(macAddress);
            macAddressEntity.setExecutorId(executorId);
            macAddressEntity.setIpAddress(ipAddress);
            macAddressEntity.setStatus(1);
            macAddressEntity.setCreateTime(LocalDateTime.now());
            macAddressEntity.setUpdateTime(LocalDateTime.now());
            save(macAddressEntity);
            log.info("MAC地址已注册 - MAC地址: {}, 执行机ID: {}, IP地址: {}", macAddress, executorId, ipAddress);
        } else {
            // 更新现有记录
            macAddressEntity.setExecutorId(executorId);
            if (ipAddress != null && !ipAddress.trim().isEmpty()) {
                macAddressEntity.setIpAddress(ipAddress);
            }
            macAddressEntity.setUpdateTime(LocalDateTime.now());
            updateById(macAddressEntity);
            log.info("MAC地址已更新 - MAC地址: {}, 执行机ID: {}, IP地址: {}", macAddress, executorId, ipAddress);
        }
        
        return macAddressEntity;
    }

    @Override
    public List<ExecutorMacAddress> getAvailableMacAddresses() {
        QueryWrapper<ExecutorMacAddress> queryWrapper = new QueryWrapper<>();
        queryWrapper.isNull("executor_id");
        queryWrapper.eq("status", 1);
        queryWrapper.eq("deleted", 0);
        queryWrapper.orderByDesc("create_time");
        return list(queryWrapper);
    }
}

