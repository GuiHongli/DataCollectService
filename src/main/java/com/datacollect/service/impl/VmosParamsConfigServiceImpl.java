package com.datacollect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.entity.VmosParamsConfig;
import com.datacollect.mapper.VmosParamsConfigMapper;
import com.datacollect.service.VmosParamsConfigService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * vMOS参数配置服务实现类
 * 
 * @author system
 * @since 2024-01-01
 */
@Service
public class VmosParamsConfigServiceImpl extends ServiceImpl<VmosParamsConfigMapper, VmosParamsConfig> implements VmosParamsConfigService {

  @Override
  public List<VmosParamsConfig> getAllVmosParamsConfig() {
    QueryWrapper<VmosParamsConfig> queryWrapper = new QueryWrapper<>();
    queryWrapper.orderByAsc("service");
    return list(queryWrapper);
  }

  @Override
  public VmosParamsConfig getVmosParamsConfigByService(String service) {
    if (service == null || service.isEmpty()) {
      return null;
    }
    QueryWrapper<VmosParamsConfig> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("service", service);
    queryWrapper.last("LIMIT 1");
    return getOne(queryWrapper);
  }

  @Override
  public boolean saveOrUpdateVmosParamsConfig(VmosParamsConfig config) {
    if (config == null || config.getService() == null || config.getService().isEmpty()) {
      return false;
    }
    
    // 先查询是否已存在该业务大类的配置
    VmosParamsConfig existing = getVmosParamsConfigByService(config.getService());
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
