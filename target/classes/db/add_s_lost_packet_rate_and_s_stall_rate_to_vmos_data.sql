-- 在vmos_data表中添加s_lost_packet_rate和s_stall_rate字段
-- 添加时间：2024-01-01

ALTER TABLE `vmos_data` 
ADD COLUMN `s_lost_packet_rate` varchar(50) DEFAULT NULL COMMENT '丢包率（s_lost_packet_rate）' AFTER `presentation_experience`,
ADD COLUMN `s_stall_rate` varchar(50) DEFAULT NULL COMMENT '卡顿占比（s_stall_rate）' AFTER `s_lost_packet_rate`;

