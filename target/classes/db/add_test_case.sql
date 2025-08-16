-- 测试用例表
CREATE TABLE IF NOT EXISTS `test_case` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `test_case_set_id` bigint(20) NOT NULL COMMENT '用例集ID',
  `name` varchar(200) NOT NULL COMMENT '用例名称',
  `number` varchar(100) NOT NULL COMMENT '用例编号',
  `logic_network` varchar(100) DEFAULT NULL COMMENT '用例逻辑组网',
  `test_steps` text COMMENT '测试步骤',
  `expected_result` text COMMENT '预期结果',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint(4) DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_test_case_set_id` (`test_case_set_id`),
  KEY `idx_number` (`number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试用例表';
