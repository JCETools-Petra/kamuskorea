<?php
/**
 * Check if api.php has been updated with new DB variable support
 * URL: https://webtechsolution.my.id/kamuskorea/check_api_version.php
 */

header("Content-Type: text/plain; charset=UTF-8");

echo "===========================================\n";
echo "API.PHP VERSION CHECKER\n";
echo "===========================================\n\n";

$apiPath = __DIR__ . '/api.php';

if (!file_exists($apiPath)) {
    echo "❌ api.php not found at: $apiPath\n";
    exit;
}

echo "✅ api.php found\n";
echo "Path: $apiPath\n";
echo "Size: " . filesize($apiPath) . " bytes\n";
echo "Modified: " . date('Y-m-d H:i:s', filemtime($apiPath)) . "\n\n";

// Read api.php and check for updated DB variable code
$content = file_get_contents($apiPath);

echo "Checking for updated database connection code...\n\n";

// Check for the new fallback syntax
if (strpos($content, "\$_ENV['DB_USER'] ?? \$_ENV['DB_USERNAME']") !== false) {
    echo "✅ UPDATED VERSION DETECTED!\n";
    echo "   api.php supports both DB_USER and DB_USERNAME\n\n";
    $isUpdated = true;
} elseif (strpos($content, "\$_ENV['DB_USERNAME']") !== false) {
    echo "⚠️  OLD VERSION DETECTED!\n";
    echo "   api.php only supports DB_USERNAME (old format)\n";
    echo "   Needs to be updated to support both formats\n\n";
    $isUpdated = false;
} else {
    echo "❓ UNKNOWN - Cannot determine version\n\n";
    $isUpdated = false;
}

// Check .env file
echo "===========================================\n";
echo "Checking .env file...\n";
echo "===========================================\n\n";

$envPath = __DIR__ . '/../.env';
if (file_exists($envPath)) {
    echo "✅ .env found\n";
    echo "Path: $envPath\n";
    echo "Size: " . filesize($envPath) . " bytes\n\n";

    $envContent = file_get_contents($envPath);

    // Check for database variables
    echo "Database configuration in .env:\n";

    $foundVars = [];

    if (preg_match('/^DB_HOST\s*=\s*(.+)$/m', $envContent, $m)) {
        $foundVars['DB_HOST'] = trim($m[1]);
    }
    if (preg_match('/^DB_USER\s*=\s*(.+)$/m', $envContent, $m)) {
        $foundVars['DB_USER'] = trim($m[1]);
    }
    if (preg_match('/^DB_USERNAME\s*=\s*(.+)$/m', $envContent, $m)) {
        $foundVars['DB_USERNAME'] = trim($m[1]);
    }
    if (preg_match('/^DB_PASS\s*=\s*(.+)$/m', $envContent, $m)) {
        $foundVars['DB_PASS'] = trim($m[1]);
    }
    if (preg_match('/^DB_PASSWORD\s*=\s*(.+)$/m', $envContent, $m)) {
        $foundVars['DB_PASSWORD'] = trim($m[1]);
    }
    if (preg_match('/^DB_NAME\s*=\s*(.+)$/m', $envContent, $m)) {
        $foundVars['DB_NAME'] = trim($m[1]);
    }
    if (preg_match('/^DB_DATABASE\s*=\s*(.+)$/m', $envContent, $m)) {
        $foundVars['DB_DATABASE'] = trim($m[1]);
    }
    if (preg_match('/^APP_ENV\s*=\s*(.+)$/m', $envContent, $m)) {
        $foundVars['APP_ENV'] = trim($m[1]);
    }

    foreach ($foundVars as $key => $value) {
        // Check if password has quotes
        if (($key === 'DB_PASS' || $key === 'DB_PASSWORD')) {
            if (preg_match("/^['\"](.+)['\"]$/", $value, $m)) {
                echo "  ⚠️  $key = $value (HAS QUOTES - REMOVE THEM!)\n";
            } else {
                echo "  ✅ $key = $value\n";
            }
        } elseif ($key === 'DB_USER' || $key === 'DB_USERNAME') {
            echo "  ✅ $key = $value\n";
        } else {
            echo "  ✅ $key = $value\n";
        }
    }

    echo "\n";

    // Check compatibility
    echo "===========================================\n";
    echo "COMPATIBILITY CHECK\n";
    echo "===========================================\n\n";

    $hasDbUser = isset($foundVars['DB_USER']);
    $hasDbUsername = isset($foundVars['DB_USERNAME']);
    $hasDbPass = isset($foundVars['DB_PASS']);
    $hasDbPassword = isset($foundVars['DB_PASSWORD']);
    $hasDbName = isset($foundVars['DB_NAME']);
    $hasDbDatabase = isset($foundVars['DB_DATABASE']);

    if ($isUpdated) {
        echo "API Version: ✅ Updated (supports both formats)\n\n";

        if ($hasDbUser || $hasDbUsername) {
            echo "✅ User variable found\n";
        } else {
            echo "❌ Missing DB_USER or DB_USERNAME\n";
        }

        if ($hasDbPass || $hasDbPassword) {
            echo "✅ Password variable found\n";
        } else {
            echo "❌ Missing DB_PASS or DB_PASSWORD\n";
        }

        if ($hasDbName || $hasDbDatabase) {
            echo "✅ Database variable found\n";
        } else {
            echo "❌ Missing DB_NAME or DB_DATABASE\n";
        }
    } else {
        echo "API Version: ⚠️  Old (only supports DB_USERNAME format)\n\n";

        if ($hasDbUsername) {
            echo "✅ DB_USERNAME found (compatible)\n";
        } else {
            echo "❌ Missing DB_USERNAME (required for old api.php)\n";
        }

        if ($hasDbPassword) {
            echo "✅ DB_PASSWORD found (compatible)\n";
        } else {
            echo "❌ Missing DB_PASSWORD (required for old api.php)\n";
        }

        if ($hasDbDatabase) {
            echo "✅ DB_DATABASE found (compatible)\n";
        } else {
            echo "❌ Missing DB_DATABASE (required for old api.php)\n";
        }
    }

} else {
    echo "❌ .env not found at: $envPath\n";
}

echo "\n===========================================\n";
echo "SUMMARY\n";
echo "===========================================\n\n";

if ($isUpdated && (($hasDbUser || $hasDbUsername) && ($hasDbPass || $hasDbPassword) && ($hasDbName || $hasDbDatabase))) {
    echo "✅ Configuration looks good!\n\n";
    echo "If you still get database connection error:\n";
    echo "1. Check if password has quotes - remove them\n";
    echo "2. Verify database credentials in cPanel\n";
    echo "3. Check PHP error log for detailed error\n";
} else {
    echo "⚠️  Configuration issues found!\n\n";
    echo "Recommendations:\n";
    if (!$isUpdated) {
        echo "1. Upload the updated api.php to server\n";
    }
    echo "2. Ensure .env has correct database variables\n";
    echo "3. Remove quotes from password in .env\n";
}

echo "\n===========================================\n";
