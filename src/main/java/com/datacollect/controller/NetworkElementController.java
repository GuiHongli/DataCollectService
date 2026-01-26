package com.datacollect.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datacollect.common.Result;
import com.datacollect.dto.NetworkElementDTO;
import com.datacollect.service.NetworkElementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 网元管理控制器
 *
 * @author system
 * @since 2025-01-20
 */
@RestController
@RequestMapping("/network-element")
public class NetworkElementController {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkElementController.class);

    @Autowired
    private NetworkElementService networkElementService;

    /**
     * 分页查询网元列表
     *
     * @param current 当前页
     * @param size 每页大小
     * @param name 网元名称（可选，用于搜索）
     * @return 分页结果
     */
    @GetMapping("/page")
    public Result<Page<NetworkElementDTO>> getNetworkElementPage(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name) {
        try {
            Page<NetworkElementDTO> page = networkElementService.getNetworkElementPage(current, size, name);
            return Result.success(page);
        } catch (Exception e) {
            LOGGER.error("query网元列表failed: {}", e.getMessage(), e);
            return Result.error("查询网元列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID查询网元及其属性
     *
     * @param id 网元ID
     * @return 网元DTO
     */
    @GetMapping("/{id}")
    public Result<NetworkElementDTO> getNetworkElementById(@PathVariable Long id) {
        try {
            NetworkElementDTO dto = networkElementService.getNetworkElementWithAttributes(id);
            if (dto == null) {
                return Result.error("网元不存在");
            }
            return Result.success(dto);
        } catch (Exception e) {
            LOGGER.error("query网元failed: {}", e.getMessage(), e);
            return Result.error("查询网元失败: " + e.getMessage());
        }
    }

    /**
     * 创建网元及其属性
     *
     * @param networkElementDTO 网元DTO
     * @param httpRequest HTTP请求
     * @return 创建结果
     */
    @PostMapping
    public Result<NetworkElementDTO> createNetworkElement(
            @RequestBody NetworkElementDTO networkElementDTO,
            HttpServletRequest httpRequest) {
        try {
            String createBy = (String) httpRequest.getAttribute("username");
            if (createBy == null) {
                createBy = "system";
            }
            
            if (networkElementDTO.getNetworkElement() == null) {
                return Result.error("网元信息不能为空");
            }
            
            networkElementDTO.getNetworkElement().setCreateBy(createBy);
            networkElementDTO.getNetworkElement().setUpdateBy(createBy);
            
            NetworkElementDTO result = networkElementService.saveNetworkElementWithAttributes(networkElementDTO);
            LOGGER.info("create网元success - 网元ID: {}, create人: {}", result.getNetworkElement().getId(), createBy);
            return Result.success(result);
        } catch (Exception e) {
            LOGGER.error("create网元failed: {}", e.getMessage(), e);
            return Result.error("创建网元失败: " + e.getMessage());
        }
    }

    /**
     * 更新网元及其属性
     *
     * @param id 网元ID
     * @param networkElementDTO 网元DTO
     * @param httpRequest HTTP请求
     * @return 更新结果
     */
    @PutMapping("/{id}")
    public Result<NetworkElementDTO> updateNetworkElement(
            @PathVariable Long id,
            @RequestBody NetworkElementDTO networkElementDTO,
            HttpServletRequest httpRequest) {
        try {
            String updateBy = (String) httpRequest.getAttribute("username");
            if (updateBy == null) {
                updateBy = "system";
            }
            
            if (networkElementDTO.getNetworkElement() == null) {
                return Result.error("网元信息不能为空");
            }
            
            networkElementDTO.getNetworkElement().setId(id);
            networkElementDTO.getNetworkElement().setUpdateBy(updateBy);
            
            NetworkElementDTO result = networkElementService.updateNetworkElementWithAttributes(networkElementDTO);
            LOGGER.info("update网元success - 网元ID: {}, update人: {}", id, updateBy);
            return Result.success(result);
        } catch (Exception e) {
            LOGGER.error("update网元failed: {}", e.getMessage(), e);
            return Result.error("更新网元失败: " + e.getMessage());
        }
    }

    /**
     * 删除网元及其所有属性
     *
     * @param id 网元ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteNetworkElement(@PathVariable Long id) {
        try {
            boolean success = networkElementService.deleteNetworkElementWithAttributes(id);
            if (success) {
                LOGGER.info("delete网元success - 网元ID: {}", id);
                return Result.success(null);
            } else {
                return Result.error("删除网元失败");
            }
        } catch (Exception e) {
            LOGGER.error("delete网元failed: {}", e.getMessage(), e);
            return Result.error("删除网元失败: " + e.getMessage());
        }
    }
}



















