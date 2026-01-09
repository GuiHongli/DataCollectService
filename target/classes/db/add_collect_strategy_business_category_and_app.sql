-- 为collect_strategy表添加业务大类和App字段
ALTER TABLE `collect_strategy` 
ADD COLUMN `business_category` varchar(100) DEFAULT NULL COMMENT '业务大类筛选' AFTER `test_case_set_id`,
ADD COLUMN `app` varchar(100) DEFAULT NULL COMMENT 'App筛选' AFTER `business_category`;

-- 添加索引以提高查询性能
ALTER TABLE `collect_strategy` 
ADD INDEX `idx_business_category` (`business_category`),
ADD INDEX `idx_app` (`app`);
