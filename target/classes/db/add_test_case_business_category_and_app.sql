-- 为test_case表添加业务大类和App字段
ALTER TABLE `test_case` 
ADD COLUMN `business_category` varchar(100) DEFAULT NULL COMMENT '用例-业务大类' AFTER `logic_network`,
ADD COLUMN `app` varchar(100) DEFAULT NULL COMMENT '用例-App' AFTER `business_category`;

-- 添加索引以提高查询性能
ALTER TABLE `test_case` 
ADD INDEX `idx_business_category` (`business_category`),
ADD INDEX `idx_app` (`app`);
