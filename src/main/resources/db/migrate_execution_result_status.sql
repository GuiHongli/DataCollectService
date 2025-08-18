-- 用例执行结果状态标准化迁移脚本
-- 将多种状态统一为：SUCCESS、FAILED、BLOCKED

USE data_collect;

-- 备份现有数据（可选）
-- CREATE TABLE test_case_execution_instance_backup AS SELECT * FROM test_case_execution_instance;

-- 1. 将TIMEOUT状态改为FAILED
UPDATE test_case_execution_instance 
SET result = 'FAILED' 
WHERE result = 'TIMEOUT';

-- 2. 将PARTIAL_SUCCESS状态改为FAILED
UPDATE test_case_execution_instance 
SET result = 'FAILED' 
WHERE result = 'PARTIAL_SUCCESS';

-- 3. 将PARTIAL_FAILURE状态改为FAILED
UPDATE test_case_execution_instance 
SET result = 'FAILED' 
WHERE result = 'PARTIAL_FAILURE';

-- 4. 将ERROR状态改为FAILED
UPDATE test_case_execution_instance 
SET result = 'FAILED' 
WHERE result = 'ERROR';

-- 5. 检查迁移结果
SELECT 
    result,
    COUNT(*) as count
FROM test_case_execution_instance 
GROUP BY result
ORDER BY result;

-- 6. 验证数据完整性
SELECT 
    'Total records' as check_type,
    COUNT(*) as count
FROM test_case_execution_instance
UNION ALL
SELECT 
    'Records with valid result' as check_type,
    COUNT(*) as count
FROM test_case_execution_instance 
WHERE result IN ('SUCCESS', 'FAILED', 'BLOCKED')
UNION ALL
SELECT 
    'Records with NULL result' as check_type,
    COUNT(*) as count
FROM test_case_execution_instance 
WHERE result IS NULL;

-- 7. 显示迁移后的状态分布
SELECT 
    'Status distribution after migration' as info,
    '' as detail
UNION ALL
SELECT 
    result,
    CONCAT(COUNT(*), ' records') as detail
FROM test_case_execution_instance 
WHERE result IS NOT NULL
GROUP BY result
ORDER BY result;
