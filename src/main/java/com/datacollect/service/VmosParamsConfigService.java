package com.datacollect.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.datacollect.entity.VmosParamsConfig;

import java.util.List;

/**
 * vMOS参数配置服务接口
 * 
 * @author system
 * @since 2024-01-01
 */
public interface VmosParamsConfigService extends IService<VmosParamsConfig> {
  
  /**
   * 获取所有业务大类的vMOS参数配置
   */
  List<VmosParamsConfig> getAllVmosParamsConfig();
  
  /**
   * 根据业务大类获取vMOS参数配置
   */
  VmosParamsConfig getVmosParamsConfigByService(String service);
  
  /**
   * 保存或更新vMOS参数配置
   */
  boolean saveOrUpdateVmosParamsConfig(VmosParamsConfig config);
}
