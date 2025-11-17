<?php
require_once __DIR__ . '/admin_config.php';
requireAdminLogin();

$pdo = getAdminDB();
$message = '';
$error = '';

$action = $_GET['action'] ?? 'list';

// Delete exam
if ($action === 'delete' && isset($_GET['id'])) {
    $id = (int)$_GET['id'];
    try {
        $stmt = $pdo->prepare("DELETE FROM questions WHERE assessment_id = ?");
        $stmt->execute([$id]);
        $stmt = $pdo->prepare("DELETE FROM assessments WHERE id = ? AND type = 'exam'");
        $stmt->execute([$id]);
        $message = 'Ujian dan semua soalnya berhasil dihapus!';
    } catch (Exception $e) {
        $error = 'Gagal menghapus ujian: ' . $e->getMessage();
    }
    $action = 'list';
}

// Save exam
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['save_exam'])) {
    $id = $_POST['id'] ?? null;
    $title = trim($_POST['title'] ?? '');
    $description = trim($_POST['description'] ?? '');
    $category_id = $_POST['category_id'] ?: null;
    $duration_minutes = (int)($_POST['duration_minutes'] ?? 60);
    $passing_score = (int)($_POST['passing_score'] ?? 70);
    $is_premium = isset($_POST['is_premium']) ? 1 : 0;
    $order_index = (int)($_POST['order_index'] ?? 0);

    if (empty($title)) {
        $error = 'Judul ujian tidak boleh kosong!';
    } else {
        try {
            if ($id) {
                $stmt = $pdo->prepare("UPDATE assessments SET title = ?, description = ?, category_id = ?, duration_minutes = ?, passing_score = ?, is_premium = ?, order_index = ? WHERE id = ? AND type = 'exam'");
                $stmt->execute([$title, $description, $category_id, $duration_minutes, $passing_score, $is_premium, $order_index, $id]);
                $message = 'Ujian berhasil diperbarui!';
            } else {
                $stmt = $pdo->prepare("INSERT INTO assessments (title, description, type, category_id, duration_minutes, passing_score, is_premium, order_index) VALUES (?, ?, 'exam', ?, ?, ?, ?, ?)");
                $stmt->execute([$title, $description, $category_id, $duration_minutes, $passing_score, $is_premium, $order_index]);
                $message = 'Ujian berhasil ditambahkan!';
            }
            $action = 'list';
        } catch (Exception $e) {
            $error = 'Gagal menyimpan ujian: ' . $e->getMessage();
        }
    }
}

// Get exam for editing
$editExam = null;
if ($action === 'edit' && isset($_GET['id'])) {
    $stmt = $pdo->prepare("SELECT * FROM assessments WHERE id = ? AND type = 'exam'");
    $stmt->execute([(int)$_GET['id']]);
    $editExam = $stmt->fetch();
    if (!$editExam) {
        $error = 'Ujian tidak ditemukan!';
        $action = 'list';
    }
}

// Get categories
$categories = $pdo->query("SELECT * FROM assessment_categories WHERE type = 'exam' ORDER BY order_index")->fetchAll();

// Get all exams
$exams = [];
if ($action === 'list') {
    $stmt = $pdo->query("
        SELECT a.*, c.name as category_name,
               (SELECT COUNT(*) FROM questions WHERE assessment_id = a.id) as question_count
        FROM assessments a
        LEFT JOIN assessment_categories c ON a.category_id = c.id
        WHERE a.type = 'exam'
        ORDER BY a.order_index ASC
    ");
    $exams = $stmt->fetchAll();
}
?>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Kelola Ujian/UBT - Admin Kamus Korea</title>
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
                    <a href="admin_exams.php" class="active"><i class="bi bi-file-earmark-text"></i> Ujian/UBT</a>
                    <a href="admin_questions.php"><i class="bi bi-list-check"></i> Semua Soal</a>
                    <hr class="text-white">
                    <a href="admin_logout.php"><i class="bi bi-box-arrow-right"></i> Logout</a>
                </nav>
            </div>

            <!-- Main Content -->
            <div class="col-md-10 p-4">
                <?php if ($message): ?>
                    <div class="alert alert-success alert-dismissible fade show">
                        <?= htmlspecialchars($message) ?>
                        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                    </div>
                <?php endif; ?>

                <?php if ($error): ?>
                    <div class="alert alert-danger alert-dismissible fade show">
                        <?= htmlspecialchars($error) ?>
                        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                    </div>
                <?php endif; ?>

                <?php if ($action === 'list'): ?>
                    <!-- List Exams -->
                    <div class="d-flex justify-content-between align-items-center mb-4">
                        <h2><i class="bi bi-file-earmark-text"></i> Kelola Ujian/UBT</h2>
                        <a href="?action=add" class="btn btn-success">
                            <i class="bi bi-plus-circle"></i> Tambah Ujian
                        </a>
                    </div>

                    <div class="card">
                        <div class="card-body">
                            <div class="table-responsive">
                                <table class="table table-striped table-hover">
                                    <thead class="table-dark">
                                        <tr>
                                            <th>ID</th>
                                            <th>Judul</th>
                                            <th>Kategori</th>
                                            <th>Durasi</th>
                                            <th>Passing Score</th>
                                            <th>Jumlah Soal</th>
                                            <th>Premium</th>
                                            <th>Aksi</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <?php if (empty($exams)): ?>
                                            <tr><td colspan="8" class="text-center">Belum ada ujian</td></tr>
                                        <?php else: ?>
                                            <?php foreach ($exams as $exam): ?>
                                                <tr>
                                                    <td><?= $exam['id'] ?></td>
                                                    <td><?= htmlspecialchars($exam['title']) ?></td>
                                                    <td><?= htmlspecialchars($exam['category_name'] ?? '-') ?></td>
                                                    <td><?= $exam['duration_minutes'] ?> menit</td>
                                                    <td><?= $exam['passing_score'] ?>%</td>
                                                    <td>
                                                        <span class="badge bg-info"><?= $exam['question_count'] ?> soal</span>
                                                    </td>
                                                    <td>
                                                        <?php if ($exam['is_premium']): ?>
                                                            <span class="badge bg-warning">Premium</span>
                                                        <?php else: ?>
                                                            <span class="badge bg-secondary">Free</span>
                                                        <?php endif; ?>
                                                    </td>
                                                    <td>
                                                        <a href="admin_questions.php?assessment_id=<?= $exam['id'] ?>" class="btn btn-sm btn-info" title="Kelola Soal">
                                                            <i class="bi bi-list-check"></i>
                                                        </a>
                                                        <a href="?action=edit&id=<?= $exam['id'] ?>" class="btn btn-sm btn-warning" title="Edit">
                                                            <i class="bi bi-pencil"></i>
                                                        </a>
                                                        <a href="?action=delete&id=<?= $exam['id'] ?>" class="btn btn-sm btn-danger"
                                                           onclick="return confirm('Yakin ingin menghapus ujian ini beserta semua soalnya?')" title="Hapus">
                                                            <i class="bi bi-trash"></i>
                                                        </a>
                                                    </td>
                                                </tr>
                                            <?php endforeach; ?>
                                        <?php endif; ?>
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>

                <?php elseif ($action === 'add' || $action === 'edit'): ?>
                    <!-- Add/Edit Exam Form -->
                    <div class="d-flex justify-content-between align-items-center mb-4">
                        <h2>
                            <i class="bi bi-<?= $action === 'add' ? 'plus-circle' : 'pencil' ?>"></i>
                            <?= $action === 'add' ? 'Tambah' : 'Edit' ?> Ujian/UBT
                        </h2>
                        <a href="admin_exams.php" class="btn btn-secondary">
                            <i class="bi bi-arrow-left"></i> Kembali
                        </a>
                    </div>

                    <div class="card">
                        <div class="card-body">
                            <form method="POST">
                                <input type="hidden" name="id" value="<?= $editExam['id'] ?? '' ?>">

                                <div class="row">
                                    <div class="col-md-8">
                                        <div class="mb-3">
                                            <label class="form-label">Judul Ujian *</label>
                                            <input type="text" name="title" class="form-control" required
                                                   value="<?= htmlspecialchars($editExam['title'] ?? '') ?>"
                                                   placeholder="Contoh: EPS-TOPIK Simulasi 1">
                                        </div>
                                    </div>
                                    <div class="col-md-4">
                                        <div class="mb-3">
                                            <label class="form-label">Kategori</label>
                                            <select name="category_id" class="form-select">
                                                <option value="">-- Pilih Kategori --</option>
                                                <?php foreach ($categories as $cat): ?>
                                                    <option value="<?= $cat['id'] ?>" <?= ($editExam['category_id'] ?? '') == $cat['id'] ? 'selected' : '' ?>>
                                                        <?= htmlspecialchars($cat['name']) ?>
                                                    </option>
                                                <?php endforeach; ?>
                                            </select>
                                        </div>
                                    </div>
                                </div>

                                <div class="mb-3">
                                    <label class="form-label">Deskripsi</label>
                                    <textarea name="description" class="form-control" rows="3"
                                              placeholder="Deskripsi singkat tentang ujian ini"><?= htmlspecialchars($editExam['description'] ?? '') ?></textarea>
                                </div>

                                <div class="row">
                                    <div class="col-md-4">
                                        <div class="mb-3">
                                            <label class="form-label">Durasi (menit)</label>
                                            <input type="number" name="duration_minutes" class="form-control" min="1"
                                                   value="<?= $editExam['duration_minutes'] ?? 60 ?>">
                                            <small class="text-muted">EPS-TOPIK biasanya 70 menit</small>
                                        </div>
                                    </div>
                                    <div class="col-md-4">
                                        <div class="mb-3">
                                            <label class="form-label">Passing Score (%)</label>
                                            <input type="number" name="passing_score" class="form-control" min="0" max="100"
                                                   value="<?= $editExam['passing_score'] ?? 70 ?>">
                                        </div>
                                    </div>
                                    <div class="col-md-4">
                                        <div class="mb-3">
                                            <label class="form-label">Urutan</label>
                                            <input type="number" name="order_index" class="form-control" min="0"
                                                   value="<?= $editExam['order_index'] ?? 0 ?>">
                                        </div>
                                    </div>
                                </div>

                                <div class="mb-3">
                                    <div class="form-check">
                                        <input type="checkbox" name="is_premium" class="form-check-input" id="is_premium"
                                               <?= ($editExam['is_premium'] ?? 0) ? 'checked' : '' ?>>
                                        <label class="form-check-label" for="is_premium">
                                            <i class="bi bi-star-fill text-warning"></i> Konten Premium
                                        </label>
                                    </div>
                                </div>

                                <button type="submit" name="save_exam" class="btn btn-primary">
                                    <i class="bi bi-save"></i> Simpan Ujian
                                </button>

                                <?php if ($editExam): ?>
                                    <a href="admin_questions.php?assessment_id=<?= $editExam['id'] ?>" class="btn btn-info">
                                        <i class="bi bi-list-check"></i> Kelola Soal
                                    </a>
                                <?php endif; ?>
                            </form>
                        </div>
                    </div>
                <?php endif; ?>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
