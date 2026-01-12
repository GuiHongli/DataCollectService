-- 创建app版本变更自动采集配置表
CREATE TABLE IF NOT EXISTS `app_version_auto_collect` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `app_name` varchar(200) NOT NULL COMMENT '应用名称',
  `platform_type` tinyint(1) NOT NULL COMMENT '平台类型（0-安卓，1-iOS）',
  `auto_collect` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否自动采集（0-否，1-是）',
  `template_id` bigint(20) COMMENT '采集任务模版ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否删除（0-未删除，1-已删除）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_app_name_platform` (`app_name`, `platform_type`, `deleted`),
  KEY `idx_template_id` (`template_id`),
  KEY `idx_auto_collect` (`auto_collect`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='app版本变更自动采集配置表';

