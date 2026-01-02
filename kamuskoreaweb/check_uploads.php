<?php
/**
 * Upload Directory Checker
 * Upload file ini ke folder kamuskorea/ di server, lalu akses via browser:
 * https://webtechsolution.my.id/kamuskorea/check_uploads.php
 */

header("Content-Type: text/plain; charset=UTF-8");

echo "===========================================\n";
echo "UPLOAD DIRECTORY CHECKER\n";
echo "===========================================\n\n";

// Check uploads directory
$uploadsDir = __DIR__ . '/uploads/';
echo "1. Checking uploads/ directory\n";
echo "   Path: $uploadsDir\n";
echo "   Exists: " . (is_dir($uploadsDir) ? "✅ YES" : "❌ NO") . "\n";
if (is_dir($uploadsDir)) {
    echo "   Writable: " . (is_writable($uploadsDir) ? "✅ YES" : "❌ NO") . "\n";
    echo "   Permissions: " . substr(sprintf('%o', fileperms($uploadsDir)), -4) . "\n";
} else {
    echo "   ⚠️  Creating directory...\n";
    if (mkdir($uploadsDir, 0755, true)) {
        echo "   ✅ Directory created successfully\n";
    } else {
        echo "   ❌ Failed to create directory\n";
    }
}
echo "\n";

// Check profiles subdirectory
$profilesDir = __DIR__ . '/uploads/profiles/';
echo "2. Checking uploads/profiles/ directory\n";
echo "   Path: $profilesDir\n";
echo "   Exists: " . (is_dir($profilesDir) ? "✅ YES" : "❌ NO") . "\n";
if (is_dir($profilesDir)) {
    echo "   Writable: " . (is_writable($profilesDir) ? "✅ YES" : "❌ NO") . "\n";
    echo "   Permissions: " . substr(sprintf('%o', fileperms($profilesDir)), -4) . "\n";
} else {
    echo "   ⚠️  Creating directory...\n";
    if (mkdir($profilesDir, 0755, true)) {
        echo "   ✅ Directory created successfully\n";
    } else {
        echo "   ❌ Failed to create directory\n";
    }
}
echo "\n";

// Test write
$testFile = $profilesDir . 'test_write.txt';
echo "3. Testing write permission\n";
echo "   Test file: $testFile\n";
if (is_writable($profilesDir)) {
    if (file_put_contents($testFile, 'Test write')) {
        echo "   ✅ Write test successful\n";
        @unlink($testFile);
        echo "   ✅ File deleted successfully\n";
    } else {
        echo "   ❌ Write test failed\n";
    }
} else {
    echo "   ❌ Directory not writable\n";
}
echo "\n";

// Check PHP upload settings
echo "4. PHP Upload Settings\n";
echo "   upload_max_filesize: " . ini_get('upload_max_filesize') . "\n";
echo "   post_max_size: " . ini_get('post_max_size') . "\n";
echo "   memory_limit: " . ini_get('memory_limit') . "\n";
echo "   max_execution_time: " . ini_get('max_execution_time') . "s\n";
echo "\n";

// Check GD extension
echo "5. Image Processing Extensions\n";
echo "   GD Extension: " . (extension_loaded('gd') ? "✅ YES" : "❌ NO") . "\n";
echo "   Imagick Extension: " . (extension_loaded('imagick') ? "✅ YES" : "❌ NO") . "\n";
echo "\n";

echo "===========================================\n";
echo "SUMMARY\n";
echo "===========================================\n\n";

$issues = [];

if (!is_dir($uploadsDir)) {
    $issues[] = "❌ uploads/ directory doesn't exist";
}
if (!is_writable($uploadsDir)) {
    $issues[] = "❌ uploads/ directory is not writable";
}
if (!is_dir($profilesDir)) {
    $issues[] = "❌ uploads/profiles/ directory doesn't exist";
}
if (!is_writable($profilesDir)) {
    $issues[] = "❌ uploads/profiles/ directory is not writable";
}

if (empty($issues)) {
    echo "✅ ALL CHECKS PASSED!\n";
    echo "   Upload directory is ready.\n\n";
    echo "Next steps:\n";
    echo "1. Delete this file for security: check_uploads.php\n";
    echo "2. Test profile picture upload from Android app\n";
} else {
    echo "⚠️  ISSUES FOUND:\n\n";
    foreach ($issues as $issue) {
        echo "   $issue\n";
    }
    echo "\n";
    echo "RECOMMENDED ACTIONS:\n";
    echo "1. Create missing directories via cPanel File Manager\n";
    echo "2. Set permissions to 755 or 775 for uploads directories\n";
    echo "3. Contact hosting support if permission issues persist\n";
}

echo "\n";
echo "===========================================\n";
