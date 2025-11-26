-- 为 collect_task 表添加 create_by 字段（下发人）
-- 如果字段已存在，则不会报错（使用 IF NOT EXISTS 语法）

ALTER TABLE `collect_task` 
ADD COLUMN IF NOT EXISTS `create_by` varchar(50) DEFAULT NULL COMMENT '创建人（下发人）' AFTER `custom_params`;

-- 为 create_by 字段添加索引，提高查询性能
CREATE INDEX IF NOT EXISTS `idx_create_by` ON `collect_task` (`create_by`);





