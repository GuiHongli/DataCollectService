package com.datacollect.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.entity.NetworkType;
import com.datacollect.mapper.NetworkTypeMapper;
import com.datacollect.service.NetworkTypeService;
import org.springframework.stereotype.Service;

@Service
public class NetworkTypeServiceImpl extends ServiceImpl<NetworkTypeMapper, NetworkType> implements NetworkTypeService {
}
