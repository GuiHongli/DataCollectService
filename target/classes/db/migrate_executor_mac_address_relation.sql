-- 数据库迁移脚本：修改executor和executor_mac_address表的关联关系
-- 执行前请备份数据库
-- 
-- 变更说明：
-- 1. executor表添加mac_address_id字段（关联executor_mac_address表的id）
-- 2. executor_mac_address表移除executor_id字段
-- 
-- 关联关系变更：
-- 之前：executor_mac_address.executor_id -> executor.id
-- 现在：executor.mac_address_id -> executor_mac_address.id

USE `data_collect`;

-- 1. 在executor表添加mac_address_id字段
ALTER TABLE `executor` 
ADD COLUMN `mac_address_id` bigint(20) DEFAULT NULL COMMENT '关联的MAC地址ID（关联executor_mac_address表的id）' AFTER `mac_address`;

-- 2. 添加索引
ALTER TABLE `executor` 
ADD INDEX `idx_mac_address_id` (`mac_address_id`);

-- 3. 迁移现有数据：将executor_mac_address表中的executor_id关联关系迁移到executor表
-- 注意：如果一个executor关联了多个executor_mac_address记录，这里只迁移第一个
UPDATE `executor` e
INNER JOIN (
    SELECT `executor_id`, MIN(`id`) as `mac_address_id`
    FROM `executor_mac_address`
    WHERE `executor_id` IS NOT NULL
    AND `deleted` = 0
    GROUP BY `executor_id`
) ema ON e.`id` = ema.`executor_id`
SET e.`mac_address_id` = ema.`mac_address_id`
WHERE e.`deleted` = 0;

-- 4. 从executor_mac_address表移除executor_id字段的索引
ALTER TABLE `executor_mac_address` 
DROP INDEX IF EXISTS `idx_executor_id`;

-- 5. 从executor_mac_address表移除executor_id字段
ALTER TABLE `executor_mac_address` 
DROP COLUMN `executor_id`;




