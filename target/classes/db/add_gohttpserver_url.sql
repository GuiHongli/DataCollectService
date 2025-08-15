-- 为test_case_set表添加gohttpserver_url字段
ALTER TABLE `test_case_set` 
ADD COLUMN `gohttpserver_url` varchar(500) DEFAULT NULL COMMENT 'gohttpserver文件访问URL' 
AFTER `file_path`;

-- 添加索引
ALTER TABLE `test_case_set` 
ADD INDEX `idx_gohttpserver_url` (`gohttpserver_url`);
