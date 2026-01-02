<?php
/**
 * Show Recent PHP Errors
 * Upload ke kamuskorea/, akses via browser untuk lihat error log
 * URL: https://webtechsolution.my.id/kamuskorea/show_errors.php
 */

header("Content-Type: text/html; charset=UTF-8");

// Enable error display
ini_set('display_errors', 1);
error_reporting(E_ALL);

echo "<!DOCTYPE html>
<html>
<head>
    <title>PHP Error Log Viewer</title>
    <style>
        body { font-family: monospace; padding: 20px; background: #1e1e1e; color: #d4d4d4; }
        pre { background: #252526; padding: 15px; border: 1px solid #3e3e42; white-space: pre-wrap; word-wrap: break-word; }
        .error { color: #f48771; }
        .warning { color: #dcdcaa; }
        .success { color: #4ec9b0; }
        h2 { color: #569cd6; }
    </style>
</head>
<body>
    <h1>🔍 PHP Error Log Viewer</h1>
";

$errorLogPaths = [
    __DIR__ . '/error_log',
    __DIR__ . '/../error_log',
    __DIR__ . '/../../error_log',
    ini_get('error_log'),
    '/home/apsx2353/public_html/error_log',
    '/home/apsx2353/public_html/webtechsolution.my.id/error_log',
    '/home/apsx2353/public_html/webtechsolution.my.id/kamuskorea/error_log',
];

echo "<h2>📂 Searching for error logs...</h2>";
echo "<pre>";

$foundLogs = [];
foreach ($errorLogPaths as $path) {
    if ($path && file_exists($path)) {
        $foundLogs[] = $path;
        echo "<span class='success'>✅ Found: $path</span>\n";
    }
}

if (empty($foundLogs)) {
    echo "<span class='warning'>⚠️ No error log files found in common locations</span>\n";
    echo "\nTried:\n";
    foreach ($errorLogPaths as $path) {
        if ($path) echo "  - $path\n";
    }
} else {
    echo "\n<span class='success'>Found " . count($foundLogs) . " log file(s)</span>\n";
}

echo "</pre>";

// Show recent errors from each log
foreach ($foundLogs as $logPath) {
    echo "<h2>📄 " . basename(dirname($logPath)) . '/' . basename($logPath) . "</h2>";
    echo "<p style='color: #858585;'>Path: $logPath</p>";

    if (!is_readable($logPath)) {
        echo "<pre><span class='error'>❌ File not readable</span></pre>";
        continue;
    }

    $fileSize = filesize($logPath);
    echo "<p style='color: #858585;'>Size: " . number_format($fileSize) . " bytes</p>";

    if ($fileSize > 1000000) {
        echo "<p style='color: #dcdcaa;'>⚠️ Large file, showing last 100 lines only</p>";
    }

    // Read last N lines
    $lines = [];
    $fp = fopen($logPath, 'r');
    if ($fp) {
        // Read last 200KB or whole file if smaller
        $readSize = min($fileSize, 200000);
        fseek($fp, -$readSize, SEEK_END);
        $content = fread($fp, $readSize);
        fclose($fp);

        $allLines = explode("\n", $content);
        $lines = array_slice($allLines, -100); // Last 100 lines
    }

    echo "<pre>";
    if (empty($lines)) {
        echo "<span class='warning'>⚠️ No content or empty file</span>";
    } else {
        $lineCount = count($lines);
        echo "<span class='success'>Showing last $lineCount lines:</span>\n";
        echo str_repeat("=", 80) . "\n\n";

        foreach ($lines as $line) {
            if (empty(trim($line))) continue;

            // Highlight errors
            if (stripos($line, 'fatal error') !== false || stripos($line, 'parse error') !== false) {
                echo "<span class='error'>$line</span>\n";
            } elseif (stripos($line, 'warning') !== false) {
                echo "<span class='warning'>$line</span>\n";
            } else {
                echo htmlspecialchars($line) . "\n";
            }
        }
    }
    echo "</pre>";
}

// Show PHP configuration
echo "<h2>⚙️ PHP Configuration</h2>";
echo "<pre>";
echo "error_log location: " . (ini_get('error_log') ?: 'not set') . "\n";
echo "log_errors: " . (ini_get('log_errors') ? 'On' : 'Off') . "\n";
echo "display_errors: " . (ini_get('display_errors') ? 'On' : 'Off') . "\n";
echo "error_reporting: " . error_reporting() . "\n";
echo "</pre>";

// Show environment
echo "<h2>🌍 Environment Info</h2>";
echo "<pre>";
echo "PHP Version: " . PHP_VERSION . "\n";
echo "Current dir: " . __DIR__ . "\n";
echo "Server software: " . ($_SERVER['SERVER_SOFTWARE'] ?? 'unknown') . "\n";
echo "Document root: " . ($_SERVER['DOCUMENT_ROOT'] ?? 'unknown') . "\n";
echo "</pre>";

echo "
    <hr>
    <p><small>⚠️ <strong>IMPORTANT:</strong> Delete this file after checking for security!</small></p>
    <p><small>This file exposes error logs which may contain sensitive information.</small></p>
</body>
</html>
";
