-- 为collect_strategy表添加采集意图字段
ALTER TABLE `collect_strategy` 
ADD COLUMN `intent` varchar(20) DEFAULT NULL COMMENT '采集意图' AFTER `app`;

-- 添加索引以提高查询性能
ALTER TABLE `collect_strategy` 
ADD INDEX `idx_intent` (`intent`);
