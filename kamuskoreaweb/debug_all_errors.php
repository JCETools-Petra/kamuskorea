<?php
/**
 * Comprehensive Error & Request Logger for api.php
 * URL: https://webtechsolution.my.id/kamuskorea/debug_all_errors.php
 *
 * Instructions:
 * 1. Upload this file to server
 * 2. Add require_once at TOP of api.php (after <?php):
 *    require_once __DIR__ . '/debug_all_errors.php';
 * 3. Test from Android app
 * 4. View logs: https://webtechsolution.my.id/kamuskorea/debug_all_errors.php?view=logs
 */

define('DEBUG_LOG_FILE', __DIR__ . '/debug_requests.log');

// Custom error handler - catches ALL errors
set_error_handler(function($errno, $errstr, $errfile, $errline) {
    $logMsg = sprintf(
        "[%s] PHP ERROR %d: %s in %s:%d\n",
        date('Y-m-d H:i:s'),
        $errno,
        $errstr,
        $errfile,
        $errline
    );
    error_log($logMsg);
    file_put_contents(DEBUG_LOG_FILE, $logMsg, FILE_APPEND);
    return false; // Let PHP handle it too
});

// Custom exception handler - catches uncaught exceptions
set_exception_handler(function($exception) {
    $logMsg = sprintf(
        "[%s] UNCAUGHT EXCEPTION: %s in %s:%d\nStack trace:\n%s\n",
        date('Y-m-d H:i:s'),
        $exception->getMessage(),
        $exception->getFile(),
        $exception->getLine(),
        $exception->getTraceAsString()
    );
    error_log($logMsg);
    file_put_contents(DEBUG_LOG_FILE, $logMsg, FILE_APPEND);

    // Send error response
    http_response_code(500);
    header('Content-Type: application/json; charset=UTF-8');
    echo json_encode([
        'success' => false,
        'message' => 'Server error: ' . $exception->getMessage()
    ]);
    exit;
});

// Shutdown handler - catches fatal errors
register_shutdown_function(function() {
    $error = error_get_last();
    if ($error && in_array($error['type'], [E_ERROR, E_PARSE, E_CORE_ERROR, E_COMPILE_ERROR])) {
        $logMsg = sprintf(
            "[%s] FATAL ERROR: %s in %s:%d\n",
            date('Y-m-d H:i:s'),
            $error['message'],
            $error['file'],
            $error['line']
        );
        error_log($logMsg);
        file_put_contents(DEBUG_LOG_FILE, $logMsg, FILE_APPEND);

        // Try to send error response
        if (!headers_sent()) {
            http_response_code(500);
            header('Content-Type: application/json; charset=UTF-8');
            echo json_encode([
                'success' => false,
                'message' => 'Fatal error occurred'
            ]);
        }
    }
});

// Log incoming request
if (basename($_SERVER['SCRIPT_NAME']) === 'api.php') {
    $requestLog = sprintf(
        "\n========== [%s] ==========\n" .
        "Method: %s\n" .
        "URI: %s\n" .
        "Headers:\n%s\n" .
        "Body: %s\n",
        date('Y-m-d H:i:s'),
        $_SERVER['REQUEST_METHOD'],
        $_SERVER['REQUEST_URI'],
        json_encode(getallheaders(), JSON_PRETTY_PRINT),
        file_get_contents('php://input') ?: '(empty)'
    );
    file_put_contents(DEBUG_LOG_FILE, $requestLog, FILE_APPEND);
}

// OUTPUT BUFFER CAPTURE - This catches what gets sent to browser
ob_start(function($buffer) {
    $responseLog = sprintf(
        "[%s] RESPONSE BODY (%d bytes):\n%s\n========== END ==========\n\n",
        date('Y-m-d H:i:s'),
        strlen($buffer),
        $buffer ?: '(EMPTY RESPONSE - THIS IS THE PROBLEM!)'
    );
    file_put_contents(DEBUG_LOG_FILE, $responseLog, FILE_APPEND);

    // If buffer is empty and we're in api.php, log it as ERROR
    if (empty($buffer) && basename($_SERVER['SCRIPT_NAME']) === 'api.php') {
        $errorMsg = "[" . date('Y-m-d H:i:s') . "] ❌❌❌ EMPTY RESPONSE DETECTED! Check why sendResponse() was not called!\n";
        error_log($errorMsg);
        file_put_contents(DEBUG_LOG_FILE, $errorMsg, FILE_APPEND);
    }

    return $buffer;
});

// ============================================
// VIEW LOGS (access this script directly)
// ============================================
if (basename($_SERVER['SCRIPT_NAME']) === 'debug_all_errors.php' && isset($_GET['view'])) {
    header("Content-Type: text/plain; charset=UTF-8");

    if ($_GET['view'] === 'logs') {
        if (file_exists(DEBUG_LOG_FILE)) {
            // Show last 100 lines
            $lines = file(DEBUG_LOG_FILE);
            echo "Showing last 100 lines of debug log:\n\n";
            echo "===========================================\n\n";
            echo implode('', array_slice($lines, -100));
        } else {
            echo "No debug log file found yet.\n";
        }
        exit;
    }

    if ($_GET['view'] === 'clear') {
        if (file_exists(DEBUG_LOG_FILE)) {
            unlink(DEBUG_LOG_FILE);
            echo "Debug log cleared!\n";
        } else {
            echo "No log file to clear.\n";
        }
        exit;
    }
}

// If accessed directly without params, show instructions
if (basename($_SERVER['SCRIPT_NAME']) === 'debug_all_errors.php') {
    ?>
<!DOCTYPE html>
<html>
<head>
    <title>Debug Error Logger</title>
    <style>
        body { font-family: monospace; padding: 20px; background: #1e1e1e; color: #d4d4d4; }
        h1 { color: #4ec9b0; }
        .box { background: #252526; padding: 15px; margin: 10px 0; border-left: 3px solid #007acc; }
        a { color: #4fc3f7; text-decoration: none; }
        a:hover { text-decoration: underline; }
        code { background: #1e1e1e; padding: 2px 6px; color: #ce9178; }
        .warning { color: #ff9800; }
        .success { color: #4caf50; }
    </style>
</head>
<body>
    <h1>🔍 Comprehensive Debug Error Logger</h1>

    <div class="box">
        <h3>📋 Setup Instructions:</h3>
        <ol>
            <li>Add this line at the <strong>TOP</strong> of <code>api.php</code> (right after <code>&lt;?php</code>):<br>
                <code>require_once __DIR__ . '/debug_all_errors.php';</code>
            </li>
            <li>Save and upload <code>api.php</code></li>
            <li>Test from Android app (upload profile picture, etc.)</li>
            <li>View logs below</li>
        </ol>
    </div>

    <div class="box">
        <h3>🔗 Actions:</h3>
        <ul>
            <li><a href="?view=logs" target="_blank">📄 View Debug Logs (Last 100 lines)</a></li>
            <li><a href="?view=clear">🗑️ Clear Debug Logs</a></li>
            <li><a href="check_php_errors.php" target="_blank">📋 View PHP Error Log</a></li>
        </ul>
    </div>

    <div class="box warning">
        <h3>⚠️ What This Does:</h3>
        <ul>
            <li>Catches ALL PHP errors, warnings, and exceptions</li>
            <li>Logs every incoming request (method, URI, headers, body)</li>
            <li>Captures response body (even if empty!)</li>
            <li>Detects empty responses and logs them as errors</li>
            <li>Saves everything to <code>debug_requests.log</code></li>
        </ul>
    </div>

    <div class="box success">
        <h3>✅ Current Status:</h3>
        <p>Log file: <?php echo file_exists(DEBUG_LOG_FILE) ? '<span class="success">EXISTS (' . filesize(DEBUG_LOG_FILE) . ' bytes)</span>' : '<span class="warning">NOT CREATED YET</span>'; ?></p>
        <p>Error handler: <span class="success">ACTIVE</span></p>
        <p>Output buffer: <span class="success">ACTIVE</span></p>
    </div>
</body>
</html>
    <?php
    exit;
}
