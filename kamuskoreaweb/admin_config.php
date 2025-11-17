<?php
/**
 * Admin Configuration
 * Konfigurasi untuk panel admin quiz & exam
 * Login menggunakan database MySQL dari .env
 * SECURED: Anti brute force, CSRF protection, session security
 */

// Session settings - SECURE
ini_set('session.cookie_httponly', 1);
ini_set('session.use_only_cookies', 1);
ini_set('session.cookie_secure', isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on');
ini_set('session.cookie_samesite', 'Strict');
ini_set('session.use_strict_mode', 1);
ini_set('session.gc_maxlifetime', 3600); // 1 hour

session_start();

// Regenerate session ID to prevent session fixation
if (!isset($_SESSION['initiated'])) {
    session_regenerate_id(true);
    $_SESSION['initiated'] = true;
}

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
            error_log("Database connection failed: " . $e->getMessage());
            die('Database connection failed');
        }
    }

    return $pdo;
}

/**
 * Check if admin is logged in
 */
function isAdminLoggedIn() {
    if (!isset($_SESSION['admin_logged_in']) || $_SESSION['admin_logged_in'] !== true) {
        return false;
    }

    // Check session timeout (1 hour)
    if (isset($_SESSION['admin_login_time']) && (time() - $_SESSION['admin_login_time'] > 3600)) {
        adminLogout();
        return false;
    }

    // Verify IP hasn't changed (session hijacking protection)
    if (isset($_SESSION['admin_ip']) && $_SESSION['admin_ip'] !== $_SERVER['REMOTE_ADDR']) {
        adminLogout();
        return false;
    }

    // Verify user agent hasn't changed
    if (isset($_SESSION['admin_user_agent']) && $_SESSION['admin_user_agent'] !== $_SERVER['HTTP_USER_AGENT']) {
        adminLogout();
        return false;
    }

    return true;
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
 * Generate CSRF token
 */
function generateCSRFToken() {
    if (empty($_SESSION['csrf_token'])) {
        $_SESSION['csrf_token'] = bin2hex(random_bytes(32));
    }
    return $_SESSION['csrf_token'];
}

/**
 * Verify CSRF token
 */
function verifyCSRFToken($token) {
    return isset($_SESSION['csrf_token']) && hash_equals($_SESSION['csrf_token'], $token);
}

/**
 * Check if IP is blocked due to too many failed attempts
 */
function isIPBlocked($ip) {
    $pdo = getAdminDB();

    // Clean old attempts (older than 30 minutes)
    $stmt = $pdo->prepare("DELETE FROM admin_login_attempts WHERE attempt_time < DATE_SUB(NOW(), INTERVAL 30 MINUTE)");
    $stmt->execute();

    // Count recent failed attempts
    $stmt = $pdo->prepare("SELECT COUNT(*) as attempts FROM admin_login_attempts WHERE ip_address = ? AND attempt_time > DATE_SUB(NOW(), INTERVAL 30 MINUTE)");
    $stmt->execute([$ip]);
    $result = $stmt->fetch();

    return $result['attempts'] >= 5; // Block after 5 failed attempts
}

/**
 * Record failed login attempt
 */
function recordFailedAttempt($ip, $username) {
    try {
        $pdo = getAdminDB();
        $stmt = $pdo->prepare("INSERT INTO admin_login_attempts (ip_address, username, attempt_time) VALUES (?, ?, NOW())");
        $stmt->execute([$ip, $username]);
    } catch (Exception $e) {
        // Table might not exist, log error but don't break login
        error_log("Failed to record login attempt: " . $e->getMessage());
    }
}

/**
 * Clear failed attempts after successful login
 */
function clearFailedAttempts($ip) {
    try {
        $pdo = getAdminDB();
        $stmt = $pdo->prepare("DELETE FROM admin_login_attempts WHERE ip_address = ?");
        $stmt->execute([$ip]);
    } catch (Exception $e) {
        error_log("Failed to clear login attempts: " . $e->getMessage());
    }
}

/**
 * Login admin using database - SECURED
 */
function adminLogin($username, $password) {
    $ip = $_SERVER['REMOTE_ADDR'];

    // Check for brute force
    if (isIPBlocked($ip)) {
        error_log("Blocked login attempt from IP: $ip (too many failed attempts)");
        return ['success' => false, 'message' => 'Terlalu banyak percobaan gagal. Coba lagi dalam 30 menit.'];
    }

    // Sanitize username (alphanumeric only)
    $username = preg_replace('/[^a-zA-Z0-9_]/', '', $username);

    if (empty($username) || empty($password)) {
        return ['success' => false, 'message' => 'Username dan password harus diisi.'];
    }

    if (strlen($username) > 50 || strlen($password) > 255) {
        return ['success' => false, 'message' => 'Input tidak valid.'];
    }

    try {
        $pdo = getAdminDB();

        // Use prepared statement to prevent SQL injection
        $stmt = $pdo->prepare("
            SELECT id, username, password, full_name, is_active
            FROM admin_users
            WHERE username = ? AND is_active = 1
            LIMIT 1
        ");
        $stmt->execute([$username]);
        $admin = $stmt->fetch();

        if (!$admin) {
            recordFailedAttempt($ip, $username);
            // Generic error message to prevent username enumeration
            return ['success' => false, 'message' => 'Username atau password salah.'];
        }

        // Verify password using timing-safe comparison
        if (!password_verify($password, $admin['password'])) {
            recordFailedAttempt($ip, $username);
            return ['success' => false, 'message' => 'Username atau password salah.'];
        }

        // Successful login
        clearFailedAttempts($ip);

        // Update last login
        $updateStmt = $pdo->prepare("UPDATE admin_users SET last_login = NOW() WHERE id = ?");
        $updateStmt->execute([$admin['id']]);

        // Regenerate session ID to prevent session fixation
        session_regenerate_id(true);

        // Set session variables
        $_SESSION['admin_logged_in'] = true;
        $_SESSION['admin_id'] = $admin['id'];
        $_SESSION['admin_username'] = $admin['username'];
        $_SESSION['admin_fullname'] = $admin['full_name'];
        $_SESSION['admin_login_time'] = time();
        $_SESSION['admin_ip'] = $ip;
        $_SESSION['admin_user_agent'] = $_SERVER['HTTP_USER_AGENT'];

        // Generate new CSRF token
        $_SESSION['csrf_token'] = bin2hex(random_bytes(32));

        return ['success' => true, 'message' => 'Login berhasil.'];

    } catch (Exception $e) {
        error_log("Admin login error: " . $e->getMessage());
        return ['success' => false, 'message' => 'Terjadi kesalahan sistem. Silakan coba lagi.'];
    }
}

/**
 * Logout admin - SECURED
 */
function adminLogout() {
    // Unset all session variables
    $_SESSION = array();

    // Delete session cookie
    if (ini_get("session.use_cookies")) {
        $params = session_get_cookie_params();
        setcookie(session_name(), '', time() - 42000,
            $params["path"], $params["domain"],
            $params["secure"], $params["httponly"]
        );
    }

    // Destroy session
    session_destroy();

    header('Location: admin_login.php');
    exit();
}

/**
 * Change admin password - SECURED
 */
function changeAdminPassword($adminId, $currentPassword, $newPassword) {
    if (strlen($newPassword) < 8) {
        return ['success' => false, 'message' => 'Password baru minimal 8 karakter.'];
    }

    try {
        $pdo = getAdminDB();

        // Verify current password first
        $stmt = $pdo->prepare("SELECT password FROM admin_users WHERE id = ?");
        $stmt->execute([$adminId]);
        $admin = $stmt->fetch();

        if (!$admin || !password_verify($currentPassword, $admin['password'])) {
            return ['success' => false, 'message' => 'Password saat ini salah.'];
        }

        // Hash new password with strong algorithm
        $hashedPassword = password_hash($newPassword, PASSWORD_DEFAULT, ['cost' => 12]);

        $stmt = $pdo->prepare("UPDATE admin_users SET password = ?, updated_at = NOW() WHERE id = ?");
        $stmt->execute([$hashedPassword, $adminId]);

        return ['success' => true, 'message' => 'Password berhasil diubah.'];

    } catch (Exception $e) {
        error_log("Change password error: " . $e->getMessage());
        return ['success' => false, 'message' => 'Terjadi kesalahan sistem.'];
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

/**
 * Escape output to prevent XSS
 */
function escapeHtml($string) {
    return htmlspecialchars($string, ENT_QUOTES, 'UTF-8');
}
?>
