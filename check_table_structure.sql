-- 查看test_case_set表结构
SHOW CREATE TABLE test_case_set;

-- 查看表的索引
SHOW INDEX FROM test_case_set;

-- 查看所有记录（包括软删除的）
SELECT id, name, version, deleted, create_time FROM test_case_set ORDER BY id;

-- 查看软删除的记录
SELECT id, name, version, deleted, create_time FROM test_case_set WHERE deleted = 1 ORDER BY id;
