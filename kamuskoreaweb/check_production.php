<?php
/**
 * Production Environment Checker
 * Upload file ini ke server production untuk cek konfigurasi
 */

header("Content-Type: text/plain; charset=UTF-8");

echo "===========================================\n";
echo "PRODUCTION ENVIRONMENT CHECKER\n";
echo "===========================================\n\n";

// Check PHP version
echo "1. PHP Version: " . PHP_VERSION . "\n\n";

// Check current directory
echo "2. Current Directory: " . __DIR__ . "\n\n";

// Check vendor/autoload.php
$vendorPath = __DIR__ . '/../vendor/autoload.php';
echo "3. Checking vendor/autoload.php\n";
echo "   Path: $vendorPath\n";
echo "   Exists: " . (file_exists($vendorPath) ? "✅ YES" : "❌ NO") . "\n";
if (file_exists($vendorPath)) {
    echo "   Size: " . filesize($vendorPath) . " bytes\n";
}
echo "\n";

// Check .env
$envPath = __DIR__ . '/../../.env';
echo "4. Checking .env file\n";
echo "   Path: $envPath\n";
echo "   Exists: " . (file_exists($envPath) ? "✅ YES" : "❌ NO") . "\n";
if (file_exists($envPath)) {
    echo "   Size: " . filesize($envPath) . " bytes\n";
    echo "   Readable: " . (is_readable($envPath) ? "✅ YES" : "❌ NO") . "\n";
}
echo "\n";

// Check firebase-service-account.json
$firebasePath = __DIR__ . '/../../firebase-service-account.json';
echo "5. Checking firebase-service-account.json\n";
echo "   Path: $firebasePath\n";
echo "   Exists: " . (file_exists($firebasePath) ? "✅ YES" : "❌ NO") . "\n";
if (file_exists($firebasePath)) {
    echo "   Size: " . filesize($firebasePath) . " bytes\n";
    echo "   Readable: " . (is_readable($firebasePath) ? "✅ YES" : "❌ NO") . "\n";
}
echo "\n";

// Try to load vendor
echo "6. Testing vendor/autoload.php\n";
try {
    if (file_exists($vendorPath)) {
        require_once $vendorPath;
        echo "   Status: ✅ Loaded successfully\n";

        // Check if key classes exist
        echo "   - Dotenv\\Dotenv: " . (class_exists('Dotenv\\Dotenv') ? "✅" : "❌") . "\n";
        echo "   - PHPMailer\\PHPMailer\\PHPMailer: " . (class_exists('PHPMailer\\PHPMailer\\PHPMailer') ? "✅" : "❌") . "\n";
        echo "   - Kreait\\Firebase\\Factory: " . (class_exists('Kreait\\Firebase\\Factory') ? "✅" : "❌") . "\n";
    } else {
        echo "   Status: ❌ File not found\n";
    }
} catch (Exception $e) {
    echo "   Status: ❌ Error: " . $e->getMessage() . "\n";
}
echo "\n";

// Try to load .env
echo "7. Testing .env loading\n";
try {
    if (file_exists($vendorPath) && file_exists($envPath)) {
        require_once $vendorPath;
        $dotenv = Dotenv\Dotenv::createImmutable(__DIR__ . '/../..');
        $dotenv->load();
        echo "   Status: ✅ Loaded successfully\n";
        echo "   - DB_HOST: " . (getenv('DB_HOST') ? "✅ Set" : "❌ Not set") . "\n";
        echo "   - DB_DATABASE: " . (getenv('DB_DATABASE') ? "✅ Set" : "❌ Not set") . "\n";
        echo "   - APP_ENV: " . (getenv('APP_ENV') ?: 'production (default)') . "\n";
    } else {
        echo "   Status: ❌ Required files not found\n";
    }
} catch (Exception $e) {
    echo "   Status: ❌ Error: " . $e->getMessage() . "\n";
}
echo "\n";

// Check error log
$errorLogPath = __DIR__ . '/php_error.log';
echo "8. Error Log\n";
echo "   Path: $errorLogPath\n";
if (file_exists($errorLogPath)) {
    echo "   Exists: ✅ YES\n";
    echo "   Size: " . filesize($errorLogPath) . " bytes\n";
    echo "\n   Last 10 lines:\n";
    echo "   " . str_repeat("-", 70) . "\n";
    $lines = file($errorLogPath);
    $lastLines = array_slice($lines, -10);
    foreach ($lastLines as $line) {
        echo "   " . trim($line) . "\n";
    }
} else {
    echo "   Exists: ❌ NO (No errors logged yet)\n";
}
echo "\n";

echo "===========================================\n";
echo "DIAGNOSIS:\n";
echo "===========================================\n";

$issues = [];
if (!file_exists($vendorPath)) {
    $issues[] = "❌ vendor/autoload.php NOT FOUND - Run 'composer install'";
}
if (!file_exists($envPath)) {
    $issues[] = "❌ .env NOT FOUND - Create .env file";
}
if (!file_exists($firebasePath)) {
    $issues[] = "❌ firebase-service-account.json NOT FOUND - Upload from Firebase Console";
}

if (empty($issues)) {
    echo "✅ All files found! If still getting 500 error, check error log above.\n";
} else {
    foreach ($issues as $issue) {
        echo $issue . "\n";
    }
}

echo "\n";
echo "Upload this file to: /kamuskorea/kamuskoreaweb/check_production.php\n";
echo "Access via: https://webtechsolution.my.id/kamuskorea/kamuskoreaweb/check_production.php\n";
?>
