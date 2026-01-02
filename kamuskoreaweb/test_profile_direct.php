<?php
/**
 * Direct test for /user/profile endpoint
 * URL: https://webtechsolution.my.id/kamuskorea/test_profile_direct.php
 */

// Turn on all error reporting
error_reporting(E_ALL);
ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);

header("Content-Type: text/plain; charset=UTF-8");

echo "===========================================\n";
echo "TESTING USER PROFILE ENDPOINT\n";
echo "===========================================\n\n";

// Simulate request to api.php
$url = 'https://webtechsolution.my.id/kamuskorea/api.php/user/profile';

// You need to replace this with a REAL Firebase ID token
// Get it from Android Logcat: grep "ID Token" or from Firebase Auth
$testToken = 'PASTE_REAL_FIREBASE_TOKEN_HERE';

echo "Testing URL: $url\n";
echo "Authorization: Bearer [TOKEN]\n\n";

$ch = curl_init($url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch, CURLOPT_HEADER, true);
curl_setopt($ch, CURLOPT_HTTPHEADER, [
    'Authorization: Bearer ' . $testToken,
    'Content-Type: application/json'
]);

$response = curl_exec($ch);
$httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
$headerSize = curl_getinfo($ch, CURLINFO_HEADER_SIZE);
curl_close($ch);

$headers = substr($response, 0, $headerSize);
$body = substr($response, $headerSize);

echo "HTTP Status: $httpCode\n";
echo str_repeat("-", 60) . "\n";
echo "Response Headers:\n";
echo $headers;
echo "\n" . str_repeat("-", 60) . "\n";
echo "Response Body:\n";
echo $body;
echo "\n" . str_repeat("-", 60) . "\n";
echo "Body Length: " . strlen($body) . " bytes\n";

if (empty($body)) {
    echo "\n❌ ERROR: Response body is EMPTY!\n";
    echo "This is causing 'End of input' error in Android app.\n";
} else {
    echo "\n✅ Response body received\n";

    // Try to decode JSON
    $json = json_decode($body, true);
    if (json_last_error() === JSON_ERROR_NONE) {
        echo "✅ JSON is valid\n";
        print_r($json);
    } else {
        echo "❌ JSON decode error: " . json_last_error_msg() . "\n";
    }
}
