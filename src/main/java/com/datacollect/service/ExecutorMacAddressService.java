package com.datacollect.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datacollect.entity.ExecutorMacAddress;

import java.util.List;

/**
 * 执行机MAC地址服务接口
 * 
 * @author system
 * @since 2024-01-01
 */
public interface ExecutorMacAddressService extends IService<ExecutorMacAddress> {
    
    /**
     * 根据MAC地址查找记录
     * 
     * @param macAddress MAC地址
     * @return MAC地址记录
     */
    ExecutorMacAddress getByMacAddress(String macAddress);
    
    /**
     * 根据执行机ID查找MAC地址列表
     * 
     * @param executorId 执行机ID
     * @return MAC地址列表
     */
    List<ExecutorMacAddress> getByExecutorId(Long executorId);
    
    /**
     * 注册或更新MAC地址
     * 
     * @param macAddress MAC地址
     * @param executorId 执行机ID（可为空）
     * @param ipAddress IP地址（可为空）
     * @return MAC地址记录
     */
    ExecutorMacAddress registerOrUpdateMacAddress(String macAddress, Long executorId, String ipAddress);
    
    /**
     * 获取所有可用的MAC地址列表（未分配给执行机的）
     * 
     * @return MAC地址列表
     */
    List<ExecutorMacAddress> getAvailableMacAddresses();
}

