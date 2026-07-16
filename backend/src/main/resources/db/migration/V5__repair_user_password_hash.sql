SET @password_hash_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'sys_user'
      AND column_name = 'password_hash'
);

SET @password_hash_ddl = IF(
    @password_hash_exists = 0,
    'ALTER TABLE sys_user ADD COLUMN password_hash VARCHAR(100) NULL COMMENT ''BCrypt密码摘要'' AFTER username',
    'ALTER TABLE sys_user MODIFY COLUMN password_hash VARCHAR(100) NULL COMMENT ''BCrypt密码摘要'''
);

PREPARE password_hash_stmt FROM @password_hash_ddl;
EXECUTE password_hash_stmt;
DEALLOCATE PREPARE password_hash_stmt;

UPDATE sys_user
SET password_hash = '$2a$10$5mk8apW7ZxEB5vGecJbKiOZxoGTw1Tq98oA4wQQgrJydsSmPYi7oy'
WHERE password_hash IS NULL OR TRIM(password_hash) = '';

ALTER TABLE sys_user
    MODIFY COLUMN password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt密码摘要';