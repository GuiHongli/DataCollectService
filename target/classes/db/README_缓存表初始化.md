# 仪表盘缓存表初始化说明

## 数据库表创建

请执行以下SQL脚本创建缓存表：

```sql
-- 执行文件: create_dashboard_cache_table.sql
```

或者手动执行：

```sql
-- 仪表盘统计数据缓存表
CREATE TABLE IF NOT EXISTS `dashboard_cache` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `cache_key` varchar(100) NOT NULL COMMENT '缓存键（OVERALL_STATS或regionId）',
  `cache_type` varchar(20) NOT NULL COMMENT '缓存类型（OVERALL-总体统计，REGION-地域统计）',
  `cache_value` text NOT NULL COMMENT '缓存值，JSON格式存储统计数据',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cache_key_type` (`cache_key`, `cache_type`),
  KEY `idx_cache_type` (`cache_type`),
  KEY `idx_update_time` (`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仪表盘统计数据缓存表';
```

## 缓存机制说明

### 1. 总体统计缓存
- **缓存键**: `OVERALL_STATS`
- **缓存类型**: `OVERALL`
- **内容**: 地域数量、执行机数量、UE数量、任务数量

### 2. 地域统计缓存
- **缓存键**: `{regionId}` (地域ID的字符串形式)
- **缓存类型**: `REGION`
- **内容**: APP数量、采集次数、执行机数量、APP列表

### 3. 更新机制
- **系统启动**: 立即执行一次统计并更新缓存
- **定时任务**: 每30分钟自动更新一次（cron: `0 */30 * * * ?`）
- **更新范围**: 更新所有地域的统计数据

### 4. 数据存储格式
所有统计数据以JSON格式存储在 `cache_value` 字段中，例如：

```json
{
  "regionCount": 10,
  "executorCount": 50,
  "ueCount": 100,
  "taskCount": 200
}
```

地域统计示例：
```json
{
  "regionName": "北京",
  "level": 4,
  "executorCount": 5,
  "appCount": 3,
  "collectCount": 150,
  "appList": [
    {"appName": "App1", "collectCount": 80},
    {"appName": "App2", "collectCount": 50},
    {"appName": "App3", "collectCount": 20}
  ]
}
```

## 优势

1. **持久化**: 缓存数据存储在数据库中，系统重启后仍然保留
2. **性能**: 页面访问时直接从数据库读取，无需实时查询
3. **可扩展**: 可以方便地查看历史缓存数据
4. **可靠性**: 数据库事务保证数据一致性

































