-- 删除执行机表IP地址的唯一约束
-- 解决IP地址重复插入的问题

-- 删除唯一约束
ALTER TABLE executor DROP INDEX uk_ip_address;

-- 验证修改结果
-- 现在可以插入相同IP地址的执行机记录
