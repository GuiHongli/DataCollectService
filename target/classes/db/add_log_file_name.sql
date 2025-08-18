-- 为test_case_execution_result表添加logFilePath字段
USE data_collect;

-- 添加日志文件路径字段
ALTER TABLE test_case_execution_result 
ADD COLUMN log_file_path VARCHAR(500) COMMENT '日志文件路径或HTTP链接' AFTER failure_reason;

-- 添加索引
ALTER TABLE test_case_execution_result 
ADD INDEX idx_log_file_path (log_file_path);

-- 查看表结构
DESCRIBE test_case_execution_result;
