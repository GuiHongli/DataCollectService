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
     * 根据MAC地址查找记录（返回第一条，兼容旧逻辑）
     * 
     * @param macAddress MAC地址
     * @return MAC地址记录
     */
    ExecutorMacAddress getByMacAddress(String macAddress);
    
    /**
     * 根据MAC地址查找所有记录（支持一个MAC地址关联多个IP）
     * 
     * @param macAddress MAC地址
     * @return MAC地址记录列表
     */
    List<ExecutorMacAddress> getAllByMacAddress(String macAddress);
    
    /**
     * 注册或更新MAC地址
     * 
     * @param macAddress MAC地址
     * @param ipAddress IP地址（可为空）
     * @return MAC地址记录
     */
    ExecutorMacAddress registerOrUpdateMacAddress(String macAddress, String ipAddress);
    
    /**
     * 获取所有可用的MAC地址列表
     * 
     * @return MAC地址列表
     */
    List<ExecutorMacAddress> getAvailableMacAddresses();
}

