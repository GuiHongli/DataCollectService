-- 修改执行机MAC地址表，支持一个MAC地址关联多个IP
-- 1. 删除旧的唯一约束
ALTER TABLE `executor_mac_address` DROP INDEX IF EXISTS `uk_mac_address`;

-- 2. 修改ip_address字段为NOT NULL（如果之前允许为空）
ALTER TABLE `executor_mac_address` MODIFY COLUMN `ip_address` varchar(50) NOT NULL COMMENT 'IP地址（一个MAC地址可以关联多个IP）';

-- 3. 添加新的唯一约束（MAC地址+IP地址的组合唯一）
ALTER TABLE `executor_mac_address` ADD UNIQUE KEY `uk_mac_address_ip` (`mac_address`, `ip_address`);

-- 4. 添加MAC地址索引（用于快速查询一个MAC地址的所有IP）
ALTER TABLE `executor_mac_address` ADD INDEX `idx_mac_address` (`mac_address`);












