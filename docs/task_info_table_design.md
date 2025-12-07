# TaskInfo 数据库表设计文档

## 表名
`task_info` - 端侧任务信息表

## 表说明
该表用于存储从端侧 FTP 服务器上传的压缩包中解析出的 taskinfo.json 文件信息。这些信息包含了任务执行的基本信息和数据报告。

## 表结构

### 字段说明

| 字段名 | 类型 | 长度 | 是否为空 | 默认值 | 说明 |
|--------|------|------|----------|--------|------|
| id | bigint | 20 | NOT NULL | AUTO_INCREMENT | 主键ID |
| task_id | varchar | 255 | NULL | NULL | 任务ID |
| nation | varchar | 100 | NULL | NULL | 国家信息 |
| operator | varchar | 100 | NULL | NULL | 运营商信息 |
| prb | varchar | 50 | NULL | NULL | PRB（Physical Resource Block） |
| rsrp | varchar | 50 | NULL | NULL | RSRP（Reference Signal Received Power） |
| service | varchar | 100 | NULL | NULL | 业务大类 |
| app | varchar | 100 | NULL | NULL | 应用名称 |
| start_time | varchar | 50 | NULL | NULL | 开始时间（格式：年月日时分秒，24小时制） |
| end_time | varchar | 50 | NULL | NULL | 结束时间（格式：年月日时分秒，24小时制） |
| user_category | varchar | 50 | NULL | NULL | 用户类别 |
| device_id | varchar | 100 | NULL | NULL | 设备ID |
| summary | text | - | NULL | NULL | 数据报告（JSON格式） |
| create_time | datetime | - | NOT NULL | CURRENT_TIMESTAMP | 创建时间 |
| update_time | datetime | - | NOT NULL | CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |
| deleted | tinyint | 4 | NULL | 0 | 逻辑删除标记（0-未删除，1-已删除） |

### Summary 字段 JSON 结构

`summary` 字段存储为 JSON 格式的字符串，包含以下字段：

```json
{
  "stunNumber": "卡顿次数",
  "stunRate": "卡顿率",
  "avgUplinkRtt": "平均上行RTT",
  "avgDownlinkRtt": "平均下行RTT",
  "avgUplinkSpeed": "平均上行速率",
  "avgDownlinkSpeed": "平均下行速率",
  "avgUplinkLost": "平均上行丢包率",
  "avgDownlinkLost": "平均下行丢包率",
  "avgLost": "平均丢包率"
}
```

## 索引设计

### 主键索引
- `PRIMARY KEY (id)` - 主键索引

### 普通索引
- `idx_task_id (task_id)` - 任务ID索引，用于快速查询特定任务的信息
- `idx_app (app)` - 应用名称索引，用于按应用统计和查询
- `idx_service (service)` - 业务大类索引，用于按业务类型统计
- `idx_nation (nation)` - 国家索引，用于按国家统计
- `idx_operator (operator)` - 运营商索引，用于按运营商统计
- `idx_device_id (device_id)` - 设备ID索引，用于查询特定设备的数据
- `idx_deleted (deleted)` - 逻辑删除索引，用于过滤已删除数据
- `idx_create_time (create_time)` - 创建时间索引，用于按时间范围查询

## 表特性

1. **字符集**: utf8mb4 - 支持完整的 UTF-8 字符集，包括 emoji 等特殊字符
2. **存储引擎**: InnoDB - 支持事务和外键约束
3. **逻辑删除**: 使用 `deleted` 字段实现软删除，不会物理删除数据
4. **自动时间戳**: `create_time` 和 `update_time` 字段自动维护

## 使用场景

1. **数据存储**: 从端侧 FTP 服务器上传的压缩包中解析 taskinfo.json 后，自动保存到该表
2. **数据查询**: 支持按任务ID、应用、业务类型、国家、运营商等维度查询
3. **统计分析**: 基于 summary 字段中的性能指标进行数据分析和统计
4. **数据追溯**: 通过时间索引可以追溯历史数据

## 关联关系

- 该表为独立表，不直接关联其他业务表
- `task_id` 字段可能与 `collect_task` 表或其他任务表存在逻辑关联（通过业务逻辑关联，非外键约束）

## 数据量预估

- 单条记录大小：约 1-2 KB（主要取决于 summary JSON 的大小）
- 预计数据量：根据任务执行频率，可能达到百万级别
- 建议定期归档历史数据，保持表性能

## 维护建议

1. **定期清理**: 建议定期清理已删除的数据（deleted=1 且超过一定时间）
2. **数据归档**: 对于历史数据，建议归档到历史表或备份
3. **索引维护**: 定期检查索引使用情况，优化查询性能
4. **JSON 解析**: 如需频繁查询 summary 中的字段，可考虑将常用字段提取为独立列

## SQL 脚本位置

数据库创建脚本位于：`src/main/resources/db/create_task_info_table.sql`

## 执行方式

```sql
-- 方式1：直接执行 SQL 脚本
source src/main/resources/db/create_task_info_table.sql;

-- 方式2：在 MySQL 客户端中执行
mysql -u root -p data_collect < src/main/resources/db/create_task_info_table.sql;
```

