-- MySQL dump 10.13  Distrib 9.4.0, for macos14.7 (x86_64)
--
-- Host: localhost    Database: data_collect
-- ------------------------------------------------------
-- Server version	9.4.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `collect_strategy`
--

DROP TABLE IF EXISTS `collect_strategy`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `collect_strategy` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '策略名称',
  `collect_count` int NOT NULL COMMENT '采集次数',
  `logic_environment_id` bigint NOT NULL COMMENT '逻辑环境ID',
  `description` text COMMENT '描述',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) DEFAULT NULL COMMENT '修改人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_logic_environment_id` (`logic_environment_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='采集策略表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `collect_strategy`
--

LOCK TABLES `collect_strategy` WRITE;
/*!40000 ALTER TABLE `collect_strategy` DISABLE KEYS */;
INSERT INTO `collect_strategy` VALUES (1,'北京Android性能监控',100,1,'北京地区Android设备性能监控策略',1,NULL,NULL,'2025-08-14 15:55:41','2025-08-14 15:55:41',0),(2,'北京iOS日志采集',50,2,'北京地区iOS设备日志采集策略',1,NULL,NULL,'2025-08-14 15:55:41','2025-08-14 15:55:41',0),(3,'上海Android网络监控',80,3,'上海地区Android设备网络监控策略',1,NULL,NULL,'2025-08-14 15:55:41','2025-08-14 15:55:41',0);
/*!40000 ALTER TABLE `collect_strategy` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `collect_task`
--

DROP TABLE IF EXISTS `collect_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `collect_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '任务名称',
  `strategy_id` bigint NOT NULL COMMENT '采集策略ID',
  `schedule` varchar(100) DEFAULT NULL COMMENT '定时表达式',
  `description` text COMMENT '描述',
  `status` tinyint DEFAULT '0' COMMENT '状态：0-停止，1-运行中，2-暂停',
  `last_run_time` datetime DEFAULT NULL COMMENT '上次运行时间',
  `next_run_time` datetime DEFAULT NULL COMMENT '下次运行时间',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) DEFAULT NULL COMMENT '修改人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_strategy_id` (`strategy_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='采集任务表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `collect_task`
--

LOCK TABLES `collect_task` WRITE;
/*!40000 ALTER TABLE `collect_task` DISABLE KEYS */;
INSERT INTO `collect_task` VALUES (1,'北京Android性能监控任务',1,'0 */5 * * * ?','北京地区Android设备性能监控任务',0,NULL,NULL,NULL,NULL,'2025-08-14 15:55:41','2025-08-14 15:55:41',0),(2,'北京iOS日志采集任务',2,'0 0 */1 * * ?','北京地区iOS设备日志采集任务',0,NULL,NULL,NULL,NULL,'2025-08-14 15:55:41','2025-08-14 15:55:41',0),(3,'上海Android网络监控任务',3,'0 */10 * * * ?','上海地区Android设备网络监控任务',0,NULL,NULL,NULL,NULL,'2025-08-14 15:55:41','2025-08-14 15:55:41',0);
/*!40000 ALTER TABLE `collect_task` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `executor`
--

DROP TABLE IF EXISTS `executor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `executor` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ip_address` varchar(50) NOT NULL COMMENT '执行机IP地址',
  `name` varchar(100) NOT NULL COMMENT '执行机名称',
  `region_id` bigint NOT NULL COMMENT '执行机所属地域ID',
  `description` text COMMENT '描述',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-离线，1-在线，2-故障',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) DEFAULT NULL COMMENT '修改人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ip_address` (`ip_address`),
  KEY `idx_region_id` (`region_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='执行机表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `executor`
--

LOCK TABLES `executor` WRITE;
/*!40000 ALTER TABLE `executor` DISABLE KEYS */;
INSERT INTO `executor` VALUES (1,'192.168.1.100','执行机-北京-01',4,'北京地区执行机01',1,NULL,NULL,'2025-08-14 15:55:41','2025-08-14 15:55:41',0),(2,'192.168.1.101','执行机-北京-02',4,'北京地区执行机02',1,NULL,NULL,'2025-08-14 15:55:41','2025-08-14 15:55:41',0),(3,'192.168.2.100','执行机-上海-01',5,'上海地区执行机01',1,NULL,NULL,'2025-08-14 15:55:41','2025-08-14 15:55:41',0),(4,'192.168.3.100','执行机-沈阳-01',3,'沈阳地区执行机01',1,NULL,NULL,'2025-08-14 15:55:41','2025-08-14 15:55:41',0);
/*!40000 ALTER TABLE `executor` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `logic_environment`
--

DROP TABLE IF EXISTS `logic_environment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `logic_environment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '逻辑环境名称',
  `executor_id` bigint NOT NULL COMMENT '执行机ID',
  `description` text COMMENT '描述',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-不可用，1-可用',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) DEFAULT NULL COMMENT '修改人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_executor_id` (`executor_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='逻辑环境表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `logic_environment`
--

LOCK TABLES `logic_environment` WRITE;
/*!40000 ALTER TABLE `logic_environment` DISABLE KEYS */;
INSERT INTO `logic_environment` VALUES (1,'北京-Android环境',1,'北京地区Android测试环境',1,NULL,NULL,'2025-08-14 15:55:41','2025-08-14 15:55:41',0),(2,'北京-iOS环境',1,'北京地区iOS测试环境',1,NULL,NULL,'2025-08-14 15:55:41','2025-08-14 15:55:41',0),(3,'上海-Android环境',3,'上海地区Android测试环境',1,NULL,NULL,'2025-08-14 15:55:41','2025-08-14 15:55:41',0),(4,'沈阳-Android环境',4,'沈阳地区Android测试环境',1,NULL,NULL,'2025-08-14 15:55:41','2025-08-14 15:55:41',0);
/*!40000 ALTER TABLE `logic_environment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `logic_environment_ue`
--

DROP TABLE IF EXISTS `logic_environment_ue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `logic_environment_ue` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `logic_environment_id` bigint NOT NULL COMMENT '逻辑环境ID',
  `ue_id` bigint NOT NULL COMMENT 'UE ID',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) DEFAULT NULL COMMENT '修改人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_logic_environment_ue` (`logic_environment_id`,`ue_id`),
  KEY `idx_logic_environment_id` (`logic_environment_id`),
  KEY `idx_ue_id` (`ue_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='逻辑环境UE关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `logic_environment_ue`
--

LOCK TABLES `logic_environment_ue` WRITE;
/*!40000 ALTER TABLE `logic_environment_ue` DISABLE KEYS */;
INSERT INTO `logic_environment_ue` VALUES (1,1,1,NULL,NULL,'2025-08-14 15:55:41','2025-08-14 15:55:41',0),(2,1,2,NULL,NULL,'2025-08-14 15:55:41','2025-08-14 15:55:41',0),(3,2,3,NULL,NULL,'2025-08-14 15:55:41','2025-08-14 15:55:41',0),(4,2,4,NULL,NULL,'2025-08-14 15:55:41','2025-08-14 15:55:41',0),(5,3,1,NULL,NULL,'2025-08-14 15:55:41','2025-08-14 15:55:41',0),(6,3,2,NULL,NULL,'2025-08-14 15:55:41','2025-08-14 15:55:41',0),(7,4,1,NULL,NULL,'2025-08-14 15:55:41','2025-08-14 15:55:41',0),(8,4,3,NULL,NULL,'2025-08-14 15:55:41','2025-08-14 15:55:41',0);
/*!40000 ALTER TABLE `logic_environment_ue` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `network_type`
--

DROP TABLE IF EXISTS `network_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `network_type` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '网络类型名称',
  `code` varchar(50) NOT NULL COMMENT '网络类型代码',
  `description` text COMMENT '描述',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) DEFAULT NULL COMMENT '修改人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='网络类型表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `network_type`
--

LOCK TABLES `network_type` WRITE;
/*!40000 ALTER TABLE `network_type` DISABLE KEYS */;
INSERT INTO `network_type` VALUES (1,'正常网络','normal','正常网络环境',1,NULL,NULL,'2025-08-14 15:55:41','2025-08-14 17:39:18',1),(2,'弱网','weak','弱网环境',1,NULL,NULL,'2025-08-14 15:55:41','2025-08-14 15:55:41',0),(3,'拥塞','congestion','网络拥塞环境',1,NULL,NULL,'2025-08-14 15:55:41','2025-08-14 15:55:41',0),(4,'弱网+拥塞','weak_congestion','弱网+拥塞环境',1,NULL,NULL,'2025-08-14 15:55:41','2025-08-14 15:55:41',0);
/*!40000 ALTER TABLE `network_type` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `region`
--

DROP TABLE IF EXISTS `region`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `region` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '地域名称',
  `code` varchar(50) NOT NULL COMMENT '地域代码',
  `parent_id` bigint DEFAULT NULL COMMENT '父级ID',
  `level` tinyint NOT NULL COMMENT '层级：1-片区，2-国家，3-省份，4-城市',
  `description` text COMMENT '描述',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) DEFAULT NULL COMMENT '修改人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='地域表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `region`
--

LOCK TABLES `region` WRITE;
/*!40000 ALTER TABLE `region` DISABLE KEYS */;
INSERT INTO `region` VALUES (1,'中国','China',NULL,1,'中国片区',1,NULL,NULL,'2025-08-14 15:55:41','2025-08-14 15:55:41',0),(2,'辽宁省','LN',1,2,'辽宁省',1,NULL,NULL,'2025-08-14 15:55:41','2025-08-14 15:55:41',0),(3,'沈阳市','SY',2,3,'沈阳市',1,NULL,NULL,'2025-08-14 15:55:41','2025-08-14 15:55:41',0),(4,'北京市','BJ',1,2,'北京市',1,NULL,NULL,'2025-08-14 15:55:41','2025-08-14 15:55:41',0),(5,'上海市','SH',1,2,'上海市',1,NULL,NULL,'2025-08-14 15:55:41','2025-08-14 15:55:41',0);
/*!40000 ALTER TABLE `region` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `test_case_set`
--

DROP TABLE IF EXISTS `test_case_set`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `test_case_set` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '用例集名称',
  `version` varchar(50) NOT NULL COMMENT '用例集版本',
  `file_path` varchar(500) NOT NULL COMMENT '用例集文件路径',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小（字节）',
  `description` text COMMENT '描述',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) DEFAULT NULL COMMENT '修改人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name_version` (`name`,`version`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用例集表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `test_case_set`
--

LOCK TABLES `test_case_set` WRITE;
/*!40000 ALTER TABLE `test_case_set` DISABLE KEYS */;
INSERT INTO `test_case_set` VALUES (1,'短视频采集','v0.1','/uploads/testcase/短视频采集_v0.1.zip',1024000,'短视频采集用例集',1,NULL,NULL,'2025-08-14 15:55:41','2025-08-14 15:55:41',0),(2,'直播测试','v1.0','/uploads/testcase/直播测试_v1.0.zip',2048000,'直播测试用例集',1,NULL,NULL,'2025-08-14 15:55:41','2025-08-14 15:55:41',0),(3,'游戏测试','v0.5','/uploads/testcase/游戏测试_v0.5.zip',1536000,'游戏测试用例集',1,NULL,NULL,'2025-08-14 15:55:41','2025-08-14 15:55:41',0);
/*!40000 ALTER TABLE `test_case_set` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ue`
--

DROP TABLE IF EXISTS `ue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ue` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `ue_id` varchar(100) NOT NULL COMMENT 'UE ID',
  `name` varchar(100) NOT NULL COMMENT 'UE名称',
  `purpose` varchar(200) NOT NULL COMMENT 'UE用途',
  `network_type_id` bigint NOT NULL COMMENT '网络类型ID',
  `description` text COMMENT '描述',
  `status` tinyint DEFAULT '1' COMMENT '状态：0-不可用，1-可用',
  `create_by` varchar(50) DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(50) DEFAULT NULL COMMENT '修改人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `deleted` tinyint DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ue_id` (`ue_id`),
  KEY `idx_network_type_id` (`network_type_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='UE表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ue`
--

LOCK TABLES `ue` WRITE;
/*!40000 ALTER TABLE `ue` DISABLE KEYS */;
INSERT INTO `ue` VALUES (1,'UE001','UE-Android-01','短视频测试',1,'Android UE设备01',1,NULL,NULL,'2025-08-14 15:55:41','2025-08-14 15:55:41',0),(2,'UE002','UE-Android-02','直播测试',2,'Android UE设备02',1,NULL,NULL,'2025-08-14 15:55:41','2025-08-14 15:55:41',0),(3,'UE003','UE-iOS-01','游戏测试',1,'iOS UE设备01',1,NULL,NULL,'2025-08-14 15:55:41','2025-08-14 15:55:41',0),(4,'UE004','UE-iOS-02','社交测试',3,'iOS UE设备02',1,NULL,NULL,'2025-08-14 15:55:41','2025-08-14 15:55:41',0);
/*!40000 ALTER TABLE `ue` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-08-14 17:59:52
