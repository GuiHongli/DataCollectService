package com.datacollect.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.datacollect.entity.ClientTestData;
import org.apache.ibatis.annotations.Mapper;

/**
 * 端侧测试数据Mapper
 * 
 * @author system
 * @since 2024-01-01
 */
@Mapper
public interface ClientTestDataMapper extends BaseMapper<ClientTestData> {
}

