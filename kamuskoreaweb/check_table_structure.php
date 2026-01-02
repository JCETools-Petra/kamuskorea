<?php
/**
 * Check assessment_results table structure
 * URL: https://webtechsolution.my.id/kamuskorea/check_table_structure.php
 */

header("Content-Type: text/plain; charset=UTF-8");

echo "===========================================\n";
echo "CHECKING TABLE STRUCTURE\n";
echo "===========================================\n\n";

// Load environment variables from .env file (same as api.php)
$envFile = __DIR__ . '/.env';
if (file_exists($envFile)) {
    $lines = file($envFile, FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES);
    foreach ($lines as $line) {
        if (strpos(trim($line), '#') === 0) continue;
        list($name, $value) = explode('=', $line, 2);
        $_ENV[trim($name)] = trim($value);
    }
}

// Database connection using .env
$host = $_ENV['DB_HOST'] ?? 'localhost';
$dbname = $_ENV['DB_NAME'] ?? $_ENV['DB_DATABASE'];
$username = $_ENV['DB_USER'] ?? $_ENV['DB_USERNAME'];
$password = $_ENV['DB_PASS'] ?? $_ENV['DB_PASSWORD'];

echo "Connecting to database...\n";
echo "Host: $host\n";
echo "Database: $dbname\n";
echo "User: $username\n\n";

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8mb4", $username, $password, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC
    ]);

    echo "Database connected successfully!\n\n";

    // Check assessment_results table structure
    echo "Table: assessment_results\n";
    echo str_repeat("-", 60) . "\n";

    $stmt = $pdo->query("DESCRIBE assessment_results");
    $columns = $stmt->fetchAll();

    echo "Columns:\n";
    foreach ($columns as $col) {
        echo "  - {$col['Field']} ({$col['Type']}) ";
        if ($col['Null'] === 'NO') echo "[NOT NULL] ";
        if ($col['Key'] === 'PRI') echo "[PRIMARY KEY] ";
        if ($col['Default'] !== null) echo "[DEFAULT: {$col['Default']}] ";
        echo "\n";
    }

    echo "\n" . str_repeat("=", 60) . "\n\n";

    // Check users table structure
    echo "Table: users\n";
    echo str_repeat("-", 60) . "\n";

    $stmt2 = $pdo->query("DESCRIBE users");
    $userCols = $stmt2->fetchAll();

    echo "Columns:\n";
    foreach ($userCols as $col) {
        echo "  - {$col['Field']} ({$col['Type']}) ";
        if ($col['Null'] === 'NO') echo "[NOT NULL] ";
        if ($col['Key'] === 'PRI') echo "[PRIMARY KEY] ";
        echo "\n";
    }

    echo "\n" . str_repeat("=", 60) . "\n\n";

    // Check assessments table structure
    echo "Table: assessments\n";
    echo str_repeat("-", 60) . "\n";

    $stmt3 = $pdo->query("DESCRIBE assessments");
    $assessCols = $stmt3->fetchAll();

    echo "Columns:\n";
    foreach ($assessCols as $col) {
        echo "  - {$col['Field']} ({$col['Type']}) ";
        if ($col['Null'] === 'NO') echo "[NOT NULL] ";
        if ($col['Key'] === 'PRI') echo "[PRIMARY KEY] ";
        echo "\n";
    }

    echo "\n" . str_repeat("=", 60) . "\n\n";

    // Sample data from assessment_results
    echo "Sample data from assessment_results (last 3):\n";
    echo str_repeat("-", 60) . "\n";

    $stmt4 = $pdo->query("SELECT * FROM assessment_results ORDER BY id DESC LIMIT 3");
    $samples = $stmt4->fetchAll();

    if (count($samples) > 0) {
        echo "Column names: " . implode(", ", array_keys($samples[0])) . "\n\n";
        foreach ($samples as $i => $sample) {
            echo "Record " . ($i+1) . ":\n";
            foreach ($sample as $key => $value) {
                echo "  $key: $value\n";
            }
            echo "\n";
        }
    } else {
        echo "No data in assessment_results table\n";
    }

} catch (PDOException $e) {
    echo "Database error: " . $e->getMessage() . "\n";
}
