# 数据库迁移说明

## 迁移内容

本次迁移移除了以下字段：
1. `region` 表的 `code` 字段
2. `network_type` 表的 `code` 字段

## 迁移执行记录

### 执行时间
2025-08-14 18:01

### 执行步骤

1. **备份数据库**
```bash
mysqldump -u root -pghl12306Happy@ data_collect > data_collect_backup_20250814_175949.sql
```

2. **检查数据库结构**
```sql
-- 检查地域表结构（发现已经不存在code字段）
DESCRIBE region;

-- 检查网络类型表结构（发现还存在code字段）
DESCRIBE network_type;
```

3. **执行迁移脚本**
```bash
mysql -u root -pghl12306Happy@ data_collect < src/main/resources/db/migration.sql
```

4. **重新插入测试数据**
```sql
DELETE FROM network_type;
INSERT INTO network_type (name, description, status) VALUES 
('正常网络', '正常网络环境', 1),
('弱网', '弱网环境', 1),
('拥塞', '网络拥塞环境', 1),
('弱网+拥塞', '弱网+拥塞环境', 1);
```

### 迁移结果

✅ **地域表**：code字段已成功移除
✅ **网络类型表**：code字段已成功移除，测试数据已重新插入

### 验证结果

```sql
-- 地域表结构验证
DESCRIBE region;
-- 结果：不包含code字段

-- 网络类型表结构验证
DESCRIBE network_type;
-- 结果：不包含code字段

-- 数据验证
SELECT * FROM region LIMIT 5;
SELECT * FROM network_type LIMIT 5;
-- 结果：数据正常，不包含code字段
```

## 注意事项

1. **备份重要**：执行迁移前请务必备份数据库
2. **停机时间**：迁移过程中建议停止应用服务
3. **数据验证**：迁移完成后请验证数据完整性
4. **应用重启**：迁移完成后需要重启应用服务

## 回滚方案

如果需要回滚，可以执行以下SQL：

```sql
-- 恢复地域表code字段
ALTER TABLE `region` ADD COLUMN `code` varchar(50) NOT NULL COMMENT '地域代码' AFTER `name`;
ALTER TABLE `region` ADD UNIQUE KEY `uk_code` (`code`);

-- 恢复网络类型表code字段
ALTER TABLE `network_type` ADD COLUMN `code` varchar(50) NOT NULL COMMENT '网络类型代码' AFTER `name`;
ALTER TABLE `network_type` ADD UNIQUE KEY `uk_code` (`code`);

-- 重新插入code数据
UPDATE `region` SET `code` = 'China' WHERE `name` = '中国';
UPDATE `region` SET `code` = 'LN' WHERE `name` = '辽宁省';
UPDATE `region` SET `code` = 'SY' WHERE `name` = '沈阳市';
UPDATE `region` SET `code` = 'BJ' WHERE `name` = '北京市';
UPDATE `region` SET `code` = 'SH' WHERE `name` = '上海市';

UPDATE `network_type` SET `code` = 'normal' WHERE `name` = '正常网络';
UPDATE `network_type` SET `code` = 'weak' WHERE `name` = '弱网';
UPDATE `network_type` SET `code` = 'congestion' WHERE `name` = '拥塞';
UPDATE `network_type` SET `code` = 'weak_congestion' WHERE `name` = '弱网+拥塞';
```
