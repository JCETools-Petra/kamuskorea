-- ========================================
-- FIX USER GAMIFICATION SCHEMA
-- ========================================
-- Issue: user_id column tidak memiliki UNIQUE constraint
-- Impact: Multiple users dapat memiliki user_id yang sama
-- Fix: Add UNIQUE constraint + cleanup duplicate data
-- ========================================

-- STEP 1: Check for duplicate user_ids (DIAGNOSTIC)
-- Run this first to see if there are duplicates
SELECT
    user_id,
    COUNT(*) as count,
    GROUP_CONCAT(id) as row_ids,
    GROUP_CONCAT(total_xp) as xp_values,
    GROUP_CONCAT(username) as usernames
FROM user_gamification
GROUP BY user_id
HAVING COUNT(*) > 1;

-- STEP 2: Check current table structure
SHOW CREATE TABLE user_gamification;

-- STEP 3: If duplicates exist, keep only the latest record per user
-- WARNING: This will delete older records! Backup first!
-- Uncomment to execute:
/*
DELETE ug1 FROM user_gamification ug1
INNER JOIN user_gamification ug2
WHERE ug1.user_id = ug2.user_id
  AND ug1.id < ug2.id;
*/

-- STEP 4: Add UNIQUE constraint on user_id (if not exists)
-- This will fail if duplicates exist - run STEP 3 first
ALTER TABLE user_gamification
ADD UNIQUE KEY idx_user_id_unique (user_id);

-- STEP 5: Verify the fix
SELECT
    COLUMN_NAME,
    COLUMN_TYPE,
    COLUMN_KEY,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'user_gamification'
ORDER BY ORDINAL_POSITION;

-- STEP 6: List all users in leaderboard
SELECT
    id,
    user_id,
    username,
    total_xp,
    current_level,
    last_xp_sync,
    created_at
FROM user_gamification
ORDER BY total_xp DESC;

-- ========================================
-- NOTES:
-- ========================================
-- 1. Run STEP 1 first to check for duplicates
-- 2. If duplicates found, backup database first!
-- 3. Uncomment STEP 3 to remove duplicates
-- 4. Run STEP 4 to add UNIQUE constraint
-- 5. Run STEP 6 to verify all users are separate
-- ========================================
