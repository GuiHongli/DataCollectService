-- 逻辑组网表
CREATE TABLE IF NOT EXISTS `logic_network` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '逻辑组网名称',
  `description` text COMMENT '描述',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint(4) DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='逻辑组网表';

-- 逻辑环境组网关联表
CREATE TABLE IF NOT EXISTS `logic_environment_network` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `logic_environment_id` bigint(20) NOT NULL COMMENT '逻辑环境ID',
  `logic_network_id` bigint(20) NOT NULL COMMENT '逻辑组网ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint(4) DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_logic_environment_network` (`logic_environment_id`, `logic_network_id`),
  KEY `idx_logic_environment_id` (`logic_environment_id`),
  KEY `idx_logic_network_id` (`logic_network_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='逻辑环境组网关联表';

-- 插入一些示例逻辑组网数据
INSERT INTO `logic_network` (`name`, `description`) VALUES
('4G标准网络', '4G标准网络环境，适用于一般测试场景'),
('4G弱网环境', '4G弱网环境，模拟网络信号较差的场景'),
('5G高速网络', '5G高速网络环境，适用于高带宽测试场景'),
('5G低延迟网络', '5G低延迟网络环境，适用于实时性要求高的场景'),
('WiFi标准网络', 'WiFi标准网络环境，适用于室内测试场景'),
('WiFi弱网环境', 'WiFi弱网环境，模拟WiFi信号较差的场景');
