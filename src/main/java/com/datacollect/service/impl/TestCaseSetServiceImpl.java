package com.datacollect.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.datacollect.entity.TestCaseSet;
import com.datacollect.mapper.TestCaseSetMapper;
import com.datacollect.service.TestCaseSetService;
import org.springframework.stereotype.Service;

@Service
public class TestCaseSetServiceImpl extends ServiceImpl<TestCaseSetMapper, TestCaseSet> implements TestCaseSetService {
}
