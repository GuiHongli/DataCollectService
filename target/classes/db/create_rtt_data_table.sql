-- 创建RTT数据表
CREATE TABLE IF NOT EXISTS `rtt_data` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` varchar(255) DEFAULT NULL COMMENT '任务ID（关联client_task_info表的task_id）',
  `index_time` varchar(50) DEFAULT NULL COMMENT '时间索引',
  `dl_delay` varchar(50) DEFAULT NULL COMMENT '下行RTT（ms）',
  `ul_delay` varchar(50) DEFAULT NULL COMMENT '上行RTT（ms）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(4) DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RTT数据表';

