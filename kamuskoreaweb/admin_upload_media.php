<?php
/**
 * Admin Media Upload API
 * Upload gambar/audio/video untuk soal quiz dan ujian
 */

require_once __DIR__ . '/admin_config.php';

// Check if admin is logged in
if (!isAdminLoggedIn()) {
    http_response_code(401);
    echo json_encode(['success' => false, 'message' => 'Unauthorized']);
    exit();
}

header('Content-Type: application/json');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['success' => false, 'message' => 'Method not allowed']);
    exit();
}

if (!isset($_FILES['media_file'])) {
    http_response_code(400);
    echo json_encode(['success' => false, 'message' => 'No file uploaded']);
    exit();
}

$file = $_FILES['media_file'];

// Check for upload errors
if ($file['error'] !== UPLOAD_ERR_OK) {
    $errorMessages = [
        UPLOAD_ERR_INI_SIZE => 'File terlalu besar (melebihi upload_max_filesize)',
        UPLOAD_ERR_FORM_SIZE => 'File terlalu besar (melebihi MAX_FILE_SIZE)',
        UPLOAD_ERR_PARTIAL => 'File hanya terupload sebagian',
        UPLOAD_ERR_NO_FILE => 'Tidak ada file yang diupload',
        UPLOAD_ERR_NO_TMP_DIR => 'Missing temporary folder',
        UPLOAD_ERR_CANT_WRITE => 'Gagal menulis file ke disk',
        UPLOAD_ERR_EXTENSION => 'Upload dibatalkan oleh extension'
    ];
    $message = $errorMessages[$file['error']] ?? 'Unknown upload error';
    http_response_code(400);
    echo json_encode(['success' => false, 'message' => $message]);
    exit();
}

// Validate file type
$allowedTypes = [
    // Images
    'image/jpeg' => 'jpg',
    'image/png' => 'png',
    'image/gif' => 'gif',
    'image/webp' => 'webp',
    // Audio
    'audio/mpeg' => 'mp3',
    'audio/mp3' => 'mp3',
    'audio/wav' => 'wav',
    'audio/ogg' => 'ogg',
    'audio/webm' => 'webm',
    // Video
    'video/mp4' => 'mp4',
    'video/webm' => 'webm',
    'video/ogg' => 'ogv'
];

$finfo = new finfo(FILEINFO_MIME_TYPE);
$mimeType = $finfo->file($file['tmp_name']);

if (!isset($allowedTypes[$mimeType])) {
    http_response_code(400);
    echo json_encode([
        'success' => false,
        'message' => 'Tipe file tidak didukung. Gunakan: JPG, PNG, GIF, WEBP, MP3, WAV, OGG, MP4'
    ]);
    exit();
}

// Check file size (max 50MB)
$maxSize = 50 * 1024 * 1024; // 50MB
if ($file['size'] > $maxSize) {
    http_response_code(400);
    echo json_encode(['success' => false, 'message' => 'File terlalu besar. Maksimal 50MB']);
    exit();
}

// Determine media type
$mediaType = 'unknown';
if (strpos($mimeType, 'image/') === 0) {
    $mediaType = 'image';
} elseif (strpos($mimeType, 'audio/') === 0) {
    $mediaType = 'audio';
} elseif (strpos($mimeType, 'video/') === 0) {
    $mediaType = 'video';
}

// Create upload directory
$uploadDir = __DIR__ . '/uploads/questions/';
if (!is_dir($uploadDir)) {
    if (!mkdir($uploadDir, 0755, true)) {
        http_response_code(500);
        echo json_encode(['success' => false, 'message' => 'Gagal membuat direktori upload']);
        exit();
    }
}

// Generate unique filename
$extension = $allowedTypes[$mimeType];
$filename = $mediaType . '_' . time() . '_' . bin2hex(random_bytes(8)) . '.' . $extension;
$uploadPath = $uploadDir . $filename;

// Move uploaded file
if (!move_uploaded_file($file['tmp_name'], $uploadPath)) {
    http_response_code(500);
    echo json_encode(['success' => false, 'message' => 'Gagal menyimpan file']);
    exit();
}

// Generate public URL
// Note: Adjust this URL based on your hosting configuration
$baseUrl = (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? 'https' : 'http')
           . '://' . $_SERVER['HTTP_HOST'];
$scriptPath = dirname($_SERVER['SCRIPT_NAME']);
$publicUrl = $baseUrl . $scriptPath . '/uploads/questions/' . $filename;

// Return success response
echo json_encode([
    'success' => true,
    'message' => 'File berhasil diupload',
    'url' => $publicUrl,
    'filename' => $filename,
    'type' => $mediaType,
    'size' => $file['size'],
    'mime_type' => $mimeType
]);
?>
