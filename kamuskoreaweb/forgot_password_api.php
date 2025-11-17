<?php
/**
 * Forgot Password API Endpoint
 * Path: /api.php/auth/forgot-password
 * Method: POST
 */

// CORS headers
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

// Handle preflight
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

require_once 'config.php'; // Database connection
require_once 'email_helper.php'; // Email functions (create this next)

// Only allow POST
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode([
        'success' => false,
        'message' => 'Method not allowed'
    ]);
    exit;
}

// Get JSON input
$json = file_get_contents('php://input');
$data = json_decode($json, true);

// Validate input
if (!isset($data['email']) || empty($data['email'])) {
    http_response_code(400);
    echo json_encode([
        'success' => false,
        'message' => 'Email tidak boleh kosong'
    ]);
    exit;
}

$email = filter_var($data['email'], FILTER_SANITIZE_EMAIL);

if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
    http_response_code(400);
    echo json_encode([
        'success' => false,
        'message' => 'Format email tidak valid'
    ]);
    exit;
}

try {
    // Check if user exists
    $stmt = $pdo->prepare("SELECT firebase_uid, email, name, auth_type FROM users WHERE email = ? LIMIT 1");
    $stmt->execute([$email]);
    $user = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if (!$user) {
        // Don't reveal if email exists (security best practice)
        http_response_code(200);
        echo json_encode([
            'success' => true,
            'message' => 'Jika email terdaftar, link reset password akan dikirim ke email Anda'
        ]);
        exit;
    }
    
    // Check auth_type
    if ($user['auth_type'] === 'google') {
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => 'Akun ini menggunakan Google Sign-In. Silakan login dengan Google.',
            'auth_type' => 'google'
        ]);
        exit;
    }
    
    // Generate reset token
    $token = bin2hex(random_bytes(32));
    $expires_at = date('Y-m-d H:i:s', strtotime('+15 minutes'));
    
    // Save token to database
    $stmt = $pdo->prepare("
        INSERT INTO password_reset_tokens 
        (firebase_uid, email, token, expires_at, created_at) 
        VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
        ON DUPLICATE KEY UPDATE 
            token = VALUES(token), 
            expires_at = VALUES(expires_at), 
            created_at = CURRENT_TIMESTAMP
    ");
    $stmt->execute([$user['firebase_uid'], $email, $token, $expires_at]);
    
    // Send email
    $reset_link = "https://webtechsolution.my.id/kamuskorea/reset-password?token=" . $token;
    $email_sent = send_reset_email($email, $user['name'], $reset_link);
    
    if ($email_sent) {
        http_response_code(200);
        echo json_encode([
            'success' => true,
            'message' => 'Link reset password telah dikirim ke email Anda. Periksa inbox atau folder spam.'
        ]);
    } else {
        http_response_code(500);
        echo json_encode([
            'success' => false,
            'message' => 'Gagal mengirim email. Silakan coba lagi nanti.'
        ]);
    }
    
    // Log for debugging
    error_log("Password reset requested for: $email");
    
} catch (PDOException $e) {
    error_log("Database error in forgot password: " . $e->getMessage());
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Terjadi kesalahan server. Silakan coba lagi nanti.'
    ]);
}
?>