<?php
require_once __DIR__ . '/admin_config.php';
requireAdminLogin();

$pdo = getAdminDB();
$message = '';
$error = '';
$admin = getCurrentAdmin();

// Get full admin data from database
$stmt = $pdo->prepare("SELECT * FROM admin_users WHERE id = ?");
$stmt->execute([$admin['id']]);
$adminData = $stmt->fetch();

// Handle profile update
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['update_profile'])) {
    $csrfToken = $_POST['csrf_token'] ?? '';
    if (!verifyCSRFToken($csrfToken)) {
        $error = 'Sesi tidak valid. Silakan refresh halaman.';
    } else {
        $fullName = trim($_POST['full_name'] ?? '');
        $email = trim($_POST['email'] ?? '');

        if (empty($fullName)) {
            $error = 'Nama lengkap tidak boleh kosong.';
        } elseif (!empty($email) && !filter_var($email, FILTER_VALIDATE_EMAIL)) {
            $error = 'Format email tidak valid.';
        } else {
            try {
                $stmt = $pdo->prepare("UPDATE admin_users SET full_name = ?, email = ?, updated_at = NOW() WHERE id = ?");
                $stmt->execute([$fullName, $email, $admin['id']]);

                // Update session
                $_SESSION['admin_fullname'] = $fullName;

                $message = 'Profil berhasil diperbarui!';

                // Refresh admin data
                $stmt = $pdo->prepare("SELECT * FROM admin_users WHERE id = ?");
                $stmt->execute([$admin['id']]);
                $adminData = $stmt->fetch();
            } catch (Exception $e) {
                error_log("Profile update error: " . $e->getMessage());
                $error = 'Gagal memperbarui profil.';
            }
        }
    }
}

// Handle password change
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['change_password'])) {
    $csrfToken = $_POST['csrf_token'] ?? '';
    if (!verifyCSRFToken($csrfToken)) {
        $error = 'Sesi tidak valid. Silakan refresh halaman.';
    } else {
        $currentPassword = $_POST['current_password'] ?? '';
        $newPassword = $_POST['new_password'] ?? '';
        $confirmPassword = $_POST['confirm_password'] ?? '';

        if (empty($currentPassword) || empty($newPassword) || empty($confirmPassword)) {
            $error = 'Semua field password harus diisi.';
        } elseif ($newPassword !== $confirmPassword) {
            $error = 'Password baru dan konfirmasi tidak cocok.';
        } elseif (strlen($newPassword) < 8) {
            $error = 'Password baru minimal 8 karakter.';
        } elseif (!preg_match('/[A-Z]/', $newPassword)) {
            $error = 'Password harus mengandung minimal 1 huruf besar.';
        } elseif (!preg_match('/[a-z]/', $newPassword)) {
            $error = 'Password harus mengandung minimal 1 huruf kecil.';
        } elseif (!preg_match('/[0-9]/', $newPassword)) {
            $error = 'Password harus mengandung minimal 1 angka.';
        } else {
            $result = changeAdminPassword($admin['id'], $currentPassword, $newPassword);
            if ($result['success']) {
                $message = $result['message'];
            } else {
                $error = $result['message'];
            }
        }
    }
}

$csrfToken = generateCSRFToken();
?>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta name="robots" content="noindex, nofollow">
    <title>Profil Admin - Kamus Korea</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css" rel="stylesheet">
    <style>
        .sidebar {
            min-height: 100vh;
            background: #2c3e50;
        }
        .sidebar a {
            color: #ecf0f1;
            padding: 15px 20px;
            display: block;
            text-decoration: none;
            transition: all 0.3s;
        }
        .sidebar a:hover, .sidebar a.active {
            background: #34495e;
            color: white;
        }
        .sidebar i {
            margin-right: 10px;
        }
        .profile-card {
            border-radius: 15px;
            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
        }
        .password-strength {
            height: 5px;
            border-radius: 3px;
            margin-top: 5px;
        }
    </style>
</head>
<body>
    <div class="container-fluid">
        <div class="row">
            <!-- Sidebar -->
            <div class="col-md-2 sidebar p-0">
                <div class="text-center py-4">
                    <h4 class="text-white">📚 Admin Panel</h4>
                </div>
                                <nav>
                    <a href="admin_dashboard.php"><i class="bi bi-speedometer2"></i> Dashboard</a>
                    <a href="admin_categories.php"><i class="bi bi-folder"></i> Kategori</a>
                    <a href="admin_quizzes.php"><i class="bi bi-patch-question"></i> Quiz</a>
                    <a href="admin_exams.php"><i class="bi bi-file-earmark-text"></i> Ujian/UBT</a>
                    <a href="admin_questions.php"><i class="bi bi-list-check"></i> Semua Soal</a>
                    <a href="admin_pdfs.php"><i class="bi bi-file-pdf"></i> E-Book PDF</a>
                    <a href="admin_video_hafalan.php"><i class="bi bi-camera-video"></i> Video Hafalan</a>
                    <hr class="text-white">
                    <a href="https://webtechsolution.my.id/kamuskorea/send_notification.php" target="_blank"><i class="bi bi-bell"></i> Push Notification</a>
                    <hr class="text-white">
                    <a href="admin_profile.php" class="active"><i class="bi bi-person-circle"></i> Profil</a>
                    <a href="admin_logout.php"><i class="bi bi-box-arrow-right"></i> Logout</a>
                </nav>
            </div>

            <!-- Main Content -->
            <div class="col-md-10 p-4">
                <div class="d-flex justify-content-between align-items-center mb-4">
                    <h2><i class="bi bi-person-circle"></i> Profil Admin</h2>
                    <span class="text-muted">
                        Login terakhir: <?= $adminData['last_login'] ? date('d M Y H:i', strtotime($adminData['last_login'])) : '-' ?>
                    </span>
                </div>

                <?php if ($message): ?>
                    <div class="alert alert-success alert-dismissible fade show">
                        <i class="bi bi-check-circle"></i> <?= htmlspecialchars($message) ?>
                        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                    </div>
                <?php endif; ?>

                <?php if ($error): ?>
                    <div class="alert alert-danger alert-dismissible fade show">
                        <i class="bi bi-exclamation-circle"></i> <?= htmlspecialchars($error) ?>
                        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                    </div>
                <?php endif; ?>

                <div class="row">
                    <!-- Profile Information -->
                    <div class="col-md-6 mb-4">
                        <div class="card profile-card">
                            <div class="card-header bg-primary text-white">
                                <h5 class="mb-0"><i class="bi bi-person-badge"></i> Informasi Profil</h5>
                            </div>
                            <div class="card-body">
                                <form method="POST">
                                    <input type="hidden" name="csrf_token" value="<?= htmlspecialchars($csrfToken) ?>">

                                    <div class="mb-3">
                                        <label class="form-label">Username</label>
                                        <input type="text" class="form-control" value="<?= htmlspecialchars($adminData['username']) ?>" disabled>
                                        <small class="text-muted">Username tidak dapat diubah</small>
                                    </div>

                                    <div class="mb-3">
                                        <label class="form-label">Nama Lengkap *</label>
                                        <input type="text" name="full_name" class="form-control" required
                                               value="<?= htmlspecialchars($adminData['full_name'] ?? '') ?>"
                                               maxlength="100">
                                    </div>

                                    <div class="mb-3">
                                        <label class="form-label">Email</label>
                                        <input type="email" name="email" class="form-control"
                                               value="<?= htmlspecialchars($adminData['email'] ?? '') ?>"
                                               maxlength="100">
                                    </div>

                                    <div class="mb-3">
                                        <label class="form-label">Dibuat</label>
                                        <input type="text" class="form-control"
                                               value="<?= date('d M Y H:i', strtotime($adminData['created_at'])) ?>" disabled>
                                    </div>

                                    <button type="submit" name="update_profile" class="btn btn-primary">
                                        <i class="bi bi-save"></i> Simpan Perubahan
                                    </button>
                                </form>
                            </div>
                        </div>
                    </div>

                    <!-- Change Password -->
                    <div class="col-md-6 mb-4">
                        <div class="card profile-card">
                            <div class="card-header bg-warning">
                                <h5 class="mb-0"><i class="bi bi-shield-lock"></i> Ganti Password</h5>
                            </div>
                            <div class="card-body">
                                <form method="POST" id="passwordForm">
                                    <input type="hidden" name="csrf_token" value="<?= htmlspecialchars($csrfToken) ?>">

                                    <div class="mb-3">
                                        <label class="form-label">Password Saat Ini *</label>
                                        <div class="input-group">
                                            <input type="password" name="current_password" class="form-control" required
                                                   id="currentPassword">
                                            <button type="button" class="btn btn-outline-secondary" onclick="togglePassword('currentPassword')">
                                                <i class="bi bi-eye"></i>
                                            </button>
                                        </div>
                                    </div>

                                    <div class="mb-3">
                                        <label class="form-label">Password Baru *</label>
                                        <div class="input-group">
                                            <input type="password" name="new_password" class="form-control" required
                                                   id="newPassword" minlength="8" onkeyup="checkPasswordStrength()">
                                            <button type="button" class="btn btn-outline-secondary" onclick="togglePassword('newPassword')">
                                                <i class="bi bi-eye"></i>
                                            </button>
                                        </div>
                                        <div id="passwordStrength" class="password-strength"></div>
                                        <small class="text-muted">
                                            Minimal 8 karakter, harus mengandung huruf besar, huruf kecil, dan angka
                                        </small>
                                    </div>

                                    <div class="mb-3">
                                        <label class="form-label">Konfirmasi Password Baru *</label>
                                        <div class="input-group">
                                            <input type="password" name="confirm_password" class="form-control" required
                                                   id="confirmPassword" onkeyup="checkPasswordMatch()">
                                            <button type="button" class="btn btn-outline-secondary" onclick="togglePassword('confirmPassword')">
                                                <i class="bi bi-eye"></i>
                                            </button>
                                        </div>
                                        <div id="passwordMatch" class="mt-1"></div>
                                    </div>

                                    <button type="submit" name="change_password" class="btn btn-warning">
                                        <i class="bi bi-key"></i> Ganti Password
                                    </button>
                                </form>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Security Info -->
                <div class="card profile-card">
                    <div class="card-header bg-info text-white">
                        <h5 class="mb-0"><i class="bi bi-info-circle"></i> Informasi Keamanan</h5>
                    </div>
                    <div class="card-body">
                        <div class="row">
                            <div class="col-md-4">
                                <strong>Session ID:</strong><br>
                                <code><?= substr(session_id(), 0, 16) ?>...</code>
                            </div>
                            <div class="col-md-4">
                                <strong>IP Address:</strong><br>
                                <code><?= htmlspecialchars($_SERVER['REMOTE_ADDR']) ?></code>
                            </div>
                            <div class="col-md-4">
                                <strong>Login Sejak:</strong><br>
                                <?= date('d M Y H:i:s', $_SESSION['admin_login_time']) ?>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        function togglePassword(fieldId) {
            const field = document.getElementById(fieldId);
            if (field.type === 'password') {
                field.type = 'text';
            } else {
                field.type = 'password';
            }
        }

        function checkPasswordStrength() {
            const password = document.getElementById('newPassword').value;
            const strengthBar = document.getElementById('passwordStrength');

            let strength = 0;
            if (password.length >= 8) strength++;
            if (password.match(/[a-z]/)) strength++;
            if (password.match(/[A-Z]/)) strength++;
            if (password.match(/[0-9]/)) strength++;
            if (password.match(/[^a-zA-Z0-9]/)) strength++;

            const colors = ['#dc3545', '#fd7e14', '#ffc107', '#28a745', '#20c997'];
            const widths = ['20%', '40%', '60%', '80%', '100%'];

            if (password.length === 0) {
                strengthBar.style.width = '0%';
                strengthBar.style.backgroundColor = '#ccc';
            } else {
                strengthBar.style.width = widths[strength - 1] || '20%';
                strengthBar.style.backgroundColor = colors[strength - 1] || '#dc3545';
            }
        }

        function checkPasswordMatch() {
            const newPass = document.getElementById('newPassword').value;
            const confirmPass = document.getElementById('confirmPassword').value;
            const matchDiv = document.getElementById('passwordMatch');

            if (confirmPass.length === 0) {
                matchDiv.innerHTML = '';
            } else if (newPass === confirmPass) {
                matchDiv.innerHTML = '<small class="text-success"><i class="bi bi-check-circle"></i> Password cocok</small>';
            } else {
                matchDiv.innerHTML = '<small class="text-danger"><i class="bi bi-x-circle"></i> Password tidak cocok</small>';
            }
        }
    </script>
</body>
</html>
