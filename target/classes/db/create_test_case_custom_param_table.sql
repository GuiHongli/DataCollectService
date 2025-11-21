-- 用例自定义参数表
CREATE TABLE IF NOT EXISTS `test_case_custom_param` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `business_category` varchar(100) NOT NULL COMMENT '业务大类',
  `app` varchar(100) NOT NULL COMMENT 'APP',
  `param_name` varchar(200) NOT NULL COMMENT '自定义参数名称',
  `param_values` text NOT NULL COMMENT '自定义参数值，JSON数组格式，如：["value1", "value2", "value3"]',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint(4) DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_business_category` (`business_category`),
  KEY `idx_app` (`app`),
  KEY `idx_param_name` (`param_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用例自定义参数表';



















