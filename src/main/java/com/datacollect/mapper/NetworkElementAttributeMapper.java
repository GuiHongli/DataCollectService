package com.datacollect.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.datacollect.entity.NetworkElementAttribute;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 网元属性Mapper接口
 *
 * @author system
 * @since 2025-01-20
 */
@Mapper
public interface NetworkElementAttributeMapper extends BaseMapper<NetworkElementAttribute> {

    /**
     * 根据网元ID查询所有属性
     *
     * @param networkElementId 网元ID
     * @return 属性列表
     */
    default List<NetworkElementAttribute> selectByNetworkElementId(Long networkElementId) {
        LambdaQueryWrapper<NetworkElementAttribute> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(NetworkElementAttribute::getNetworkElementId, networkElementId);
        queryWrapper.orderByAsc(NetworkElementAttribute::getCreateTime);
        return this.selectList(queryWrapper);
    }

    /**
     * 根据网元ID删除所有属性
     *
     * @param networkElementId 网元ID
     * @return 删除数量
     */
    default int deleteByNetworkElementId(Long networkElementId) {
        LambdaQueryWrapper<NetworkElementAttribute> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(NetworkElementAttribute::getNetworkElementId, networkElementId);
        return this.delete(queryWrapper);
    }
}

