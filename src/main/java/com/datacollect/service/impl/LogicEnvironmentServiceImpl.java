package com.datacollect.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.entity.LogicEnvironment;
import com.datacollect.mapper.LogicEnvironmentMapper;
import com.datacollect.service.LogicEnvironmentService;
import org.springframework.stereotype.Service;

@Service
public class LogicEnvironmentServiceImpl extends ServiceImpl<LogicEnvironmentMapper, LogicEnvironment> implements LogicEnvironmentService {
}
