# Database Migration Guide

## 📋 Fitur Baru

### 1. Option Types (Text/Image/Audio untuk Jawaban)
Sekarang setiap pilihan jawaban (1,2,3,4) bisa berupa:
- **Text** (HTML formatted)
- **Image** (URL gambar)
- **Audio** (URL audio)

### 2. Multiple Media untuk Soal
Soal sekarang bisa punya hingga 3 media sekaligus:
- Text + Gambar + Audio
- Atau kombinasi lainnya

### 3. Quiz Hafalan Auto-Generate
Sistem quiz baru yang:
- Ambil kata dari vocabulary
- Generate 3 jawaban salah otomatis
- Auto-next setelah 4 detik
- Infinite loop

## 🚀 Cara Menjalankan Migration

### Option 1: Via phpMyAdmin
1. Login ke **phpMyAdmin** (cPanel > phpMyAdmin)
2. Pilih database **apsx2353_webtech_api**
3. Klik tab **SQL**
4. Copy-paste isi file `migration_add_option_types_and_multiple_media.sql`
5. Klik **Go** / **Jalankan**

### Option 2: Via MySQL Command Line
```bash
mysql -u apsx2353_webtech_api -p apsx2353_webtech_api < migration_add_option_types_and_multiple_media.sql
```

### Option 3: Via cPanel Terminal
```bash
cd /home/apsx2353/public_html/webtechsolution.my.id/kamuskorea
mysql -u apsx2353_webtech_api -p apsx2353_webtech_api < migration_add_option_types_and_multiple_media.sql
```

## ✅ Verifikasi Migration Berhasil

Setelah menjalankan migration, cek di phpMyAdmin atau MySQL:

```sql
-- Cek kolom baru di table questions
DESCRIBE questions;

-- Harusnya ada kolom:
-- option_a_type, option_b_type, option_c_type, option_d_type
-- media_url_2, media_url_3
-- media_type_1, media_type_2, media_type_3

-- Cek table vocabulary
DESCRIBE vocabulary;

-- Cek table quiz_hafalan_results
DESCRIBE quiz_hafalan_results;
```

## 📊 Schema Baru

### Table: questions
```sql
- option_a (TEXT) - Isi jawaban 1
- option_a_type (ENUM: 'text', 'image', 'audio') - Tipe jawaban 1
- option_b (TEXT) - Isi jawaban 2
- option_b_type (ENUM: 'text', 'image', 'audio') - Tipe jawaban 2
- option_c (TEXT) - Isi jawaban 3
- option_c_type (ENUM: 'text', 'image', 'audio') - Tipe jawaban 3
- option_d (TEXT) - Isi jawaban 4
- option_d_type (ENUM: 'text', 'image', 'audio') - Tipe jawaban 4
- media_url (TEXT) - Media 1 (existing)
- media_url_2 (TEXT) - Media 2 (new)
- media_url_3 (TEXT) - Media 3 (new)
- media_type_1 (ENUM) - Tipe media 1
- media_type_2 (ENUM) - Tipe media 2
- media_type_3 (ENUM) - Tipe media 3
```

### Table: vocabulary (NEW)
```sql
- id (INT PRIMARY KEY)
- korean (VARCHAR) - Kata dalam bahasa Korea
- indonesian (VARCHAR) - Arti dalam bahasa Indonesia
- romaji (VARCHAR) - Romanisasi
- category (VARCHAR) - Kategori (greeting, food, etc)
- level (ENUM: beginner, intermediate, advanced)
- image_url (TEXT) - Gambar ilustrasi
- audio_url (TEXT) - Audio pronunciaton
- example_sentence (TEXT) - Contoh kalimat
- created_at, updated_at (TIMESTAMP)
```

### Table: quiz_hafalan_results (NEW)
```sql
- id (INT PRIMARY KEY)
- user_id (VARCHAR) - Firebase UID
- vocabulary_id (INT) - ID kata dari vocabulary table
- is_correct (BOOLEAN) - Benar/salah
- selected_answer (VARCHAR) - Jawaban yang dipilih user
- correct_answer (VARCHAR) - Jawaban yang benar
- time_spent (INT) - Waktu dalam detik
- created_at (TIMESTAMP)
```

## ⚠️ Catatan Penting

1. **Backup Database** sebelum menjalankan migration!
2. Migration ini **AMAN** - hanya menambah kolom baru, tidak menghapus data lama
3. Existing data akan tetap berfungsi normal
4. Kolom baru diberi default value `'text'` untuk backward compatibility

## 🔄 Rollback (Jika Diperlukan)

Jika ada masalah dan ingin rollback:

```sql
ALTER TABLE questions
DROP COLUMN option_a_type,
DROP COLUMN option_b_type,
DROP COLUMN option_c_type,
DROP COLUMN option_d_type,
DROP COLUMN media_url_2,
DROP COLUMN media_url_3,
DROP COLUMN media_type_1,
DROP COLUMN media_type_2,
DROP COLUMN media_type_3;

DROP TABLE IF EXISTS quiz_hafalan_results;
-- DROP TABLE IF EXISTS vocabulary; -- Hati-hati jika sudah ada data!
```

## 📞 Support

Jika ada error saat migration, cek:
1. Error message di phpMyAdmin
2. Pastikan user database punya permission CREATE/ALTER
3. Pastikan table `questions` sudah ada
