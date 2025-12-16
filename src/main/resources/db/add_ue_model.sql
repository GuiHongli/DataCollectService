-- 为ue表添加型号字段
ALTER TABLE `ue` 
ADD COLUMN `model` varchar(100) DEFAULT NULL COMMENT 'UE型号' AFTER `vendor`;

-- 添加索引以提高查询性能
ALTER TABLE `ue` 
ADD INDEX `idx_model` (`model`);


