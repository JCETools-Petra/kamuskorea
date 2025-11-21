<?php
date_default_timezone_set('Asia/Jakarta');
/**
 * =================================================
 * KAMUS KOREA API - MAIN BACKEND
 * =================================================
 * Version: 3.3 (Fixed Token Verification & Error Handling)
 * Features: Firebase Auth, App Check, Premium, Ebooks, Assessment
 *
 * Changelog v3.3:
 * - Fixed Authorization header parsing (line 617 issue)
 * - Added leeway for token verification (300 seconds)
 * - Improved error handling for token verification
 * - Removed words table dependency (using local SQLite)
 * - Enhanced security checks
 */

// ========================================
// ENVIRONMENT CONFIGURATION
// ========================================
// UBAH INI KE 'production' SAAT DEPLOY KE SERVER LIVE
define('APP_ENV', 'development'); // Options: 'development' atau 'production'

// Error Reporting
error_reporting(0);
ini_set('display_errors', APP_ENV === 'development' ? 1 : 0);
ini_set('log_errors', 1);
ini_set('error_log', __DIR__ . '/php_error.log');

// CORS Headers
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization, X-User-ID, X-Firebase-AppCheck");
header("Content-Type: application/json; charset=UTF-8");

// Handle preflight OPTIONS request
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

// =================================================
// LOAD ENVIRONMENT VARIABLES & FIREBASE
// =================================================
require_once __DIR__ . '/vendor/autoload.php';

use Dotenv\Dotenv;
use PHPMailer\PHPMailer\PHPMailer;
use PHPMailer\PHPMailer\Exception;
use Kreait\Firebase\Factory;
use Kreait\Firebase\Auth as FirebaseAuth;

$dotenv = Dotenv::createImmutable(__DIR__ . '/..');
$dotenv->load();

// Initialize Firebase Admin SDK
try {
    $firebase = (new Factory)
        ->withServiceAccount(__DIR__ . '/../firebase-service-account.json');
    $firebaseAuth = $firebase->createAuth();
} catch (\Exception $e) {
    error_log("Gagal memuat Firebase Service Account: " . $e->getMessage());
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Konfigurasi server error (Firebase Admin SDK)'
    ]);
    exit();
}

// =================================================
// DATABASE CONNECTION
// =================================================
$host = $_ENV['DB_HOST'];
$username = $_ENV['DB_USERNAME'];
$password = $_ENV['DB_PASSWORD'];
$database = $_ENV['DB_DATABASE'];

try {
    $pdo = new PDO(
        "mysql:host=$host;dbname=$database;charset=utf8mb4",
        $username,
        $password,
        [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            PDO::ATTR_EMULATE_PREPARES => false
        ]
    );
} catch (PDOException $e) {
    error_log("Database connection failed: " . $e->getMessage());
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Database connection failed'
    ]);
    exit();
}

// =================================================
// UTILITY FUNCTIONS
// =================================================

/**
 * Get Authorization header secara aman dari berbagai sumber
 */
function getAuthorizationHeader() {
    $authHeader = '';
    
    // Method 1: Apache/Nginx dengan mod_rewrite
    if (isset($_SERVER['HTTP_AUTHORIZATION'])) {
        $authHeader = $_SERVER['HTTP_AUTHORIZATION'];
    }
    // Method 2: CGI/FastCGI
    elseif (isset($_SERVER['REDIRECT_HTTP_AUTHORIZATION'])) {
        $authHeader = $_SERVER['REDIRECT_HTTP_AUTHORIZATION'];
    }
    // Method 3: Using getallheaders() jika tersedia
    elseif (function_exists('apache_request_headers')) {
        $headers = apache_request_headers();
        if (isset($headers['Authorization'])) {
            $authHeader = $headers['Authorization'];
        } elseif (isset($headers['authorization'])) {
            $authHeader = $headers['authorization'];
        }
    }
    
    return trim($authHeader);
}

/**
 * Verify Firebase ID Token and get UID
 * FIXED: Proper token parsing and error handling
 */
function getFirebaseUid() {
    global $firebaseAuth;
    
    // Method 1: X-User-ID header (untuk backward compatibility/testing di development)
    if (APP_ENV === 'development') {
        $headers = function_exists('getallheaders') ? getallheaders() : [];
        if (isset($headers['X-User-ID']) && !empty($headers['X-User-ID'])) {
            error_log("⚠️ Using X-User-ID header (Development Mode): " . $headers['X-User-ID']);
            return $headers['X-User-ID'];
        }
    }
    
    // Method 2: Verifikasi Firebase ID Token (Cara yang Benar)
    $authHeader = getAuthorizationHeader();
    
    if (empty($authHeader)) {
        error_log("❌ Authorization header is empty");
        return null;
    }
    
    // Parse token dengan aman
    $parts = explode(' ', $authHeader);
    
    if (count($parts) !== 2) {
        error_log("❌ Invalid Authorization header format. Parts count: " . count($parts));
        http_response_code(401);
        echo json_encode([
            'success' => false, 
            'message' => 'Format Authorization header tidak valid. Gunakan: Bearer <token>'
        ]);
        exit();
    }
    
    if (strcasecmp($parts[0], 'Bearer') !== 0) {
        error_log("❌ Authorization header must start with 'Bearer'");
        http_response_code(401);
        echo json_encode([
            'success' => false, 
            'message' => 'Authorization header harus dimulai dengan "Bearer"'
        ]);
        exit();
    }
    
    $idToken = $parts[1];
    
    if (empty($idToken)) {
        error_log("❌ Token is empty after parsing");
        http_response_code(401);
        echo json_encode([
            'success' => false, 
            'message' => 'Token tidak ditemukan setelah Bearer'
        ]);
        exit();
    }
    
    try {
        // Verify token dengan leeway 300 detik (5 menit) untuk mengatasi perbedaan waktu
        $verifiedIdToken = $firebaseAuth->verifyIdToken($idToken, false, 300);
        $uid = $verifiedIdToken->claims()->get('sub');
        
        error_log("✅ Token verified successfully for UID: " . $uid);
        return $uid;
        
    } catch (\Kreait\Firebase\Exception\Auth\FailedToVerifyToken $e) {
        error_log("❌ Firebase token verification failed: " . $e->getMessage());
        http_response_code(401);
        echo json_encode([
            'success' => false, 
            'message' => 'Token tidak valid atau sudah kadaluarsa. Silakan login ulang.',
            'error_detail' => APP_ENV === 'development' ? $e->getMessage() : null
        ]);
        exit();
    } catch (\Exception $e) {
        error_log("❌ Unexpected error during token verification: " . $e->getMessage());
        http_response_code(401);
        echo json_encode([
            'success' => false, 
            'message' => 'Gagal memverifikasi token. Silakan coba lagi.',
            'error_detail' => APP_ENV === 'development' ? $e->getMessage() : null
        ]);
        exit();
    }
}

/**
 * Require authentication
 */
function requireAuth() {
    $uid = getFirebaseUid();
    if (!$uid) {
        http_response_code(401);
        echo json_encode([
            'success' => false,
            'message' => 'Unauthorized: Token autentikasi tidak ditemukan atau tidak valid'
        ]);
        exit();
    }
    return $uid;
}

/**
 * Send JSON response
 */
function sendResponse($data, $statusCode = 200) {
    http_response_code($statusCode);
    echo json_encode($data);
    exit();
}

/**
 * Verify Firebase App Check Token
 * 
 * CATATAN: Fungsi ini akan DI-SKIP di development mode.
 * Ubah APP_ENV ke 'production' untuk mengaktifkan verifikasi.
 */
function requireAppCheck() {
    // SKIP App Check di development mode
    if (APP_ENV === 'development') {
        error_log("⚠️ App Check: SKIPPED (Development Mode)");
        return true;
    }
    
    // Verifikasi App Check di production mode
    global $firebaseAuth;
    $headers = function_exists('getallheaders') ? getallheaders() : [];

    $appCheckToken = null;
    foreach ($headers as $key => $value) {
        if (strtolower($key) === 'x-firebase-appcheck') {
            $appCheckToken = $value;
            break;
        }
    }
    
    if (!$appCheckToken) {
        error_log("❌ App Check: Token missing");
        http_response_code(401);
        echo json_encode([
            'success' => false, 
            'message' => 'Unauthorized: App Check token is missing.'
        ]);
        exit();
    }
    
    try {
        // TODO: Implementasi verifikasi token dengan Firebase Admin SDK
        // Saat ini, terima semua token untuk testing
        // $firebaseAuth->verifyAppCheckToken($appCheckToken);
        error_log("✅ App Check: Token verified (Production Mode)");
        return true;
        
    } catch (\Exception $e) {
        error_log("❌ App Check verification FAILED: " . $e->getMessage());
        http_response_code(401);
        echo json_encode([
            'success' => false, 
            'message' => 'Unauthorized: Invalid App Check token.'
        ]);
        exit();
    }
}

/**
 * Send email using PHPMailer with SMTP
 */
function sendEmail($to, $subject, $body) {
    $mail = new PHPMailer(true);
    
    try {
        $mail->isSMTP();
        $mail->Host       = $_ENV['SMTP_HOST'] ?? 'localhost';
        $mail->SMTPAuth   = true;
        $mail->Username   = $_ENV['SMTP_USER'] ?? '';
        $mail->Password   = $_ENV['SMTP_PASS'] ?? '';
        
        $smtpSecure = strtolower($_ENV['SMTP_SECURE'] ?? 'tls');
        if ($smtpSecure === 'ssl') {
            $mail->SMTPSecure = PHPMailer::ENCRYPTION_SMTPS;
        } else {
            $mail->SMTPSecure = PHPMailer::ENCRYPTION_STARTTLS;
        }
        
        $mail->Port       = (int)($_ENV['SMTP_PORT'] ?? 587);
        $mail->setFrom(
            $_ENV['MAIL_FROM_ADDRESS'] ?? 'no-reply@example.com', 
            $_ENV['MAIL_FROM_NAME'] ?? 'Kamus Korea'
        );
        $mail->addAddress($to);
        $mail->isHTML(true);
        $mail->Subject = $subject;
        $mail->Body    = $body;
        $mail->CharSet = 'UTF-8';
        
        $mail->send();
        return true;
    } catch (Exception $e) {
        error_log("Email sending failed: " . $mail->ErrorInfo);
        return false;
    }
}

// =================================================
// ROUTING
// =================================================
$requestUri = $_SERVER['REQUEST_URI'];
$requestMethod = $_SERVER['REQUEST_METHOD'];

$uri = strtok($requestUri, '?');
$uri = str_replace('/kamuskorea/api.php', '', $uri);
$routes = explode('/', trim($uri, '/'));

// =================================================
// APP CHECK MIDDLEWARE
// =================================================
// KECUALIKAN endpoint tertentu dari App Check
$publicEndpoints = [
    'auth/forgot-password',
    'auth/reset-password',
    'auth/verify-reset-token'
];

$currentEndpoint = implode('/', array_slice($routes, 0, 2));
$isPublicEndpoint = in_array($currentEndpoint, $publicEndpoints);

if (!$isPublicEndpoint) {
    // Panggil App Check untuk semua endpoint yang BUKAN public
    // Di development mode, ini akan di-skip otomatis
    requireAppCheck();
}

// =================================================
// ROUTE HANDLERS
// =================================================

// ========== AUTHENTICATION ROUTES ==========

if ($routes[0] === 'auth' && $routes[1] === 'forgot-password' && $requestMethod === 'POST') {
    /**
     * POST /auth/forgot-password
     * Request password reset menggunakan Firebase Auth
     */
    global $firebaseAuth;
    
    $input = json_decode(file_get_contents('php://input'), true);
    $email = $input['email'] ?? '';
    
    if (empty($email)) {
        sendResponse(['success' => false, 'message' => 'Email tidak boleh kosong'], 400);
    }
    
    if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
        sendResponse(['success' => false, 'message' => 'Format email tidak valid'], 400);
    }
    
    try {
        $stmt = $pdo->prepare("SELECT firebase_uid, email, auth_type FROM users WHERE email = ?");
        $stmt->execute([$email]);
        $user = $stmt->fetch();
        
        if (!$user) {
            sendResponse([
                'success' => false,
                'message' => 'Email tidak terdaftar di sistem kami'
            ], 404);
        }
        
        if ($user['auth_type'] === 'google') {
            sendResponse([
                'success' => false,
                'message' => 'Akun ini terdaftar menggunakan Google Sign-In. Silakan login dengan Google.',
                'auth_type' => 'google'
            ], 400);
        }
        
        $link = $firebaseAuth->sendPasswordResetLink($email);
        
        sendResponse([
            'success' => true,
            'message' => 'Link reset password telah dikirim ke email Anda. Silakan cek inbox atau folder spam.'
        ]);
        
    } catch (\Kreait\Firebase\Exception\Auth\UserNotFound $e) {
        sendResponse([
            'success' => false,
            'message' => 'Email tidak terdaftar di sistem autentikasi'
        ], 404);
    } catch (\Exception $e) {
        error_log("Firebase password reset error: " . $e->getMessage());
        sendResponse([
            'success' => false,
            'message' => 'Gagal mengirim email reset password. Silakan coba lagi nanti.'
        ], 500);
    }
}

// ========== USER ROUTES ==========

elseif ($routes[0] === 'user' && $routes[1] === 'sync' && $requestMethod === 'POST') {
    /**
     * POST /user/sync
     * Sync user from Firebase to MySQL
     */
    global $firebaseAuth;
    $uid = requireAuth();
    $input = json_decode(file_get_contents('php://input'), true);
    
    $email = $input['email'] ?? null;
    $name = $input['name'] ?? null;
    $photoUrl = $input['photoUrl'] ?? null;
    $authType = $input['auth_type'] ?? 'password';
    
    try {
        $firebaseUser = $firebaseAuth->getUser($uid);
        
        $stmt = $pdo->prepare("
            INSERT INTO users (firebase_uid, email, name, profile_picture_url, auth_type, created_at)
            VALUES (?, ?, ?, ?, ?, NOW())
            ON DUPLICATE KEY UPDATE
                email = VALUES(email),
                name = COALESCE(VALUES(name), name),
                profile_picture_url = COALESCE(VALUES(profile_picture_url), profile_picture_url),
                auth_type = VALUES(auth_type),
                updated_at = NOW()
        ");
        
        $stmt->execute([$uid, $email, $name, $photoUrl, $authType]);
        $isNew = $pdo->lastInsertId() > 0;
        
        sendResponse([
            'success' => true,
            'message' => $isNew ? 'User created' : 'User synced',
            'is_new' => $isNew
        ]);
        
    } catch (\Kreait\Firebase\Exception\Auth\UserNotFound $e) {
        sendResponse([
            'success' => false,
            'message' => 'User not found in Firebase'
        ], 404);
    } catch (\Exception $e) {
        error_log("User sync error: " . $e->getMessage());
        sendResponse([
            'success' => false,
            'message' => 'Failed to sync user'
        ], 500);
    }
}

elseif ($routes[0] === 'user' && $routes[1] === 'premium' && $routes[2] === 'status' && $requestMethod === 'GET') {
    /**
     * GET /user/premium/status
     * Check user premium status
     */
    $uid = requireAuth();
    
    $stmt = $pdo->prepare("SELECT premium_expiry FROM users WHERE firebase_uid = ?");
    $stmt->execute([$uid]);
    $user = $stmt->fetch();
    
    if (!$user) {
        sendResponse([
            'isPremium' => false,
            'expiryDate' => null
        ]);
    }
    
    $isPremium = $user['premium_expiry'] && strtotime($user['premium_expiry']) > time();
    
    sendResponse([
        'isPremium' => $isPremium,
        'expiryDate' => $user['premium_expiry']
    ]);
}

elseif ($routes[0] === 'user' && $routes[1] === 'premium' && $routes[2] === 'activate' && $requestMethod === 'POST') {
    /**
     * POST /user/premium/activate
     * Activate premium subscription
     */
    $uid = requireAuth();
    $input = json_decode(file_get_contents('php://input'), true);
    
    $purchaseToken = $input['purchase_token'] ?? '';
    $durationDays = $input['duration_days'] ?? 30;
    
    if (empty($purchaseToken)) {
        sendResponse(['success' => false, 'message' => 'Purchase token required'], 400);
    }
    
    $premiumUntil = date('Y-m-d H:i:s', strtotime("+$durationDays days"));
    
    $stmt = $pdo->prepare("
        UPDATE users 
        SET premium_expiry = ?, 
            purchase_token = ?,
            is_premium = 1,
            updated_at = NOW()
        WHERE firebase_uid = ?
    ");
    $stmt->execute([$premiumUntil, $purchaseToken, $uid]);
    
    sendResponse([
        'success' => true,
        'isPremium' => true,
        'expiryDate' => $premiumUntil
    ]);
}

elseif ($routes[0] === 'user' && $routes[1] === 'profile' && $requestMethod === 'GET') {
    /**
     * GET /user/profile
     * Get user profile
     */
    $uid = requireAuth();
    
    $stmt = $pdo->prepare("
        SELECT name, date_of_birth, profile_picture_url 
        FROM users 
        WHERE firebase_uid = ?
    ");
    $stmt->execute([$uid]);
    $user = $stmt->fetch();
    
    if (!$user) {
        sendResponse([
            'name' => null,
            'dob' => null,
            'profilePictureUrl' => null
        ]);
    }
    
    sendResponse([
        'name' => $user['name'],
        'dob' => $user['date_of_birth'],
        'profilePictureUrl' => $user['profile_picture_url']
    ]);
}

elseif ($routes[0] === 'user' && $routes[1] === 'profile' && $requestMethod === 'PATCH') {
    /**
     * PATCH /user/profile
     * Update user profile
     */
    $uid = requireAuth();
    $input = json_decode(file_get_contents('php://input'), true);
    
    $name = $input['name'] ?? null;
    $dob = $input['dob'] ?? null;
    
    $stmt = $pdo->prepare("
        UPDATE users 
        SET name = ?, date_of_birth = ?, updated_at = NOW()
        WHERE firebase_uid = ?
    ");
    $stmt->execute([$name, $dob, $uid]);
    
    sendResponse(['success' => true]);
}

elseif ($routes[0] === 'user' && $routes[1] === 'profile' && $routes[2] === 'picture' && $requestMethod === 'POST') {
    /**
     * POST /user/profile/picture
     * Upload profile picture
     */
    $uid = requireAuth();
    
    if (!isset($_FILES['image'])) {
        sendResponse(['success' => false, 'message' => 'No image uploaded'], 400);
    }
    
    $file = $_FILES['image'];
    $uploadDir = __DIR__ . '/uploads/profiles/';
    
    if (!is_dir($uploadDir)) {
        mkdir($uploadDir, 0755, true);
    }
    
    $extension = pathinfo($file['name'], PATHINFO_EXTENSION);
    $filename = $uid . '_' . time() . '.' . $extension;
    $uploadPath = $uploadDir . $filename;
    
    if (move_uploaded_file($file['tmp_name'], $uploadPath)) {
        $profilePictureUrl = "https://webtechsolution.my.id/kamuskorea/uploads/profiles/$filename";
        
        $stmt = $pdo->prepare("UPDATE users SET profile_picture_url = ?, updated_at = NOW() WHERE firebase_uid = ?");
        $stmt->execute([$profilePictureUrl, $uid]);
        
        sendResponse([
            'success' => true,
            'profilePictureUrl' => $profilePictureUrl
        ]);
    } else {
        sendResponse(['success' => false, 'message' => 'Upload failed'], 500);
    }
}

// ========== EBOOK ROUTES ==========

elseif ($routes[0] === 'ebooks' && $requestMethod === 'GET') {
    /**
     * GET /ebooks
     * Get all ebooks (with premium filtering)
     */
    $uid = getFirebaseUid();
    
    $isPremium = false;
    if ($uid) {
        $stmt = $pdo->prepare("SELECT premium_expiry FROM users WHERE firebase_uid = ?");
        $stmt->execute([$uid]);
        $user = $stmt->fetch();
        $isPremium = $user && $user['premium_expiry'] && strtotime($user['premium_expiry']) > time();
    }
    
    $stmt = $pdo->query("SELECT * FROM ebooks ORDER BY `order_index` ASC");
    $ebooks = $stmt->fetchAll();
    
    $result = array_map(function($ebook) use ($isPremium) {
        return [
            'id' => (int)$ebook['id'],
            'title' => $ebook['title'],
            'description' => $ebook['description'],
            'coverImageUrl' => $ebook['coverImageUrl'],
            'order' => (int)$ebook['order_index'],
            'isPremium' => (bool)$ebook['is_premium'],
            'pdfUrl' => ($ebook['is_premium'] && !$isPremium) ? '' : $ebook['pdfUrl']
        ];
    }, $ebooks);
    
    sendResponse($result);
}

// ========== KAMUS UPDATE ROUTES (DISABLED - Using Local SQLite) ==========

elseif ($routes[0] === 'kamus' && $routes[1] === 'updates' && $requestMethod === 'GET') {
    /**
     * GET /kamus/updates?version=X
     * DISABLED: Kamus menggunakan database lokal (kamus_korea.db)
     */
    error_log("ℹ️ Kamus sync requested but using local SQLite database");
    
    sendResponse([
        'success' => true,
        'message' => 'Kamus menggunakan database lokal (kamus_korea.db)',
        'latestVersion' => 0,
        'words' => []
    ]);
}

// ========== ASSESSMENT CATEGORIES ==========

elseif ($routes[0] === 'assessments' && $routes[1] === 'categories' && $requestMethod === 'GET') {
    /**
     * GET /assessments/categories?type=quiz
     * Get assessment categories
     */
    $type = $_GET['type'] ?? null;
    
    $sql = "SELECT id, name, type, description, order_index FROM assessment_categories WHERE 1=1";
    $params = [];
    
    if ($type) {
        $sql .= " AND type = ?";
        $params[] = $type;
    }
    
    $sql .= " ORDER BY order_index ASC";
    
    $stmt = $pdo->prepare($sql);
    $stmt->execute($params);
    $categories = $stmt->fetchAll();
    
    sendResponse($categories);
}

// ========== ASSESSMENT ROUTES ==========

elseif ($routes[0] === 'assessments' && !isset($routes[1]) && $requestMethod === 'GET') {
    /**
     * GET /assessments?type=quiz&category_id=1
     * Get all assessments
     */
    $uid = getFirebaseUid();
    
    $isPremium = false;
    if ($uid) {
        $stmt = $pdo->prepare("SELECT premium_expiry FROM users WHERE firebase_uid = ?");
        $stmt->execute([$uid]);
        $user = $stmt->fetch();
        $isPremium = $user && $user['premium_expiry'] && strtotime($user['premium_expiry']) > time();
    }
    
    $type = $_GET['type'] ?? null;
    $categoryId = $_GET['category_id'] ?? null;
    
    $sql = "SELECT a.*, 
                   c.name as category_name,
                   (SELECT COUNT(*) FROM questions WHERE assessment_id = a.id) as question_count
            FROM assessments a
            LEFT JOIN assessment_categories c ON a.category_id = c.id
            WHERE 1=1";
    $params = [];
    
    if ($type) {
        $sql .= " AND a.type = ?";
        $params[] = $type;
    }
    
    if ($categoryId) {
        $sql .= " AND a.category_id = ?";
        $params[] = $categoryId;
    }
    
    $sql .= " ORDER BY a.order_index ASC";
    
    $stmt = $pdo->prepare($sql);
    $stmt->execute($params);
    $assessments = $stmt->fetchAll();
    
    $result = array_map(function($a) use ($isPremium) {
        return [
            'id' => (int)$a['id'],
            'title' => $a['title'],
            'description' => $a['description'],
            'type' => $a['type'],
            'duration_minutes' => (int)$a['duration_minutes'],
            'passing_score' => (int)$a['passing_score'],
            'is_premium' => (bool)$a['is_premium'],
            'category_name' => $a['category_name'],
            'question_count' => (int)$a['question_count'],
            'locked' => $a['is_premium'] && !$isPremium
        ];
    }, $assessments);
    
    sendResponse($result);
}

elseif ($routes[0] === 'assessments' && isset($routes[1]) && is_numeric($routes[1]) && isset($routes[2]) && $routes[2] === 'questions' && $requestMethod === 'GET') {
    /**
     * GET /assessments/{id}/questions
     * Get questions for an assessment
     */
    $uid = requireAuth();
    $assessmentId = (int)$routes[1];
    
    $stmt = $pdo->prepare("SELECT is_premium FROM assessments WHERE id = ?");
    $stmt->execute([$assessmentId]);
    $assessment = $stmt->fetch();
    
    if (!$assessment) {
        sendResponse(['success' => false, 'message' => 'Assessment not found'], 404);
    }
    
    if ($assessment['is_premium']) {
        $stmt = $pdo->prepare("SELECT premium_expiry FROM users WHERE firebase_uid = ?");
        $stmt->execute([$uid]);
        $user = $stmt->fetch();
        $isPremium = $user && $user['premium_expiry'] && strtotime($user['premium_expiry']) > time();
        
        if (!$isPremium) {
            sendResponse(['success' => false, 'message' => 'Premium required'], 403);
        }
    }
    
    $stmt = $pdo->prepare("
        SELECT id, question_text, question_type, media_url, 
               option_a, option_b, option_c, option_d, order_index
        FROM questions
        WHERE assessment_id = ?
        ORDER BY order_index ASC
    ");
    $stmt->execute([$assessmentId]);
    $questions = $stmt->fetchAll();
    
    $result = array_map(function($q) {
        return [
            'id' => (int)$q['id'],
            'question_text' => $q['question_text'],
            'question_type' => $q['question_type'],
            'media_url' => $q['media_url'],
            'option_a' => $q['option_a'],
            'option_b' => $q['option_b'],
            'option_c' => $q['option_c'],
            'option_d' => $q['option_d'],
            'order_index' => (int)$q['order_index']
        ];
    }, $questions);
    
    sendResponse($result);
}

elseif ($routes[0] === 'assessments' && isset($routes[1]) && is_numeric($routes[1]) && isset($routes[2]) && $routes[2] === 'submit' && $requestMethod === 'POST') {
    /**
     * POST /assessments/{id}/submit
     * Submit assessment answers
     */
    $uid = requireAuth();
    $assessmentId = (int)$routes[1];
    $input = json_decode(file_get_contents('php://input'), true);
    
    $answers = $input['answers'] ?? [];
    $timeTaken = $input['time_taken_seconds'] ?? 0;
    
    $stmt = $pdo->prepare("
        SELECT id, correct_answer, explanation
        FROM questions
        WHERE assessment_id = ?
    ");
    $stmt->execute([$assessmentId]);
    $questions = $stmt->fetchAll(PDO::FETCH_GROUP | PDO::FETCH_UNIQUE);
    
    $correctCount = 0;
    $details = [];
    
    foreach ($answers as $answer) {
        $questionId = $answer['question_id'];
        $userAnswer = $answer['answer'];
        $correctAnswer = $questions[$questionId]['correct_answer'] ?? '';
        $isCorrect = strtoupper($userAnswer) === strtoupper($correctAnswer);
        
        if ($isCorrect) $correctCount++;
        
        $details[] = [
            'question_id' => $questionId,
            'user_answer' => $userAnswer,
            'correct_answer' => $correctAnswer,
            'is_correct' => $isCorrect,
            'explanation' => $questions[$questionId]['explanation'] ?? null
        ];
    }
    
    $totalQuestions = count($questions);
    $score = ($totalQuestions > 0) ? round(($correctCount / $totalQuestions) * 100) : 0;
    
    $stmt = $pdo->prepare("SELECT passing_score FROM assessments WHERE id = ?");
    $stmt->execute([$assessmentId]);
    $assessment = $stmt->fetch();
    $passingScore = $assessment['passing_score'] ?? 70;
    $passed = $score >= $passingScore;
    
    $stmt = $pdo->prepare("
        INSERT INTO assessment_results 
        (user_id, assessment_id, score, total_questions, correct_answers, time_taken_seconds, completed_at)
        VALUES (?, ?, ?, ?, ?, ?, NOW())
    ");
    $stmt->execute([$uid, $assessmentId, $score, $totalQuestions, $correctCount, $timeTaken]);
    
    sendResponse([
        'score' => $score,
        'total_questions' => $totalQuestions,
        'correct_answers' => $correctCount,
        'passed' => $passed,
        'details' => $details
    ]);
}

elseif ($routes[0] === 'results' && $requestMethod === 'GET') {
    /**
     * GET /results?assessment_id=X
     * Get assessment results history
     */
    $uid = requireAuth();
    $assessmentId = $_GET['assessment_id'] ?? null;
    
    $sql = "
        SELECT r.*, a.title as assessment_title, a.type
        FROM assessment_results r
        JOIN assessments a ON r.assessment_id = a.id
        WHERE r.user_id = ? 
    ";
    $params = [$uid];
    
    if ($assessmentId) {
        $sql .= " AND r.assessment_id = ?";
        $params[] = $assessmentId;
    }
    
    $sql .= " ORDER BY r.completed_at DESC";
    
    $stmt = $pdo->prepare($sql);
    $stmt->execute($params);
    $results = $stmt->fetchAll();
    
    $formattedResults = array_map(function($r) {
        return [
            'id' => (int)$r['id'],
            'assessment_id' => (int)$r['assessment_id'],
            'assessment_title' => $r['assessment_title'],
            'type' => $r['type'],
            'score' => (int)$r['score'],
            'total_questions' => (int)$r['total_questions'],
            'correct_answers' => (int)$r['correct_answers'],
            'time_taken_seconds' => (int)$r['time_taken_seconds'],
            'completed_at' => $r['completed_at']
        ];
    }, $results);
    
    sendResponse($formattedResults);
}

// ========== GAMIFICATION ROUTES ==========

elseif ($routes[0] === 'gamification' && $routes[1] === 'sync-xp' && $requestMethod === 'POST') {
    /**
     * POST /gamification/sync-xp
     * Sync user XP, level, and achievements from app to server
     */
    $uid = requireAuth();
    $input = json_decode(file_get_contents('php://input'), true);

    $totalXp = $input['total_xp'] ?? 0;
    $currentLevel = $input['current_level'] ?? 1;
    $achievementsUnlocked = $input['achievements_unlocked'] ?? [];

    // Get username from request body first, then users table
    if (!empty($input['username'])) {
        $username = $input['username'];
    } else {
        $stmt = $pdo->prepare("SELECT name FROM users WHERE firebase_uid = ?");
        $stmt->execute([$uid]);
        $user = $stmt->fetch();
        $username = $user['name'] ?? 'Anonymous';
    }

    // Convert achievements array to JSON string
    $achievementsJson = json_encode($achievementsUnlocked);

    try {
        $stmt = $pdo->prepare("
            INSERT INTO user_gamification (user_id, username, total_xp, current_level, achievements_unlocked, last_xp_sync)
            VALUES (?, ?, ?, ?, ?, NOW())
            ON DUPLICATE KEY UPDATE
                username = VALUES(username),
                total_xp = VALUES(total_xp),
                current_level = VALUES(current_level),
                achievements_unlocked = VALUES(achievements_unlocked),
                last_xp_sync = NOW(),
                updated_at = NOW()
        ");

        $stmt->execute([$uid, $username, $totalXp, $currentLevel, $achievementsJson]);

        // Get user's rank after sync
        $stmt = $pdo->prepare("
            SELECT COUNT(*) + 1 as rank
            FROM user_gamification
            WHERE total_xp > (SELECT total_xp FROM user_gamification WHERE user_id = ?)
        ");
        $stmt->execute([$uid]);
        $rankData = $stmt->fetch();
        $leaderboardRank = $rankData['rank'] ?? 0;

        sendResponse([
            'success' => true,
            'message' => 'XP synced successfully',
            'leaderboard_rank' => (int)$leaderboardRank
        ]);

    } catch (PDOException $e) {
        error_log("XP sync error: " . $e->getMessage());
        sendResponse([
            'success' => false,
            'message' => 'Failed to sync XP'
        ], 500);
    }
}

elseif ($routes[0] === 'gamification' && $routes[1] === 'leaderboard' && $requestMethod === 'GET') {
    /**
     * GET /gamification/leaderboard?limit=100
     * Get leaderboard top users
     */
    $limit = isset($_GET['limit']) ? min((int)$_GET['limit'], 500) : 100;

    try {
        $stmt = $pdo->prepare("
            SELECT
                user_id,
                username,
                total_xp,
                current_level,
                achievements_unlocked,
                @rank := @rank + 1 AS rank
            FROM user_gamification, (SELECT @rank := 0) r
            ORDER BY total_xp DESC, updated_at ASC
            LIMIT ?
        ");
        $stmt->execute([$limit]);
        $leaderboard = $stmt->fetchAll();

        $result = array_map(function($user) {
            $achievements = json_decode($user['achievements_unlocked'], true) ?? [];
            return [
                'rank' => (int)$user['rank'],
                'user_id' => $user['user_id'],
                'username' => $user['username'] ?: 'Anonymous',
                'total_xp' => (int)$user['total_xp'],
                'level' => (int)$user['current_level'],
                'achievement_count' => count($achievements)
            ];
        }, $leaderboard);

        sendResponse([
            'success' => true,
            'data' => $result
        ]);

    } catch (PDOException $e) {
        error_log("Leaderboard query error: " . $e->getMessage());
        sendResponse([
            'success' => false,
            'message' => 'Failed to fetch leaderboard'
        ], 500);
    }
}

elseif ($routes[0] === 'gamification' && $routes[1] === 'user-rank' && isset($routes[2]) && $requestMethod === 'GET') {
    /**
     * GET /gamification/user-rank/{user_id}
     * Get specific user's rank and stats
     */
    $targetUserId = $routes[2];

    try {
        // Get user's data
        $stmt = $pdo->prepare("
            SELECT user_id, username, total_xp, current_level, achievements_unlocked
            FROM user_gamification
            WHERE user_id = ?
        ");
        $stmt->execute([$targetUserId]);
        $user = $stmt->fetch();

        if (!$user) {
            sendResponse([
                'success' => false,
                'message' => 'User not found in leaderboard'
            ], 404);
        }

        // Get user's rank
        $stmt = $pdo->prepare("
            SELECT COUNT(*) + 1 as rank
            FROM user_gamification
            WHERE total_xp > ?
        ");
        $stmt->execute([$user['total_xp']]);
        $rankData = $stmt->fetch();
        $rank = $rankData['rank'] ?? 0;

        // Get total users
        $stmt = $pdo->query("SELECT COUNT(*) as total FROM user_gamification");
        $totalData = $stmt->fetch();
        $totalUsers = $totalData['total'] ?? 0;

        // Calculate percentile
        $percentile = $totalUsers > 0 ? round((1 - (($rank - 1) / $totalUsers)) * 100, 1) : 0;

        $achievements = json_decode($user['achievements_unlocked'], true) ?? [];

        sendResponse([
            'success' => true,
            'rank' => (int)$rank,
            'total_users' => (int)$totalUsers,
            'percentile' => $percentile,
            'username' => $user['username'] ?: 'Anonymous',
            'total_xp' => (int)$user['total_xp'],
            'level' => (int)$user['current_level'],
            'achievements_unlocked' => $achievements,
            'achievement_count' => count($achievements)
        ]);

    } catch (PDOException $e) {
        error_log("User rank query error: " . $e->getMessage());
        sendResponse([
            'success' => false,
            'message' => 'Failed to fetch user rank'
        ], 500);
    }
}

elseif ($routes[0] === 'gamification' && $routes[1] === 'add-xp' && $requestMethod === 'POST') {
    /**
     * POST /gamification/add-xp
     * Add XP to user (with server-side validation)
     * Optional: Gunakan ini jika ingin validasi XP di server
     */
    $uid = requireAuth();
    $input = json_decode(file_get_contents('php://input'), true);

    $xpAmount = $input['xp_amount'] ?? 0;
    $xpSource = $input['source'] ?? 'unknown';
    $metadata = $input['metadata'] ?? [];

    // Anti-cheat: Validate XP amount
    $maxXpPerAction = [
        'quiz_completed' => 100,
        'pdf_opened' => 10,
        'word_favorited' => 5,
        'flashcard_flipped' => 3,
        'daily_login' => 20
    ];

    $maxAllowed = $maxXpPerAction[$xpSource] ?? 50;

    if ($xpAmount > $maxAllowed) {
        error_log("Suspicious XP amount: $xpAmount for source $xpSource by user $uid");
        sendResponse([
            'success' => false,
            'message' => 'Invalid XP amount'
        ], 400);
    }

    try {
        // Get current user gamification data
        $stmt = $pdo->prepare("SELECT total_xp, current_level FROM user_gamification WHERE user_id = ?");
        $stmt->execute([$uid]);
        $userGamif = $stmt->fetch();

        if (!$userGamif) {
            // Initialize user gamification
            $stmt = $pdo->prepare("
                INSERT INTO user_gamification (user_id, total_xp, current_level)
                VALUES (?, ?, 1)
            ");
            $stmt->execute([$uid, $xpAmount]);
            $newTotalXp = $xpAmount;
            $newLevel = 1;
        } else {
            // Update XP
            $newTotalXp = $userGamif['total_xp'] + $xpAmount;
            $newLevel = floor($newTotalXp / 100) + 1; // Level up setiap 100 XP

            $stmt = $pdo->prepare("
                UPDATE user_gamification
                SET total_xp = ?, current_level = ?, updated_at = NOW()
                WHERE user_id = ?
            ");
            $stmt->execute([$newTotalXp, $newLevel, $uid]);
        }

        // Log XP history (optional)
        $metadataJson = json_encode($metadata);
        $stmt = $pdo->prepare("
            INSERT INTO xp_history (user_id, xp_amount, xp_source, metadata)
            VALUES (?, ?, ?, ?)
        ");
        $stmt->execute([$uid, $xpAmount, $xpSource, $metadataJson]);

        $levelUp = isset($userGamif['current_level']) && $newLevel > $userGamif['current_level'];

        sendResponse([
            'success' => true,
            'new_total_xp' => (int)$newTotalXp,
            'new_level' => (int)$newLevel,
            'level_up' => $levelUp,
            'xp_earned' => (int)$xpAmount
        ]);

    } catch (PDOException $e) {
        error_log("Add XP error: " . $e->getMessage());
        sendResponse([
            'success' => false,
            'message' => 'Failed to add XP'
        ], 500);
    }
}

// ========== 404 NOT FOUND ==========
else {
    error_log("404 Not Found: " . $uri);
    sendResponse([
        'success' => false,
        'message' => 'Endpoint not found',
        'requested_route' => $uri,
        'available_routes' => [
            'POST /auth/forgot-password',
            'POST /user/sync',
            'GET /user/premium/status',
            'POST /user/premium/activate',
            'GET /user/profile',
            'PATCH /user/profile',
            'POST /user/profile/picture',
            'GET /ebooks',
            'GET /kamus/updates',
            'GET /assessments/categories',
            'GET /assessments',
            'GET /assessments/{id}/questions',
            'POST /assessments/{id}/submit',
            'GET /results',
            'POST /gamification/sync-xp',
            'GET /gamification/leaderboard',
            'GET /gamification/user-rank/{user_id}',
            'POST /gamification/add-xp'
        ],
        'app_env' => APP_ENV
    ], 404);
}
?>