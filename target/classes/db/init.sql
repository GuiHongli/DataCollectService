-- 创建数据库
CREATE DATABASE IF NOT EXISTS `data_collect` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `data_collect`;

-- 地域表
CREATE TABLE IF NOT EXISTS `region` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '地域名称',
  `parent_id` bigint(20) DEFAULT NULL COMMENT '父级ID',
  `level` tinyint(4) NOT NULL COMMENT '层级：1-片区，2-国家，3-省份，4-城市',
  `description` text COMMENT '描述',
  `status` tinyint(4) DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) DEFAULT NULL COMMENT '修改人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint(4) DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='地域表';

-- 网络类型表
CREATE TABLE IF NOT EXISTS `network_type` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '网络类型名称',
  `description` text COMMENT '描述',
  `status` tinyint(4) DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) DEFAULT NULL COMMENT '修改人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint(4) DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网络类型表';

-- 执行机表
CREATE TABLE IF NOT EXISTS `executor` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ip_address` varchar(50) NOT NULL COMMENT '执行机IP地址',
  `name` varchar(100) NOT NULL COMMENT '执行机名称',
  `region_id` bigint(20) NOT NULL COMMENT '执行机所属地域ID',
  `description` text COMMENT '描述',
  `status` tinyint(4) DEFAULT '1' COMMENT '状态：0-离线，1-在线，2-故障',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) DEFAULT NULL COMMENT '修改人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint(4) DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ip_address` (`ip_address`),
  KEY `idx_region_id` (`region_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='执行机表';

-- UE表
CREATE TABLE IF NOT EXISTS `ue` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ue_id` varchar(100) NOT NULL COMMENT 'UE ID',
  `name` varchar(100) NOT NULL COMMENT 'UE名称',
  `purpose` varchar(200) NOT NULL COMMENT 'UE用途',
  `network_type_id` bigint(20) NOT NULL COMMENT '网络类型ID',
  `description` text COMMENT '描述',
  `status` tinyint(4) DEFAULT '1' COMMENT '状态：0-不可用，1-可用',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) DEFAULT NULL COMMENT '修改人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint(4) DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ue_id` (`ue_id`),
  KEY `idx_network_type_id` (`network_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='UE表';

-- 逻辑环境表
CREATE TABLE IF NOT EXISTS `logic_environment` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '逻辑环境名称',
  `executor_id` bigint(20) NOT NULL COMMENT '执行机ID',
  `description` text COMMENT '描述',
  `status` tinyint(4) DEFAULT '1' COMMENT '状态：0-不可用，1-可用',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) DEFAULT NULL COMMENT '修改人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint(4) DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_executor_id` (`executor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='逻辑环境表';

-- 逻辑环境UE关联表
CREATE TABLE IF NOT EXISTS `logic_environment_ue` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `logic_environment_id` bigint(20) NOT NULL COMMENT '逻辑环境ID',
  `ue_id` bigint(20) NOT NULL COMMENT 'UE ID',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) DEFAULT NULL COMMENT '修改人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint(4) DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_logic_environment_ue` (`logic_environment_id`, `ue_id`),
  KEY `idx_logic_environment_id` (`logic_environment_id`),
  KEY `idx_ue_id` (`ue_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='逻辑环境UE关联表';

-- 用例集表
CREATE TABLE IF NOT EXISTS `test_case_set` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '用例集名称',
  `version` varchar(50) NOT NULL COMMENT '用例集版本',
  `file_path` varchar(500) NOT NULL COMMENT '用例集文件路径',
  `file_size` bigint(20) DEFAULT NULL COMMENT '文件大小（字节）',
  `description` text COMMENT '描述',
  `status` tinyint(4) DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) DEFAULT NULL COMMENT '修改人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint(4) DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name_version` (`name`, `version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用例集表';

-- 采集策略表
CREATE TABLE IF NOT EXISTS `collect_strategy` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '策略名称',
  `collect_count` int(11) NOT NULL COMMENT '采集次数',
  `logic_environment_id` bigint(20) NOT NULL COMMENT '逻辑环境ID',
  `description` text COMMENT '描述',
  `status` tinyint(4) DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) DEFAULT NULL COMMENT '修改人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint(4) DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_logic_environment_id` (`logic_environment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采集策略表';

-- 采集任务表
CREATE TABLE IF NOT EXISTS `collect_task` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '任务名称',
  `strategy_id` bigint(20) NOT NULL COMMENT '采集策略ID',
  `schedule` varchar(100) DEFAULT NULL COMMENT '定时表达式',
  `description` text COMMENT '描述',
  `status` tinyint(4) DEFAULT '0' COMMENT '状态：0-停止，1-运行中，2-暂停',
  `last_run_time` datetime DEFAULT NULL COMMENT '上次运行时间',
  `next_run_time` datetime DEFAULT NULL COMMENT '下次运行时间',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) DEFAULT NULL COMMENT '修改人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint(4) DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_strategy_id` (`strategy_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采集任务表';

-- 插入测试数据

-- 插入地域数据
INSERT INTO `region` (`name`, `parent_id`, `level`, `description`, `status`) VALUES
('中国', NULL, 1, '中国片区', 1),
('北京市', 1, 2, '北京市（直辖市）', 1),
('上海市', 1, 2, '上海市（直辖市）', 1),
('广东省', 1, 2, '广东省', 1),
('广州市', 4, 3, '广州市', 1),
('深圳市', 4, 3, '深圳市', 1),
('江苏省', 1, 2, '江苏省', 1),
('南京市', 7, 3, '南京市', 1),
('苏州市', 7, 3, '苏州市', 1);

-- 插入网络类型数据
INSERT INTO `network_type` (`name`, `description`, `status`) VALUES
('正常网络', '正常网络环境', 1),
('弱网', '弱网环境', 1),
('拥塞', '网络拥塞环境', 1),
('弱网+拥塞', '弱网+拥塞环境', 1);

-- 插入执行机数据
INSERT INTO `executor` (`ip_address`, `name`, `region_id`, `description`, `status`) VALUES
('192.168.1.100', '执行机-北京-01', 2, '北京地区执行机01', 1),
('192.168.1.101', '执行机-北京-02', 2, '北京地区执行机02', 1),
('192.168.2.100', '执行机-上海-01', 3, '上海地区执行机01', 1),
('192.168.3.100', '执行机-广州-01', 5, '广州地区执行机01', 1);

-- 插入UE数据
INSERT INTO `ue` (`ue_id`, `name`, `purpose`, `network_type_id`, `description`, `status`) VALUES
('UE001', 'UE-Android-01', '短视频测试', 1, 'Android UE设备01', 1),
('UE002', 'UE-Android-02', '直播测试', 2, 'Android UE设备02', 1),
('UE003', 'UE-iOS-01', '游戏测试', 1, 'iOS UE设备01', 1),
('UE004', 'UE-iOS-02', '社交测试', 3, 'iOS UE设备02', 1);

-- 插入逻辑环境数据
INSERT INTO `logic_environment` (`name`, `executor_id`, `description`, `status`) VALUES
('北京-Android环境', 1, '北京地区Android测试环境', 1),
('北京-iOS环境', 1, '北京地区iOS测试环境', 1),
('上海-Android环境', 3, '上海地区Android测试环境', 1),
('沈阳-Android环境', 4, '沈阳地区Android测试环境', 1);

-- 插入逻辑环境UE关联数据
INSERT INTO `logic_environment_ue` (`logic_environment_id`, `ue_id`) VALUES
(1, 1), (1, 2), -- 北京-Android环境关联UE001和UE002
(2, 3), (2, 4), -- 北京-iOS环境关联UE003和UE004
(3, 1), (3, 2), -- 上海-Android环境关联UE001和UE002
(4, 1), (4, 3); -- 沈阳-Android环境关联UE001和UE003

-- 插入用例集数据
INSERT INTO `test_case_set` (`name`, `version`, `file_path`, `file_size`, `description`, `status`) VALUES
('短视频采集', 'v0.1', '/uploads/testcase/短视频采集_v0.1.zip', 1024000, '短视频采集用例集', 1),
('直播测试', 'v1.0', '/uploads/testcase/直播测试_v1.0.zip', 2048000, '直播测试用例集', 1),
('游戏测试', 'v0.5', '/uploads/testcase/游戏测试_v0.5.zip', 1536000, '游戏测试用例集', 1);

-- 插入采集策略数据
INSERT INTO `collect_strategy` (`name`, `collect_count`, `logic_environment_id`, `description`, `status`) VALUES
('北京Android性能监控', 100, 1, '北京地区Android设备性能监控策略', 1),
('北京iOS日志采集', 50, 2, '北京地区iOS设备日志采集策略', 1),
('上海Android网络监控', 80, 3, '上海地区Android设备网络监控策略', 1);

-- 插入采集任务数据
INSERT INTO `collect_task` (`name`, `strategy_id`, `schedule`, `description`, `status`) VALUES
('北京Android性能监控任务', 1, '0 */5 * * * ?', '北京地区Android设备性能监控任务', 0),
('北京iOS日志采集任务', 2, '0 0 */1 * * ?', '北京地区iOS设备日志采集任务', 0),
('上海Android网络监控任务', 3, '0 */10 * * * ?', '上海地区Android设备网络监控任务', 0);
