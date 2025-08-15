-- 测试数据：可用逻辑环境匹配功能
-- 执行前请确保数据库已初始化

USE `data_collect`;

-- 1. 插入测试用例数据（如果不存在）
INSERT IGNORE INTO `test_case` (`test_case_set_id`, `name`, `code`, `logic_network`, `test_steps`, `expected_result`) VALUES
(1, '4G网络连接测试', 'TC001', '4G标准网络;4G弱网环境', '1. 启动测试应用\n2. 检查网络连接状态\n3. 执行网络连接测试\n4. 验证连接稳定性', '应用成功连接到4G网络，网络状态显示正常，连接稳定'),
(1, '5G网络性能测试', 'TC002', '5G高速网络;5G低延迟网络', '1. 启动性能测试工具\n2. 执行网络带宽测试\n3. 执行网络延迟测试\n4. 记录测试结果', '网络带宽达到预期指标，延迟低于10ms，性能测试通过'),
(1, 'WiFi连接稳定性测试', 'TC003', 'WiFi标准网络;WiFi弱网环境', '1. 连接到WiFi网络\n2. 进行数据传输测试\n3. 监控连接稳定性\n4. 测试网络切换', 'WiFi连接稳定，数据传输正常，网络切换流畅'),
(1, '多网络环境切换测试', 'TC004', '4G标准网络;5G高速网络;WiFi标准网络', '1. 在4G网络下启动应用\n2. 切换到5G网络\n3. 切换到WiFi网络\n4. 验证网络切换效果', '网络切换成功，应用正常运行，无数据丢失'),
(1, '弱网环境测试', 'TC005', '4G弱网环境;WiFi弱网环境', '1. 在弱网环境下启动应用\n2. 执行基本功能测试\n3. 测试网络重连机制\n4. 验证用户体验', '应用在弱网环境下仍能基本运行，网络重连机制正常');

-- 2. 插入逻辑组网数据（如果不存在）
INSERT IGNORE INTO `logic_network` (`name`, `description`) VALUES
('4G标准网络', '4G标准网络环境，适用于一般测试场景'),
('4G弱网环境', '4G弱网环境，模拟网络信号较差的场景'),
('5G高速网络', '5G高速网络环境，适用于高带宽测试场景'),
('5G低延迟网络', '5G低延迟网络环境，适用于实时性要求高的场景'),
('WiFi标准网络', 'WiFi标准网络环境，适用于室内测试场景'),
('WiFi弱网环境', 'WiFi弱网环境，模拟WiFi信号较差的场景');

-- 3. 插入逻辑环境数据（如果不存在）
INSERT IGNORE INTO `logic_environment` (`name`, `executor_id`, `description`, `status`) VALUES
('北京-Android环境', 1, '北京地区Android测试环境，支持4G网络测试', 1),
('北京-iOS环境', 1, '北京地区iOS测试环境，支持WiFi网络测试', 1),
('上海-Android环境', 3, '上海地区Android测试环境，支持5G网络测试', 1),
('广州-Android环境', 4, '广州地区Android测试环境，支持多种网络测试', 1);

-- 4. 插入逻辑环境组网关联数据
-- 先删除可能存在的关联数据，避免重复
DELETE FROM `logic_environment_network` WHERE `logic_environment_id` IN (1, 2, 3, 4);

-- 插入新的关联数据
INSERT INTO `logic_environment_network` (`logic_environment_id`, `logic_network_id`) VALUES
-- 北京-Android环境：支持4G网络测试
(1, 1), -- 4G标准网络
(1, 2), -- 4G弱网环境

-- 北京-iOS环境：支持WiFi网络测试
(2, 5), -- WiFi标准网络
(2, 6), -- WiFi弱网环境

-- 上海-Android环境：支持5G网络测试
(3, 3), -- 5G高速网络
(3, 4), -- 5G低延迟网络

-- 广州-Android环境：支持多种网络测试
(4, 1), -- 4G标准网络
(4, 3), -- 5G高速网络
(4, 5); -- WiFi标准网络

-- 5. 验证数据插入结果
SELECT '测试用例数量' as info, COUNT(*) as count FROM `test_case` WHERE `test_case_set_id` = 1
UNION ALL
SELECT '逻辑组网数量', COUNT(*) FROM `logic_network`
UNION ALL
SELECT '逻辑环境数量', COUNT(*) FROM `logic_environment` WHERE `id` IN (1, 2, 3, 4)
UNION ALL
SELECT '环境组网关联数量', COUNT(*) FROM `logic_environment_network` WHERE `logic_environment_id` IN (1, 2, 3, 4);

-- 6. 查看逻辑环境及其关联的组网信息
SELECT 
    le.id,
    le.name as environment_name,
    le.description as environment_desc,
    e.name as executor_name,
    e.ip_address as executor_ip,
    GROUP_CONCAT(ln.name ORDER BY ln.name SEPARATOR ', ') as networks
FROM `logic_environment` le
LEFT JOIN `executor` e ON le.executor_id = e.id
LEFT JOIN `logic_environment_network` len ON le.id = len.logic_environment_id
LEFT JOIN `logic_network` ln ON len.logic_network_id = ln.id
WHERE le.id IN (1, 2, 3, 4)
GROUP BY le.id, le.name, le.description, e.name, e.ip_address
ORDER BY le.id;

-- 7. 查看测试用例及其环境组网需求
SELECT 
    tc.id,
    tc.name as test_case_name,
    tc.code as test_case_code,
    tc.logic_network as required_networks
FROM `test_case` tc
WHERE tc.test_case_set_id = 1
ORDER BY tc.code;
