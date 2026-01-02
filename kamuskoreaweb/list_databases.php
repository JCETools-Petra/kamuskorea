<?php
/**
 * List Available Databases
 * Shows all databases accessible to current user
 * URL: https://webtechsolution.my.id/kamuskorea/list_databases.php
 */

header("Content-Type: text/plain; charset=UTF-8");

echo "===========================================\n";
echo "DATABASE DISCOVERY\n";
echo "===========================================\n\n";

// Try to get user from system
$systemUser = get_current_user();
echo "System user: $systemUser\n\n";

// Common database patterns for cPanel
$possibleDbNames = [
    "${systemUser}_kamuskorea",
    "${systemUser}_korea",
    "${systemUser}_learning",
    "${systemUser}_learningkorea",
    "kamuskorea",
    "learning_korea",
];

echo "Possible database names to try:\n";
foreach ($possibleDbNames as $dbName) {
    echo "  - $dbName\n";
}

echo "\n";
echo "Possible database users to try:\n";
echo "  - ${systemUser}_kamsuser\n";
echo "  - ${systemUser}_user\n";
echo "  - ${systemUser}_admin\n";
echo "  - $systemUser\n";

echo "\n===========================================\n";
echo "HOW TO FIND EXACT CREDENTIALS:\n";
echo "===========================================\n\n";

echo "1. Login to cPanel\n";
echo "2. Search for 'MySQL Databases' or 'Databases'\n";
echo "3. You'll see:\n";
echo "   - Current Databases (list of database names)\n";
echo "   - Current Users (list of database users)\n";
echo "4. Note down the database name and user\n";
echo "5. If you forgot password:\n";
echo "   - Click 'Change Password' next to the user\n";
echo "   - Set a new password\n\n";

echo "===========================================\n";
echo "ALTERNATIVE: CHECK EXISTING CONFIG FILES\n";
echo "===========================================\n\n";

// Look for existing config files that might have DB credentials
$configFiles = [
    __DIR__ . '/../config.php',
    __DIR__ . '/config.php',
    __DIR__ . '/../wp-config.php',
    __DIR__ . '/wp-config.php',
];

$foundConfigs = [];
foreach ($configFiles as $file) {
    if (file_exists($file)) {
        $foundConfigs[] = $file;
    }
}

if (!empty($foundConfigs)) {
    echo "Found existing config files:\n";
    foreach ($foundConfigs as $file) {
        echo "\n📄 $file\n";
        echo "   Check this file for DB credentials\n";

        // Try to read DB credentials safely
        $content = file_get_contents($file);
        if (preg_match("/DB_NAME['\"]?\s*[=:,]\s*['\"]?([^'\";\s]+)/i", $content, $matches)) {
            echo "   Possible DB_NAME: {$matches[1]}\n";
        }
        if (preg_match("/DB_USER['\"]?\s*[=:,]\s*['\"]?([^'\";\s]+)/i", $content, $matches)) {
            echo "   Possible DB_USER: {$matches[1]}\n";
        }
    }
} else {
    echo "No existing config files found.\n";
    echo "You need to get credentials from cPanel.\n";
}

echo "\n===========================================\n";
