-- 修改采集策略表，将逻辑环境关联改为用例集关联
ALTER TABLE collect_strategy 
DROP COLUMN logic_environment_id,
ADD COLUMN test_case_set_id bigint(20) NOT NULL COMMENT '用例集ID' AFTER collect_count;

-- 添加索引
ALTER TABLE collect_strategy 
ADD INDEX idx_test_case_set_id (test_case_set_id);

-- 查看修改后的表结构
DESCRIBE collect_strategy;
