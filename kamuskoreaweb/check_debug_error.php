<?php
/**
 * Check why debug_response.php is failing
 * URL: https://webtechsolution.my.id/kamuskorea/check_debug_error.php
 */

header("Content-Type: text/plain; charset=UTF-8");

echo "===========================================\n";
echo "CHECKING PHP ERROR LOG\n";
echo "===========================================\n\n";

$logFile = __DIR__ . '/php_error.log';

if (file_exists($logFile)) {
    echo "Log file found. Showing last 50 lines:\n";
    echo str_repeat("-", 60) . "\n";

    $lines = file($logFile);
    $lastLines = array_slice($lines, -50);

    foreach ($lastLines as $line) {
        echo $line;
    }

    echo "\n" . str_repeat("-", 60) . "\n";
} else {
    echo "No error log file found.\n";
}

echo "\n===========================================\n";
echo "Checking debug_response.php syntax:\n";
echo "===========================================\n\n";

$debugFile = __DIR__ . '/debug_response.php';

if (file_exists($debugFile)) {
    // Check for syntax errors
    exec("php -l " . escapeshellarg($debugFile), $output, $returnCode);

    if ($returnCode === 0) {
        echo "✅ Syntax check passed\n";
    } else {
        echo "❌ Syntax errors found:\n";
        echo implode("\n", $output);
    }
} else {
    echo "debug_response.php not found\n";
}
