<?php
/**
 * Admin Login Page - SECURED
 * Anti brute force, CSRF protection, XSS protection
 */

// Disable error display for production
error_reporting(0);
ini_set('display_errors', 0);
ini_set('log_errors', 1);

$error = '';
$success = '';

try {
    // Load config
    require_once __DIR__ . '/admin_config.php';

    // Security headers
    header('X-Frame-Options: DENY');
    header('X-Content-Type-Options: nosniff');
    header('X-XSS-Protection: 1; mode=block');
    header('Referrer-Policy: strict-origin-when-cross-origin');

    // Redirect if already logged in
    if (isAdminLoggedIn()) {
        header('Location: admin_dashboard.php');
        exit();
    }

    // Generate CSRF token for the form
    $csrfToken = generateCSRFToken();

    // Handle login POST request
    if ($_SERVER['REQUEST_METHOD'] === 'POST') {
        // Verify CSRF token
        $submittedToken = $_POST['csrf_token'] ?? '';
        if (!verifyCSRFToken($submittedToken)) {
            $error = 'Sesi tidak valid. Silakan refresh halaman dan coba lagi.';
        } else {
            $username = trim($_POST['username'] ?? '');
            $password = $_POST['password'] ?? '';

            $result = adminLogin($username, $password);

            if ($result['success']) {
                // Successful login
                header('Location: admin_dashboard.php');
                exit();
            } else {
                $error = $result['message'];
            }
        }

        // Regenerate CSRF token after each attempt
        $_SESSION['csrf_token'] = bin2hex(random_bytes(32));
        $csrfToken = $_SESSION['csrf_token'];
    }
} catch (Exception $e) {
    error_log("Admin login page error: " . $e->getMessage());
    $error = 'Terjadi kesalahan sistem. Silakan coba lagi.';
    $csrfToken = '';
}
?>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="robots" content="noindex, nofollow">
    <title>Admin Login - Kamus Korea</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        body {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        .login-card {
            background: white;
            border-radius: 15px;
            box-shadow: 0 10px 40px rgba(0,0,0,0.2);
            padding: 40px;
            width: 100%;
            max-width: 400px;
        }
        .logo {
            font-size: 3rem;
            margin-bottom: 20px;
        }
        .form-control:focus {
            border-color: #667eea;
            box-shadow: 0 0 0 0.2rem rgba(102, 126, 234, 0.25);
        }
        .btn-primary {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            border: none;
        }
        .btn-primary:hover {
            background: linear-gradient(135deg, #5a6fd6 0%, #6a4190 100%);
        }
    </style>
</head>
<body>
    <div class="login-card">
        <div class="text-center">
            <div class="logo">📚</div>
            <h2 class="mb-4">Admin Panel</h2>
            <p class="text-muted">Kamus Korea Quiz & Exam Management</p>
        </div>

        <?php if ($error): ?>
            <div class="alert alert-danger alert-dismissible fade show" role="alert">
                <?= htmlspecialchars($error, ENT_QUOTES, 'UTF-8') ?>
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
            </div>
        <?php endif; ?>

        <?php if ($success): ?>
            <div class="alert alert-success alert-dismissible fade show" role="alert">
                <?= htmlspecialchars($success, ENT_QUOTES, 'UTF-8') ?>
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
            </div>
        <?php endif; ?>

        <form method="POST" autocomplete="off">
            <!-- CSRF Protection -->
            <input type="hidden" name="csrf_token" value="<?= htmlspecialchars($csrfToken, ENT_QUOTES, 'UTF-8') ?>">

            <div class="mb-3">
                <label for="username" class="form-label">Username</label>
                <input type="text"
                       class="form-control"
                       id="username"
                       name="username"
                       required
                       autofocus
                       maxlength="50"
                       pattern="[a-zA-Z0-9_]+"
                       title="Hanya huruf, angka, dan underscore"
                       autocomplete="username">
            </div>
            <div class="mb-3">
                <label for="password" class="form-label">Password</label>
                <input type="password"
                       class="form-control"
                       id="password"
                       name="password"
                       required
                       maxlength="255"
                       autocomplete="current-password">
            </div>
            <button type="submit" class="btn btn-primary w-100">
                <i class="bi bi-box-arrow-in-right"></i> Login
            </button>
        </form>

        <div class="text-center mt-4">
            <small class="text-muted">
                &copy; <?= date('Y') ?> Kamus Korea. Secure Admin Panel.
            </small>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        // Prevent form resubmission on page refresh
        if (window.history.replaceState) {
            window.history.replaceState(null, null, window.location.href);
        }

        // Clear password field on page load (security)
        document.getElementById('password').value = '';
    </script>
</body>
</html>
