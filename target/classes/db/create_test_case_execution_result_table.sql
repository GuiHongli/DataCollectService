-- 创建用例执行结果表
CREATE TABLE IF NOT EXISTS `test_case_execution_result` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` varchar(100) NOT NULL COMMENT '任务ID',
  `test_case_id` bigint(20) NOT NULL COMMENT '用例ID',
  `round` int(11) NOT NULL COMMENT '轮次',
  `status` varchar(20) NOT NULL COMMENT '执行状态 (SUCCESS/FAILED/TIMEOUT)',
  `result` text COMMENT '执行结果描述',
  `execution_time` bigint(20) COMMENT '执行耗时（毫秒）',
  `start_time` datetime COMMENT '开始时间',
  `end_time` datetime COMMENT '结束时间',
  `error_message` text COMMENT '错误信息（如果执行失败）',
  `executor_ip` varchar(50) NOT NULL COMMENT '执行机IP',
  `test_case_set_id` bigint(20) NOT NULL COMMENT '用例集ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_task_id` (`task_id`),
  KEY `idx_test_case_id` (`test_case_id`),
  KEY `idx_test_case_set_id` (`test_case_set_id`),
  KEY `idx_executor_ip` (`executor_ip`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用例执行结果表';
