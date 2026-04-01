#!/bin/bash

# LogicNetwork表与NetworkType表合并脚本
# 执行前请确保已备份数据库！

echo "开始执行LogicNetwork表与NetworkType表合并..."

# 检查数据库连接
echo "检查数据库连接..."
mysql -u root -p -e "USE data_collect; SELECT 'Database connection successful' as status;" || {
    echo "数据库连接失败，请检查数据库配置"
    exit 1
}

# 1. 备份数据
echo "步骤1：备份原始数据..."
mysql -u root -p data_collect << EOF
-- 备份原始数据
CREATE TABLE IF NOT EXISTS \`logic_network_backup\` AS SELECT * FROM \`logic_network\`;
CREATE TABLE IF NOT EXISTS \`logic_environment_network_backup\` AS SELECT * FROM \`logic_environment_network\`;
SELECT 'Data backup completed' as status;
EOF

# 2. 执行数据迁移
echo "步骤2：执行数据迁移..."
mysql -u root -p data_collect < src/main/resources/db/merge_logic_network_to_network_type.sql

# 3. 更新字段注释
echo "步骤3：更新字段注释..."
mysql -u root -p data_collect < src/main/resources/db/update_logic_environment_network_comment.sql

# 4. 验证合并结果
echo "步骤4：验证合并结果..."
mysql -u root -p data_collect << EOF
-- 验证数据迁移结果
SELECT '=== 数据迁移验证 ===' as info;
SELECT 'network_type表数据量' as info, COUNT(*) as count FROM \`network_type\` WHERE deleted = 0;
SELECT 'logic_environment_network表关联数据量' as info, COUNT(*) as count FROM \`logic_environment_network\` WHERE deleted = 0;

-- 检查是否有孤立的关联数据
SELECT '孤立的关联数据' as info, COUNT(*) as count 
FROM \`logic_environment_network\` len
LEFT JOIN \`network_type\` nt ON len.logic_network_id = nt.id AND nt.deleted = 0
WHERE len.deleted = 0 AND nt.id IS NULL;

-- 显示迁移后的数据示例
SELECT '=== 迁移后的网络类型数据示例 ===' as info;
SELECT id, name, description, status FROM \`network_type\` WHERE deleted = 0 ORDER BY id LIMIT 10;
EOF

echo "合并完成！"
echo "请验证以下内容："
echo "1. 检查network_type表是否包含所有原logic_network的数据"
echo "2. 检查logic_environment_network表的关联是否正确"
echo "3. 检查前端功能是否正常"
echo "4. 确认无误后可以执行: mysql -u root -p -e 'USE data_collect; DROP TABLE logic_network;'"
