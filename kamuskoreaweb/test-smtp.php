<?php
error_reporting(E_ALL);
ini_set('display_errors', 1);

echo "<h1>Environment Check</h1>";

// Check vendor
$vendorPath = __DIR__ . '/../vendor/autoload.php';
echo "<p>Vendor path: $vendorPath</p>";
echo "<p>Vendor exists: " . (file_exists($vendorPath) ? 'YES' : 'NO') . "</p>";

if (file_exists($vendorPath)) {
    require_once $vendorPath;
    echo "<p>✅ Vendor loaded successfully</p>";
} else {
    echo "<p>❌ Vendor not found</p>";
    exit();
}

// Check .env
$envPath = __DIR__ . '/..';
echo "<p>.env path: {$envPath}/.env</p>";
echo "<p>.env exists: " . (file_exists($envPath . '/.env') ? 'YES' : 'NO') . "</p>";

if (file_exists($envPath . '/.env')) {
    use Dotenv\Dotenv;
    $dotenv = Dotenv::createImmutable($envPath);
    $dotenv->load();
    echo "<p>✅ .env loaded successfully</p>";
    
    echo "<h2>Environment Variables</h2>";
    echo "<pre>";
    echo "DB_HOST: " . ($_ENV['DB_HOST'] ?? 'NOT SET') . "\n";
    echo "DB_USERNAME: " . ($_ENV['DB_USERNAME'] ?? 'NOT SET') . "\n";
    echo "DB_DATABASE: " . ($_ENV['DB_DATABASE'] ?? 'NOT SET') . "\n";
    echo "SMTP_HOST: " . ($_ENV['SMTP_HOST'] ?? 'NOT SET') . "\n";
    echo "SMTP_USER: " . ($_ENV['SMTP_USER'] ?? 'NOT SET') . "\n";
    echo "</pre>";
} else {
    echo "<p>❌ .env not found</p>";
}

// Check PHPMailer
if (class_exists('PHPMailer\PHPMailer\PHPMailer')) {
    echo "<p>✅ PHPMailer class exists</p>";
} else {
    echo "<p>❌ PHPMailer class not found</p>";
}

// Check database connection
try {
    $pdo = new PDO(
        "mysql:host={$_ENV['DB_HOST']};dbname={$_ENV['DB_DATABASE']};charset=utf8mb4",
        $_ENV['DB_USERNAME'],
        $_ENV['DB_PASSWORD']
    );
    echo "<p>✅ Database connection successful</p>";
} catch (PDOException $e) {
    echo "<p>❌ Database connection failed: " . $e->getMessage() . "</p>";
}
?>