-- 为采集任务表添加失败原因字段
ALTER TABLE `collect_task` 
ADD COLUMN `failure_reason` text COMMENT '失败原因（当任务状态为FAILED时记录）';

-- 为失败原因字段添加索引
ALTER TABLE `collect_task` ADD INDEX `idx_failure_reason` (`failure_reason`(100));
