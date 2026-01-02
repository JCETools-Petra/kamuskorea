<?php
/**
 * Check if specific email exists in users table
 * URL: https://webtechsolution.my.id/kamuskorea/check_user_email.php
 */

header("Content-Type: text/plain; charset=UTF-8");

echo "===========================================\n";
echo "CHECK USER EMAIL IN DATABASE\n";
echo "===========================================\n\n";

// Load environment variables from .env file
$envFile = __DIR__ . '/.env';
if (file_exists($envFile)) {
    $lines = file($envFile, FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES);
    foreach ($lines as $line) {
        if (strpos(trim($line), '#') === 0) continue;
        list($name, $value) = explode('=', $line, 2);
        $_ENV[trim($name)] = trim($value);
    }
}

// Database connection
$host = $_ENV['DB_HOST'] ?? 'localhost';
$dbname = $_ENV['DB_NAME'] ?? $_ENV['DB_DATABASE'];
$username = $_ENV['DB_USER'] ?? $_ENV['DB_USERNAME'];
$password = $_ENV['DB_PASS'] ?? $_ENV['DB_PASSWORD'];

$testEmail = 'bellhoteldev@gmail.com';

echo "Testing email: $testEmail\n\n";

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8mb4", $username, $password, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC
    ]);

    echo "Database connected successfully!\n\n";

    // Check exact match
    echo "Test 1: Exact match search\n";
    echo str_repeat("-", 60) . "\n";

    $stmt = $pdo->prepare("SELECT firebase_uid, email, name, auth_type, created_at FROM users WHERE email = ?");
    $stmt->execute([$testEmail]);
    $user = $stmt->fetch();

    if ($user) {
        echo "✅ FOUND! User details:\n";
        foreach ($user as $key => $value) {
            echo "  $key: $value\n";
        }
    } else {
        echo "❌ NOT FOUND with exact match\n";
    }

    echo "\n" . str_repeat("=", 60) . "\n\n";

    // Check all users with similar email
    echo "Test 2: Search for similar emails\n";
    echo str_repeat("-", 60) . "\n";

    $stmt2 = $pdo->prepare("SELECT firebase_uid, email, name, auth_type FROM users WHERE email LIKE ?");
    $stmt2->execute(['%bellhotel%']);
    $users = $stmt2->fetchAll();

    if (count($users) > 0) {
        echo "Found " . count($users) . " user(s) with 'bellhotel' in email:\n";
        foreach ($users as $u) {
            echo "  - Email: '{$u['email']}' (length: " . strlen($u['email']) . ")\n";
            echo "    Name: {$u['name']}\n";
            echo "    Auth Type: {$u['auth_type']}\n";
            echo "    Firebase UID: {$u['firebase_uid']}\n\n";
        }
    } else {
        echo "❌ NO users found with 'bellhotel' in email\n";
    }

    echo "\n" . str_repeat("=", 60) . "\n\n";

    // Show last 5 registered users
    echo "Test 3: Last 5 registered users\n";
    echo str_repeat("-", 60) . "\n";

    $stmt3 = $pdo->query("SELECT firebase_uid, email, name, auth_type, created_at FROM users ORDER BY created_at DESC LIMIT 5");
    $recent = $stmt3->fetchAll();

    echo "Recent registrations:\n";
    foreach ($recent as $r) {
        echo "  - {$r['email']} ({$r['name']}) - {$r['auth_type']} - {$r['created_at']}\n";
    }

    echo "\n" . str_repeat("=", 60) . "\n\n";

    // Count total users
    $stmt4 = $pdo->query("SELECT COUNT(*) as total FROM users");
    $total = $stmt4->fetch();
    echo "Total users in database: {$total['total']}\n";

} catch (PDOException $e) {
    echo "Database error: " . $e->getMessage() . "\n";
}
