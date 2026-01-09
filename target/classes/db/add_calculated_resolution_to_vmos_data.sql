-- 在vmos_data表中添加calculated_resolution字段
-- 添加时间：2024-01-01

ALTER TABLE `vmos_data` 
ADD COLUMN `calculated_resolution` varchar(50) DEFAULT NULL COMMENT '计算分辨率' AFTER `bitrate`;

