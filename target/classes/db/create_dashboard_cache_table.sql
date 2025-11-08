-- 仪表盘统计数据缓存表
CREATE TABLE IF NOT EXISTS `dashboard_cache` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `cache_key` varchar(100) NOT NULL COMMENT '缓存键（OVERALL_STATS或regionId）',
  `cache_type` varchar(20) NOT NULL COMMENT '缓存类型（OVERALL-总体统计，REGION-地域统计）',
  `cache_value` text NOT NULL COMMENT '缓存值，JSON格式存储统计数据',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cache_key_type` (`cache_key`, `cache_type`),
  KEY `idx_cache_type` (`cache_type`),
  KEY `idx_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仪表盘统计数据缓存表';






