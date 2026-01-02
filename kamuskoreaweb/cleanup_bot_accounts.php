<?php
/**
 * Identify and Clean Up Bot Accounts
 * URL: https://webtechsolution.my.id/kamuskorea/cleanup_bot_accounts.php
 *
 * DANGER: This script can DELETE user accounts!
 * Use ?mode=preview first to see what will be deleted
 * Use ?mode=delete&confirm=yes to actually delete
 */

header("Content-Type: text/plain; charset=UTF-8");

// Load environment variables
$envFile = __DIR__ . '/.env';
if (file_exists($envFile)) {
    $lines = file($envFile, FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES);
    foreach ($lines as $line) {
        if (strpos(trim($line), '#') === 0) continue;
        list($name, $value) = explode('=', $line, 2);
        $_ENV[trim($name)] = trim($value);
    }
}

$host = $_ENV['DB_HOST'] ?? 'localhost';
$dbname = $_ENV['DB_NAME'] ?? $_ENV['DB_DATABASE'];
$username = $_ENV['DB_USER'] ?? $_ENV['DB_USERNAME'];
$password = $_ENV['DB_PASS'] ?? $_ENV['DB_PASSWORD'];

$mode = $_GET['mode'] ?? 'preview';
$confirm = $_GET['confirm'] ?? 'no';

echo "===========================================\n";
echo "BOT ACCOUNT CLEANUP TOOL\n";
echo "===========================================\n\n";

echo "Mode: " . strtoupper($mode) . "\n";
if ($mode === 'delete' && $confirm === 'yes') {
    echo "⚠️  DELETE MODE ACTIVE - ACCOUNTS WILL BE DELETED!\n";
} else {
    echo "Preview mode - no changes will be made\n";
}
echo "\n";

try {
    $pdo = new PDO("mysql:host=$host;dbname=$dbname;charset=utf8mb4", $username, $password, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC
    ]);

    // Define bot detection patterns
    $botPatterns = [
        '%@gmail.com',  // Will filter further with name pattern below
    ];

    echo "Searching for potential bot accounts...\n";
    echo str_repeat("-", 60) . "\n\n";

    // Query users with suspicious patterns
    $stmt = $pdo->query("
        SELECT id, firebase_uid, email, name, auth_type, created_at, updated_at
        FROM users
        WHERE email LIKE '%@gmail.com'
        AND name NOT LIKE '%petra%'
        AND name NOT LIKE '%josh%'
        AND name NOT LIKE '%sayyid%'
        AND name NOT LIKE '%abdullah%'
        AND name NOT LIKE '%irvin%'
        ORDER BY created_at DESC
    ");

    $allUsers = $stmt->fetchAll();
    $botAccounts = [];

    // Filter by bot name pattern: firstname+lastname.numbers@gmail.com
    foreach ($allUsers as $user) {
        $email = $user['email'];
        $name = $user['name'];

        // Bot pattern detection:
        // 1. Email matches: word.digits@gmail.com or wordword.digits@gmail.com
        // 2. Name is short or generic
        if (preg_match('/^[a-z]+\.?\d{5}@gmail\.com$/i', $email) ||
            preg_match('/^[a-z]+[a-z]+\.\d{5}@gmail\.com$/i', $email)) {
            $botAccounts[] = $user;
        }
    }

    echo "Found " . count($botAccounts) . " potential bot accounts:\n\n";

    if (count($botAccounts) === 0) {
        echo "✅ No bot accounts found!\n";
        exit;
    }

    // Display bot accounts
    foreach ($botAccounts as $i => $bot) {
        echo ($i + 1) . ". Email: {$bot['email']}\n";
        echo "   Name: {$bot['name']}\n";
        echo "   ID: {$bot['id']}, Firebase UID: {$bot['firebase_uid']}\n";
        echo "   Auth: {$bot['auth_type']}, Created: {$bot['created_at']}\n";
        echo "\n";
    }

    echo str_repeat("=", 60) . "\n";

    // Delete mode
    if ($mode === 'delete' && $confirm === 'yes') {
        echo "\n⚠️  DELETING " . count($botAccounts) . " BOT ACCOUNTS...\n\n";

        $deletedCount = 0;
        $deleteStmt = $pdo->prepare("DELETE FROM users WHERE id = ?");

        foreach ($botAccounts as $bot) {
            try {
                $deleteStmt->execute([$bot['id']]);
                $deletedCount++;
                echo "✅ Deleted: {$bot['email']} (ID: {$bot['id']})\n";
            } catch (Exception $e) {
                echo "❌ Failed to delete {$bot['email']}: {$e->getMessage()}\n";
            }
        }

        echo "\n" . str_repeat("=", 60) . "\n";
        echo "SUMMARY: Deleted $deletedCount out of " . count($botAccounts) . " accounts\n";
        echo "\n⚠️  NOTE: These accounts are still in Firebase Authentication!\n";
        echo "You need to manually delete them from Firebase Console:\n";
        echo "https://console.firebase.google.com → Authentication → Users\n";

    } else {
        echo "\nTo DELETE these accounts:\n";
        echo "1. Access: " . $_SERVER['REQUEST_SCHEME'] . "://" . $_SERVER['HTTP_HOST'] . $_SERVER['SCRIPT_NAME'] . "?mode=delete&confirm=yes\n";
        echo "2. Then manually delete from Firebase Console\n";
        echo "\nTo just preview again:\n";
        echo "Access: " . $_SERVER['REQUEST_SCHEME'] . "://" . $_SERVER['HTTP_HOST'] . $_SERVER['SCRIPT_NAME'] . "?mode=preview\n";
    }

} catch (PDOException $e) {
    echo "Database error: " . $e->getMessage() . "\n";
}
