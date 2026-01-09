-- 为test_case_execution_instance表添加failure_reason字段
USE data_collect;

-- 添加失败原因字段
ALTER TABLE test_case_execution_instance 
ADD COLUMN failure_reason TEXT COMMENT '失败原因' AFTER result;

-- 添加索引
ALTER TABLE test_case_execution_instance 
ADD INDEX idx_failure_reason (failure_reason(100));

-- 查看表结构
DESCRIBE test_case_execution_instance;
