-- 远程登录日志表
CREATE TABLE IF NOT EXISTS `remote_login_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `executor_ip` varchar(50) NOT NULL COMMENT '执行机IP地址',
  `logic_environment_name` varchar(100) DEFAULT NULL COMMENT '逻辑环境名称',
  `os_type` varchar(20) NOT NULL COMMENT '操作系统类型：linux, windows',
  `connection_type` varchar(20) NOT NULL COMMENT '连接方式：ssh, rdp, vnc',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `port` int(11) DEFAULT NULL COMMENT '端口号',
  `operation_note` text COMMENT '操作说明',
  `connect_time` datetime DEFAULT NULL COMMENT '连接时间',
  `disconnect_time` datetime DEFAULT NULL COMMENT '断开时间',
  `status` varchar(20) DEFAULT 'CONNECTING' COMMENT '状态：CONNECTING, CONNECTED, DISCONNECTED, FAILED',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) DEFAULT NULL COMMENT '更新人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(4) DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_executor_ip` (`executor_ip`),
  KEY `idx_connect_time` (`connect_time`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='远程登录日志表';
