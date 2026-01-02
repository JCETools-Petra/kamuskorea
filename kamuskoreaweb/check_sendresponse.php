<?php
/**
 * Check sendResponse function in api.php
 * URL: https://webtechsolution.my.id/kamuskorea/check_sendresponse.php
 */

header("Content-Type: text/plain; charset=UTF-8");

echo "===========================================\n";
echo "CHECKING sendResponse FUNCTION\n";
echo "===========================================\n\n";

$apiFile = __DIR__ . '/api.php';

if (!file_exists($apiFile)) {
    die("ERROR: api.php not found!\n");
}

$content = file_get_contents($apiFile);

// Find sendResponse function with better pattern that handles nested braces
$lines = explode("\n", $content);
$functionStart = -1;
$functionEnd = -1;
$braceCount = 0;
$inFunction = false;

foreach ($lines as $lineNum => $line) {
    if (strpos($line, 'function sendResponse') !== false) {
        $functionStart = $lineNum;
        $inFunction = true;
    }

    if ($inFunction) {
        $braceCount += substr_count($line, '{') - substr_count($line, '}');

        if ($braceCount === 0 && strpos($line, '}') !== false) {
            $functionEnd = $lineNum;
            break;
        }
    }
}

if ($functionStart === -1) {
    echo "ERROR: sendResponse function not found!\n";
} else {
    $functionLines = array_slice($lines, $functionStart, $functionEnd - $functionStart + 1);
    $functionCode = implode("\n", $functionLines);

    echo "sendResponse function found (lines " . ($functionStart + 1) . "-" . ($functionEnd + 1) . "):\n";
    echo str_repeat("-", 60) . "\n";
    // Show first 15 lines only for brevity
    echo implode("\n", array_slice($functionLines, 0, 15));
    if (count($functionLines) > 15) {
        echo "\n... (" . (count($functionLines) - 15) . " more lines)\n";
    }
    echo "\n" . str_repeat("-", 60) . "\n\n";

    // Check for Content-Type header
    if (strpos($functionCode, "Content-Type") !== false) {
        preg_match('/header\([\'"]Content-Type.*?\)/', $functionCode, $ctMatch);
        echo "✅ Content-Type header FOUND: " . ($ctMatch[0] ?? '') . "\n";
    } else {
        echo "❌ Content-Type header NOT FOUND\n";
    }

    // Check for Content-Length header
    if (strpos($functionCode, "Content-Length") !== false) {
        preg_match('/header\([\'"]Content-Length.*?\)/', $functionCode, $clMatch);
        echo "✅ Content-Length header FOUND: " . ($clMatch[0] ?? '') . "\n";
    } else {
        echo "❌ Content-Length header NOT FOUND\n";
    }
}

echo "\n===========================================\n";
echo "File Info:\n";
echo "===========================================\n";
echo "Last modified: " . date('Y-m-d H:i:s', filemtime($apiFile)) . "\n";
echo "File size: " . filesize($apiFile) . " bytes\n";
