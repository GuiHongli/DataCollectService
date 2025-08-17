-- 为test_case_execution_instance表添加result字段
USE data_collect;

-- 添加执行结果字段
ALTER TABLE test_case_execution_instance 
ADD COLUMN result VARCHAR(20) COMMENT '执行结果 (SUCCESS/FAILED/BLOCKED)' AFTER status;

-- 更新现有数据的result字段
-- 如果status是FAILED，则result也设为FAILED
UPDATE test_case_execution_instance 
SET result = 'FAILED' 
WHERE status = 'FAILED';

-- 如果status是COMPLETED但没有result，则设为SUCCESS
UPDATE test_case_execution_instance 
SET result = 'SUCCESS' 
WHERE status = 'COMPLETED' AND (result IS NULL OR result = '');

-- 如果status是PENDING或RUNNING，则result设为NULL
UPDATE test_case_execution_instance 
SET result = NULL 
WHERE status IN ('PENDING', 'RUNNING');

-- 添加索引
ALTER TABLE test_case_execution_instance 
ADD INDEX idx_result (result);

-- 查看表结构
DESCRIBE test_case_execution_instance;
