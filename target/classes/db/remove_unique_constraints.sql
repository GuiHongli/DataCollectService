-- 删除逻辑环境UE关联表的唯一约束
ALTER TABLE `logic_environment_ue` DROP INDEX `uk_logic_environment_ue`;

-- 删除逻辑环境组网关联表的唯一约束
ALTER TABLE `logic_environment_network` DROP INDEX `uk_logic_environment_network`;

-- 验证约束是否已删除
SHOW INDEX FROM `logic_environment_ue`;
SHOW INDEX FROM `logic_environment_network`;
