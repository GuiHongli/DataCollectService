package com.datacollect.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.datacollect.common.Result;
import com.datacollect.dto.NetworkElementDTO;
import com.datacollect.service.NetworkElementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * 网元管理控制器
 *
 * @author system
 * @since 2025-01-20
 */
@Slf4j
@RestController
@RequestMapping("/network-element")
public class NetworkElementController {

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
            log.error("查询网元列表失败: {}", e.getMessage(), e);
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
            log.error("查询网元失败: {}", e.getMessage(), e);
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
            log.info("创建网元成功 - 网元ID: {}, 创建人: {}", result.getNetworkElement().getId(), createBy);
            return Result.success(result);
        } catch (Exception e) {
            log.error("创建网元失败: {}", e.getMessage(), e);
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
            log.info("更新网元成功 - 网元ID: {}, 更新人: {}", id, updateBy);
            return Result.success(result);
        } catch (Exception e) {
            log.error("更新网元失败: {}", e.getMessage(), e);
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
                log.info("删除网元成功 - 网元ID: {}", id);
                return Result.success(null);
            } else {
                return Result.error("删除网元失败");
            }
        } catch (Exception e) {
            log.error("删除网元失败: {}", e.getMessage(), e);
            return Result.error("删除网元失败: " + e.getMessage());
        }
    }
}





