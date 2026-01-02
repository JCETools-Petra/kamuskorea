<?php
/**
 * Test Endpoint Direct - Simulate exact API call
 * URL: https://webtechsolution.my.id/kamuskorea/test_endpoint_direct.php
 */

// Force error display
ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);

echo "===========================================\n";
echo "TESTING SENDRESPONSE FUNCTION\n";
echo "===========================================\n\n";

// Load all dependencies exactly like api.php
require_once __DIR__ . '/vendor/autoload.php';

use Dotenv\Dotenv;

$dotenv = Dotenv::createImmutable(__DIR__ . '/..');
$dotenv->load();

define('APP_ENV', $_ENV['APP_ENV'] ?? 'production');

echo "Step 1: Environment loaded\n";
echo "APP_ENV: " . APP_ENV . "\n\n";

// Test the sendResponse function
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

echo "Step 2: Testing sendResponse function...\n\n";

// Test exactly like the premium/status endpoint would
sendResponse([
    'isPremium' => false,
    'expiryDate' => null,
    'test' => 'This is a test response',
    'timestamp' => date('Y-m-d H:i:s')
]);
