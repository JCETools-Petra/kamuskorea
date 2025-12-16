<?php
require_once __DIR__ . '/admin_config.php';
requireAdminLogin();

$pdo = getAdminDB();

// Get statistics
$stats = [];
$stmt = $pdo->query("SELECT COUNT(*) as total FROM assessments WHERE type = 'quiz'");
$stats['quiz_count'] = $stmt->fetch()['total'];

$stmt = $pdo->query("SELECT COUNT(*) as total FROM assessments WHERE type = 'exam'");
$stats['exam_count'] = $stmt->fetch()['total'];

$stmt = $pdo->query("SELECT COUNT(*) as total FROM questions");
$stats['question_count'] = $stmt->fetch()['total'];

$stmt = $pdo->query("SELECT COUNT(*) as total FROM assessment_categories");
$stats['category_count'] = $stmt->fetch()['total'];
?>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Admin Dashboard - Kamus Korea</title>
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
        .stat-card {
            border-radius: 15px;
            padding: 25px;
            color: white;
            transition: transform 0.3s;
        }
        .stat-card:hover {
            transform: translateY(-5px);
        }
        .stat-icon {
            font-size: 3rem;
            opacity: 0.8;
        }
        .stat-number {
            font-size: 2.5rem;
            font-weight: bold;
        }
        .bg-quiz { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
        .bg-exam { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); }
        .bg-question { background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); }
        .bg-category { background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%); }
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
                    <a href="admin_dashboard.php" class="active"><i class="bi bi-speedometer2"></i> Dashboard</a>
                    <a href="admin_categories.php"><i class="bi bi-folder"></i> Kategori</a>
                    <a href="admin_quizzes.php"><i class="bi bi-patch-question"></i> Quiz</a>
                    <a href="admin_exams.php"><i class="bi bi-file-earmark-text"></i> Ujian/UBT</a>
                    <a href="admin_questions.php"><i class="bi bi-list-check"></i> Semua Soal</a>
                    <a href="admin_pdfs.php"><i class="bi bi-file-pdf"></i> E-Book PDF</a>
                    <hr class="text-white">
                    <a href="https://webtechsolution.my.id/kamuskorea/send_notification.php" target="_blank"><i class="bi bi-bell"></i> Push Notification</a>
                    <hr class="text-white">
                    <a href="admin_profile.php"><i class="bi bi-person-circle"></i> Profil</a>
                    <a href="admin_logout.php"><i class="bi bi-box-arrow-right"></i> Logout</a>
                </nav>
            </div>

            <!-- Main Content -->
            <div class="col-md-10 p-4">
                <div class="d-flex justify-content-between align-items-center mb-4">
                    <h2>Dashboard</h2>
                    <span class="text-muted">Selamat datang, <?= htmlspecialchars($_SESSION['admin_fullname'] ?? $_SESSION['admin_username']) ?>!</span>
                </div>

                <!-- Statistics Cards -->
                <div class="row g-4 mb-4">
                    <div class="col-md-3">
                        <div class="stat-card bg-quiz">
                            <div class="d-flex justify-content-between">
                                <div>
                                    <div class="stat-number"><?= $stats['quiz_count'] ?></div>
                                    <div>Quiz</div>
                                </div>
                                <div class="stat-icon"><i class="bi bi-patch-question"></i></div>
                            </div>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="stat-card bg-exam">
                            <div class="d-flex justify-content-between">
                                <div>
                                    <div class="stat-number"><?= $stats['exam_count'] ?></div>
                                    <div>Ujian/UBT</div>
                                </div>
                                <div class="stat-icon"><i class="bi bi-file-earmark-text"></i></div>
                            </div>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="stat-card bg-question">
                            <div class="d-flex justify-content-between">
                                <div>
                                    <div class="stat-number"><?= $stats['question_count'] ?></div>
                                    <div>Total Soal</div>
                                </div>
                                <div class="stat-icon"><i class="bi bi-list-check"></i></div>
                            </div>
                        </div>
                    </div>
                    <div class="col-md-3">
                        <div class="stat-card bg-category">
                            <div class="d-flex justify-content-between">
                                <div>
                                    <div class="stat-number"><?= $stats['category_count'] ?></div>
                                    <div>Kategori</div>
                                </div>
                                <div class="stat-icon"><i class="bi bi-folder"></i></div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Quick Actions -->
                <div class="card">
                    <div class="card-header">
                        <h5 class="mb-0"><i class="bi bi-lightning"></i> Aksi Cepat</h5>
                    </div>
                    <div class="card-body">
                        <div class="row g-3">
                            <div class="col-md-3">
                                <a href="admin_quizzes.php?action=add" class="btn btn-outline-primary w-100 py-3">
                                    <i class="bi bi-plus-circle d-block fs-3 mb-2"></i>
                                    Tambah Quiz Baru
                                </a>
                            </div>
                            <div class="col-md-3">
                                <a href="admin_exams.php?action=add" class="btn btn-outline-danger w-100 py-3">
                                    <i class="bi bi-plus-circle d-block fs-3 mb-2"></i>
                                    Tambah Ujian Baru
                                </a>
                            </div>
                            <div class="col-md-3">
                                <a href="admin_categories.php?action=add" class="btn btn-outline-success w-100 py-3">
                                    <i class="bi bi-folder-plus d-block fs-3 mb-2"></i>
                                    Tambah Kategori
                                </a>
                            </div>
                            <div class="col-md-3">
                                <a href="admin_questions.php" class="btn btn-outline-info w-100 py-3">
                                    <i class="bi bi-search d-block fs-3 mb-2"></i>
                                    Lihat Semua Soal
                                </a>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
