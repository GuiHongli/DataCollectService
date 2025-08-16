-- 更新collect_task表结构
USE data_collect;

-- 添加缺失的字段
ALTER TABLE collect_task 
ADD COLUMN collect_strategy_name VARCHAR(100) COMMENT '采集策略名称' AFTER strategy_id,
ADD COLUMN test_case_set_id BIGINT COMMENT '用例集ID' AFTER collect_strategy_name,
ADD COLUMN test_case_set_name VARCHAR(100) COMMENT '用例集名称' AFTER test_case_set_id,
ADD COLUMN collect_count INT DEFAULT 1 COMMENT '采集次数' AFTER test_case_set_name,
ADD COLUMN region_id BIGINT COMMENT '地域ID' AFTER collect_count,
ADD COLUMN country_id BIGINT COMMENT '国家ID' AFTER region_id,
ADD COLUMN province_id BIGINT COMMENT '省份ID' AFTER country_id,
ADD COLUMN city_id BIGINT COMMENT '城市ID' AFTER province_id,
ADD COLUMN total_test_case_count INT DEFAULT 0 COMMENT '总用例数' AFTER city_id,
ADD COLUMN completed_test_case_count INT DEFAULT 0 COMMENT '已完成用例数' AFTER total_test_case_count,
ADD COLUMN success_test_case_count INT DEFAULT 0 COMMENT '成功用例数' AFTER completed_test_case_count,
ADD COLUMN failed_test_case_count INT DEFAULT 0 COMMENT '失败用例数' AFTER success_test_case_count,
ADD COLUMN start_time DATETIME COMMENT '开始时间' AFTER failed_test_case_count,
ADD COLUMN end_time DATETIME COMMENT '结束时间' AFTER start_time;

-- 修改status字段类型为VARCHAR，支持字符串状态
ALTER TABLE collect_task MODIFY COLUMN status VARCHAR(20) DEFAULT 'PENDING' COMMENT '任务状态';

-- 删除不需要的字段
ALTER TABLE collect_task 
DROP COLUMN schedule,
DROP COLUMN last_run_time,
DROP COLUMN next_run_time,
DROP COLUMN create_by,
DROP COLUMN update_by,
DROP COLUMN deleted;

