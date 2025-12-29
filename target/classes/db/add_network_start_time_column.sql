-- 为client_task_info表添加network_start_time字段
-- 用于保存用户选择的网络侧开始时间，用于速率对比

ALTER TABLE `client_task_info` 
ADD COLUMN `network_start_time` varchar(50) DEFAULT NULL COMMENT '网络侧开始时间（用户选择的网络侧开始时间，用于速率对比，格式：2025-10-27 07:05:00 UTC+0）' 
AFTER `device_id`;



