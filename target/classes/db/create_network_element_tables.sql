-- 创建网元表
CREATE TABLE IF NOT EXISTS `network_element` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '网元名称',
  `description` varchar(500) DEFAULT NULL COMMENT '描述',
  `status` int(1) DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) DEFAULT NULL COMMENT '更新人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int(1) DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_name` (`name`),
  KEY `idx_status` (`status`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网元表';

-- 创建网元属性表
CREATE TABLE IF NOT EXISTS `network_element_attribute` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `network_element_id` bigint(20) NOT NULL COMMENT '网元ID',
  `attribute_name` varchar(100) NOT NULL COMMENT '属性名称',
  `attribute_value` varchar(500) DEFAULT NULL COMMENT '属性取值',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) DEFAULT NULL COMMENT '更新人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int(1) DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_network_element_id` (`network_element_id`),
  KEY `idx_deleted` (`deleted`),
  CONSTRAINT `fk_network_element_attribute_network_element` 
    FOREIGN KEY (`network_element_id`) 
    REFERENCES `network_element` (`id`) 
    ON DELETE CASCADE 
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网元属性表';




















