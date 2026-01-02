<?php
/**
 * Minimal API Test - Load full environment but return simple response
 * URL: https://webtechsolution.my.id/kamuskorea/test_api_minimal.php
 */

// Force error display
ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);

echo "Step 1: Starting...\n";

// Load composer
require_once __DIR__ . '/vendor/autoload.php';
echo "Step 2: Vendor loaded\n";

use Dotenv\Dotenv;
use Kreait\Firebase\Factory;

// Load .env
$dotenv = Dotenv::createImmutable(__DIR__ . '/..');
$dotenv->load();
echo "Step 3: .env loaded\n";

// Define APP_ENV
define('APP_ENV', $_ENV['APP_ENV'] ?? getenv('APP_ENV') ?: 'production');
echo "Step 4: APP_ENV = " . APP_ENV . "\n";

// Initialize Firebase
try {
    $serviceAccountPath = $_ENV['FIREBASE_SERVICE_ACCOUNT_PATH'] ?? __DIR__ . '/../firebase-service-account.json';
    $firebase = (new Factory)->withServiceAccount($serviceAccountPath);
    $firebaseAuth = $firebase->createAuth();
    $firebaseAppCheck = $firebase->createAppCheck();
    echo "Step 5: Firebase initialized\n";
} catch (\Exception $e) {
    die("Firebase init failed: " . $e->getMessage());
}

// Database connection
$host = $_ENV['DB_HOST'] ?? 'localhost';
$username = $_ENV['DB_USER'] ?? $_ENV['DB_USERNAME'] ?? '';
$password = $_ENV['DB_PASS'] ?? $_ENV['DB_PASSWORD'] ?? '';
$database = $_ENV['DB_NAME'] ?? $_ENV['DB_DATABASE'] ?? '';

echo "Step 6: DB vars - Host:$host, User:$username, DB:$database\n";

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
    echo "Step 7: Database connected\n";
} catch (PDOException $e) {
    die("Database connection failed: " . $e->getMessage());
}

// Return JSON response
header("Content-Type: application/json");
echo json_encode([
    'success' => true,
    'message' => 'All initialization successful!',
    'app_env' => APP_ENV,
    'database' => $database,
    'timestamp' => date('Y-m-d H:i:s')
]);
