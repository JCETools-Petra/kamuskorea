-- ========================================
-- Migration: Create Video Hafalan Table
-- ========================================
-- Date: 2026-01-01
-- Purpose:
--   1. Create table for Video Hafalan feature
--   2. Store video URLs and metadata
-- ========================================

USE apsx2353_webtech_api;

-- Create video_hafalan table
CREATE TABLE IF NOT EXISTS video_hafalan (
    id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL COMMENT 'Judul video',
    description TEXT NULL COMMENT 'Deskripsi video (opsional)',
    video_url TEXT NOT NULL COMMENT 'URL video (YouTube, direct URL, dll)',
    thumbnail_url TEXT NULL COMMENT 'URL thumbnail/preview image',
    duration_minutes INT NULL COMMENT 'Durasi video dalam menit (opsional)',
    category VARCHAR(100) NULL COMMENT 'Kategori video (Grammar, Vocabulary, dll)',
    order_index INT DEFAULT 0 COMMENT 'Urutan tampilan',
    is_premium TINYINT(1) DEFAULT 0 COMMENT '0=Free, 1=Premium only',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create indexes for better performance
CREATE INDEX idx_category ON video_hafalan(category);
CREATE INDEX idx_order ON video_hafalan(order_index);
CREATE INDEX idx_premium ON video_hafalan(is_premium);

-- Insert sample data
INSERT INTO video_hafalan (title, description, video_url, thumbnail_url, duration_minutes, category, order_index, is_premium) VALUES
('Pengenalan Hangeul', 'Video pengenalan dasar huruf Korea (Hangeul)', 'https://www.youtube.com/watch?v=sample1', NULL, 15, 'Dasar', 1, 0),
('Angka dalam Bahasa Korea', 'Belajar menghitung angka Korea (sino-Korean dan native)', 'https://www.youtube.com/watch?v=sample2', NULL, 20, 'Vocabulary', 2, 0),
('Grammar: Partikel 은/는', 'Penjelasan penggunaan partikel subjek 은/는', 'https://www.youtube.com/watch?v=sample3', NULL, 25, 'Grammar', 3, 1);

-- ========================================
-- VERIFICATION
-- ========================================
SELECT 'Migration completed successfully!' AS status;
SELECT * FROM video_hafalan;
