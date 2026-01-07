package com.datacollect.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.datacollect.dto.NetworkElementDTO;
import com.datacollect.entity.NetworkElement;

/**
 * 网元服务接口
 *
 * @author system
 * @since 2025-01-20
 */
public interface NetworkElementService extends IService<NetworkElement> {

    /**
     * 分页查询网元列表（包含属性）
     *
     * @param current 当前页
     * @param size 每页大小
     * @param name 网元名称（可选，用于搜索）
     * @return 分页结果
     */
    Page<NetworkElementDTO> getNetworkElementPage(Integer current, Integer size, String name);

    /**
     * 根据ID查询网元及其属性
     *
     * @param id 网元ID
     * @return 网元DTO
     */
    NetworkElementDTO getNetworkElementWithAttributes(Long id);

    /**
     * 保存网元及其属性
     *
     * @param networkElementDTO 网元DTO
     * @return 保存后的网元DTO
     */
    NetworkElementDTO saveNetworkElementWithAttributes(NetworkElementDTO networkElementDTO);

    /**
     * 更新网元及其属性
     *
     * @param networkElementDTO 网元DTO
     * @return 更新后的网元DTO
     */
    NetworkElementDTO updateNetworkElementWithAttributes(NetworkElementDTO networkElementDTO);

    /**
     * 删除网元及其所有属性
     *
     * @param id 网元ID
     * @return 是否删除成功
     */
    boolean deleteNetworkElementWithAttributes(Long id);
}

