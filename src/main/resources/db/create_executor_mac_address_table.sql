-- 创建执行机MAC地址表
CREATE TABLE IF NOT EXISTS `executor_mac_address` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `mac_address` varchar(20) NOT NULL COMMENT 'MAC地址',
  `executor_id` bigint(20) DEFAULT NULL COMMENT '关联的执行机ID（可为空，表示未分配）',
  `ip_address` varchar(50) DEFAULT NULL COMMENT 'IP地址（用于记录注册时的IP）',
  `status` tinyint(4) DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) DEFAULT NULL COMMENT '修改人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint(4) DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mac_address` (`mac_address`),
  KEY `idx_executor_id` (`executor_id`),
  KEY `idx_ip_address` (`ip_address`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='执行机MAC地址表';

