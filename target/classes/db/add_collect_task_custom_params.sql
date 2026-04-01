-- 为采集任务表添加自定义参数字段
ALTER TABLE collect_task ADD COLUMN custom_params TEXT COMMENT '自定义参数列表（JSON格式）';
