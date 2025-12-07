-- 为端侧FTP服务器信息表和网络侧FTP服务器信息表添加目录字段

-- 端侧FTP服务器信息表添加目录字段
ALTER TABLE `test_settings_client_ftp` 
ADD COLUMN `directory` varchar(255) DEFAULT NULL COMMENT '目录' AFTER `check_md5`;

-- 网络侧FTP服务器信息表添加目录字段
ALTER TABLE `test_settings_network_ftp` 
ADD COLUMN `directory` varchar(255) DEFAULT NULL COMMENT '目录' AFTER `check_md5`;



