-- 删除test_case_set表的name+version唯一索引
ALTER TABLE test_case_set DROP INDEX uk_name_version;

-- 查看表结构确认索引已删除
SHOW INDEX FROM test_case_set;
