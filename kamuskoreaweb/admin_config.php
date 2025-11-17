<?php
/**
 * Admin Configuration
 * Konfigurasi untuk panel admin quiz & exam
 * Login menggunakan database MySQL dari .env
 */

// Session settings
ini_set('session.cookie_httponly', 1);
ini_set('session.use_only_cookies', 1);
ini_set('session.cookie_secure', isset($_SERVER['HTTPS']));

session_start();

/**
 * Get database connection
 */
function getAdminDB() {
    static $pdo = null;

    if ($pdo === null) {
        require_once __DIR__ . '/vendor/autoload.php';

        $dotenv = Dotenv\Dotenv::createImmutable(__DIR__ . '/..');
        $dotenv->load();

        $host = $_ENV['DB_HOST'];
        $username = $_ENV['DB_USERNAME'];
        $password = $_ENV['DB_PASSWORD'];
        $database = $_ENV['DB_DATABASE'];

        try {
            $pdo = new PDO(
                "mysql:host=$host;dbname=$database;charset=utf8mb4",
                $username,
                $password,
                [
                    PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
                    PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
                    PDO::ATTR_EMULATE_PREPARES => false
                ]
            );
        } catch (PDOException $e) {
            die('Database connection failed: ' . $e->getMessage());
        }
    }

    return $pdo;
}

/**
 * Check if admin is logged in
 */
function isAdminLoggedIn() {
    return isset($_SESSION['admin_logged_in']) && $_SESSION['admin_logged_in'] === true;
}

/**
 * Require admin login
 */
function requireAdminLogin() {
    if (!isAdminLoggedIn()) {
        header('Location: admin_login.php');
        exit();
    }
}

/**
 * Login admin using database
 */
function adminLogin($username, $password) {
    $logFile = __DIR__ . '/admin_login.log';

    try {
        writeAdminLog($logFile, "=== LOGIN ATTEMPT ===");
        writeAdminLog($logFile, "Username: " . $username);
        writeAdminLog($logFile, "Password length: " . strlen($password));

        $pdo = getAdminDB();
        writeAdminLog($logFile, "Database connection: SUCCESS");

        $stmt = $pdo->prepare("
            SELECT id, username, password, full_name, is_active
            FROM admin_users
            WHERE username = ? AND is_active = 1
        ");
        $stmt->execute([$username]);
        $admin = $stmt->fetch();

        if (!$admin) {
            writeAdminLog($logFile, "User not found in database or not active");
            return false;
        }

        writeAdminLog($logFile, "User found: ID=" . $admin['id'] . ", Full Name=" . $admin['full_name']);
        writeAdminLog($logFile, "Stored password hash: " . substr($admin['password'], 0, 20) . "...");

        $passwordMatch = password_verify($password, $admin['password']);
        writeAdminLog($logFile, "Password verify result: " . ($passwordMatch ? "MATCH" : "NO MATCH"));

        if ($passwordMatch) {
            // Update last login
            $updateStmt = $pdo->prepare("UPDATE admin_users SET last_login = NOW() WHERE id = ?");
            $updateStmt->execute([$admin['id']]);

            // Set session
            $_SESSION['admin_logged_in'] = true;
            $_SESSION['admin_id'] = $admin['id'];
            $_SESSION['admin_username'] = $admin['username'];
            $_SESSION['admin_fullname'] = $admin['full_name'];
            $_SESSION['admin_login_time'] = time();

            writeAdminLog($logFile, "Login SUCCESS - Session created");
            return true;
        }

        writeAdminLog($logFile, "Login FAILED - Password mismatch");
        return false;
    } catch (Exception $e) {
        writeAdminLog($logFile, "EXCEPTION: " . $e->getMessage());
        writeAdminLog($logFile, "Stack trace: " . $e->getTraceAsString());
        error_log("Admin login error: " . $e->getMessage());
        return false;
    }
}

/**
 * Write to admin log file
 */
function writeAdminLog($logFile, $message) {
    $timestamp = date('Y-m-d H:i:s');
    $logMessage = "[$timestamp] $message\n";
    file_put_contents($logFile, $logMessage, FILE_APPEND | LOCK_EX);
}

/**
 * Logout admin
 */
function adminLogout() {
    session_destroy();
    header('Location: admin_login.php');
    exit();
}

/**
 * Change admin password
 */
function changeAdminPassword($adminId, $newPassword) {
    try {
        $pdo = getAdminDB();
        $hashedPassword = password_hash($newPassword, PASSWORD_DEFAULT);

        $stmt = $pdo->prepare("UPDATE admin_users SET password = ? WHERE id = ?");
        $stmt->execute([$hashedPassword, $adminId]);

        return true;
    } catch (Exception $e) {
        error_log("Change password error: " . $e->getMessage());
        return false;
    }
}

/**
 * Get current admin info
 */
function getCurrentAdmin() {
    if (!isAdminLoggedIn()) {
        return null;
    }

    return [
        'id' => $_SESSION['admin_id'] ?? null,
        'username' => $_SESSION['admin_username'] ?? null,
        'fullname' => $_SESSION['admin_fullname'] ?? null,
        'login_time' => $_SESSION['admin_login_time'] ?? null
    ];
}
?>
