-- 体验测试设置相关表

-- 端侧FTP服务器信息表（只允许一条记录）
CREATE TABLE IF NOT EXISTS `test_settings_client_ftp` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `server_address` varchar(255) NOT NULL COMMENT '服务器地址',
  `account` varchar(100) NOT NULL COMMENT '账户',
  `password` varchar(255) NOT NULL COMMENT '密码',
  `check_md5` tinyint(4) DEFAULT '0' COMMENT '是否检验MD5值：0-否，1-是',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) DEFAULT NULL COMMENT '修改人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint(4) DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='端侧FTP服务器信息表';

-- 网络侧FTP服务器信息表（只允许一条记录）
CREATE TABLE IF NOT EXISTS `test_settings_network_ftp` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `server_address` varchar(255) NOT NULL COMMENT '服务器地址',
  `account` varchar(100) NOT NULL COMMENT '账户',
  `password` varchar(255) NOT NULL COMMENT '密码',
  `check_md5` tinyint(4) DEFAULT '0' COMMENT '是否检验MD5值：0-否，1-是',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) DEFAULT NULL COMMENT '修改人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint(4) DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网络侧FTP服务器信息表';

-- deviceid和IMSI对应关系表（可以有多条记录）
CREATE TABLE IF NOT EXISTS `test_settings_device_imsi_mapping` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `device_id` varchar(100) NOT NULL COMMENT 'deviceid',
  `imsi` varchar(100) NOT NULL COMMENT 'IMSI',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) DEFAULT NULL COMMENT '修改人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint(4) DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_device_id` (`device_id`),
  UNIQUE KEY `uk_imsi` (`imsi`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='deviceid和IMSI对应关系表';



