-- 将 deviceid 和 IMSI 对应关系表中的 imsi 字段改为 gpsi

-- 删除旧的唯一索引
ALTER TABLE `test_settings_device_imsi_mapping` 
DROP INDEX IF EXISTS `uk_imsi`;

-- 重命名字段
ALTER TABLE `test_settings_device_imsi_mapping` 
CHANGE COLUMN `imsi` `gpsi` varchar(100) NOT NULL COMMENT 'GPSI';

-- 添加新的唯一索引
ALTER TABLE `test_settings_device_imsi_mapping` 
ADD UNIQUE KEY `uk_gpsi` (`gpsi`);

-- 更新表注释
ALTER TABLE `test_settings_device_imsi_mapping` 
COMMENT = 'deviceid和GPSI对应关系表';

