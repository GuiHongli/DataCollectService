package com.datacollect.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.dto.NetworkElementDTO;
import com.datacollect.entity.NetworkElement;
import com.datacollect.entity.NetworkElementAttribute;
import com.datacollect.mapper.NetworkElementAttributeMapper;
import com.datacollect.mapper.NetworkElementMapper;
import com.datacollect.service.NetworkElementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 网元服务实现类
 *
 * @author system
 * @since 2025-01-20
 */
@Slf4j
@Service
public class NetworkElementServiceImpl extends ServiceImpl<NetworkElementMapper, NetworkElement> implements NetworkElementService {

    @Autowired
    private NetworkElementAttributeMapper networkElementAttributeMapper;

    @Override
    public Page<NetworkElementDTO> getNetworkElementPage(Integer current, Integer size, String name) {
        Page<NetworkElement> page = new Page<>(current, size);
        LambdaQueryWrapper<NetworkElement> queryWrapper = new LambdaQueryWrapper<>();
        
        if (name != null && !name.trim().isEmpty()) {
            queryWrapper.like(NetworkElement::getName, name);
        }
        
        queryWrapper.orderByDesc(NetworkElement::getCreateTime);
        Page<NetworkElement> networkElementPage = this.page(page, queryWrapper);
        
        Page<NetworkElementDTO> resultPage = new Page<>(current, size);
        resultPage.setTotal(networkElementPage.getTotal());
        
        List<NetworkElementDTO> dtoList = networkElementPage.getRecords().stream()
                .map(networkElement -> {
                    NetworkElementDTO dto = new NetworkElementDTO();
                    dto.setNetworkElement(networkElement);
                    List<NetworkElementAttribute> attributes = networkElementAttributeMapper.selectByNetworkElementId(networkElement.getId());
                    dto.setAttributes(attributes);
                    return dto;
                })
                .collect(java.util.stream.Collectors.toList());
        
        resultPage.setRecords(dtoList);
        return resultPage;
    }

    @Override
    public NetworkElementDTO getNetworkElementWithAttributes(Long id) {
        NetworkElement networkElement = this.getById(id);
        if (networkElement == null) {
            return null;
        }
        
        NetworkElementDTO dto = new NetworkElementDTO();
        dto.setNetworkElement(networkElement);
        List<NetworkElementAttribute> attributes = networkElementAttributeMapper.selectByNetworkElementId(id);
        dto.setAttributes(attributes);
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NetworkElementDTO saveNetworkElementWithAttributes(NetworkElementDTO networkElementDTO) {
        NetworkElement networkElement = networkElementDTO.getNetworkElement();
        this.save(networkElement);
        
        List<NetworkElementAttribute> attributes = networkElementDTO.getAttributes();
        if (attributes != null && !attributes.isEmpty()) {
            for (NetworkElementAttribute attribute : attributes) {
                attribute.setNetworkElementId(networkElement.getId());
                networkElementAttributeMapper.insert(attribute);
            }
        }
        
        return getNetworkElementWithAttributes(networkElement.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NetworkElementDTO updateNetworkElementWithAttributes(NetworkElementDTO networkElementDTO) {
        NetworkElement networkElement = networkElementDTO.getNetworkElement();
        this.updateById(networkElement);
        
        // 删除原有属性
        networkElementAttributeMapper.deleteByNetworkElementId(networkElement.getId());
        
        // 插入新属性
        List<NetworkElementAttribute> attributes = networkElementDTO.getAttributes();
        if (attributes != null && !attributes.isEmpty()) {
            for (NetworkElementAttribute attribute : attributes) {
                attribute.setNetworkElementId(networkElement.getId());
                attribute.setId(null); // 确保是新记录
                networkElementAttributeMapper.insert(attribute);
            }
        }
        
        return getNetworkElementWithAttributes(networkElement.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteNetworkElementWithAttributes(Long id) {
        // 删除属性
        networkElementAttributeMapper.deleteByNetworkElementId(id);
        // 删除网元（逻辑删除）
        return this.removeById(id);
    }
}

