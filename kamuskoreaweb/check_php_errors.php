<?php
/**
 * Check PHP error logs
 * URL: https://webtechsolution.my.id/kamuskorea/check_php_errors.php
 */

header("Content-Type: text/plain; charset=UTF-8");

echo "===========================================\n";
echo "PHP ERROR LOG CHECK\n";
echo "===========================================\n\n";

// Common error log locations
$possibleLogs = [
    __DIR__ . '/error_log',
    __DIR__ . '/../error_log',
    '/home/webtechsolution/public_html/error_log',
    '/home/webtechsolution/public_html/webtechsolution.my.id/error_log',
    '/home/webtechsolution/public_html/webtechsolution.my.id/kamuskorea/error_log',
    ini_get('error_log'),
];

echo "Searching for error logs...\n\n";

foreach ($possibleLogs as $logPath) {
    if (empty($logPath)) continue;

    echo "Checking: $logPath\n";

    if (file_exists($logPath)) {
        echo "✅ FOUND! Reading last 50 lines...\n";
        echo str_repeat("-", 60) . "\n";

        $lines = file($logPath, FILE_IGNORE_NEW_LINES);
        $lastLines = array_slice($lines, -50);

        foreach ($lastLines as $line) {
            echo $line . "\n";
        }

        echo str_repeat("-", 60) . "\n";
        echo "Total lines in log: " . count($lines) . "\n\n";
    } else {
        echo "❌ Not found\n\n";
    }
}

echo "\n===========================================\n";
echo "PHP Configuration:\n";
echo "===========================================\n";
echo "display_errors: " . ini_get('display_errors') . "\n";
echo "error_reporting: " . error_reporting() . "\n";
echo "log_errors: " . ini_get('log_errors') . "\n";
echo "error_log: " . ini_get('error_log') . "\n";
echo "output_buffering: " . ini_get('output_buffering') . "\n";
