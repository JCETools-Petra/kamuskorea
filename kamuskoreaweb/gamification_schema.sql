-- ====================================================
-- KAMUS KOREA - GAMIFICATION SCHEMA
-- ====================================================
-- Version: 1.0
-- Description: XP, Level, Achievements, dan Leaderboard system
--
-- CARA IMPORT:
-- 1. Buka phpMyAdmin atau MySQL client
-- 2. Pilih database Kamus Korea Anda
-- 3. Import file ini atau copy-paste ke SQL tab
-- ====================================================

-- ====================================================
-- TABLE: user_gamification
-- Menyimpan XP, level, dan achievements user
-- ====================================================
CREATE TABLE IF NOT EXISTS `user_gamification` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `user_id` VARCHAR(255) NOT NULL,
    `username` VARCHAR(100),
    `total_xp` INT DEFAULT 0,
    `current_level` INT DEFAULT 1,
    `achievements_unlocked` TEXT, -- JSON array: ["first_quiz","7_day_streak","100_words"]
    `last_xp_sync` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY `idx_user_id` (`user_id`),
    INDEX `idx_total_xp` (`total_xp` DESC), -- Untuk query leaderboard
    INDEX `idx_level` (`current_level` DESC),
    INDEX `idx_last_sync` (`last_xp_sync`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ====================================================
-- TABLE: xp_history (OPTIONAL - untuk analytics)
-- Menyimpan riwayat perolehan XP
-- Tabel ini opsional, bisa di-skip jika ingin hemat storage
-- ====================================================
CREATE TABLE IF NOT EXISTS `xp_history` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `user_id` VARCHAR(255) NOT NULL,
    `xp_amount` INT NOT NULL,
    `xp_source` VARCHAR(50), -- "quiz_completed", "pdf_opened", "word_favorited", dll
    `metadata` TEXT, -- JSON: {"quiz_id":123,"score":80,"duration":120}
    `earned_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_source` (`xp_source`),
    INDEX `idx_earned_at` (`earned_at`),

    FOREIGN KEY (`user_id`) REFERENCES `user_gamification`(`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ====================================================
-- SAMPLE DATA (untuk testing)
-- ====================================================
-- Uncomment baris di bawah untuk insert sample data

-- INSERT INTO user_gamification (user_id, username, total_xp, current_level, achievements_unlocked)
-- VALUES
--     ('test_uid_1', 'TestUser1', 450, 5, '["first_quiz","7_day_streak","100_words"]'),
--     ('test_uid_2', 'TestUser2', 320, 4, '["first_quiz","50_quizzes"]'),
--     ('test_uid_3', 'TestUser3', 890, 9, '["first_quiz","7_day_streak","30_day_streak","100_words","quiz_master"]');

-- ====================================================
-- UTILITY QUERIES
-- ====================================================

-- Query untuk melihat leaderboard top 100:
-- SELECT
--     user_id, username, total_xp, current_level,
--     JSON_LENGTH(achievements_unlocked) as achievement_count
-- FROM user_gamification
-- ORDER BY total_xp DESC
-- LIMIT 100;

-- Query untuk melihat ranking user tertentu:
-- SET @user_id = 'firebase_uid_here';
-- SELECT
--     ranking,
--     user_id,
--     username,
--     total_xp,
--     current_level
-- FROM (
--     SELECT
--         user_id,
--         username,
--         total_xp,
--         current_level,
--         @rank := @rank + 1 AS ranking
--     FROM user_gamification, (SELECT @rank := 0) r
--     ORDER BY total_xp DESC
-- ) ranked
-- WHERE user_id = @user_id;

-- Query untuk statistik XP per source:
-- SELECT
--     xp_source,
--     COUNT(*) as event_count,
--     SUM(xp_amount) as total_xp_earned,
--     AVG(xp_amount) as avg_xp_per_event
-- FROM xp_history
-- GROUP BY xp_source
-- ORDER BY total_xp_earned DESC;

-- Query untuk tracking XP harian user:
-- SELECT
--     DATE(earned_at) as date,
--     COUNT(*) as xp_events,
--     SUM(xp_amount) as total_xp
-- FROM xp_history
-- WHERE user_id = 'firebase_uid_here'
--   AND earned_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
-- GROUP BY DATE(earned_at)
-- ORDER BY date DESC;
