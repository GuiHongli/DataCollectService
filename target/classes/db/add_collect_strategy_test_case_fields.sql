-- 为采集策略表添加用例自定义参数和执行次数字段
-- 执行时间: 2024-01-XX
-- 描述: 支持用例级别的自定义参数配置和执行次数设置

-- 添加用例自定义参数字段
ALTER TABLE collect_strategy 
ADD COLUMN test_case_custom_params TEXT COMMENT '用例自定义参数，JSON格式存储';

-- 添加用例执行次数字段  
ALTER TABLE collect_strategy 
ADD COLUMN test_case_execution_counts TEXT COMMENT '用例执行次数，JSON格式存储';

-- 添加字段注释
ALTER TABLE collect_strategy 
MODIFY COLUMN test_case_custom_params TEXT COMMENT '用例自定义参数，JSON格式存储，格式: {"testCaseId": [{"key": "param1", "value": "value1"}]}';

ALTER TABLE collect_strategy 
MODIFY COLUMN test_case_execution_counts TEXT COMMENT '用例执行次数，JSON格式存储，格式: {"testCaseId": 3}';










