-- 为测试用例表添加新字段
-- 添加时间：2025-10-11
-- 说明：增加 APPEN、模型场景、手机OS类 三个字段

ALTER TABLE `test_case`
ADD COLUMN `app_en` varchar(100) DEFAULT NULL COMMENT '用例APPEN' AFTER `app`,
ADD COLUMN `model_scenario` varchar(100) DEFAULT NULL COMMENT '用例模型场景' AFTER `app_en`,
ADD COLUMN `phone_os_type` varchar(50) DEFAULT NULL COMMENT '用例手机OS类' AFTER `model_scenario`;

