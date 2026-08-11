USE employment_db;

-- ============================================
-- 删除所有院级老师数据（仅删除 dept_id 为 49-64 的用户，保留管理员等）
-- ============================================

-- Step 1: 先查看将删除的数据
SELECT id, username, real_name, dept_id, remark FROM sys_user
WHERE dept_id BETWEEN 49 AND 64
ORDER BY dept_id;

-- Step 2: 解绑角色（删除 sys_user_role 中的关联记录）
DELETE FROM sys_user_role
WHERE user_id IN (
    SELECT id FROM sys_user WHERE dept_id BETWEEN 49 AND 64
);

-- Step 3: 删除用户记录
DELETE FROM sys_user
WHERE dept_id BETWEEN 49 AND 64;

-- Step 4: 确认删除结果
SELECT 'sys_user 剩余用户数' AS info, COUNT(*) AS count FROM sys_user;
SELECT 'sys_user_role 剩余记录数' AS info, COUNT(*) AS count FROM sys_user_role;
