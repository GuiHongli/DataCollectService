#!/bin/bash

# 数据库初始化脚本
echo "正在初始化数据库..."

# 尝试创建数据库（无密码）
mysql -u root -e "CREATE DATABASE IF NOT EXISTS data_collect DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>/dev/null

if [ $? -eq 0 ]; then
    echo "数据库创建成功！"
    echo "正在执行初始化SQL脚本..."
    mysql -u root data_collect < src/main/resources/db/init.sql
    echo "数据库初始化完成！"
else
    echo "数据库创建失败，请检查MySQL配置"
    echo "请尝试以下方法之一："
    echo "1. 确保MySQL服务正在运行"
    echo "2. 检查MySQL root用户密码"
    echo "3. 手动创建数据库："
    echo "   mysql -u root -p"
    echo "   CREATE DATABASE data_collect DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    echo "   exit"
    echo "   mysql -u root -p data_collect < src/main/resources/db/init.sql"
fi
