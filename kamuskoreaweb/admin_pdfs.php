<?php
require_once __DIR__ . '/admin_config.php';
requireAdminLogin();

$pdo = getAdminDB();

// Handle form submissions
$message = '';
$error = '';

// CREATE/UPDATE PDF
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['action']) && $_POST['action'] === 'save') {
    $id = $_POST['id'] ?? null;
    $title = trim($_POST['title']);
    $author = trim($_POST['author']);
    $description = trim($_POST['description']);
    $category = trim($_POST['category']);
    $is_premium = isset($_POST['is_premium']) ? 1 : 0;

    // Handle file upload
    $pdfUrl = $_POST['current_pdf_url'] ?? '';

    if (isset($_FILES['pdf_file']) && $_FILES['pdf_file']['error'] === UPLOAD_ERR_OK) {
        $uploadDir = __DIR__ . '/pdf/';

        // Create directory if not exists
        if (!is_dir($uploadDir)) {
            mkdir($uploadDir, 0755, true);
        }

        $fileName = time() . '_' . basename($_FILES['pdf_file']['name']);
        $targetPath = $uploadDir . $fileName;

        // Validate PDF file
        $fileType = strtolower(pathinfo($targetPath, PATHINFO_EXTENSION));
        if ($fileType !== 'pdf') {
            $error = 'Hanya file PDF yang diizinkan!';
        } else {
            if (move_uploaded_file($_FILES['pdf_file']['tmp_name'], $targetPath)) {
                $pdfUrl = 'pdf/' . $fileName;

                // Delete old file if updating
                if ($id && !empty($_POST['current_pdf_url'])) {
                    $oldFile = __DIR__ . '/' . $_POST['current_pdf_url'];
                    if (file_exists($oldFile)) {
                        unlink($oldFile);
                    }
                }
            } else {
                $error = 'Gagal upload file!';
            }
        }
    }

    if (empty($error)) {
        if ($id) {
            // UPDATE
            $stmt = $pdo->prepare("
                UPDATE ebooks
                SET title = ?, author = ?, description = ?, category = ?, pdf_url = ?, is_premium = ?, updated_at = NOW()
                WHERE id = ?
            ");
            $stmt->execute([$title, $author, $description, $category, $pdfUrl, $is_premium, $id]);
            $message = 'E-Book berhasil diupdate!';
        } else {
            // CREATE
            if (empty($pdfUrl)) {
                $error = 'File PDF harus diupload!';
            } else {
                $stmt = $pdo->prepare("
                    INSERT INTO ebooks (title, author, description, category, pdf_url, is_premium, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())
                ");
                $stmt->execute([$title, $author, $description, $category, $pdfUrl, $is_premium]);
                $message = 'E-Book berhasil ditambahkan!';
            }
        }
    }
}

// DELETE PDF
if (isset($_GET['action']) && $_GET['action'] === 'delete' && isset($_GET['id'])) {
    $id = intval($_GET['id']);

    // Get PDF URL to delete file
    $stmt = $pdo->prepare("SELECT pdf_url FROM ebooks WHERE id = ?");
    $stmt->execute([$id]);
    $ebook = $stmt->fetch();

    if ($ebook) {
        // Delete file
        $filePath = __DIR__ . '/' . $ebook['pdf_url'];
        if (file_exists($filePath)) {
            unlink($filePath);
        }

        // Delete from database
        $stmt = $pdo->prepare("DELETE FROM ebooks WHERE id = ?");
        $stmt->execute([$id]);
        $message = 'E-Book berhasil dihapus!';
    }
}

// Get all PDFs
$pdfs = $pdo->query("SELECT * FROM ebooks ORDER BY created_at DESC")->fetchAll();

// Get edit data if editing
$editPdf = null;
if (isset($_GET['action']) && $_GET['action'] === 'edit' && isset($_GET['id'])) {
    $stmt = $pdo->prepare("SELECT * FROM ebooks WHERE id = ?");
    $stmt->execute([$_GET['id']]);
    $editPdf = $stmt->fetch();
}
?>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Kelola E-Book PDF - Admin</title>
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
        .pdf-card {
            transition: transform 0.3s, box-shadow 0.3s;
            border-radius: 10px;
            overflow: hidden;
        }
        .pdf-card:hover {
            transform: translateY(-5px);
            box-shadow: 0 8px 20px rgba(0,0,0,0.15);
        }
        .pdf-icon {
            font-size: 4rem;
            color: #dc3545;
        }
        .badge-premium {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
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
                    <a href="admin_pdfs.php" class="active"><i class="bi bi-file-pdf"></i> E-Book PDF</a>
                    <hr class="text-white">
                    <a href="https://webtechsolution.my.id/kamuskorea/send_notification.php" target="_blank"><i class="bi bi-bell"></i> Push Notification</a>
                    <hr class="text-white">
                    <a href="admin_profile.php"><i class="bi bi-person-circle"></i> Profil</a>
                    <a href="admin_logout.php"><i class="bi bi-box-arrow-right"></i> Logout</a>
                </nav>
            </div>

            <!-- Main Content -->
            <div class="col-md-10 p-4">
                <h2 class="mb-4"><i class="bi bi-file-pdf"></i> Kelola E-Book PDF</h2>

                <?php if ($message): ?>
                    <div class="alert alert-success alert-dismissible fade show">
                        <i class="bi bi-check-circle"></i> <?= htmlspecialchars($message) ?>
                        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                    </div>
                <?php endif; ?>

                <?php if ($error): ?>
                    <div class="alert alert-danger alert-dismissible fade show">
                        <i class="bi bi-exclamation-triangle"></i> <?= htmlspecialchars($error) ?>
                        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                    </div>
                <?php endif; ?>

                <!-- Add/Edit Form -->
                <div class="card mb-4">
                    <div class="card-header bg-primary text-white">
                        <h5 class="mb-0">
                            <i class="bi bi-<?= $editPdf ? 'pencil-square' : 'plus-circle' ?>"></i>
                            <?= $editPdf ? 'Edit E-Book' : 'Tambah E-Book Baru' ?>
                        </h5>
                    </div>
                    <div class="card-body">
                        <form method="POST" enctype="multipart/form-data">
                            <input type="hidden" name="action" value="save">
                            <input type="hidden" name="id" value="<?= $editPdf['id'] ?? '' ?>">
                            <input type="hidden" name="current_pdf_url" value="<?= $editPdf['pdf_url'] ?? '' ?>">

                            <div class="row g-3">
                                <div class="col-md-6">
                                    <label class="form-label">Judul E-Book <span class="text-danger">*</span></label>
                                    <input type="text" name="title" class="form-control"
                                           value="<?= htmlspecialchars($editPdf['title'] ?? '') ?>"
                                           placeholder="Contoh: Belajar Bahasa Korea untuk Pemula"
                                           required>
                                </div>

                                <div class="col-md-6">
                                    <label class="form-label">Penulis <span class="text-danger">*</span></label>
                                    <input type="text" name="author" class="form-control"
                                           value="<?= htmlspecialchars($editPdf['author'] ?? '') ?>"
                                           placeholder="Contoh: Tim Kamus Korea"
                                           required>
                                </div>

                                <div class="col-md-6">
                                    <label class="form-label">Kategori <span class="text-danger">*</span></label>
                                    <select name="category" class="form-select" required>
                                        <option value="">Pilih Kategori...</option>
                                        <option value="Pemula" <?= ($editPdf['category'] ?? '') === 'Pemula' ? 'selected' : '' ?>>Pemula</option>
                                        <option value="Menengah" <?= ($editPdf['category'] ?? '') === 'Menengah' ? 'selected' : '' ?>>Menengah</option>
                                        <option value="Lanjutan" <?= ($editPdf['category'] ?? '') === 'Lanjutan' ? 'selected' : '' ?>>Lanjutan</option>
                                        <option value="Tata Bahasa" <?= ($editPdf['category'] ?? '') === 'Tata Bahasa' ? 'selected' : '' ?>>Tata Bahasa</option>
                                        <option value="Kosakata" <?= ($editPdf['category'] ?? '') === 'Kosakata' ? 'selected' : '' ?>>Kosakata</option>
                                        <option value="Percakapan" <?= ($editPdf['category'] ?? '') === 'Percakapan' ? 'selected' : '' ?>>Percakapan</option>
                                        <option value="TOPIK" <?= ($editPdf['category'] ?? '') === 'TOPIK' ? 'selected' : '' ?>>TOPIK</option>
                                        <option value="Lainnya" <?= ($editPdf['category'] ?? '') === 'Lainnya' ? 'selected' : '' ?>>Lainnya</option>
                                    </select>
                                </div>

                                <div class="col-md-6">
                                    <label class="form-label">File PDF <?= $editPdf ? '' : '<span class="text-danger">*</span>' ?></label>
                                    <input type="file" name="pdf_file" class="form-control" accept=".pdf"
                                           <?= $editPdf ? '' : 'required' ?>>
                                    <?php if ($editPdf && !empty($editPdf['pdf_url'])): ?>
                                        <small class="text-muted">
                                            File saat ini: <a href="<?= htmlspecialchars($editPdf['pdf_url']) ?>" target="_blank">Lihat PDF</a>
                                            <br>Upload file baru jika ingin mengganti
                                        </small>
                                    <?php endif; ?>
                                </div>

                                <div class="col-12">
                                    <label class="form-label">Deskripsi</label>
                                    <textarea name="description" class="form-control" rows="3"
                                              placeholder="Deskripsi singkat tentang e-book ini..."><?= htmlspecialchars($editPdf['description'] ?? '') ?></textarea>
                                </div>

                                <div class="col-12">
                                    <div class="form-check">
                                        <input type="checkbox" name="is_premium" class="form-check-input" id="isPremium"
                                               <?= ($editPdf['is_premium'] ?? 0) ? 'checked' : '' ?>>
                                        <label class="form-check-label" for="isPremium">
                                            <i class="bi bi-star-fill text-warning"></i> E-Book Premium (Hanya untuk user premium)
                                        </label>
                                    </div>
                                </div>
                            </div>

                            <div class="mt-3">
                                <button type="submit" class="btn btn-primary">
                                    <i class="bi bi-save"></i> <?= $editPdf ? 'Update' : 'Simpan' ?> E-Book
                                </button>
                                <?php if ($editPdf): ?>
                                    <a href="admin_pdfs.php" class="btn btn-secondary">
                                        <i class="bi bi-x-circle"></i> Batal
                                    </a>
                                <?php endif; ?>
                            </div>
                        </form>
                    </div>
                </div>

                <!-- PDF List -->
                <div class="card">
                    <div class="card-header">
                        <h5 class="mb-0"><i class="bi bi-list"></i> Daftar E-Book (<?= count($pdfs) ?>)</h5>
                    </div>
                    <div class="card-body">
                        <?php if (empty($pdfs)): ?>
                            <div class="text-center py-5">
                                <i class="bi bi-inbox display-1 text-muted"></i>
                                <p class="text-muted mt-3">Belum ada e-book. Tambahkan e-book pertama Anda!</p>
                            </div>
                        <?php else: ?>
                            <div class="row g-3">
                                <?php foreach ($pdfs as $pdf): ?>
                                    <div class="col-md-4">
                                        <div class="card pdf-card h-100">
                                            <div class="card-body text-center">
                                                <i class="bi bi-file-pdf pdf-icon mb-3"></i>
                                                <h5 class="card-title"><?= htmlspecialchars($pdf['title']) ?></h5>
                                                <p class="text-muted mb-2">
                                                    <small><i class="bi bi-person"></i> <?= htmlspecialchars($pdf['author']) ?></small>
                                                </p>
                                                <span class="badge bg-secondary mb-2"><?= htmlspecialchars($pdf['category']) ?></span>
                                                <?php if ($pdf['is_premium']): ?>
                                                    <span class="badge badge-premium mb-2"><i class="bi bi-star-fill"></i> Premium</span>
                                                <?php endif; ?>
                                                <?php if (!empty($pdf['description'])): ?>
                                                    <p class="text-muted small mt-2"><?= htmlspecialchars(substr($pdf['description'], 0, 100)) ?><?= strlen($pdf['description']) > 100 ? '...' : '' ?></p>
                                                <?php endif; ?>
                                            </div>
                                            <div class="card-footer bg-light">
                                                <div class="btn-group w-100">
                                                    <a href="<?= htmlspecialchars($pdf['pdf_url']) ?>" target="_blank" class="btn btn-sm btn-outline-primary">
                                                        <i class="bi bi-eye"></i> Lihat
                                                    </a>
                                                    <a href="?action=edit&id=<?= $pdf['id'] ?>" class="btn btn-sm btn-outline-warning">
                                                        <i class="bi bi-pencil"></i> Edit
                                                    </a>
                                                    <a href="?action=delete&id=<?= $pdf['id'] ?>"
                                                       class="btn btn-sm btn-outline-danger"
                                                       onclick="return confirm('Yakin ingin menghapus e-book ini?')">
                                                        <i class="bi bi-trash"></i> Hapus
                                                    </a>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                <?php endforeach; ?>
                            </div>
                        <?php endif; ?>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
