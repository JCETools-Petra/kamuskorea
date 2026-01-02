<?php
/**
 * Test API Request - Make actual HTTP request to api.php
 * URL: https://webtechsolution.my.id/kamuskorea/test_api_request.php
 */

header("Content-Type: text/plain; charset=UTF-8");

echo "===========================================\n";
echo "TESTING API.PHP ENDPOINTS\n";
echo "===========================================\n\n";

// Test function to make HTTP request and show full response
function testEndpoint($url, $method = 'GET', $headers = [], $body = null) {
    echo "Testing: $method $url\n";
    echo str_repeat("-", 60) . "\n";

    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $url);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_HEADER, true);
    curl_setopt($ch, CURLOPT_CUSTOMREQUEST, $method);

    if (!empty($headers)) {
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
    }

    if ($body !== null) {
        curl_setopt($ch, CURLOPT_POSTFIELDS, $body);
    }

    // Get full response (headers + body)
    $response = curl_exec($ch);
    $headerSize = curl_getinfo($ch, CURLINFO_HEADER_SIZE);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);

    $responseHeaders = substr($response, 0, $headerSize);
    $responseBody = substr($response, $headerSize);

    curl_close($ch);

    echo "HTTP Status: $httpCode\n";
    echo "\nResponse Headers:\n";
    echo $responseHeaders;
    echo "\nResponse Body Length: " . strlen($responseBody) . " bytes\n";
    echo "Response Body:\n";
    echo $responseBody;
    echo "\n\n";

    if (strlen($responseBody) === 0) {
        echo "⚠️ WARNING: Response body is EMPTY!\n\n";
    }

    echo "===========================================\n\n";
}

// Test 1: Simple test endpoint (should work)
testEndpoint('https://webtechsolution.my.id/kamuskorea/test_api.php');

// Test 2: Test the sendResponse function directly
testEndpoint('https://webtechsolution.my.id/kamuskorea/debug_response.php');

// Test 3: Test premium endpoint (will fail auth but should return JSON error)
testEndpoint('https://webtechsolution.my.id/kamuskorea/api.php/user/premium/status');

// Test 4: Test with fake token
testEndpoint(
    'https://webtechsolution.my.id/kamuskorea/api.php/user/premium/status',
    'GET',
    [
        'Authorization: Bearer fake_token_for_testing',
        'X-User-ID: test_user'
    ]
);

echo "\n===========================================\n";
echo "TESTS COMPLETED\n";
echo "===========================================\n";
