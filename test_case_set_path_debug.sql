-- 调试用例集路径问题
USE data_collect;

-- 查看任务ID为39的详细信息
SELECT 
    ct.id as task_id,
    ct.name as task_name,
    ct.test_case_set_id,
    tcs.name as test_case_set_name,
    tcs.gohttpserver_url,
    tcs.file_path
FROM collect_task ct
LEFT JOIN test_case_set tcs ON ct.test_case_set_id = tcs.id
WHERE ct.id = 39;

-- 查看用例执行例次信息
SELECT 
    tcei.id,
    tcei.collect_task_id,
    tcei.test_case_id,
    tcei.round,
    tcei.status,
    tcei.result,
    tcei.execution_task_id,
    tc.name as test_case_name,
    tc.number as test_case_number
FROM test_case_execution_instance tcei
LEFT JOIN test_case tc ON tcei.test_case_id = tc.id
WHERE tcei.collect_task_id = 39
ORDER BY tcei.test_case_id, tcei.round;
