<?php
/**
 * Clear OPcache and reset
 * URL: https://webtechsolution.my.id/kamuskorea/clear_cache.php
 */

header("Content-Type: text/plain; charset=UTF-8");

echo "===========================================\n";
echo "CACHE CLEARING\n";
echo "===========================================\n\n";

// Clear OPcache
if (function_exists('opcache_reset')) {
    if (opcache_reset()) {
        echo "✅ OPcache cleared successfully\n";
    } else {
        echo "❌ Failed to clear OPcache\n";
    }
} else {
    echo "⚠️  OPcache not available\n";
}

// Clear realpath cache
clearstatcache(true);
echo "✅ Realpath cache cleared\n";

echo "\n";
echo "===========================================\n";
echo "DONE\n";
echo "===========================================\n\n";

echo "Now test the API again.\n";
echo "Delete this file after use for security.\n";
