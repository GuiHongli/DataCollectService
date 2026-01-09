-- 为test_case_execution_result表添加collect_path和qc_result字段
-- 用于存储用例采集路径输出和质检结果

USE data_collect;

-- 添加用例采集路径输出字段
-- 注意：如果字段已存在，执行此语句会报错，需要先检查字段是否存在
ALTER TABLE `test_case_execution_result` 
ADD COLUMN `collect_path` text COMMENT '用例采集路径输出（从日志中解析 "save log in xxx" 后面的信息）' AFTER `log_file_path`;

-- 添加质检结果字段
ALTER TABLE `test_case_execution_result` 
ADD COLUMN `qc_result` text COMMENT '质检结果（从日志中解析 "===QC_Result===" 到 "===End" 中间的信息，JSON格式）' AFTER `collect_path`;

-- 查看表结构确认
DESCRIBE `test_case_execution_result`;

