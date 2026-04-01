-- 创建系统配置表（只允许一条记录）
CREATE TABLE IF NOT EXISTS `system_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `config_key` varchar(50) NOT NULL DEFAULT 'default' COMMENT '配置键（用于唯一约束，固定值）',
  `ue_disable_environment_when_in_use` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'UE使用中是否禁用环境（0-否，1-是）',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) DEFAULT NULL COMMENT '修改人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(4) DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 插入默认配置记录（如果不存在）
INSERT IGNORE INTO `system_config` (`config_key`, `ue_disable_environment_when_in_use`, `create_by`) 
VALUES ('default', 0, 'system');

