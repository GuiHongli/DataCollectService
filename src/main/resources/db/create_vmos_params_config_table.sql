-- vMOS计算参数配置表（按业务大类配置）
CREATE TABLE IF NOT EXISTS `vmos_params_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `service` varchar(50) NOT NULL COMMENT '应用大类（shortvideo, voip, watch_live, live_streaming, vod_streaming, meeting, mobile_game, mobile_game_cloud）',
  `a1` decimal(10,4) DEFAULT NULL COMMENT 'a1参数（bitrate参数）',
  `a2` decimal(10,4) DEFAULT NULL COMMENT 'a2参数（resolution参数）',
  `w1` decimal(10,4) DEFAULT NULL COMMENT 'w1参数（sQuality权重1）',
  `w2` decimal(10,4) DEFAULT NULL COMMENT 'w2参数（sQuality权重2）',
  `a3` decimal(10,4) DEFAULT NULL COMMENT 'a3参数（RTT参数）',
  `a4` decimal(10,4) DEFAULT NULL COMMENT 'a4参数（lost_packet_rate参数）',
  `a5` decimal(10,4) DEFAULT NULL COMMENT 'a5参数（stall_rate参数）',
  `g1` decimal(10,4) DEFAULT NULL COMMENT 'g1参数（sView权重1）',
  `g2` decimal(10,4) DEFAULT NULL COMMENT 'g2参数（sView权重2）',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) DEFAULT NULL COMMENT '修改人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint(4) DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_service` (`service`, `deleted`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='vMOS计算参数配置表';
