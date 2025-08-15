package com.datacollect.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.entity.LogicNetwork;
import com.datacollect.mapper.LogicNetworkMapper;
import com.datacollect.service.LogicNetworkService;
import org.springframework.stereotype.Service;

@Service
public class LogicNetworkServiceImpl extends ServiceImpl<LogicNetworkMapper, LogicNetwork> implements LogicNetworkService {
}
