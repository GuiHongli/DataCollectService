package com.datacollect.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.entity.RemoteLoginLog;
import com.datacollect.mapper.RemoteLoginLogMapper;
import com.datacollect.service.RemoteLoginLogService;
import org.springframework.stereotype.Service;

@Service
public class RemoteLoginLogServiceImpl extends ServiceImpl<RemoteLoginLogMapper, RemoteLoginLog> implements RemoteLoginLogService {
}


