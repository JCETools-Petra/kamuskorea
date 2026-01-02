<?php
require_once __DIR__ . '/admin_config.php';
requireAdminLogin();

$pdo = getAdminDB();
$message = '';
$error = '';

$action = $_GET['action'] ?? 'list';

// Delete video
if ($action === 'delete' && isset($_GET['id'])) {
    $id = (int)$_GET['id'];
    try {
        $stmt = $pdo->prepare("DELETE FROM video_hafalan WHERE id = ?");
        $stmt->execute([$id]);
        $message = 'Video berhasil dihapus!';
    } catch (Exception $e) {
        $error = 'Gagal menghapus video: ' . $e->getMessage();
    }
    $action = 'list';
}

// Handle video file upload
$uploadDir = __DIR__ . '/uploads/videos/';
if (!is_dir($uploadDir)) {
    mkdir($uploadDir, 0755, true);
}

// Save video
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['save_video'])) {
    $id = $_POST['id'] ?? null;
    $title = trim($_POST['title'] ?? '');
    $description = trim($_POST['description'] ?? '');
    $video_url = trim($_POST['video_url'] ?? '');
    $thumbnail_url = trim($_POST['thumbnail_url'] ?? '');
    $duration_minutes = (int)($_POST['duration_minutes'] ?? 0);
    $category = trim($_POST['category'] ?? '');
    $order_index = (int)($_POST['order_index'] ?? 0);
    $is_premium = isset($_POST['is_premium']) ? 1 : 0;

    // Handle video file upload
    if (isset($_FILES['video_file']) && $_FILES['video_file']['error'] === UPLOAD_ERR_OK) {
        $allowedTypes = ['video/mp4', 'video/webm', 'video/ogg', 'video/quicktime'];
        $fileType = $_FILES['video_file']['type'];

        if (!in_array($fileType, $allowedTypes)) {
            $error = 'Format video tidak didukung! Gunakan MP4, WebM, atau OGG.';
        } else {
            $extension = pathinfo($_FILES['video_file']['name'], PATHINFO_EXTENSION);
            $fileName = 'video_' . time() . '_' . uniqid() . '.' . $extension;
            $targetPath = $uploadDir . $fileName;

            if (move_uploaded_file($_FILES['video_file']['tmp_name'], $targetPath)) {
                $video_url = 'https://webtechsolution.my.id/kamuskorea/uploads/videos/' . $fileName;
            } else {
                $error = 'Gagal mengupload video!';
            }
        }
    }

    // Handle thumbnail upload
    if (isset($_FILES['thumbnail_file']) && $_FILES['thumbnail_file']['error'] === UPLOAD_ERR_OK) {
        $allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'];
        $fileType = $_FILES['thumbnail_file']['type'];

        if (!in_array($fileType, $allowedTypes)) {
            $error = 'Format thumbnail tidak didukung! Gunakan JPG, PNG, atau WebP.';
        } else {
            $extension = pathinfo($_FILES['thumbnail_file']['name'], PATHINFO_EXTENSION);
            $fileName = 'thumb_' . time() . '_' . uniqid() . '.' . $extension;
            $targetPath = $uploadDir . $fileName;

            if (move_uploaded_file($_FILES['thumbnail_file']['tmp_name'], $targetPath)) {
                $thumbnail_url = 'https://webtechsolution.my.id/kamuskorea/uploads/videos/' . $fileName;
            }
        }
    }

    if (empty($title)) {
        $error = 'Judul tidak boleh kosong!';
    } elseif (empty($video_url)) {
        $error = 'URL Video atau File Video tidak boleh kosong!';
    } else {
        try {
            if ($id) {
                $stmt = $pdo->prepare("UPDATE video_hafalan SET title = ?, description = ?, video_url = ?, thumbnail_url = ?, duration_minutes = ?, category = ?, order_index = ?, is_premium = ? WHERE id = ?");
                $stmt->execute([$title, $description, $video_url, $thumbnail_url, $duration_minutes, $category, $order_index, $is_premium, $id]);
                $message = 'Video berhasil diperbarui!';
            } else {
                $stmt = $pdo->prepare("INSERT INTO video_hafalan (title, description, video_url, thumbnail_url, duration_minutes, category, order_index, is_premium) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
                $stmt->execute([$title, $description, $video_url, $thumbnail_url, $duration_minutes, $category, $order_index, $is_premium]);
                $message = 'Video berhasil ditambahkan!';
            }
            $action = 'list';
        } catch (Exception $e) {
            $error = 'Gagal menyimpan video: ' . $e->getMessage();
        }
    }
}

// Get video for editing
$editVideo = null;
if ($action === 'edit' && isset($_GET['id'])) {
    $stmt = $pdo->prepare("SELECT * FROM video_hafalan WHERE id = ?");
    $stmt->execute([(int)$_GET['id']]);
    $editVideo = $stmt->fetch();
    if (!$editVideo) {
        $error = 'Video tidak ditemukan!';
        $action = 'list';
    }
}

// Get all videos
$videos = [];
if ($action === 'list') {
    $stmt = $pdo->query("SELECT * FROM video_hafalan ORDER BY order_index ASC, created_at DESC");
    $videos = $stmt->fetchAll();
}
?>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Kelola Video Hafalan - Admin Kamus Korea</title>
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
        .video-card {
            border-left: 4px solid #667eea;
            margin-bottom: 15px;
        }
        .video-thumbnail {
            max-width: 200px;
            max-height: 120px;
            border-radius: 8px;
        }
        .video-preview iframe {
            width: 100%;
            height: 315px;
            border-radius: 8px;
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
                    <a href="admin_video_hafalan.php" class="active"><i class="bi bi-camera-video"></i> Video Hafalan</a>
                    <hr class="text-white">
                    <a href="https://webtechsolution.my.id/kamuskorea/send_notification.php" target="_blank"><i class="bi bi-bell"></i> Push Notification</a>
                    <hr class="text-white">
                    <a href="admin_profile.php"><i class="bi bi-person-circle"></i> Profil</a>
                    <a href="admin_logout.php"><i class="bi bi-box-arrow-right"></i> Logout</a>
                </nav>
            </div>

            <?php
            // Alternatively, you can use the shared sidebar component:
            // require_once 'admin_sidebar.php';
            // renderAdminSidebar('video_hafalan');
            ?>

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
                    <!-- List Videos -->
                    <div class="d-flex justify-content-between align-items-center mb-4">
                        <div>
                            <h2><i class="bi bi-camera-video"></i> Kelola Video Hafalan</h2>
                            <p class="text-muted">Kelola video pembelajaran untuk fitur Video Hafalan</p>
                        </div>
                        <a href="?action=add" class="btn btn-success">
                            <i class="bi bi-plus-circle"></i> Tambah Video
                        </a>
                    </div>

                    <div class="card mb-3">
                        <div class="card-body">
                            <span class="badge bg-info fs-6">
                                Total: <?= count($videos) ?> video
                            </span>
                        </div>
                    </div>

                    <!-- Videos List -->
                    <?php if (empty($videos)): ?>
                        <div class="alert alert-info">
                            <i class="bi bi-info-circle"></i> Belum ada video.
                            <a href="?action=add" class="alert-link">Tambah video pertama</a>
                        </div>
                    <?php else: ?>
                        <?php foreach ($videos as $index => $video): ?>
                            <div class="card video-card">
                                <div class="card-header d-flex justify-content-between align-items-center">
                                    <div>
                                        <strong>Video #<?= $index + 1 ?></strong>
                                        <?php if ($video['is_premium']): ?>
                                            <span class="badge bg-warning text-dark ms-2">
                                                <i class="bi bi-star-fill"></i> Premium
                                            </span>
                                        <?php else: ?>
                                            <span class="badge bg-success ms-2">
                                                <i class="bi bi-unlock-fill"></i> Free
                                            </span>
                                        <?php endif; ?>
                                        <?php if (!empty($video['category'])): ?>
                                            <span class="badge bg-secondary ms-2"><?= htmlspecialchars($video['category']) ?></span>
                                        <?php endif; ?>
                                    </div>
                                    <div>
                                        <a href="?action=edit&id=<?= $video['id'] ?>" class="btn btn-sm btn-warning">
                                            <i class="bi bi-pencil"></i> Edit
                                        </a>
                                        <a href="?action=delete&id=<?= $video['id'] ?>"
                                           class="btn btn-sm btn-danger"
                                           onclick="return confirm('Yakin ingin menghapus video ini?')">
                                            <i class="bi bi-trash"></i> Hapus
                                        </a>
                                    </div>
                                </div>
                                <div class="card-body">
                                    <div class="row">
                                        <div class="col-md-3">
                                            <?php if (!empty($video['thumbnail_url'])): ?>
                                                <img src="<?= htmlspecialchars($video['thumbnail_url']) ?>"
                                                     class="video-thumbnail img-thumbnail"
                                                     alt="Thumbnail">
                                            <?php else: ?>
                                                <div class="bg-light p-4 text-center rounded">
                                                    <i class="bi bi-camera-video fs-1 text-muted"></i>
                                                    <p class="text-muted mb-0 small">No Thumbnail</p>
                                                </div>
                                            <?php endif; ?>
                                        </div>
                                        <div class="col-md-9">
                                            <h5><?= htmlspecialchars($video['title']) ?></h5>
                                            <?php if (!empty($video['description'])): ?>
                                                <p class="text-muted"><?= htmlspecialchars($video['description']) ?></p>
                                            <?php endif; ?>
                                            <div class="mb-2">
                                                <small class="text-muted">
                                                    <i class="bi bi-link-45deg"></i>
                                                    <a href="<?= htmlspecialchars($video['video_url']) ?>" target="_blank">
                                                        <?= htmlspecialchars($video['video_url']) ?>
                                                    </a>
                                                </small>
                                            </div>
                                            <?php if ($video['duration_minutes'] > 0): ?>
                                                <span class="badge bg-info">
                                                    <i class="bi bi-clock"></i> <?= $video['duration_minutes'] ?> menit
                                                </span>
                                            <?php endif; ?>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        <?php endforeach; ?>
                    <?php endif; ?>

                <?php elseif ($action === 'add' || $action === 'edit'): ?>
                    <!-- Add/Edit Video Form -->
                    <div class="d-flex justify-content-between align-items-center mb-4">
                        <h2>
                            <i class="bi bi-<?= $action === 'add' ? 'plus-circle' : 'pencil' ?>"></i>
                            <?= $action === 'add' ? 'Tambah' : 'Edit' ?> Video Hafalan
                        </h2>
                        <a href="admin_video_hafalan.php" class="btn btn-secondary">
                            <i class="bi bi-arrow-left"></i> Kembali
                        </a>
                    </div>

                    <div class="card">
                        <div class="card-body">
                            <form method="POST" enctype="multipart/form-data">
                                <input type="hidden" name="id" value="<?= $editVideo['id'] ?? '' ?>">

                                <div class="mb-3">
                                    <label class="form-label">Judul Video *</label>
                                    <input type="text" name="title" class="form-control" required
                                           value="<?= htmlspecialchars($editVideo['title'] ?? '') ?>"
                                           placeholder="Contoh: Pengenalan Hangeul">
                                </div>

                                <div class="mb-3">
                                    <label class="form-label">Deskripsi</label>
                                    <textarea name="description" class="form-control" rows="3"
                                              placeholder="Deskripsi singkat tentang video..."><?= htmlspecialchars($editVideo['description'] ?? '') ?></textarea>
                                </div>

                                <div class="alert alert-info">
                                    <strong>Pilihan Upload Video:</strong><br>
                                    1. Upload file video langsung dari komputer (MP4, WebM, OGG)<br>
                                    2. Masukkan URL YouTube atau URL video langsung
                                </div>

                                <div class="row">
                                    <div class="col-md-12">
                                        <div class="mb-3">
                                            <label class="form-label">
                                                <i class="bi bi-upload"></i> Upload File Video (Opsional)
                                            </label>
                                            <input type="file" name="video_file" class="form-control" accept="video/mp4,video/webm,video/ogg,video/quicktime">
                                            <small class="text-muted">
                                                Format: MP4, WebM, OGG, MOV. Maks 500MB. Jika upload file, URL di bawah akan diabaikan.
                                            </small>
                                        </div>
                                    </div>
                                </div>

                                <div class="text-center my-3">
                                    <strong>-- ATAU --</strong>
                                </div>

                                <div class="row">
                                    <div class="col-md-8">
                                        <div class="mb-3">
                                            <label class="form-label">URL Video (Opsional jika upload file)</label>
                                            <input type="url" name="video_url" id="video_url" class="form-control"
                                                   value="<?= htmlspecialchars($editVideo['video_url'] ?? '') ?>"
                                                   placeholder="https://www.youtube.com/watch?v=... atau URL langsung">
                                            <small class="text-muted">
                                                Mendukung YouTube, Vimeo, atau direct video URL (.mp4, .webm)
                                            </small>
                                        </div>
                                    </div>
                                    <div class="col-md-4">
                                        <div class="mb-3">
                                            <label class="form-label">Durasi (menit)</label>
                                            <input type="number" name="duration_minutes" class="form-control" min="0"
                                                   value="<?= $editVideo['duration_minutes'] ?? 0 ?>"
                                                   placeholder="15">
                                        </div>
                                    </div>
                                </div>

                                <div class="row">
                                    <div class="col-md-6">
                                        <div class="mb-3">
                                            <label class="form-label">
                                                <i class="bi bi-upload"></i> Upload Thumbnail (Opsional)
                                            </label>
                                            <input type="file" name="thumbnail_file" class="form-control" accept="image/jpeg,image/jpg,image/png,image/webp">
                                            <small class="text-muted">Format: JPG, PNG, WebP. Maks 5MB.</small>
                                        </div>
                                    </div>
                                    <div class="col-md-6">
                                        <div class="mb-3">
                                            <label class="form-label">URL Thumbnail (Opsional)</label>
                                            <input type="url" name="thumbnail_url" class="form-control"
                                                   value="<?= htmlspecialchars($editVideo['thumbnail_url'] ?? '') ?>"
                                                   placeholder="https://... (opsional)">
                                            <small class="text-muted">Atau masukkan URL gambar</small>
                                        </div>
                                    </div>
                                </div>

                                <div class="row">
                                    <div class="col-md-6">
                                        <div class="mb-3">
                                            <label class="form-label">Kategori</label>
                                            <select name="category" class="form-select">
                                                <option value="">-- Pilih Kategori --</option>
                                                <option value="Dasar" <?= ($editVideo['category'] ?? '') === 'Dasar' ? 'selected' : '' ?>>Dasar</option>
                                                <option value="Grammar" <?= ($editVideo['category'] ?? '') === 'Grammar' ? 'selected' : '' ?>>Grammar</option>
                                                <option value="Vocabulary" <?= ($editVideo['category'] ?? '') === 'Vocabulary' ? 'selected' : '' ?>>Vocabulary</option>
                                                <option value="Conversation" <?= ($editVideo['category'] ?? '') === 'Conversation' ? 'selected' : '' ?>>Conversation</option>
                                                <option value="Reading" <?= ($editVideo['category'] ?? '') === 'Reading' ? 'selected' : '' ?>>Reading</option>
                                                <option value="Listening" <?= ($editVideo['category'] ?? '') === 'Listening' ? 'selected' : '' ?>>Listening</option>
                                            </select>
                                        </div>
                                    </div>
                                    <div class="col-md-6">
                                        <div class="mb-3">
                                            <label class="form-label">Urutan Tampilan</label>
                                            <input type="number" name="order_index" class="form-control" min="0"
                                                   value="<?= $editVideo['order_index'] ?? 0 ?>">
                                            <small class="text-muted">Semakin kecil, semakin di atas</small>
                                        </div>
                                    </div>
                                </div>

                                <div class="mb-3">
                                    <div class="form-check form-switch">
                                        <input class="form-check-input" type="checkbox" name="is_premium" id="is_premium"
                                               <?= ($editVideo['is_premium'] ?? 0) ? 'checked' : '' ?>>
                                        <label class="form-check-label" for="is_premium">
                                            <strong>Premium Only</strong>
                                            <small class="text-muted d-block">Video hanya bisa diakses oleh pengguna premium</small>
                                        </label>
                                    </div>
                                </div>

                                <!-- Video Preview -->
                                <?php if (!empty($editVideo['video_url'])): ?>
                                    <div class="card bg-light mb-3">
                                        <div class="card-body">
                                            <h5 class="card-title">Preview Video</h5>
                                            <div class="video-preview">
                                                <?php
                                                $url = $editVideo['video_url'];
                                                // Check if YouTube
                                                if (preg_match('/youtube\.com\/watch\?v=([a-zA-Z0-9_-]+)/', $url, $matches)) {
                                                    $videoId = $matches[1];
                                                    echo '<iframe src="https://www.youtube.com/embed/' . $videoId . '" frameborder="0" allowfullscreen></iframe>';
                                                } elseif (preg_match('/youtu\.be\/([a-zA-Z0-9_-]+)/', $url, $matches)) {
                                                    $videoId = $matches[1];
                                                    echo '<iframe src="https://www.youtube.com/embed/' . $videoId . '" frameborder="0" allowfullscreen></iframe>';
                                                } elseif (preg_match('/\.(mp4|webm|ogg)$/i', $url)) {
                                                    echo '<video controls class="w-100" style="max-height: 400px;"><source src="' . htmlspecialchars($url) . '"></video>';
                                                } else {
                                                    echo '<p class="text-muted">Preview tidak tersedia. <a href="' . htmlspecialchars($url) . '" target="_blank">Buka video</a></p>';
                                                }
                                                ?>
                                            </div>
                                        </div>
                                    </div>
                                <?php endif; ?>

                                <button type="submit" name="save_video" class="btn btn-primary">
                                    <i class="bi bi-save"></i> Simpan Video
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
