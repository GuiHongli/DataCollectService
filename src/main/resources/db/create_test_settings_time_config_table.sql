-- 端侧和网络侧时间配置表（只允许一条记录）
CREATE TABLE IF NOT EXISTS `test_settings_time_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `time_diff` int(11) DEFAULT '0' COMMENT '端侧匹配网络侧时间差（最大10，最小0）',
  `collect_interval` int(11) DEFAULT '10' COMMENT '端侧采集时间间隔（秒）',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) DEFAULT NULL COMMENT '修改人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint(4) DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='端侧和网络侧时间配置表';




