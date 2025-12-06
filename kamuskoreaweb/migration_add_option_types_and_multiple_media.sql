-- ========================================
-- Migration: Add Option Types and Multiple Media Support
-- ========================================
-- Date: 2025-12-06
-- Purpose:
--   1. Allow options (answers) to be text, image, or audio
--   2. Allow questions to have multiple media files (text + image + audio)
-- ========================================

USE apsx2353_webtech_api;

-- Add option type columns untuk setiap pilihan jawaban
ALTER TABLE questions
ADD COLUMN option_a_type ENUM('text', 'image', 'audio') DEFAULT 'text' AFTER option_a,
ADD COLUMN option_b_type ENUM('text', 'image', 'audio') DEFAULT 'text' AFTER option_b,
ADD COLUMN option_c_type ENUM('text', 'image', 'audio') DEFAULT 'text' AFTER option_c,
ADD COLUMN option_d_type ENUM('text', 'image', 'audio') DEFAULT 'text' AFTER option_d;

-- Add additional media URLs untuk support multiple media per question
ALTER TABLE questions
ADD COLUMN media_url_2 TEXT NULL AFTER media_url,
ADD COLUMN media_url_3 TEXT NULL AFTER media_url_2,
ADD COLUMN media_type_1 ENUM('text', 'image', 'audio', 'video') NULL AFTER media_url,
ADD COLUMN media_type_2 ENUM('text', 'image', 'audio', 'video') NULL AFTER media_url_2,
ADD COLUMN media_type_3 ENUM('text', 'image', 'audio', 'video') NULL AFTER media_url_3;

-- Rename existing media_url to media_url_1 untuk konsistensi (optional)
-- ALTER TABLE questions CHANGE media_url media_url_1 TEXT NULL;

-- Set default values for existing data
UPDATE questions
SET option_a_type = 'text',
    option_b_type = 'text',
    option_c_type = 'text',
    option_d_type = 'text'
WHERE option_a_type IS NULL;

-- Create index for faster queries
CREATE INDEX idx_option_types ON questions(option_a_type, option_b_type, option_c_type, option_d_type);

-- ========================================
-- Vocabulary/Hafalan Table Check
-- ========================================
-- Jika belum ada table untuk vocabulary, buat table baru

CREATE TABLE IF NOT EXISTS vocabulary (
    id INT AUTO_INCREMENT PRIMARY KEY,
    korean VARCHAR(255) NOT NULL,
    indonesian VARCHAR(255) NOT NULL,
    romaji VARCHAR(255) NULL,
    category VARCHAR(100) NULL,
    level ENUM('beginner', 'intermediate', 'advanced') DEFAULT 'beginner',
    image_url TEXT NULL,
    audio_url TEXT NULL,
    example_sentence TEXT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_category (category),
    INDEX idx_level (level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- Quiz Hafalan Results Table
-- ========================================
-- Table untuk menyimpan hasil quiz hafalan

CREATE TABLE IF NOT EXISTS quiz_hafalan_results (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    vocabulary_id INT NOT NULL,
    is_correct BOOLEAN NOT NULL,
    selected_answer VARCHAR(255) NULL,
    correct_answer VARCHAR(255) NOT NULL,
    time_spent INT NULL COMMENT 'Waktu dalam detik',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (vocabulary_id) REFERENCES vocabulary(id) ON DELETE CASCADE,
    INDEX idx_user (user_id),
    INDEX idx_vocabulary (vocabulary_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========================================
-- Sample Data untuk Testing (Optional)
-- ========================================
-- Uncomment jika ingin insert sample data

/*
INSERT INTO vocabulary (korean, indonesian, romaji, category, level) VALUES
('안녕하세요', 'Halo (formal)', 'annyeonghaseyo', 'greeting', 'beginner'),
('감사합니다', 'Terima kasih', 'gamsahamnida', 'greeting', 'beginner'),
('사랑', 'Cinta', 'sarang', 'emotion', 'beginner'),
('학교', 'Sekolah', 'hakgyo', 'place', 'beginner'),
('책', 'Buku', 'chaek', 'object', 'beginner'),
('컴퓨터', 'Komputer', 'keompyuteo', 'object', 'beginner'),
('친구', 'Teman', 'chingu', 'people', 'beginner'),
('가족', 'Keluarga', 'gajok', 'people', 'beginner'),
('음식', 'Makanan', 'eumsik', 'food', 'beginner'),
('물', 'Air', 'mul', 'food', 'beginner');
*/

-- ========================================
-- VERIFICATION
-- ========================================
-- Check if columns were added successfully

SELECT
    COLUMN_NAME,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM
    INFORMATION_SCHEMA.COLUMNS
WHERE
    TABLE_SCHEMA = 'apsx2353_webtech_api'
    AND TABLE_NAME = 'questions'
    AND COLUMN_NAME LIKE '%type%';

-- Show vocabulary table structure
DESCRIBE vocabulary;

SELECT 'Migration completed successfully!' AS status;
