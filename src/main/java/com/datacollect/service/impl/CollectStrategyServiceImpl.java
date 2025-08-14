package com.datacollect.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.entity.CollectStrategy;
import com.datacollect.mapper.CollectStrategyMapper;
import com.datacollect.service.CollectStrategyService;
import org.springframework.stereotype.Service;

@Service
public class CollectStrategyServiceImpl extends ServiceImpl<CollectStrategyMapper, CollectStrategy> implements CollectStrategyService {
}
