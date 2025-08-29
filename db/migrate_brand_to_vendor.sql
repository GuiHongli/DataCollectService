-- 数据库迁移脚本：将UE表中的brand字段改为vendor字段
-- 执行时间：2025-08-29
-- 说明：统一UE字段命名，将brand改为vendor以更好地表达厂商概念

USE data_collect;

-- 1. 修改字段名从brand改为vendor
ALTER TABLE ue CHANGE COLUMN brand vendor VARCHAR(255) COMMENT 'UE厂商';

-- 2. 验证修改结果
-- SELECT COLUMN_NAME, DATA_TYPE, COLUMN_COMMENT 
-- FROM INFORMATION_SCHEMA.COLUMNS 
-- WHERE TABLE_SCHEMA = 'data_collect' AND TABLE_NAME = 'ue' AND COLUMN_NAME = 'vendor';

-- 3. 检查是否有相关的外键约束需要更新（如果有的话）
-- SHOW CREATE TABLE ue;

-- 4. 检查索引是否需要更新（如果有的话）
-- SHOW INDEX FROM ue WHERE Column_name = 'vendor';

-- 迁移完成后的验证查询
-- DESCRIBE ue;
