-- 修正地域数据层级关系
-- 执行前请备份数据库

USE `data_collect`;

-- 1. 清空现有地域数据
DELETE FROM `region` WHERE `deleted` = 0;

-- 2. 重新插入正确的地域数据
INSERT INTO `region` (`name`, `parent_id`, `level`, `description`, `status`) VALUES
('中国', NULL, 1, '中国片区', 1),
('北京市', 1, 2, '北京市（直辖市）', 1),
('上海市', 1, 2, '上海市（直辖市）', 1),
('广东省', 1, 2, '广东省', 1),
('广州市', 4, 3, '广州市', 1),
('深圳市', 4, 3, '深圳市', 1),
('江苏省', 1, 2, '江苏省', 1),
('南京市', 7, 3, '南京市', 1),
('苏州市', 7, 3, '苏州市', 1);

-- 3. 更新执行机的地域引用
UPDATE `executor` SET `region_id` = 2 WHERE `name` LIKE '%北京%';
UPDATE `executor` SET `region_id` = 3 WHERE `name` LIKE '%上海%';
UPDATE `executor` SET `region_id` = 5 WHERE `name` LIKE '%广州%';
