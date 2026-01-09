-- 为collect_strategy表添加自定义参数字段
ALTER TABLE `collect_strategy` 
ADD COLUMN `custom_params` text DEFAULT NULL COMMENT '自定义参数列表(JSON格式)' AFTER `intent`;

-- 添加索引以提高查询性能
ALTER TABLE `collect_strategy` 
ADD INDEX `idx_custom_params` (`custom_params`(100));
