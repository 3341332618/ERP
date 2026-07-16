UPDATE sys_user
SET username = CONCAT('removed_platform_admin_', id),
    name = '已移除平台管理员',
    status = 'DISABLED',
    updated_at = CURRENT_TIMESTAMP(6)
WHERE username = 'admin'
  AND role_code = 'ADMIN';
