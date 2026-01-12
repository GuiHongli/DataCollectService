-- 创建采集任务模版表
CREATE TABLE IF NOT EXISTS `collect_task_template` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '模版名称',
  `description` text COMMENT '模版描述',
  `network_element_ids` text COMMENT '网元ID列表（JSON格式）',
  `collect_strategy_id` bigint(20) NOT NULL COMMENT '采集策略ID',
  `collect_count` int(11) NOT NULL COMMENT '采集次数',
  `region_id` bigint(20) COMMENT '地域ID',
  `country_id` bigint(20) COMMENT '国家ID',
  `province_id` bigint(20) COMMENT '省份ID',
  `city_id` bigint(20) COMMENT '城市ID',
  `network` varchar(50) COMMENT '网络类型（normal、weak、congestion、weakcongestion、sunshang）',
  `manufacturer` text COMMENT '厂商列表（JSON格式）',
  `logic_environment_ids` text COMMENT '逻辑环境ID列表（JSON格式）',
  `task_custom_params` text COMMENT '任务级别自定义参数（JSON格式）',
  `custom_params` text COMMENT '用例配置列表（JSON格式）',
  `create_by` varchar(50) COMMENT '创建人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除（0-未删除，1-已删除）',
  PRIMARY KEY (`id`),
  KEY `idx_collect_strategy_id` (`collect_strategy_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采集任务模版表';

