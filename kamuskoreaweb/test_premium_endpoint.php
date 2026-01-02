<?php
/**
 * Test Premium Endpoint - Test with mock auth
 * URL: https://webtechsolution.my.id/kamuskorea/test_premium_endpoint.php
 */

// Capture any output that happens before our code
ob_start();

// Force error display
ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);

// Load dependencies
require_once __DIR__ . '/vendor/autoload.php';

use Dotenv\Dotenv;

$dotenv = Dotenv::createImmutable(__DIR__ . '/..');
$dotenv->load();

define('APP_ENV', $_ENV['APP_ENV'] ?? 'production');

// Database connection
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
        ]
    );
} catch (PDOException $e) {
    die("Database connection failed: " . $e->getMessage());
}

// Get any early output
$earlyOutput = ob_get_clean();

if (!empty($earlyOutput)) {
    echo "WARNING: Early output detected!\n";
    echo "Length: " . strlen($earlyOutput) . " bytes\n";
    echo "Content: " . bin2hex($earlyOutput) . "\n\n";
}

// Test sendResponse function
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

// Simulate premium status check (without auth)
// Use a test UID that doesn't exist
$testUid = 'test_user_12345';

$stmt = $pdo->prepare("SELECT premium_expiry FROM users WHERE firebase_uid = ?");
$stmt->execute([$testUid]);
$user = $stmt->fetch();

if (!$user) {
    sendResponse([
        'isPremium' => false,
        'expiryDate' => null,
        'message' => 'Test successful - user not found (expected)',
        'timestamp' => date('Y-m-d H:i:s')
    ]);
}

$isPremium = $user['premium_expiry'] && strtotime($user['premium_expiry']) > time();

sendResponse([
    'isPremium' => $isPremium,
    'expiryDate' => $user['premium_expiry'],
    'message' => 'Test successful',
    'timestamp' => date('Y-m-d H:i:s')
]);
