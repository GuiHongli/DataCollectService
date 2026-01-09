-- 为逻辑环境表添加网络和物理组网字段
ALTER TABLE `logic_environment` 
ADD COLUMN `network` varchar(50) DEFAULT NULL COMMENT '网络类型：normal、weak、congestion、weakcongestion、sunshang' AFTER `executor_id`,
ADD COLUMN `physical_network` text DEFAULT NULL COMMENT '物理组网列表，JSON格式存储' AFTER `network`;

