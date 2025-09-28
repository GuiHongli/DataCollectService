-- 更新采集任务表的状态默认值
USE data_collect;

-- 修改collect_task表的status字段默认值
ALTER TABLE collect_task MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'RUNNING' COMMENT '任务状态 (PENDING/RUNNING/COMPLETED/FAILED)';

-- 修改test_case_execution_instance表的status字段默认值
ALTER TABLE test_case_execution_instance MODIFY COLUMN status VARCHAR(20) NOT NULL DEFAULT 'RUNNING' COMMENT '执行状态 (PENDING/RUNNING/COMPLETED/FAILED)';

-- 更新现有数据：将状态为PENDING的任务更新为RUNNING（如果任务创建时间在最近1小时内）
UPDATE collect_task 
SET status = 'RUNNING', update_time = NOW() 
WHERE status = 'PENDING' 
AND create_time >= DATE_SUB(NOW(), INTERVAL 1 HOUR);

-- 更新现有数据：将状态为PENDING的用例执行例次更新为RUNNING（如果创建时间在最近1小时内）
UPDATE test_case_execution_instance 
SET status = 'RUNNING', update_time = NOW() 
WHERE status = 'PENDING' 
AND create_time >= DATE_SUB(NOW(), INTERVAL 1 HOUR);
