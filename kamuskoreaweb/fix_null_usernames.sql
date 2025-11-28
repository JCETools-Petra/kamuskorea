-- ========================================
-- FIX NULL USERNAMES IN LEADERBOARD
-- ========================================
-- This query updates user_gamification table
-- to pull usernames from users table
--
-- Run this ONCE on your database to fix existing data
-- ========================================

UPDATE user_gamification ug
INNER JOIN users u ON ug.user_id = u.firebase_uid
SET ug.username = u.name
WHERE ug.username IS NULL
   OR ug.username = ''
   OR ug.username = 'Anonymous';

-- Verify the fix
SELECT
    ug.user_id,
    ug.username,
    u.name as name_from_users,
    ug.total_xp,
    ug.current_level
FROM user_gamification ug
LEFT JOIN users u ON ug.user_id = u.firebase_uid
ORDER BY ug.total_xp DESC
LIMIT 20;
