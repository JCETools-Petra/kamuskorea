<?php
/**
 * Test API Initialization
 * Test database connection and Firebase initialization
 * URL: https://webtechsolution.my.id/kamuskorea/test_init.php
 */

// Enable error display
ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);

header("Content-Type: text/plain; charset=UTF-8");

echo "===========================================\n";
echo "API INITIALIZATION TEST\n";
echo "===========================================\n\n";

$errors = [];
$warnings = [];

// 1. Test vendor/autoload
echo "1. Testing vendor/autoload.php\n";
$vendorPath = __DIR__ . '/vendor/autoload.php';
echo "   Path: $vendorPath\n";
if (file_exists($vendorPath)) {
    echo "   ✅ File exists\n";
    try {
        require_once $vendorPath;
        echo "   ✅ Loaded successfully\n";
    } catch (Exception $e) {
        echo "   ❌ Failed to load: " . $e->getMessage() . "\n";
        $errors[] = "Vendor autoload failed";
    }
} else {
    echo "   ❌ File not found\n";
    $errors[] = "vendor/autoload.php missing - run 'composer install'";
}
echo "\n";

// 2. Test .env loading
echo "2. Testing .env file\n";
$envPath = __DIR__ . '/../.env';
echo "   Path: $envPath\n";
if (file_exists($envPath)) {
    echo "   ✅ File exists\n";
    try {
        $dotenv = \Dotenv\Dotenv::createImmutable(__DIR__ . '/..');
        $dotenv->load();
        echo "   ✅ Loaded successfully\n";

        // Check APP_ENV
        $appEnv = $_ENV['APP_ENV'] ?? getenv('APP_ENV') ?: 'not set';
        echo "   APP_ENV: $appEnv\n";

        if ($appEnv === 'development') {
            echo "   ✅ Development mode enabled\n";
        } else {
            echo "   ⚠️  Not in development mode\n";
            $warnings[] = "APP_ENV is not 'development'";
        }

    } catch (Exception $e) {
        echo "   ❌ Failed to load: " . $e->getMessage() . "\n";
        $errors[] = ".env loading failed";
    }
} else {
    echo "   ❌ File not found\n";
    $errors[] = ".env file missing";
}
echo "\n";

// 3. Test Firebase service account
echo "3. Testing firebase-service-account.json\n";
$firebasePath = __DIR__ . '/../firebase-service-account.json';
echo "   Path: $firebasePath\n";
if (file_exists($firebasePath)) {
    echo "   ✅ File exists\n";
    echo "   Size: " . filesize($firebasePath) . " bytes\n";

    if (is_readable($firebasePath)) {
        echo "   ✅ File is readable\n";

        $json = file_get_contents($firebasePath);
        $data = json_decode($json, true);

        if ($data) {
            echo "   ✅ Valid JSON format\n";
            $projectId = $data['project_id'] ?? 'unknown';
            echo "   Project ID: $projectId\n";

            if ($projectId === 'learning-korea') {
                echo "   ✅ Correct project (learning-korea)\n";
            } else {
                echo "   ⚠️  Wrong project: $projectId (expected: learning-korea)\n";
                $warnings[] = "Firebase service account project mismatch";
            }
        } else {
            echo "   ❌ Invalid JSON format\n";
            $errors[] = "firebase-service-account.json is not valid JSON";
        }
    } else {
        echo "   ❌ File not readable\n";
        $errors[] = "firebase-service-account.json not readable";
    }
} else {
    echo "   ❌ File not found\n";
    $errors[] = "firebase-service-account.json missing";
}
echo "\n";

// 4. Test Firebase initialization
echo "4. Testing Firebase Admin SDK initialization\n";
try {
    if (!class_exists('\Kreait\Firebase\Factory')) {
        throw new Exception("Firebase Factory class not found - Composer dependencies missing?");
    }

    $firebase = (new \Kreait\Firebase\Factory)
        ->withServiceAccount($firebasePath);

    echo "   ✅ Factory created\n";

    $firebaseAuth = $firebase->createAuth();
    echo "   ✅ Auth service initialized\n";

    $firebaseAppCheck = $firebase->createAppCheck();
    echo "   ✅ App Check service initialized\n";

} catch (Exception $e) {
    echo "   ❌ Failed: " . $e->getMessage() . "\n";
    echo "   Error type: " . get_class($e) . "\n";
    $errors[] = "Firebase Admin SDK initialization failed";
}
echo "\n";

// 5. Test Database connection
echo "5. Testing Database connection\n";
try {
    $dbHost = $_ENV['DB_HOST'] ?? getenv('DB_HOST') ?? 'localhost';
    $dbName = $_ENV['DB_NAME'] ?? getenv('DB_NAME') ?? 'unknown';
    $dbUser = $_ENV['DB_USER'] ?? getenv('DB_USER') ?? 'unknown';

    echo "   Host: $dbHost\n";
    echo "   Database: $dbName\n";
    echo "   User: $dbUser\n";

    $dbPass = $_ENV['DB_PASS'] ?? getenv('DB_PASS') ?? '';

    $pdo = new PDO(
        "mysql:host=$dbHost;dbname=$dbName;charset=utf8mb4",
        $dbUser,
        $dbPass,
        [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        ]
    );

    echo "   ✅ Database connected\n";

    // Test users table
    $stmt = $pdo->query("SELECT COUNT(*) as count FROM users");
    $result = $stmt->fetch();
    echo "   ✅ Users table accessible (count: {$result['count']})\n";

} catch (PDOException $e) {
    echo "   ❌ Database error: " . $e->getMessage() . "\n";
    $errors[] = "Database connection failed";
} catch (Exception $e) {
    echo "   ❌ Error: " . $e->getMessage() . "\n";
    $errors[] = "Database configuration error";
}
echo "\n";

// Summary
echo "===========================================\n";
echo "SUMMARY\n";
echo "===========================================\n\n";

if (empty($errors) && empty($warnings)) {
    echo "✅ ALL TESTS PASSED!\n\n";
    echo "API initialization is working correctly.\n";
    echo "The 500 error might be caused by:\n";
    echo "- App Check token verification (even in dev mode)\n";
    echo "- Specific endpoint logic error\n";
    echo "- Check show_errors.php for actual runtime errors\n";
} else {
    if (!empty($errors)) {
        echo "❌ ERRORS FOUND:\n";
        foreach ($errors as $error) {
            echo "   - $error\n";
        }
        echo "\n";
    }

    if (!empty($warnings)) {
        echo "⚠️  WARNINGS:\n";
        foreach ($warnings as $warning) {
            echo "   - $warning\n";
        }
        echo "\n";
    }

    echo "FIX THESE ISSUES FIRST!\n";
}

echo "\n===========================================\n";
