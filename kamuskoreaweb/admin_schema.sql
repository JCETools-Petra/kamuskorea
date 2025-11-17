-- =============================================
-- SQL SCHEMA UNTUK ADMIN PANEL KAMUS KOREA
-- =============================================
-- Jalankan SQL ini di database apsx2353_webtech_api
-- =============================================

-- Tabel untuk admin users
CREATE TABLE IF NOT EXISTS `admin_users` (
    `id` INT(11) AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL COMMENT 'Hashed password',
    `email` VARCHAR(100) NULL,
    `full_name` VARCHAR(100) NULL,
    `is_active` TINYINT(1) DEFAULT 1,
    `last_login` DATETIME NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabel untuk tracking login attempts (Anti Brute Force)
CREATE TABLE IF NOT EXISTS `admin_login_attempts` (
    `id` INT(11) AUTO_INCREMENT PRIMARY KEY,
    `ip_address` VARCHAR(45) NOT NULL,
    `username` VARCHAR(50) NOT NULL,
    `attempt_time` DATETIME NOT NULL,
    INDEX `idx_ip_time` (`ip_address`, `attempt_time`),
    INDEX `idx_attempt_time` (`attempt_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert default admin user
-- Username: admin
-- Password: kamuskorea2024
INSERT INTO `admin_users` (`username`, `password`, `email`, `full_name`) VALUES
('admin', '$2y$12$GGAwA200dDiRMIYEMCWDZeUvMmAwrzeq53ieLSLptBZQdLaRgJHN2', 'admin@kamuskorea.com', 'Administrator');

-- Catatan:
-- Password di atas adalah hash dari 'kamuskorea2024'
-- Anda bisa mengubah password dengan generate hash baru menggunakan:
-- PHP: password_hash('password_baru', PASSWORD_DEFAULT, ['cost' => 12])

-- =============================================
-- TABEL UNTUK ASSESSMENT (JIKA BELUM ADA)
-- =============================================

-- Tabel kategori assessment
CREATE TABLE IF NOT EXISTS `assessment_categories` (
    `id` INT(11) AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `type` ENUM('quiz', 'exam') NOT NULL DEFAULT 'quiz',
    `description` TEXT NULL,
    `order_index` INT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabel assessments (quiz/exam)
CREATE TABLE IF NOT EXISTS `assessments` (
    `id` INT(11) AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(200) NOT NULL,
    `description` TEXT NULL,
    `type` ENUM('quiz', 'exam') NOT NULL DEFAULT 'quiz',
    `category_id` INT(11) NULL,
    `duration_minutes` INT DEFAULT 30,
    `passing_score` INT DEFAULT 70,
    `is_premium` TINYINT(1) DEFAULT 0,
    `order_index` INT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`category_id`) REFERENCES `assessment_categories`(`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabel questions
CREATE TABLE IF NOT EXISTS `questions` (
    `id` INT(11) AUTO_INCREMENT PRIMARY KEY,
    `assessment_id` INT(11) NOT NULL,
    `question_text` TEXT NOT NULL,
    `question_type` ENUM('text', 'image', 'audio', 'video') DEFAULT 'text',
    `media_url` VARCHAR(500) NULL,
    `option_a` VARCHAR(500) NOT NULL,
    `option_b` VARCHAR(500) NOT NULL,
    `option_c` VARCHAR(500) NOT NULL,
    `option_d` VARCHAR(500) NOT NULL,
    `correct_answer` CHAR(1) NOT NULL COMMENT 'A, B, C, atau D',
    `explanation` TEXT NULL,
    `order_index` INT DEFAULT 0,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`assessment_id`) REFERENCES `assessments`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =============================================
-- SAMPLE DATA (OPSIONAL)
-- =============================================

-- Sample kategori
INSERT INTO `assessment_categories` (`name`, `type`, `description`, `order_index`) VALUES
('Kosakata Dasar', 'quiz', 'Quiz untuk kosakata dasar bahasa Korea', 1),
('Tata Bahasa', 'quiz', 'Quiz untuk tata bahasa Korea', 2),
('EPS-TOPIK', 'exam', 'Ujian simulasi EPS-TOPIK', 1);

-- Sample quiz
INSERT INTO `assessments` (`title`, `description`, `type`, `category_id`, `duration_minutes`, `passing_score`, `is_premium`, `order_index`) VALUES
('Quiz Angka Korea 1-10', 'Latihan menghafal angka Korea 1-10', 'quiz', 1, 10, 70, 0, 1);

-- Sample question
INSERT INTO `questions` (`assessment_id`, `question_text`, `question_type`, `option_a`, `option_b`, `option_c`, `option_d`, `correct_answer`, `explanation`, `order_index`) VALUES
(1, 'Apa bahasa Korea untuk angka "1"?', 'text', '하나 (hana)', '둘 (dul)', '셋 (set)', '넷 (net)', 'A', '하나 (hana) adalah angka 1 dalam bahasa Korea murni', 1),
(1, 'Apa bahasa Korea untuk angka "5"?', 'text', '넷 (net)', '다섯 (daseot)', '여섯 (yeoseot)', '일곱 (ilgop)', 'B', '다섯 (daseot) adalah angka 5 dalam bahasa Korea murni', 2);
