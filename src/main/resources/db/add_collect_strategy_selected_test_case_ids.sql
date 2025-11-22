-- 为采集策略表添加选中用例ID字段
-- 执行时间: 2024-01-XX
-- 描述: 存储策略中选中的用例ID列表，JSON数组格式

-- 添加选中用例ID字段
ALTER TABLE collect_strategy 
ADD COLUMN selected_test_case_ids TEXT COMMENT '选中的用例ID列表，JSON数组格式，如：[1, 2, 3]';

-- 添加字段注释
ALTER TABLE collect_strategy 
MODIFY COLUMN selected_test_case_ids TEXT COMMENT '选中的用例ID列表，JSON数组格式，如：[1, 2, 3]';













