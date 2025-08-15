-- 查看所有记录（包括软删除的）
SELECT id, name, version, deleted, create_time FROM test_case_set ORDER BY id;

-- 查看软删除的记录
SELECT id, name, version, deleted, create_time FROM test_case_set WHERE deleted = 1 ORDER BY id;

-- 删除软删除的记录
DELETE FROM test_case_set WHERE deleted = 1;

-- 删除相关的测试用例记录
DELETE FROM test_case WHERE test_case_set_id IN (
    SELECT id FROM test_case_set WHERE deleted = 1
);

-- 查看清理后的记录
SELECT id, name, version, deleted, create_time FROM test_case_set ORDER BY id;
