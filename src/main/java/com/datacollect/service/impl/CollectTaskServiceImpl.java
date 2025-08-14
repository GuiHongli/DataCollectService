package com.datacollect.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.entity.CollectTask;
import com.datacollect.mapper.CollectTaskMapper;
import com.datacollect.service.CollectTaskService;
import org.springframework.stereotype.Service;

@Service
public class CollectTaskServiceImpl extends ServiceImpl<CollectTaskMapper, CollectTask> implements CollectTaskService {
}
