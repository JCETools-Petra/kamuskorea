<?php
/**
 * Reset Password API Endpoint
 * Path: /api.php/auth/reset-password
 * Method: POST
 */

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization, X-User-ID');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

require_once 'config.php'; // Database config
require_once 'vendor/autoload.php'; // For Firebase Admin SDK

use Kreait\Firebase\Factory;
use Kreait\Firebase\Auth;

// Get JSON input
$json = file_get_contents('php://input');
$data = json_decode($json, true);

if (!isset($data['token']) || !isset($data['newPassword'])) {
    http_response_code(400);
    echo json_encode([
        'success' => false,
        'message' => 'Token dan password baru harus diisi'
    ]);
    exit;
}

$token = $data['token'];
$newPassword = $data['newPassword'];

// Validate password
if (strlen($newPassword) < 6) {
    http_response_code(400);
    echo json_encode([
        'success' => false,
        'message' => 'Password minimal 6 karakter'
    ]);
    exit;
}

try {
    // Verify token
    $stmt = $pdo->prepare("
        SELECT firebase_uid, email, expires_at, used 
        FROM password_resets 
        WHERE reset_token = ? 
        LIMIT 1
    ");
    $stmt->execute([$token]);
    $reset = $stmt->fetch(PDO::FETCH_ASSOC);
    
    if (!$reset) {
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => 'Token tidak valid'
        ]);
        exit;
    }
    
    // Check if token is expired
    $now = new DateTime();
    $expires = new DateTime($reset['expires_at']);
    
    if ($now > $expires) {
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => 'Token sudah kadaluarsa'
        ]);
        exit;
    }
    
    // Check if token already used
    if ($reset['used']) {
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => 'Token sudah digunakan'
        ]);
        exit;
    }
    
    $firebase_uid = $reset['firebase_uid'];
    $email = $reset['email'];
    
    // Initialize Firebase Admin SDK
    $factory = (new Factory)->withServiceAccount('path/to/your-firebase-adminsdk.json');
    $auth = $factory->createAuth();
    
    try {
        // Update password in Firebase Auth
        $auth->updateUser($firebase_uid, [
            'password' => $newPassword
        ]);
        
        // Mark token as used
        $stmt = $pdo->prepare("UPDATE password_resets SET used = TRUE WHERE reset_token = ?");
        $stmt->execute([$token]);
        
        // Optional: Send confirmation email
        // sendConfirmationEmail($email);
        
        http_response_code(200);
        echo json_encode([
            'success' => true,
            'message' => 'Password berhasil direset. Silakan login dengan password baru Anda.'
        ]);
        
    } catch (Exception $e) {
        error_log("Firebase Auth error: " . $e->getMessage());
        http_response_code(500);
        echo json_encode([
            'success' => false,
            'message' => 'Gagal mengupdate password. Silakan coba lagi.'
        ]);
    }
    
} catch (PDOException $e) {
    error_log("Database error: " . $e->getMessage());
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Terjadi kesalahan. Silakan coba lagi.'
    ]);
}
?>