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
        queryWrapper.orderByDesc("create_time");
        queryWrapper.last("LIMIT 1");
        return getOne(queryWrapper);
    }

    @Override
    public List<ExecutorMacAddress> getAllByMacAddress(String macAddress) {
        QueryWrapper<ExecutorMacAddress> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("mac_address", macAddress);
        queryWrapper.eq("deleted", 0);
        queryWrapper.orderByDesc("create_time");
        return list(queryWrapper);
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
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            log.warn("IP地址为空，无法注册MAC地址 - MAC地址: {}", macAddress);
            return null;
        }
        
        // 查找MAC地址和IP的组合是否已存在
        QueryWrapper<ExecutorMacAddress> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("mac_address", macAddress);
        queryWrapper.eq("ip_address", ipAddress);
        queryWrapper.eq("deleted", 0);
        ExecutorMacAddress existingRecord = getOne(queryWrapper);
        
        if (existingRecord == null) {
            // 如果MAC地址和IP的组合不存在，创建新记录（支持一个MAC地址关联多个IP）
            ExecutorMacAddress macAddressEntity = new ExecutorMacAddress();
            macAddressEntity.setMacAddress(macAddress);
            macAddressEntity.setExecutorId(executorId);
            macAddressEntity.setIpAddress(ipAddress);
            macAddressEntity.setStatus(1);
            macAddressEntity.setCreateTime(LocalDateTime.now());
            macAddressEntity.setUpdateTime(LocalDateTime.now());
            save(macAddressEntity);
            log.info("MAC地址已注册（新IP） - MAC地址: {}, 执行机ID: {}, IP地址: {}", macAddress, executorId, ipAddress);
            return macAddressEntity;
        } else {
            // 如果已存在，更新执行机ID（如果不同）
            if (executorId != null && !executorId.equals(existingRecord.getExecutorId())) {
                existingRecord.setExecutorId(executorId);
                existingRecord.setUpdateTime(LocalDateTime.now());
                updateById(existingRecord);
                log.info("MAC地址已更新执行机关联 - MAC地址: {}, 执行机ID: {}, IP地址: {}", macAddress, executorId, ipAddress);
            } else {
                log.debug("MAC地址和IP组合已存在 - MAC地址: {}, 执行机ID: {}, IP地址: {}", macAddress, executorId, ipAddress);
            }
            return existingRecord;
        }
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

