<?php
date_default_timezone_set('Asia/Jakarta');

// COMPREHENSIVE ERROR LOGGING - Catches ALL errors and empty responses
require_once __DIR__ . '/debug_all_errors.php';

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
// APP_ENV will be defined after loading .env file
// Error Reporting - will be configured after APP_ENV is set
error_reporting(0);
ini_set('log_errors', 1);

// FIX: Implement log rotation to prevent disk space exhaustion
$logFile = __DIR__ . '/php_error.log';
$maxLogSize = 10 * 1024 * 1024; // 10MB max log file size

if (file_exists($logFile) && filesize($logFile) > $maxLogSize) {
    // Rotate log file
    $rotatedLog = __DIR__ . '/php_error.log.' . date('Y-m-d_H-i-s');
    rename($logFile, $rotatedLog);

    // Keep only last 5 rotated logs
    $logFiles = glob(__DIR__ . '/php_error.log.*');
    if (count($logFiles) > 5) {
        usort($logFiles, function($a, $b) { return filemtime($a) - filemtime($b); });
        foreach (array_slice($logFiles, 0, count($logFiles) - 5) as $oldLog) {
            unlink($oldLog);
        }
    }
}

ini_set('error_log', $logFile);

// CORS Headers - Set for all requests (will be refined after APP_ENV is defined)
// NOTE: CORS is a browser security feature. Mobile apps (Android/iOS) do NOT need CORS headers.
header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization, X-User-ID, X-Firebase-AppCheck");

// Handle preflight OPTIONS request
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    header("Content-Type: application/json; charset=UTF-8");
    http_response_code(200);
    exit();
}

// =================================================
// LOAD ENVIRONMENT VARIABLES & FIREBASE
// =================================================
// Path adjusted for production server structure:
// Structure: /webtechsolution.my.id/kamuskorea/api.php
// - vendor/ is in /kamuskorea/vendor/ (same level as api.php)
// - .env is in /webtechsolution.my.id/.env (1 level up)
// - firebase-service-account.json is in /webtechsolution.my.id/ (1 level up)
require_once __DIR__ . '/vendor/autoload.php';

use Dotenv\Dotenv;
use PHPMailer\PHPMailer\PHPMailer;
use PHPMailer\PHPMailer\Exception;
use Kreait\Firebase\Factory;
use Kreait\Firebase\Auth as FirebaseAuth;

$dotenv = Dotenv::createImmutable(__DIR__ . '/..');
$dotenv->load();

// NOW define APP_ENV after .env is loaded
// SECURITY FIX: Default to 'production' for safety
define('APP_ENV', $_ENV['APP_ENV'] ?? getenv('APP_ENV') ?: 'production');

// Configure error display based on environment
ini_set('display_errors', APP_ENV === 'development' ? 1 : 0);

// Initialize Firebase Admin SDK
try {
    // Use environment variable for service account path (more secure)
    $serviceAccountPath = $_ENV['FIREBASE_SERVICE_ACCOUNT_PATH'] ?? __DIR__ . '/../firebase-service-account.json';

    $firebase = (new Factory)
        ->withServiceAccount($serviceAccountPath);
    $firebaseAuth = $firebase->createAuth();
    $firebaseAppCheck = $firebase->createAppCheck();
} catch (\Exception $e) {
    error_log("Gagal memuat Firebase Service Account: " . $e->getMessage());
    sendResponse([
        'success' => false,
        'message' => 'Konfigurasi server error (Firebase Admin SDK)'
    ], 500);
}

// =================================================
// DATABASE CONNECTION
// =================================================
$host = $_ENV['DB_HOST'] ?? 'localhost';
$username = $_ENV['DB_USER'] ?? $_ENV['DB_USERNAME'] ?? '';
$password = $_ENV['DB_PASS'] ?? $_ENV['DB_PASSWORD'] ?? '';
$database = $_ENV['DB_NAME'] ?? $_ENV['DB_DATABASE'] ?? '';

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
    sendResponse([
        'success' => false,
        'message' => 'Database connection failed'
    ], 500);
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
 *
 * @param bool $optional If true, return null on errors instead of exiting
 * @return string|null UID or null
 */
function getFirebaseUid($optional = false) {
    global $firebaseAuth;

    // SECURITY FIX: X-User-ID header bypass COMPLETELY REMOVED
    // If you need local testing, use Firebase Local Emulator Suite instead
    // https://firebase.google.com/docs/emulator-suite

    // Method 2: Verifikasi Firebase ID Token (Cara yang Benar)
    $authHeader = getAuthorizationHeader();

    if (empty($authHeader)) {
        error_log("❌ Authorization header is empty");
        return null;
    }

    // FIXED: Trim whitespace and normalize spaces before parsing
    $authHeader = trim(preg_replace('/\s+/', ' ', $authHeader));

    // Parse token dengan aman
    $parts = explode(' ', $authHeader, 2);  // Limit to 2 parts max

    if (count($parts) !== 2) {
        error_log("❌ Invalid Authorization header format. Parts count: " . count($parts) . " Header: " . substr($authHeader, 0, 50));
        if ($optional) {
            return null;
        }
        sendResponse([
            'success' => false,
            'message' => 'Format Authorization header tidak valid. Gunakan: Bearer <token>'
        ], 401);
    }

    if (strcasecmp($parts[0], 'Bearer') !== 0) {
        error_log("❌ Authorization header must start with 'Bearer'");
        if ($optional) {
            return null;
        }
        sendResponse([
            'success' => false,
            'message' => 'Authorization header harus dimulai dengan "Bearer"'
        ], 401);
    }

    $idToken = trim($parts[1]);

    if (empty($idToken)) {
        error_log("❌ Token is empty after parsing");
        if ($optional) {
            return null;
        }
        sendResponse([
            'success' => false,
            'message' => 'Token tidak ditemukan setelah Bearer'
        ], 401);
    }

    try {
        // FIX: Reduced leeway from 300s to 60s for better security
        // 60 seconds (1 minute) is sufficient for clock skew tolerance
        $verifiedIdToken = $firebaseAuth->verifyIdToken($idToken, false, 60);
        $uid = $verifiedIdToken->claims()->get('sub');

        error_log("✅ Token verified successfully for UID: " . $uid);
        return $uid;

    } catch (\Kreait\Firebase\Exception\Auth\FailedToVerifyToken $e) {
        error_log("❌ Firebase token verification failed: " . $e->getMessage());
        if ($optional) {
            return null;
        }
        sendResponse([
            'success' => false,
            'message' => 'Token tidak valid atau sudah kadaluarsa. Silakan login ulang.',
            'error_detail' => APP_ENV === 'development' ? $e->getMessage() : null
        ], 401);
    } catch (\Exception $e) {
        error_log("❌ Unexpected error during token verification: " . $e->getMessage());
        if ($optional) {
            return null;
        }
        sendResponse([
            'success' => false,
            'message' => 'Gagal memverifikasi token. Silakan coba lagi.',
            'error_detail' => APP_ENV === 'development' ? $e->getMessage() : null
        ], 401);
    }
}

/**
 * Require authentication
 */
function requireAuth() {
    $uid = getFirebaseUid();
    if (!$uid) {
        sendResponse([
            'success' => false,
            'message' => 'Unauthorized: Token autentikasi tidak ditemukan atau tidak valid'
        ], 401);
    }
    return $uid;
}

/**
 * Send JSON response
 */
function sendResponse($data, $statusCode = 200) {
    http_response_code($statusCode);

    // Encode JSON
    $json = json_encode($data);

    // Debug: Log if JSON encoding fails
    if ($json === false) {
        error_log("❌ JSON encode failed: " . json_last_error_msg());
        $json = json_encode(['success' => false, 'message' => 'JSON encoding error']);
    }

    // Set headers for JSON response
    header('Content-Type: application/json; charset=UTF-8');
    header('Content-Length: ' . strlen($json));

    // Output and flush
    echo $json;
    if (ob_get_level()) ob_end_flush();
    flush();
    exit();
}

/**
 * Verify Firebase App Check Token
 *
 * SECURITY: App Check is ALWAYS verified in production.
 * In development, verification is optional but logged.
 */
function requireAppCheck() {
    global $firebaseAppCheck;
    $headers = function_exists('getallheaders') ? getallheaders() : [];

    $appCheckToken = null;
    foreach ($headers as $key => $value) {
        if (strtolower($key) === 'x-firebase-appcheck') {
            $appCheckToken = $value;
            break;
        }
    }

    // In development, allow missing token but log warning
    if (APP_ENV === 'development' && !$appCheckToken) {
        error_log("⚠️ App Check: Token missing (Development Mode - ALLOWED)");
        return true;
    }

    if (!$appCheckToken) {
        error_log("❌ App Check: Token missing (Production Mode - BLOCKED)");
        sendResponse([
            'success' => false,
            'message' => 'Unauthorized: App Check token is missing.'
        ], 401);
    }

    try {
        // Verify App Check token using Firebase Admin SDK
        $verifiedToken = $firebaseAppCheck->verifyToken($appCheckToken);

        // Token is valid (verifyToken throws exception if invalid)
        error_log("✅ App Check: Token verified successfully");
        return true;

    } catch (\Exception $e) {
        // In development, log error but allow request
        if (APP_ENV === 'development') {
            error_log("⚠️ App Check verification FAILED (Development Mode - ALLOWED): " . $e->getMessage());
            return true;
        }

        // In production, reject invalid tokens
        error_log("❌ App Check verification FAILED (Production Mode - BLOCKED): " . $e->getMessage());
        sendResponse([
            'success' => false,
            'message' => 'Unauthorized: Invalid App Check token.'
        ], 401);
    }
}

/**
 * Verify Google Play Purchase Token
 *
 * @param string $purchaseToken The purchase token from Google Play
 * @param string $productId The product/SKU ID
 * @return bool True if purchase is valid
 * @throws Exception if verification fails
 *
 * IMPLEMENTATION NOTE:
 * This function requires Google Play Developer API credentials.
 * Setup steps:
 * 1. Enable Google Play Developer API in Google Cloud Console
 * 2. Create a service account with "Finance" permissions
 * 3. Download service account JSON key
 * 4. Add path to .env as GOOGLE_PLAY_SERVICE_ACCOUNT_PATH
 * 5. Install Google API PHP Client: composer require google/apiclient
 *
 * For now, this returns true in development mode.
 * In production, it MUST verify with Google Play API.
 */
function verifyGooglePlayPurchase($purchaseToken, $productId) {
    // In development, skip verification but log it
    if (APP_ENV === 'development') {
        error_log("⚠️ Purchase Verification: SKIPPED (Development Mode)");
        error_log("   Token: " . substr($purchaseToken, 0, 20) . "...");
        error_log("   Product: $productId");
        return true;
    }

    // PRODUCTION: Verify with Google Play API
    // Uncomment and configure when ready to implement:
    /*
    try {
        $client = new \Google_Client();
        $client->setAuthConfig($_ENV['GOOGLE_PLAY_SERVICE_ACCOUNT_PATH']);
        $client->addScope(\Google_Service_AndroidPublisher::ANDROIDPUBLISHER);

        $service = new \Google_Service_AndroidPublisher($client);
        $packageName = 'com.webtech.learningkorea'; // Your app package name

        // Verify in-app product purchase
        $purchase = $service->purchases_products->get(
            $packageName,
            $productId,
            $purchaseToken
        );

        // Check if purchase is valid and not refunded
        if ($purchase->getPurchaseState() !== 0) { // 0 = Purchased, 1 = Canceled
            error_log("❌ Purchase canceled or refunded");
            return false;
        }

        // Check if purchase is consumed (for consumable products)
        if ($purchase->getConsumptionState() === 1) { // 1 = Consumed
            error_log("❌ Purchase token already consumed");
            return false;
        }

        error_log("✅ Purchase verified successfully");
        return true;

    } catch (\Exception $e) {
        error_log("❌ Google Play API Error: " . $e->getMessage());
        throw new \Exception("Failed to verify purchase with Google Play");
    }
    */

    // TEMPORARY: Until Google Play API is configured, reject all purchases in production
    error_log("❌ Purchase Verification: NOT IMPLEMENTED (Production Mode - Rejecting)");
    throw new \Exception("Purchase verification not yet configured. Contact support.");
}

/**
 * Rate limiting function
 * Prevents brute force and DoS attacks
 *
 * @param string $identifier Unique identifier for rate limiting (e.g., IP, user ID)
 * @param int $maxRequests Maximum requests allowed in the time window
 * @param int $windowSeconds Time window in seconds
 * @param string $action Action being rate limited (for logging)
 * @return bool True if within limit, exits with 429 if exceeded
 */
function checkRateLimit($identifier, $maxRequests = 100, $windowSeconds = 3600, $action = 'API') {
    $rateLimitDir = sys_get_temp_dir() . '/ratelimit';

    // Create rate limit directory if it doesn't exist
    if (!is_dir($rateLimitDir)) {
        @mkdir($rateLimitDir, 0755, true);
    }

    // Clean old rate limit files (older than 24 hours)
    $cleanupThreshold = time() - 86400;
    foreach (glob($rateLimitDir . '/rl_*') as $file) {
        if (filemtime($file) < $cleanupThreshold) {
            @unlink($file);
        }
    }

    // Create safe filename from identifier
    $safeIdentifier = preg_replace('/[^a-zA-Z0-9_-]/', '_', $identifier);
    $rateLimitFile = $rateLimitDir . '/rl_' . $safeIdentifier . '_' . $action . '.json';

    // Load existing rate limit data
    $data = [];
    if (file_exists($rateLimitFile)) {
        $content = @file_get_contents($rateLimitFile);
        if ($content !== false) {
            $data = json_decode($content, true) ?: [];
        }
    }

    $currentTime = time();
    $windowStart = $currentTime - $windowSeconds;

    // Remove requests outside the current window
    $data = array_filter($data, function($timestamp) use ($windowStart) {
        return $timestamp > $windowStart;
    });

    // Check if limit exceeded
    if (count($data) >= $maxRequests) {
        // Calculate remaining time until window resets
        $oldestRequest = min($data);
        $resetTime = $oldestRequest + $windowSeconds;
        $retryAfter = $resetTime - $currentTime;

        error_log("❌ Rate limit exceeded for $identifier on $action: " . count($data) . "/$maxRequests requests");

        header('Retry-After: ' . $retryAfter);
        sendResponse([
            'success' => false,
            'message' => 'Too many requests. Please try again later.',
            'retry_after' => $retryAfter
        ], 429);
    }

    // Add current request timestamp
    $data[] = $currentTime;

    // Save updated data
    @file_put_contents($rateLimitFile, json_encode($data), LOCK_EX);

    return true;
}

/**
 * Get client IP address (considers proxies)
 */
function getClientIP() {
    $ipHeaders = [
        'HTTP_CF_CONNECTING_IP', // Cloudflare
        'HTTP_X_FORWARDED_FOR',  // Standard proxy header
        'HTTP_X_REAL_IP',        // Nginx proxy
        'REMOTE_ADDR'            // Direct connection
    ];

    foreach ($ipHeaders as $header) {
        if (isset($_SERVER[$header]) && !empty($_SERVER[$header])) {
            $ip = $_SERVER[$header];
            // If X-Forwarded-For contains multiple IPs, take the first one
            if (strpos($ip, ',') !== false) {
                $ip = trim(explode(',', $ip)[0]);
            }
            // Validate IP address
            if (filter_var($ip, FILTER_VALIDATE_IP, FILTER_FLAG_NO_PRIV_RANGE | FILTER_FLAG_NO_RES_RANGE)) {
                return $ip;
            }
        }
    }

    return $_SERVER['REMOTE_ADDR'] ?? 'unknown';
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
// GENERAL RATE LIMITING
// =================================================
// Apply general rate limit to all API requests (200 req/hour per IP)
// Specific endpoints may have stricter limits
$clientIP = getClientIP();
checkRateLimit($clientIP, 200, 3600, 'general-api');

// =================================================
// APP CHECK MIDDLEWARE
// =================================================
// KECUALIKAN endpoint tertentu dari App Check
$publicEndpoints = [
    'auth/forgot-password',
    'auth/reset-password',
    'auth/verify-reset-token',
    // Assessment endpoints (untuk akses publik)
    'assessments/categories',
    // NOTE: Endpoint lain tetap butuh App Check untuk keamanan
];

// Endpoint yang boleh diakses tanpa App Check (tapi bisa dengan optional auth)
$optionalAppCheckEndpoints = [
    'assessments',  // GET /assessments (list)
    'ebooks',       // GET /ebooks
    'video_hafalan', // GET /video_hafalan
    'kamus'         // GET /kamus/updates
];

$currentEndpoint = implode('/', array_slice($routes, 0, 2));
$firstRoute = $routes[0] ?? '';

$isPublicEndpoint = in_array($currentEndpoint, $publicEndpoints);
$isOptionalAppCheck = in_array($firstRoute, $optionalAppCheckEndpoints);

if (!$isPublicEndpoint && !$isOptionalAppCheck) {
    // Panggil App Check untuk semua endpoint yang BUKAN public/optional
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
     * SECURITY: Rate limited to prevent abuse
     */
    global $firebaseAuth;

    // Rate limit: 5 requests per hour per IP
    $clientIP = getClientIP();
    checkRateLimit($clientIP, 5, 3600, 'forgot-password');

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
     * SECURITY: Rate limited and requires email verification for new users
     */
    global $firebaseAuth;
    $uid = requireAuth();

    // Rate limit: Max 10 sync requests per hour per IP (prevents bot spam)
    $clientIP = getClientIP();
    checkRateLimit($clientIP, 10, 3600, 'user-sync');

    $input = json_decode(file_get_contents('php://input'), true);

    $email = $input['email'] ?? null;
    $name = $input['name'] ?? null;
    $photoUrl = $input['photoUrl'] ?? null;
    $authType = $input['auth_type'] ?? 'password';

    try {
        $firebaseUser = $firebaseAuth->getUser($uid);

        // SECURITY: Require email verification for password auth (block bots)
        if ($authType === 'password' && !$firebaseUser->emailVerified) {
            sendResponse([
                'success' => false,
                'message' => 'Email belum diverifikasi. Silakan cek inbox email Anda dan klik link verifikasi.',
                'error_code' => 'EMAIL_NOT_VERIFIED'
            ], 403);
        }

        // SECURITY: Detect bot patterns in Google Sign-In
        if ($authType === 'google' && !empty($email)) {
            // Pattern: firstname.lastname.digits@gmail.com (e.g., john.doe.12345@gmail.com)
            if (preg_match('/^[a-z]+[a-z]+\.\d{5}@gmail\.com$/i', $email)) {
                error_log("⚠️ SUSPECTED BOT: Google Sign-In with pattern email: $email");
                sendResponse([
                    'success' => false,
                    'message' => 'Registrasi gagal. Silakan hubungi support.',
                    'error_code' => 'SUSPICIOUS_ACCOUNT'
                ], 403);
            }
        }
        
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
    error_log("DEBUG: Entering /user/premium/status endpoint");
    $uid = requireAuth();
    error_log("DEBUG: UID after requireAuth: " . ($uid ?? 'NULL'));

    $stmt = $pdo->prepare("SELECT premium_expiry FROM users WHERE firebase_uid = ?");
    $stmt->execute([$uid]);
    $user = $stmt->fetch();
    error_log("DEBUG: User data fetched: " . ($user ? json_encode($user) : 'NULL'));
    
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
     * SECURITY: Validates purchase token with Google Play API, rate limited
     */
    $uid = requireAuth();

    // Rate limit: 10 attempts per hour per user (prevents premium bypass attempts)
    checkRateLimit($uid, 10, 3600, 'premium-activate');

    $input = json_decode(file_get_contents('php://input'), true);

    $purchaseToken = $input['purchase_token'] ?? '';
    $productId = $input['product_id'] ?? '';
    $durationDays = (int)($input['duration_days'] ?? 30);

    // Validate inputs
    if (empty($purchaseToken)) {
        sendResponse(['success' => false, 'message' => 'Purchase token required'], 400);
    }
    if (empty($productId)) {
        sendResponse(['success' => false, 'message' => 'Product ID required'], 400);
    }
    if ($durationDays <= 0 || $durationDays > 3650) {
        sendResponse(['success' => false, 'message' => 'Invalid duration'], 400);
    }

    // Check if token was already used
    $stmt = $pdo->prepare("SELECT firebase_uid FROM users WHERE purchase_token = ? AND firebase_uid != ?");
    $stmt->execute([$purchaseToken, $uid]);
    if ($stmt->fetch()) {
        sendResponse(['success' => false, 'message' => 'Purchase token already used'], 400);
    }

    // Verify purchase with Google Play Billing API
    try {
        $isValid = verifyGooglePlayPurchase($purchaseToken, $productId);
        if (!$isValid) {
            error_log("❌ Invalid purchase token for user $uid");
            sendResponse(['success' => false, 'message' => 'Invalid purchase token'], 400);
        }
    } catch (\Exception $e) {
        error_log("❌ Purchase verification error: " . $e->getMessage());
        sendResponse(['success' => false, 'message' => 'Purchase verification failed'], 500);
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

    error_log("✅ Premium activated for user $uid until $premiumUntil");

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
     * Update user profile (name and date_of_birth)
     * SECURITY: Validates input length and format
     */
    $uid = requireAuth();
    $input = json_decode(file_get_contents('php://input'), true);

    $name = $input['name'] ?? null;
    $dob = $input['dob'] ?? null;

    // Validate name length
    if ($name !== null) {
        if (strlen($name) > 100) {
            sendResponse(['success' => false, 'message' => 'Name must be 100 characters or less'], 400);
        }
        if (strlen(trim($name)) === 0) {
            sendResponse(['success' => false, 'message' => 'Name cannot be empty'], 400);
        }
        // Sanitize name (allow letters, spaces, and common characters)
        $name = trim($name);
    }

    // Validate date of birth format and range
    if ($dob !== null) {
        // Check if it's a valid date format (YYYY-MM-DD or YYYY/MM/DD)
        $dobTimestamp = strtotime($dob);
        if ($dobTimestamp === false) {
            sendResponse(['success' => false, 'message' => 'Invalid date format. Use YYYY-MM-DD'], 400);
        }

        // Check if date is reasonable (not in future, not before 1900)
        $currentTime = time();
        $minDate = strtotime('1900-01-01');
        if ($dobTimestamp > $currentTime) {
            sendResponse(['success' => false, 'message' => 'Date of birth cannot be in the future'], 400);
        }
        if ($dobTimestamp < $minDate) {
            sendResponse(['success' => false, 'message' => 'Date of birth must be after 1900'], 400);
        }

        // Normalize to YYYY-MM-DD format
        $dob = date('Y-m-d', $dobTimestamp);
    }

    // Update user profile - create if not exists
    try {
        // Try to update first
        $updates = [];
        $params = [];
        
        if ($name !== null) {
            $updates[] = "name = ?";
            $params[] = $name;
        }
        
        if ($dob !== null) {
            $updates[] = "date_of_birth = ?";
            $params[] = $dob;
        }
        
        if (!empty($updates)) {
            $updates[] = "updated_at = NOW()";
            $params[] = $uid;
            
            $sql = "UPDATE users SET " . implode(", ", $updates) . " WHERE firebase_uid = ?";
            $stmt = $pdo->prepare($sql);
            $stmt->execute($params);
            
            // If no rows affected, user doesn't exist - create them
            if ($stmt->rowCount() === 0) {
                $stmt = $pdo->prepare("
                    INSERT INTO users (firebase_uid, name, date_of_birth, created_at, updated_at)
                    VALUES (?, ?, ?, NOW(), NOW())
                ");
                $stmt->execute([$uid, $name, $dob]);
            }
        }
        
        sendResponse(['success' => true]);
    } catch (Exception $e) {
        error_log("Profile update error: " . $e->getMessage());
        sendResponse(['success' => false, 'message' => 'Failed to update profile'], 500);
    }
}

elseif ($routes[0] === 'user' && $routes[1] === 'profile' && $routes[2] === 'picture' && $requestMethod === 'POST') {
    /**
     * POST /user/profile/picture
     * Upload profile picture
     */
    error_log("DEBUG: Entering /user/profile/picture endpoint");
    $uid = requireAuth();
    error_log("DEBUG: UID: " . ($uid ?? 'NULL'));

    if (!isset($_FILES['image'])) {
        error_log("DEBUG: No image file in request");
        sendResponse(['success' => false, 'message' => 'No image uploaded'], 400);
    }

    $file = $_FILES['image'];
    error_log("DEBUG: File received - name: " . $file['name'] . ", size: " . $file['size'] . ", error: " . $file['error']);

    // Check for upload errors
    if ($file['error'] !== UPLOAD_ERR_OK) {
        error_log("ERROR: Upload error code: " . $file['error']);
        $errorMessage = 'Upload failed: ';
        switch ($file['error']) {
            case UPLOAD_ERR_INI_SIZE:
            case UPLOAD_ERR_FORM_SIZE:
                $errorMessage .= 'File too large';
                break;
            case UPLOAD_ERR_NO_FILE:
                $errorMessage .= 'No file uploaded';
                break;
            default:
                $errorMessage .= 'Unknown error';
        }
        sendResponse(['success' => false, 'message' => $errorMessage], 400);
    }

    $uploadDir = __DIR__ . '/uploads/profiles/';

    if (!is_dir($uploadDir)) {
        mkdir($uploadDir, 0755, true);
    }

    $extension = pathinfo($file['name'], PATHINFO_EXTENSION);
    $filename = $uid . '_' . time() . '.' . $extension;
    $uploadPath = $uploadDir . $filename;

    error_log("DEBUG: Attempting to upload to: " . $uploadPath);

    if (move_uploaded_file($file['tmp_name'], $uploadPath)) {
        $profilePictureUrl = "https://webtechsolution.my.id/kamuskorea/uploads/profiles/$filename";

        $stmt = $pdo->prepare("UPDATE users SET profile_picture_url = ?, updated_at = NOW() WHERE firebase_uid = ?");
        $stmt->execute([$profilePictureUrl, $uid]);

        error_log("DEBUG: Upload successful - URL: " . $profilePictureUrl);
        sendResponse([
            'success' => true,
            'profilePictureUrl' => $profilePictureUrl
        ]);
    } else {
        error_log("ERROR: move_uploaded_file failed");
        sendResponse(['success' => false, 'message' => 'Upload failed'], 500);
    }
}

// ========== EBOOK ROUTES ==========

elseif ($routes[0] === 'ebooks' && $requestMethod === 'GET') {
    /**
     * GET /ebooks
     * Get all ebooks (with premium filtering)
     */
    $uid = getFirebaseUid(true); // Optional auth - bisa diakses tanpa login
    
    $isPremium = false;
    if ($uid) {
        $stmt = $pdo->prepare("SELECT premium_expiry FROM users WHERE firebase_uid = ?");
        $stmt->execute([$uid]);
        $user = $stmt->fetch();
        $isPremium = $user && $user['premium_expiry'] && strtotime($user['premium_expiry']) > time();
    }
    
    $stmt = $pdo->query("SELECT * FROM ebooks ORDER BY `order_index` ASC");
    $ebooks = $stmt->fetchAll();

    $baseUrl = 'https://webtechsolution.my.id/kamuskorea/';

    $result = array_map(function($ebook) use ($isPremium, $baseUrl) {
        // Convert relative paths to full URLs
        $coverImageUrl = $ebook['coverImageUrl'];
        if (!empty($coverImageUrl) && !str_starts_with($coverImageUrl, 'http')) {
            $coverImageUrl = $baseUrl . $coverImageUrl;
        }

        $pdfUrl = $ebook['pdfUrl'];
        if (!empty($pdfUrl) && !str_starts_with($pdfUrl, 'http')) {
            $pdfUrl = $baseUrl . $pdfUrl;
        }

        return [
            'id' => (int)$ebook['id'],
            'title' => $ebook['title'],
            'description' => $ebook['description'],
            'coverImageUrl' => $coverImageUrl,
            'order' => (int)$ebook['order_index'],
            'isPremium' => (bool)$ebook['is_premium'],
            'pdfUrl' => ($ebook['is_premium'] && !$isPremium) ? '' : $pdfUrl
        ];
    }, $ebooks);
    
    sendResponse($result);
}

// ========== VIDEO HAFALAN ROUTES ==========

elseif ($routes[0] === 'video_hafalan' && $requestMethod === 'GET') {
    /**
     * GET /video_hafalan
     * Get all video hafalan (with premium filtering)
     */
    $uid = getFirebaseUid(true); // Optional auth - bisa diakses tanpa login

    $isPremium = false;
    if ($uid) {
        $stmt = $pdo->prepare("SELECT premium_expiry FROM users WHERE firebase_uid = ?");
        $stmt->execute([$uid]);
        $user = $stmt->fetch();
        $isPremium = $user && $user['premium_expiry'] && strtotime($user['premium_expiry']) > time();
    }

    $stmt = $pdo->query("SELECT * FROM video_hafalan ORDER BY `order_index` ASC");
    $videos = $stmt->fetchAll();

    $result = array_map(function($video) use ($isPremium) {
        return [
            'id' => (int)$video['id'],
            'title' => $video['title'],
            'description' => $video['description'],
            'videoUrl' => ($video['is_premium'] && !$isPremium) ? '' : $video['video_url'],
            'thumbnailUrl' => $video['thumbnail_url'],
            'durationMinutes' => (int)($video['duration_minutes'] ?? 0),
            'category' => $video['category'],
            'order' => (int)$video['order_index'],
            'isPremium' => (bool)$video['is_premium']
        ];
    }, $videos);

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
    $uid = getFirebaseUid(true); // Optional auth - bisa diakses tanpa login
    
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
        SELECT id, question_text, question_type, media_url, media_url_2, media_url_3,
               option_a, option_a_type, option_b, option_b_type,
               option_c, option_c_type, option_d, option_d_type, order_index,
               box_text, box_media_url, box_position
        FROM questions
        WHERE assessment_id = ?
        ORDER BY order_index ASC
    ");
    $stmt->execute([$assessmentId]);
    $questions = $stmt->fetchAll();

    $result = array_map(function($q) {
        return [
            'id' => (int)$q['id'],
            'question_text' => $q['question_text'], // Now contains HTML formatting
            'question_type' => $q['question_type'],
            'media_url' => $q['media_url'],
            'media_url_2' => $q['media_url_2'] ?? null,
            'media_url_3' => $q['media_url_3'] ?? null,
            'option_a' => $q['option_a'], // Can be HTML text, image URL, or audio URL
            'option_a_type' => $q['option_a_type'] ?? 'text',
            'option_b' => $q['option_b'],
            'option_b_type' => $q['option_b_type'] ?? 'text',
            'option_c' => $q['option_c'],
            'option_c_type' => $q['option_c_type'] ?? 'text',
            'option_d' => $q['option_d'],
            'option_d_type' => $q['option_d_type'] ?? 'text',
            'order_index' => (int)$q['order_index'],
            'box_text' => $q['box_text'] ?? null,
            'box_media_url' => $q['box_media_url'] ?? null,
            'box_position' => $q['box_position'] ?? 'top'
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
     * SECURITY: Type-cast assessment_id to prevent SQL injection
     */
    $uid = requireAuth();
    $assessmentId = isset($_GET['assessment_id']) ? (int)$_GET['assessment_id'] : null;
    
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

elseif ($routes[0] === 'assessments' && $routes[1] === 'leaderboard' && $requestMethod === 'GET') {
    /**
     * GET /assessments/leaderboard?type=exam&assessment_id=X
     * Get leaderboard for assessments
     * Shows top performers sorted by score (DESC) and time (ASC)
     * Optional: assessment_id to filter by specific assessment
     */
    $type = $_GET['type'] ?? 'exam';
    $assessmentId = isset($_GET['assessment_id']) ? (int)$_GET['assessment_id'] : null;

    // Query untuk mendapatkan 1 hasil terbaik per user
    // Menggunakan LEFT JOIN untuk memastikan hanya best record yang diambil
    if ($assessmentId) {
        // Leaderboard untuk assessment tertentu
        $stmt = $pdo->prepare("
            SELECT
                u.name as userName,
                ar.score,
                ar.time_taken_seconds as durationSeconds,
                ar.completed_at as completedAt
            FROM assessment_results ar
            LEFT JOIN users u ON ar.user_id = u.firebase_uid
            LEFT JOIN assessment_results ar2 ON
                ar2.user_id = ar.user_id
                AND ar2.assessment_id = ?
                AND (
                    ar2.score > ar.score
                    OR (ar2.score = ar.score AND ar2.time_taken_seconds < ar.time_taken_seconds)
                    OR (ar2.score = ar.score AND ar2.time_taken_seconds = ar.time_taken_seconds AND ar2.id > ar.id)
                )
            WHERE ar.assessment_id = ?
            AND ar2.id IS NULL
            ORDER BY ar.score DESC, ar.time_taken_seconds ASC
            LIMIT 50
        ");
        $stmt->execute([$assessmentId, $assessmentId]);
    } else {
        // Leaderboard untuk semua assessment dengan type tertentu
        $stmt = $pdo->prepare("
            SELECT
                u.name as userName,
                ar.score,
                ar.time_taken_seconds as durationSeconds,
                ar.completed_at as completedAt
            FROM assessment_results ar
            LEFT JOIN users u ON ar.user_id = u.firebase_uid
            INNER JOIN assessments a ON ar.assessment_id = a.id
            LEFT JOIN assessment_results ar2 ON
                ar2.user_id = ar.user_id
                AND ar2.assessment_id IN (SELECT id FROM assessments WHERE type = ?)
                AND (
                    ar2.score > ar.score
                    OR (ar2.score = ar.score AND ar2.time_taken_seconds < ar.time_taken_seconds)
                    OR (ar2.score = ar.score AND ar2.time_taken_seconds = ar.time_taken_seconds AND ar2.id > ar.id)
                )
            WHERE a.type = ?
            AND ar2.id IS NULL
            ORDER BY ar.score DESC, ar.time_taken_seconds ASC
            LIMIT 50
        ");
        $stmt->execute([$type, $type]);
    }
    $leaderboard = $stmt->fetchAll();

    // PRIVACY: Email removed from leaderboard to protect user privacy
    $result = array_map(function($entry) {
        return [
            'userName' => $entry['userName'] ?? 'Anonymous',
            'score' => (int)$entry['score'],
            'durationSeconds' => (int)$entry['durationSeconds'],
            'completedAt' => $entry['completedAt'] ?? null
        ];
    }, $leaderboard);

    sendResponse($result);
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

    // ENHANCED LOGGING: Track which UID is syncing
    error_log("📊 XP Sync Request - UID: $uid, XP: $totalXp, Level: $currentLevel");

    // Get username: Priority 1) request body, 2) users table, 3) fallback
    $username = null;

    if (!empty($input['username'])) {
        $username = trim($input['username']);
        error_log("  ✓ Username from request: $username");
    }

    // If no username in request, fetch from users table
    if (empty($username)) {
        $stmt = $pdo->prepare("SELECT name FROM users WHERE firebase_uid = ?");
        $stmt->execute([$uid]);
        $user = $stmt->fetch();
        $username = !empty($user['name']) ? trim($user['name']) : null;
        if ($username) {
            error_log("  ✓ Username from users table: $username");
        }
    }

    // Final fallback if still empty
    if (empty($username)) {
        $username = 'Anonymous';
        error_log("  ⚠️ XP Sync: No username found for UID $uid, using 'Anonymous'");
    }

    // Convert achievements array to JSON string
    $achievementsJson = json_encode($achievementsUnlocked);

    try {
        // Check if user already exists in gamification table
        $checkStmt = $pdo->prepare("SELECT id, user_id, total_xp FROM user_gamification WHERE user_id = ?");
        $checkStmt->execute([$uid]);
        $existingUser = $checkStmt->fetch();

        if ($existingUser) {
            error_log("  📝 UPDATING existing user - ID: {$existingUser['id']}, Old XP: {$existingUser['total_xp']}, New XP: $totalXp");
        } else {
            error_log("  ✨ INSERTING new user - UID: $uid, XP: $totalXp");
        }

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

        // Get affected rows to verify operation
        $affectedRows = $stmt->rowCount();
        error_log("  ✅ Sync completed - Affected rows: $affectedRows");

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