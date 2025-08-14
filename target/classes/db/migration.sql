-- 数据库迁移脚本：移除code字段
-- 执行前请备份数据库

USE `data_collect`;

-- 1. 移除网络类型表的code字段（地域表已经不存在code字段）
ALTER TABLE `network_type` DROP COLUMN `code`;

-- 2. 更新测试数据（如果需要重新插入）
-- 删除现有数据
DELETE FROM `network_type`;

-- 重新插入网络类型数据（不包含code字段）
INSERT INTO `network_type` (`name`, `description`, `status`) VALUES
('正常网络', '正常网络环境', 1),
('弱网', '弱网环境', 1),
('拥塞', '网络拥塞环境', 1),
('弱网+拥塞', '弱网+拥塞环境', 1);
