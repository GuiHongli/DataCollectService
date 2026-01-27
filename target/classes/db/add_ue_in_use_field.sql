-- 为UE表添加in_use字段，用于标记UE是否正在使用中
ALTER TABLE `ue` 
ADD COLUMN `in_use` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否正在使用中：0-未使用，1-使用中' AFTER `status`;

-- 添加索引，用于快速查询正在使用的UE
CREATE INDEX `idx_in_use` ON `ue` (`in_use`);



















