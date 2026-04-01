-- 合并logic_network表和network_type表，以network_type表作为统一数据源
-- 执行前请先备份数据库！

USE `data_collect`;

-- 1. 备份当前数据（可选，但强烈建议）
-- CREATE TABLE `logic_network_backup` AS SELECT * FROM `logic_network`;
-- CREATE TABLE `logic_environment_network_backup` AS SELECT * FROM `logic_environment_network`;

-- 2. 将logic_network表的数据迁移到network_type表
-- 首先检查是否有重复的名称，如果有则处理冲突
INSERT INTO `network_type` (`name`, `description`, `status`, `create_time`, `update_time`, `deleted`)
SELECT 
    ln.name,
    ln.description,
    1 as status,  -- 默认启用状态
    ln.create_time,
    ln.update_time,
    ln.deleted
FROM `logic_network` ln
WHERE ln.deleted = 0  -- 只迁移未删除的数据
AND NOT EXISTS (
    SELECT 1 FROM `network_type` nt 
    WHERE nt.name = ln.name AND nt.deleted = 0
);

-- 3. 创建临时映射表，记录ID对应关系
CREATE TEMPORARY TABLE `id_mapping` (
    `old_id` bigint NOT NULL,
    `new_id` bigint NOT NULL,
    PRIMARY KEY (`old_id`)
);

-- 4. 填充ID映射关系
INSERT INTO `id_mapping` (`old_id`, `new_id`)
SELECT 
    ln.id as old_id,
    nt.id as new_id
FROM `logic_network` ln
INNER JOIN `network_type` nt ON ln.name = nt.name AND ln.deleted = 0 AND nt.deleted = 0;

-- 5. 更新logic_environment_network表的外键引用
-- 先备份当前的关联数据
CREATE TEMPORARY TABLE `temp_logic_environment_network` AS 
SELECT * FROM `logic_environment_network` WHERE deleted = 0;

-- 删除旧的关联数据
DELETE FROM `logic_environment_network` WHERE deleted = 0;

-- 插入更新后的关联数据
INSERT INTO `logic_environment_network` (
    `logic_environment_id`, 
    `logic_network_id`, 
    `create_time`, 
    `update_time`, 
    `deleted`
)
SELECT 
    temp.logic_environment_id,
    mapping.new_id as logic_network_id,
    temp.create_time,
    temp.update_time,
    temp.deleted
FROM `temp_logic_environment_network` temp
INNER JOIN `id_mapping` mapping ON temp.logic_network_id = mapping.old_id;

-- 6. 验证数据迁移结果
SELECT '数据迁移验证' as info;

-- 检查network_type表中的数据
SELECT 'network_type表数据量' as info, COUNT(*) as count FROM `network_type` WHERE deleted = 0;

-- 检查logic_environment_network表的关联数据
SELECT 'logic_environment_network表关联数据量' as info, COUNT(*) as count FROM `logic_environment_network` WHERE deleted = 0;

-- 检查是否有孤立的关联数据
SELECT '孤立的关联数据' as info, COUNT(*) as count 
FROM `logic_environment_network` len
LEFT JOIN `network_type` nt ON len.logic_network_id = nt.id AND nt.deleted = 0
WHERE len.deleted = 0 AND nt.id IS NULL;

-- 7. 显示迁移后的数据示例
SELECT '迁移后的网络类型数据示例' as info;
SELECT id, name, description, status FROM `network_type` WHERE deleted = 0 ORDER BY id LIMIT 10;

-- 8. 清理临时表
DROP TEMPORARY TABLE `id_mapping`;
DROP TEMPORARY TABLE `temp_logic_environment_network`;

-- 9. 删除logic_network表（谨慎操作！）
-- 注意：执行前请确认数据迁移成功，建议先注释掉这行，验证无误后再执行
-- DROP TABLE `logic_network`;

-- 10. 最终验证
SELECT '合并完成，请验证以下内容：' as info;
SELECT '1. 检查network_type表是否包含所有原logic_network的数据' as check_item;
SELECT '2. 检查logic_environment_network表的关联是否正确' as check_item;
SELECT '3. 检查前端功能是否正常' as check_item;
SELECT '4. 确认无误后执行DROP TABLE logic_network' as check_item;
