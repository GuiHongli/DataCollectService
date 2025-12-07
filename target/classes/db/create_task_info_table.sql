-- 创建端侧任务信息表
CREATE TABLE IF NOT EXISTS `task_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` varchar(255) DEFAULT NULL COMMENT '任务ID',
  `nation` varchar(100) DEFAULT NULL COMMENT '国家信息',
  `operator` varchar(100) DEFAULT NULL COMMENT '运营商信息',
  `prb` varchar(50) DEFAULT NULL COMMENT 'PRB',
  `rsrp` varchar(50) DEFAULT NULL COMMENT 'RSRP',
  `service` varchar(100) DEFAULT NULL COMMENT '业务大类',
  `app` varchar(100) DEFAULT NULL COMMENT '应用名称',
  `start_time` varchar(50) DEFAULT NULL COMMENT '开始时间（格式：年月日时分秒，24小时制）',
  `end_time` varchar(50) DEFAULT NULL COMMENT '结束时间（格式：年月日时分秒，24小时制）',
  `user_category` varchar(50) DEFAULT NULL COMMENT '用户类别',
  `device_id` varchar(100) DEFAULT NULL COMMENT '设备ID',
  `summary` text COMMENT '数据报告（JSON格式），包含：stunNumber（卡顿次数）、stunRate（卡顿率）、avgUplinkRtt（平均上行RTT）、avgDownlinkRtt（平均下行RTT）、avgUplinkSpeed（平均上行速率）、avgDownlinkSpeed（平均下行速率）、avgUplinkLost（平均上行丢包率）、avgDownlinkLost（平均下行丢包率）、avgLost（平均丢包率）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(4) DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_app` (`app`),
  KEY `idx_service` (`service`),
  KEY `idx_nation` (`nation`),
  KEY `idx_operator` (`operator`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_deleted` (`deleted`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='端侧任务信息表';

