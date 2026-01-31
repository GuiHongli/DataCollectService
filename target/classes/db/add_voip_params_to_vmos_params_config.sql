-- 为vMOS参数配置表添加voip特有的常量字段
ALTER TABLE `vmos_params_config` 
ADD COLUMN `v1` decimal(10,4) DEFAULT NULL COMMENT 'v1参数（voip特有）' AFTER `g2`,
ADD COLUMN `v2` decimal(10,4) DEFAULT NULL COMMENT 'v2参数（voip特有）' AFTER `v1`,
ADD COLUMN `v3` decimal(10,4) DEFAULT NULL COMMENT 'v3参数（voip特有）' AFTER `v2`,
ADD COLUMN `v4` decimal(10,4) DEFAULT NULL COMMENT 'v4参数（voip特有）' AFTER `v3`,
ADD COLUMN `v5` decimal(10,4) DEFAULT NULL COMMENT 'v5参数（voip特有）' AFTER `v4`,
ADD COLUMN `fr` decimal(10,4) DEFAULT NULL COMMENT 'fr参数（voip特有，帧率）' AFTER `v5`,
ADD COLUMN `v12` decimal(10,4) DEFAULT NULL COMMENT 'v12参数（voip特有）' AFTER `fr`,
ADD COLUMN `v13` decimal(10,4) DEFAULT NULL COMMENT 'v13参数（voip特有）' AFTER `v12`,
ADD COLUMN `v14` decimal(10,4) DEFAULT NULL COMMENT 'v14参数（voip特有）' AFTER `v13`,
ADD COLUMN `v58` decimal(10,4) DEFAULT NULL COMMENT 'v58参数（voip特有）' AFTER `v14`,
ADD COLUMN `v59` decimal(10,4) DEFAULT NULL COMMENT 'v59参数（voip特有）' AFTER `v58`,
ADD COLUMN `v60` decimal(10,4) DEFAULT NULL COMMENT 'v60参数（voip特有）' AFTER `v59`,
ADD COLUMN `v61` decimal(10,4) DEFAULT NULL COMMENT 'v61参数（voip特有）' AFTER `v60`,
ADD COLUMN `v62` decimal(10,4) DEFAULT NULL COMMENT 'v62参数（voip特有）' AFTER `v61`,
ADD COLUMN `v63` decimal(10,4) DEFAULT NULL COMMENT 'v63参数（voip特有）' AFTER `v62`;
