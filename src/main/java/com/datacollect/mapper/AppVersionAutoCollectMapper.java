package com.datacollect.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.datacollect.entity.AppVersionAutoCollect;
import org.apache.ibatis.annotations.Mapper;

/**
 * app版本变更自动采集配置Mapper
 * 
 * @author system
 * @since 2024-01-01
 */
@Mapper
public interface AppVersionAutoCollectMapper extends BaseMapper<AppVersionAutoCollect> {
}

