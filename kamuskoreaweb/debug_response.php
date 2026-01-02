<?php
/**
 * Debug Response - Test sendResponse function
 * URL: https://webtechsolution.my.id/kamuskorea/debug_response.php
 */

// Enable all error display
ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);

// Test sendResponse function
require_once __DIR__ . '/vendor/autoload.php';

use Dotenv\Dotenv;

$dotenv = Dotenv::createImmutable(__DIR__ . '/..');
$dotenv->load();

define('APP_ENV', $_ENV['APP_ENV'] ?? 'production');

// Load Firebase and Database (minimal)
echo "Loading...\n";

// Include the sendResponse function from api.php
// We'll copy it here to test
function sendResponse($data, $statusCode = 200) {
    http_response_code($statusCode);

    // Encode JSON
    $json = json_encode($data);

    // Debug: Log if JSON encoding fails
    if ($json === false) {
        error_log("❌ JSON encode failed: " . json_last_error_msg());
        $json = json_encode(['success' => false, 'message' => 'JSON encoding error']);
    }

    // Send response with explicit length header
    header('Content-Length: ' . strlen($json));

    // Output and flush
    echo $json;
    if (ob_get_level()) ob_end_flush();
    flush();
    exit();
}

// Test it
sendResponse([
    'success' => true,
    'message' => 'sendResponse function works!',
    'test' => true,
    'timestamp' => date('Y-m-d H:i:s')
]);
