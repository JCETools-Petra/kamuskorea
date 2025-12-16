<?php
/**
 * Script Test untuk Debugging API
 * Upload file ini ke folder yang sama dengan api.php
 * Akses via: https://webtechsolution.my.id/kamuskorea/test.php
 */

header("Content-Type: application/json");

$tests = [];

// Test 1: PHP Version
$tests['php_version'] = [
    'status' => version_compare(PHP_VERSION, '7.4', '>=') ? 'OK' : 'FAILED',
    'value' => PHP_VERSION,
    'required' => '7.4+',
    'message' => version_compare(PHP_VERSION, '7.4', '>=') ? 'PHP version compatible' : 'PHP version too old'
];

// Test 2: Required Extensions
$required_extensions = ['mysqli', 'curl', 'json', 'mbstring', 'openssl'];
$missing_extensions = [];
foreach ($required_extensions as $ext) {
    if (!extension_loaded($ext)) {
        $missing_extensions[] = $ext;
    }
}
$tests['php_extensions'] = [
    'status' => empty($missing_extensions) ? 'OK' : 'FAILED',
    'required' => $required_extensions,
    'missing' => $missing_extensions,
    'message' => empty($missing_extensions) ? 'All required extensions available' : 'Some extensions missing'
];

// Test 3: Composer Autoloader
$tests['composer'] = [
    'status' => file_exists('vendor/autoload.php') ? 'OK' : 'FAILED',
    'message' => file_exists('vendor/autoload.php') ? 'Composer vendor folder exists' : 'Composer vendor folder NOT found - run composer install'
];

// Test 4: Config File
$tests['config'] = [
    'status' => file_exists('config.php') ? 'OK' : 'FAILED',
    'message' => file_exists('config.php') ? 'Config file exists' : 'Config file NOT found'
];

// Test 5: Database Connection
if (file_exists('config.php')) {
    require_once 'config.php';
    $conn = @new mysqli($db_host, $db_user, $db_pass, $db_name);
    $tests['database'] = [
        'status' => !$conn->connect_error ? 'OK' : 'FAILED',
        'message' => !$conn->connect_error ? 'Database connection successful' : 'Database connection failed: ' . $conn->connect_error,
        'host' => $db_host,
        'database' => $db_name
    ];
    if (!$conn->connect_error) {
        $conn->close();
    }
} else {
    $tests['database'] = [
        'status' => 'SKIPPED',
        'message' => 'Cannot test database - config.php not found'
    ];
}

// Test 6: Authorization Header
$headers = getallheaders();
$tests['authorization_header'] = [
    'status' => isset($headers['Authorization']) || isset($headers['authorization']) ? 'OK' : 'WARNING',
    'message' => isset($headers['Authorization']) || isset($headers['authorization']) ? 
        'Authorization header received' : 
        'No Authorization header (send request with header to test)',
    'all_headers' => array_keys($headers)
];

// Test 7: Writable Directories
$writable_dirs = ['uploads/profile_pictures/'];
$writable_tests = [];
foreach ($writable_dirs as $dir) {
    if (!file_exists($dir)) {
        $created = @mkdir($dir, 0775, true);
        $writable_tests[$dir] = [
            'exists' => $created,
            'writable' => $created ? is_writable($dir) : false
        ];
    } else {
        $writable_tests[$dir] = [
            'exists' => true,
            'writable' => is_writable($dir)
        ];
    }
}
$all_writable = true;
foreach ($writable_tests as $result) {
    if (!$result['writable']) {
        $all_writable = false;
        break;
    }
}
$tests['writable_directories'] = [
    'status' => $all_writable ? 'OK' : 'WARNING',
    'message' => $all_writable ? 'All directories writable' : 'Some directories not writable',
    'details' => $writable_tests
];

// Test 8: .htaccess
$tests['htaccess'] = [
    'status' => file_exists('.htaccess') ? 'OK' : 'WARNING',
    'message' => file_exists('.htaccess') ? 
        '.htaccess file exists' : 
        '.htaccess file NOT found - Authorization header may not work',
    'mod_rewrite' => function_exists('apache_get_modules') ? 
        (in_array('mod_rewrite', apache_get_modules()) ? 'enabled' : 'unknown') : 
        'unknown (cannot check)'
];

// Test 9: Firebase JWT Library
$tests['firebase_jwt'] = [
    'status' => class_exists('\Firebase\JWT\JWT') ? 'OK' : 'FAILED',
    'message' => class_exists('\Firebase\JWT\JWT') ? 
        'Firebase JWT library available' : 
        'Firebase JWT library NOT found - run: composer require firebase/php-jwt'
];

// Test 10: Google API Client
$tests['google_api'] = [
    'status' => class_exists('Google_Client') ? 'OK' : 'WARNING',
    'message' => class_exists('Google_Client') ? 
        'Google API Client library available' : 
        'Google API Client library NOT found (only needed for subscription verification)'
];

// Test 11: Public Keys Cache
$tests['public_keys_cache'] = [
    'status' => file_exists('google_public_keys.json') ? 'OK' : 'INFO',
    'message' => file_exists('google_public_keys.json') ? 
        'Public keys cache exists' : 
        'Public keys cache not created yet (will be created on first request)',
    'file_size' => file_exists('google_public_keys.json') ? filesize('google_public_keys.json') : 0,
    'last_modified' => file_exists('google_public_keys.json') ? 
        date('Y-m-d H:i:s', filemtime('google_public_keys.json')) : null
];

// Test 12: Google Public Keys Connectivity
$ch = curl_init();
curl_setopt($ch, CURLOPT_URL, 'https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com');
curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
curl_setopt($ch, CURLOPT_TIMEOUT, 10);
curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, true);
$response = @curl_exec($ch);
$httpcode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

$tests['google_connectivity'] = [
    'status' => $httpcode === 200 ? 'OK' : 'FAILED',
    'message' => $httpcode === 200 ? 
        'Can fetch Google public keys' : 
        'Cannot fetch Google public keys - HTTP code: ' . $httpcode,
    'http_code' => $httpcode
];

// Summary
$total_tests = count($tests);
$passed = 0;
$failed = 0;
$warnings = 0;

foreach ($tests as $test) {
    switch ($test['status']) {
        case 'OK':
            $passed++;
            break;
        case 'FAILED':
            $failed++;
            break;
        case 'WARNING':
            $warnings++;
            break;
    }
}

$summary = [
    'total_tests' => $total_tests,
    'passed' => $passed,
    'failed' => $failed,
    'warnings' => $warnings,
    'overall_status' => $failed > 0 ? 'CRITICAL ISSUES' : ($warnings > 0 ? 'MINOR ISSUES' : 'ALL GOOD')
];

// Output
echo json_encode([
    'summary' => $summary,
    'tests' => $tests,
    'server_info' => [
        'php_version' => PHP_VERSION,
        'server_software' => $_SERVER['SERVER_SOFTWARE'] ?? 'Unknown',
        'document_root' => $_SERVER['DOCUMENT_ROOT'] ?? 'Unknown'
    ],
    'recommendations' => [
        $failed > 0 ? 'Fix failed tests before deploying to production' : null,
        !file_exists('.htaccess') ? 'Upload .htaccess file to ensure Authorization header works' : null,
        !file_exists('vendor/autoload.php') ? 'Run: composer install' : null,
        $warnings > 0 ? 'Review warnings - they may cause issues' : null
    ]
], JSON_PRETTY_PRINT);
?>