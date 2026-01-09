-- 更新logic_environment_network表的字段注释，反映它现在引用network_type表
-- 注意：数据库字段名保持不变，但实际引用的是network_type表的ID

USE `data_collect`;

-- 更新logic_environment_network表的字段注释
ALTER TABLE `logic_environment_network` 
MODIFY COLUMN `logic_network_id` bigint(20) NOT NULL COMMENT '网络类型ID（引用network_type表）';

-- 验证更新结果
SELECT 
    COLUMN_NAME,
    COLUMN_COMMENT,
    DATA_TYPE,
    IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'data_collect' 
AND TABLE_NAME = 'logic_environment_network' 
AND COLUMN_NAME = 'logic_network_id';
