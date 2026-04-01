# LogicNetwork表与NetworkType表合并指南

## 概述

本文档说明如何将`logic_network`表和`network_type`表合并，以`network_type`表作为统一的数据源，实现数据统一管理。

## 合并原因

1. **数据重复**：`logic_network`表和`network_type`表存储相同类型的网络数据
2. **维护复杂**：两套表结构增加了维护复杂度
3. **数据一致性**：统一使用`network_type`表可以确保数据一致性
4. **功能统一**：逻辑环境管理和网络类型管理使用同一套数据

## 合并方案

### 1. 数据迁移策略

- **主表**：使用`network_type`表作为主表
- **数据迁移**：将`logic_network`表的数据迁移到`network_type`表
- **关联更新**：更新`logic_environment_network`表的外键引用
- **表清理**：删除`logic_network`表

### 2. 字段映射关系

| logic_network表 | network_type表 | 说明 |
|----------------|---------------|------|
| id | id | 主键ID |
| name | name | 网络名称 |
| description | description | 描述信息 |
| create_time | create_time | 创建时间 |
| update_time | update_time | 更新时间 |
| deleted | deleted | 逻辑删除标记 |
| - | status | 状态字段（新增） |
| - | create_by | 创建人（新增） |
| - | update_by | 更新人（新增） |

### 3. 代码更新策略

- **实体类**：保留`LogicNetwork`实体类，但实际映射到`network_type`表
- **服务层**：`LogicNetworkService`实际使用`NetworkTypeService`
- **控制器**：`LogicNetworkController`使用`NetworkTypeService`
- **关联表**：`logic_environment_network`表字段名保持不变，但实际引用`network_type`表

## 执行步骤

### 步骤1：数据备份

```sql
-- 备份原始数据
CREATE TABLE `logic_network_backup` AS SELECT * FROM `logic_network`;
CREATE TABLE `logic_environment_network_backup` AS SELECT * FROM `logic_environment_network`;
```

### 步骤2：数据迁移

执行 `merge_logic_network_to_network_type.sql` 脚本：

```bash
mysql -u username -p data_collect < src/main/resources/db/merge_logic_network_to_network_type.sql
```

### 步骤3：更新字段注释

执行 `update_logic_environment_network_comment.sql` 脚本：

```bash
mysql -u username -p data_collect < src/main/resources/db/update_logic_environment_network_comment.sql
```

### 步骤4：代码更新

已完成的代码更新：

1. **CollectTaskController**：
   - 将`LogicNetwork`替换为`NetworkType`
   - 将`LogicNetworkService`替换为`NetworkTypeService`

2. **LogicEnvironmentNetwork实体**：
   - 更新字段注释，说明现在引用`network_type`表

3. **LogicNetworkController**：
   - 已使用`NetworkTypeService`和`NetworkType`实体

### 步骤5：验证合并结果

```sql
-- 验证数据迁移
SELECT 'network_type表数据量' as info, COUNT(*) as count FROM `network_type` WHERE deleted = 0;
SELECT 'logic_environment_network表关联数据量' as info, COUNT(*) as count FROM `logic_environment_network` WHERE deleted = 0;

-- 检查是否有孤立的关联数据
SELECT '孤立的关联数据' as info, COUNT(*) as count 
FROM `logic_environment_network` len
LEFT JOIN `network_type` nt ON len.logic_network_id = nt.id AND nt.deleted = 0
WHERE len.deleted = 0 AND nt.id IS NULL;
```

### 步骤6：清理旧表（谨慎操作）

```sql
-- 确认数据迁移成功后，删除logic_network表
DROP TABLE `logic_network`;
```

## 影响分析

### 1. 数据库层面

- **表结构**：删除`logic_network`表
- **关联关系**：`logic_environment_network`表继续使用，但引用`network_type`表
- **数据完整性**：通过外键约束保证数据完整性

### 2. 应用层面

- **API接口**：`/logic-network/*`接口继续可用，但实际操作`network_type`表
- **前端兼容**：前端无需修改，接口路径保持不变
- **功能统一**：逻辑环境管理和网络类型管理使用同一套数据

### 3. 性能影响

- **查询性能**：减少表连接，提升查询性能
- **维护成本**：降低数据维护复杂度
- **数据一致性**：统一数据源，避免数据不一致

## 回滚方案

如果合并过程中出现问题，可以按以下步骤回滚：

1. **恢复数据**：
```sql
-- 从备份表恢复数据
INSERT INTO `logic_network` SELECT * FROM `logic_network_backup`;
INSERT INTO `logic_environment_network` SELECT * FROM `logic_environment_network_backup`;
```

2. **恢复代码**：回滚相关的代码更改

3. **验证功能**：确保所有功能正常工作

## 注意事项

1. **数据备份**：执行前务必备份数据库
2. **测试验证**：在测试环境先验证合并过程
3. **分步执行**：建议分步骤执行，每步验证后再继续
4. **监控日志**：执行过程中监控应用日志，确保无错误
5. **功能测试**：合并后全面测试相关功能

## 验证清单

- [ ] 数据迁移完成，无数据丢失
- [ ] 关联关系正确更新
- [ ] API接口正常工作
- [ ] 前端功能正常
- [ ] 采集任务创建功能正常
- [ ] 逻辑环境管理功能正常
- [ ] 网络类型管理功能正常
- [ ] 数据库性能正常
- [ ] 应用日志无错误

## 总结

通过本次合并，实现了：

1. **数据统一**：`logic_network`和`network_type`表合并为统一的`network_type`表
2. **功能统一**：逻辑环境管理和网络类型管理使用同一套数据
3. **维护简化**：减少数据表数量，降低维护复杂度
4. **性能提升**：减少表连接，提升查询性能
5. **兼容性保持**：API接口路径保持不变，前端无需修改

合并完成后，系统将使用`network_type`表作为网络类型的唯一数据源，实现数据的完全统一管理。
