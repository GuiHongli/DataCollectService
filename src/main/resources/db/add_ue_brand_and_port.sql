-- 为ue表添加品牌和port字段
ALTER TABLE `ue` 
ADD COLUMN `brand` varchar(50) DEFAULT NULL COMMENT 'UE品牌' AFTER `network_type_id`,
ADD COLUMN `port` varchar(10) DEFAULT '0' COMMENT 'UE端口号' AFTER `brand`;

-- 添加索引以提高查询性能
ALTER TABLE `ue` 
ADD INDEX `idx_brand` (`brand`),
ADD INDEX `idx_port` (`port`);

-- 添加品牌字段的约束（可选，限制为指定的8个品牌）
-- ALTER TABLE `ue` ADD CONSTRAINT `chk_brand` CHECK (`brand` IN ('xiaomi', 'oppo', 'vivo', 'samsung', 'honor', 'huawei', 'apple', 'Huawei-Hisilicon'));
