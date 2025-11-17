<?php
/**
 * ENV Checker Script
 * Upload file ini ke /home/apsx2353/public_html/webtechsolution.my.id/kamuskorea/
 * Akses via browser: https://webtechsolution.my.id/kamuskorea/check_env.php
 * 
 * Script ini akan membantu debug masalah .env
 */

header('Content-Type: text/html; charset=utf-8');
echo "<h1>🔍 ENV File Checker</h1>";
echo "<pre>";

$envPath = '/home/apsx2353/public_html/webtechsolution.my.id/.env';

echo "=== CHECKING ENV FILE ===\n";
echo "Expected path: $envPath\n\n";

// 1. Check if file exists
if (file_exists($envPath)) {
    echo "✅ File exists!\n\n";
    
    // 2. Check file permissions
    echo "File permissions: " . substr(sprintf('%o', fileperms($envPath)), -4) . "\n";
    echo "File size: " . filesize($envPath) . " bytes\n\n";
    
    // 3. Read and display file contents
    echo "=== FILE CONTENTS ===\n";
    $contents = file_get_contents($envPath);
    echo htmlspecialchars($contents);
    echo "\n\n";
    
    // 4. Parse .env
    echo "=== PARSED VALUES ===\n";
    $lines = file($envPath, FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES);
    $env = [];
    
    foreach ($lines as $lineNum => $line) {
        $line = trim($line);
        
        // Skip comments
        if (empty($line) || strpos($line, '#') === 0) {
            echo "Line " . ($lineNum + 1) . ": [SKIPPED - Comment or empty]\n";
            continue;
        }
        
        // Parse KEY=VALUE
        if (strpos($line, '=') !== false) {
            list($key, $value) = explode('=', $line, 2);
            $key = trim($key);
            $value = trim($value);
            
            // Remove quotes
            $value = trim($value, '"\'');
            
            $env[$key] = $value;
            
            // Show parsed values (hide password)
            if ($key === 'DB_PASSWORD') {
                echo "Line " . ($lineNum + 1) . ": $key = ***HIDDEN*** (length: " . strlen($value) . ")\n";
            } else {
                echo "Line " . ($lineNum + 1) . ": $key = $value\n";
            }
        } else {
            echo "Line " . ($lineNum + 1) . ": [INVALID FORMAT] $line\n";
        }
    }
    
    echo "\n=== ENVIRONMENT VARIABLES ===\n";
    echo "DB_HOST: " . ($env['DB_HOST'] ?? '❌ NOT FOUND') . "\n";
    echo "DB_DATABASE: " . ($env['DB_DATABASE'] ?? '❌ NOT FOUND') . "\n";
    echo "DB_NAME: " . ($env['DB_NAME'] ?? '❌ NOT FOUND') . "\n";
    echo "DB_USERNAME: " . ($env['DB_USERNAME'] ?? '❌ NOT FOUND') . "\n";
    echo "DB_PASSWORD: " . (isset($env['DB_PASSWORD']) ? '✅ SET (length: ' . strlen($env['DB_PASSWORD']) . ')' : '❌ NOT FOUND') . "\n";
    
    // 5. Test database connection
    echo "\n=== TESTING DATABASE CONNECTION ===\n";
    
    // Support both DB_DATABASE and DB_NAME
    $dbname = $env['DB_DATABASE'] ?? $env['DB_NAME'] ?? null;
    
    if (isset($env['DB_HOST']) && $dbname && isset($env['DB_USERNAME']) && isset($env['DB_PASSWORD'])) {
        try {
            $conn = new mysqli(
                $env['DB_HOST'],
                $env['DB_USERNAME'],
                $env['DB_PASSWORD'],
                $dbname
            );
            
            if ($conn->connect_error) {
                echo "❌ Connection FAILED: " . $conn->connect_error . "\n";
                echo "\nPossible issues:\n";
                echo "- Username atau password salah\n";
                echo "- Database tidak ada\n";
                echo "- User tidak punya akses ke database\n";
                echo "\nCek di cPanel -> MySQL® Databases\n";
            } else {
                echo "✅ Connection SUCCESS!\n";
                echo "Database: " . $dbname . "\n";
                echo "Username: " . $env['DB_USERNAME'] . "\n";
                
                // Check tables
                echo "\n=== CHECKING TABLES ===\n";
                $tables = ['users', 'premium_users', 'kamus', 'assessment_categories', 'assessments', 'questions', 'assessment_results'];
                
                foreach ($tables as $table) {
                    $result = $conn->query("SHOW TABLES LIKE '$table'");
                    if ($result && $result->num_rows > 0) {
                        echo "✅ Table '$table' exists\n";
                    } else {
                        echo "❌ Table '$table' NOT FOUND\n";
                    }
                }
                
                $conn->close();
            }
        } catch (Exception $e) {
            echo "❌ Exception: " . $e->getMessage() . "\n";
        }
    } else {
        echo "❌ Cannot test connection - missing credentials in .env\n";
    }
    
} else {
    echo "❌ File NOT found!\n\n";
    echo "Please create .env file at: $envPath\n\n";
    echo "Example .env content:\n";
    echo "DB_HOST=localhost\n";
    echo "DB_DATABASE=apsx2353_webtech_api\n";
    echo "DB_USERNAME=apsx2353_webtech_api\n";
    echo "DB_PASSWORD=your_password_here\n";
}

echo "\n=== DONE ===\n";
echo "</pre>";

echo "<hr>";
echo "<p><strong>⚠️ PENTING:</strong> Hapus file ini setelah selesai debugging untuk keamanan!</p>";
?>