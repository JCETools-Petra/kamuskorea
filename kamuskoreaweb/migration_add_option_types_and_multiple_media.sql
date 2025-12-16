-- ========================================
-- Migration: Add Option Types and Multiple Media Support
-- ========================================
-- Date: 2025-12-06
-- Purpose:
--   1. Allow options (answers) to be text, image, or audio
--   2. Allow questions to have multiple media files (text + image + audio)
-- ========================================
-- Note: Vocabulary tables removed - Quiz Hafalan now runs offline in mobile app
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
ADD COLUMN media_url_3 TEXT NULL AFTER media_url_2;

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

SELECT 'Migration completed successfully!' AS status;
