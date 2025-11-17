<?php
error_reporting(E_ALL);
ini_set('display_errors', 1);

$error = '';
$debug = '';

// Try to load config
try {
    require_once __DIR__ . '/admin_config.php';
} catch (Exception $e) {
    $error = 'Config load error: ' . $e->getMessage();
}

if ($_SERVER['REQUEST_METHOD'] === 'POST' && empty($error)) {
    $username = trim($_POST['username'] ?? '');
    $password = $_POST['password'] ?? '';

    if (empty($username) || empty($password)) {
        $error = 'Username dan password harus diisi!';
    } else {
        try {
            if (adminLogin($username, $password)) {
                header('Location: admin_dashboard.php');
                exit();
            } else {
                $error = 'Username atau password salah! Cek file admin_login.log untuk detail.';
            }
        } catch (Exception $e) {
            $error = 'Login error: ' . $e->getMessage();
        }
    }
}

if (empty($error) && function_exists('isAdminLoggedIn') && isAdminLoggedIn()) {
    header('Location: admin_dashboard.php');
    exit();
}

// Check if log file exists and show last entries
$logFile = __DIR__ . '/admin_login.log';
if (file_exists($logFile)) {
    $logContent = file_get_contents($logFile);
    $logLines = explode("\n", trim($logContent));
    $lastLines = array_slice($logLines, -20); // Last 20 lines
    $debug = implode("\n", $lastLines);
}
?>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
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
            max-width: 500px;
        }
        .logo {
            font-size: 3rem;
            margin-bottom: 20px;
        }
        .debug-log {
            font-family: monospace;
            font-size: 11px;
            background: #f8f9fa;
            padding: 10px;
            border-radius: 5px;
            max-height: 200px;
            overflow-y: auto;
            white-space: pre-wrap;
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
            <div class="alert alert-danger"><?= htmlspecialchars($error) ?></div>
        <?php endif; ?>

        <form method="POST">
            <div class="mb-3">
                <label for="username" class="form-label">Username</label>
                <input type="text" class="form-control" id="username" name="username" required autofocus
                       value="<?= htmlspecialchars($_POST['username'] ?? '') ?>">
            </div>
            <div class="mb-3">
                <label for="password" class="form-label">Password</label>
                <input type="password" class="form-control" id="password" name="password" required>
            </div>
            <button type="submit" class="btn btn-primary w-100">Login</button>
        </form>

        <?php if (!empty($debug)): ?>
            <hr>
            <div class="mt-3">
                <h6>Debug Log (Last 20 lines):</h6>
                <div class="debug-log"><?= htmlspecialchars($debug) ?></div>
                <small class="text-muted">File: <?= $logFile ?></small>
            </div>
        <?php endif; ?>

        <hr>
        <div class="mt-3">
            <small class="text-muted">
                <strong>Troubleshooting:</strong><br>
                1. Pastikan tabel <code>admin_users</code> sudah dibuat<br>
                2. Pastikan file <code>.env</code> ada di parent directory<br>
                3. Cek file <code>admin_login.log</code> untuk detail error
            </small>
        </div>
    </div>
</body>
</html>
