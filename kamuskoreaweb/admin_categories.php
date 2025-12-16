<?php
require_once __DIR__ . '/admin_config.php';
requireAdminLogin();

$pdo = getAdminDB();
$message = '';
$error = '';

// Handle actions
$action = $_GET['action'] ?? 'list';

// Delete category
if ($action === 'delete' && isset($_GET['id'])) {
    $id = (int)$_GET['id'];
    try {
        $stmt = $pdo->prepare("DELETE FROM assessment_categories WHERE id = ?");
        $stmt->execute([$id]);
        $message = 'Kategori berhasil dihapus!';
    } catch (Exception $e) {
        $error = 'Gagal menghapus kategori: ' . $e->getMessage();
    }
    $action = 'list';
}

// Save category (add/edit)
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['save_category'])) {
    $id = $_POST['id'] ?? null;
    $name = trim($_POST['name'] ?? '');
    $type = $_POST['type'] ?? 'quiz';
    $description = trim($_POST['description'] ?? '');
    $order_index = (int)($_POST['order_index'] ?? 0);

    if (empty($name)) {
        $error = 'Nama kategori tidak boleh kosong!';
    } else {
        try {
            if ($id) {
                $stmt = $pdo->prepare("UPDATE assessment_categories SET name = ?, type = ?, description = ?, order_index = ? WHERE id = ?");
                $stmt->execute([$name, $type, $description, $order_index, $id]);
                $message = 'Kategori berhasil diperbarui!';
            } else {
                $stmt = $pdo->prepare("INSERT INTO assessment_categories (name, type, description, order_index) VALUES (?, ?, ?, ?)");
                $stmt->execute([$name, $type, $description, $order_index]);
                $message = 'Kategori berhasil ditambahkan!';
            }
            $action = 'list';
        } catch (Exception $e) {
            $error = 'Gagal menyimpan kategori: ' . $e->getMessage();
        }
    }
}

// Get category for editing
$editCategory = null;
if ($action === 'edit' && isset($_GET['id'])) {
    $stmt = $pdo->prepare("SELECT * FROM assessment_categories WHERE id = ?");
    $stmt->execute([(int)$_GET['id']]);
    $editCategory = $stmt->fetch();
    if (!$editCategory) {
        $error = 'Kategori tidak ditemukan!';
        $action = 'list';
    }
}

// Get all categories
$categories = [];
if ($action === 'list') {
    $stmt = $pdo->query("SELECT * FROM assessment_categories ORDER BY type, order_index ASC");
    $categories = $stmt->fetchAll();
}
?>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Kelola Kategori - Admin Kamus Korea</title>
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
                    <a href="admin_categories.php" class="active"><i class="bi bi-folder"></i> Kategori</a>
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
                    <!-- List Categories -->
                    <div class="d-flex justify-content-between align-items-center mb-4">
                        <h2><i class="bi bi-folder"></i> Kelola Kategori</h2>
                        <a href="?action=add" class="btn btn-success">
                            <i class="bi bi-plus-circle"></i> Tambah Kategori
                        </a>
                    </div>

                    <div class="card">
                        <div class="card-body">
                            <div class="table-responsive">
                                <table class="table table-striped table-hover">
                                    <thead class="table-dark">
                                        <tr>
                                            <th>ID</th>
                                            <th>Nama</th>
                                            <th>Tipe</th>
                                            <th>Deskripsi</th>
                                            <th>Urutan</th>
                                            <th>Aksi</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <?php if (empty($categories)): ?>
                                            <tr><td colspan="6" class="text-center">Belum ada kategori</td></tr>
                                        <?php else: ?>
                                            <?php foreach ($categories as $cat): ?>
                                                <tr>
                                                    <td><?= $cat['id'] ?></td>
                                                    <td><?= htmlspecialchars($cat['name']) ?></td>
                                                    <td>
                                                        <span class="badge bg-<?= $cat['type'] === 'quiz' ? 'primary' : 'danger' ?>">
                                                            <?= ucfirst($cat['type']) ?>
                                                        </span>
                                                    </td>
                                                    <td><?= htmlspecialchars($cat['description'] ?? '-') ?></td>
                                                    <td><?= $cat['order_index'] ?></td>
                                                    <td>
                                                        <a href="?action=edit&id=<?= $cat['id'] ?>" class="btn btn-sm btn-warning">
                                                            <i class="bi bi-pencil"></i>
                                                        </a>
                                                        <a href="?action=delete&id=<?= $cat['id'] ?>" class="btn btn-sm btn-danger"
                                                           onclick="return confirm('Yakin ingin menghapus kategori ini?')">
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
                    <!-- Add/Edit Category Form -->
                    <div class="d-flex justify-content-between align-items-center mb-4">
                        <h2>
                            <i class="bi bi-<?= $action === 'add' ? 'plus-circle' : 'pencil' ?>"></i>
                            <?= $action === 'add' ? 'Tambah' : 'Edit' ?> Kategori
                        </h2>
                        <a href="admin_categories.php" class="btn btn-secondary">
                            <i class="bi bi-arrow-left"></i> Kembali
                        </a>
                    </div>

                    <div class="card">
                        <div class="card-body">
                            <form method="POST">
                                <input type="hidden" name="id" value="<?= $editCategory['id'] ?? '' ?>">

                                <div class="mb-3">
                                    <label class="form-label">Nama Kategori *</label>
                                    <input type="text" name="name" class="form-control" required
                                           value="<?= htmlspecialchars($editCategory['name'] ?? '') ?>">
                                </div>

                                <div class="mb-3">
                                    <label class="form-label">Tipe *</label>
                                    <select name="type" class="form-select" required>
                                        <option value="quiz" <?= ($editCategory['type'] ?? '') === 'quiz' ? 'selected' : '' ?>>Quiz</option>
                                        <option value="exam" <?= ($editCategory['type'] ?? '') === 'exam' ? 'selected' : '' ?>>Ujian/UBT</option>
                                    </select>
                                </div>

                                <div class="mb-3">
                                    <label class="form-label">Deskripsi</label>
                                    <textarea name="description" class="form-control" rows="3"><?= htmlspecialchars($editCategory['description'] ?? '') ?></textarea>
                                </div>

                                <div class="mb-3">
                                    <label class="form-label">Urutan</label>
                                    <input type="number" name="order_index" class="form-control" min="0"
                                           value="<?= $editCategory['order_index'] ?? 0 ?>">
                                </div>

                                <button type="submit" name="save_category" class="btn btn-primary">
                                    <i class="bi bi-save"></i> Simpan
                                </button>
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
