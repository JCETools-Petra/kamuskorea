<?php
require_once __DIR__ . '/admin_config.php';
requireAdminLogin();

$pdo = getAdminDB();
$message = '';
$error = '';

$action = $_GET['action'] ?? 'list';
$assessment_id = $_GET['assessment_id'] ?? null;

// Get current assessment info
$currentAssessment = null;
if ($assessment_id) {
    $stmt = $pdo->prepare("SELECT * FROM assessments WHERE id = ?");
    $stmt->execute([(int)$assessment_id]);
    $currentAssessment = $stmt->fetch();
}

// Delete question
if ($action === 'delete' && isset($_GET['id'])) {
    $id = (int)$_GET['id'];
    try {
        $stmt = $pdo->prepare("DELETE FROM questions WHERE id = ?");
        $stmt->execute([$id]);
        $message = 'Soal berhasil dihapus!';
    } catch (Exception $e) {
        $error = 'Gagal menghapus soal: ' . $e->getMessage();
    }
    $action = 'list';
}

// Save question
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['save_question'])) {
    $id = $_POST['id'] ?? null;
    $q_assessment_id = (int)($_POST['assessment_id'] ?? $assessment_id);
    $question_text = trim($_POST['question_text'] ?? '');
    $question_type = $_POST['question_type'] ?? 'text';
    $media_url = trim($_POST['media_url'] ?? '');
    $option_a = trim($_POST['option_a'] ?? '');
    $option_b = trim($_POST['option_b'] ?? '');
    $option_c = trim($_POST['option_c'] ?? '');
    $option_d = trim($_POST['option_d'] ?? '');
    $correct_answer = strtoupper($_POST['correct_answer'] ?? 'A');
    $explanation = trim($_POST['explanation'] ?? '');
    $order_index = (int)($_POST['order_index'] ?? 0);

    if (empty($question_text)) {
        $error = 'Teks soal tidak boleh kosong!';
    } elseif (empty($option_a) || empty($option_b) || empty($option_c) || empty($option_d)) {
        $error = 'Semua pilihan jawaban harus diisi!';
    } else {
        try {
            if ($id) {
                $stmt = $pdo->prepare("UPDATE questions SET assessment_id = ?, question_text = ?, question_type = ?, media_url = ?, option_a = ?, option_b = ?, option_c = ?, option_d = ?, correct_answer = ?, explanation = ?, order_index = ? WHERE id = ?");
                $stmt->execute([$q_assessment_id, $question_text, $question_type, $media_url, $option_a, $option_b, $option_c, $option_d, $correct_answer, $explanation, $order_index, $id]);
                $message = 'Soal berhasil diperbarui!';
            } else {
                $stmt = $pdo->prepare("INSERT INTO questions (assessment_id, question_text, question_type, media_url, option_a, option_b, option_c, option_d, correct_answer, explanation, order_index) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                $stmt->execute([$q_assessment_id, $question_text, $question_type, $media_url, $option_a, $option_b, $option_c, $option_d, $correct_answer, $explanation, $order_index]);
                $message = 'Soal berhasil ditambahkan!';
            }
            $action = 'list';
        } catch (Exception $e) {
            $error = 'Gagal menyimpan soal: ' . $e->getMessage();
        }
    }
}

// Get question for editing
$editQuestion = null;
if ($action === 'edit' && isset($_GET['id'])) {
    $stmt = $pdo->prepare("SELECT * FROM questions WHERE id = ?");
    $stmt->execute([(int)$_GET['id']]);
    $editQuestion = $stmt->fetch();
    if (!$editQuestion) {
        $error = 'Soal tidak ditemukan!';
        $action = 'list';
    } else {
        $assessment_id = $editQuestion['assessment_id'];
    }
}

// Get all assessments for dropdown
$assessments = $pdo->query("SELECT id, title, type FROM assessments ORDER BY type, order_index")->fetchAll();

// Get questions
$questions = [];
if ($action === 'list') {
    $sql = "SELECT q.*, a.title as assessment_title, a.type as assessment_type
            FROM questions q
            JOIN assessments a ON q.assessment_id = a.id";
    $params = [];

    if ($assessment_id) {
        $sql .= " WHERE q.assessment_id = ?";
        $params[] = (int)$assessment_id;
    }

    $sql .= " ORDER BY a.type, a.order_index, q.order_index ASC";

    $stmt = $pdo->prepare($sql);
    $stmt->execute($params);
    $questions = $stmt->fetchAll();
}
?>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Kelola Soal - Admin Kamus Korea</title>
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
        .question-card {
            border-left: 4px solid #667eea;
            margin-bottom: 15px;
        }
        .correct-answer {
            background: #d4edda;
            border-color: #28a745;
        }
        .upload-area {
            border: 2px dashed #ccc;
            border-radius: 10px;
            padding: 20px;
            text-align: center;
            cursor: pointer;
            transition: all 0.3s;
            background: #f8f9fa;
        }
        .upload-area:hover {
            border-color: #667eea;
            background: #e8f4ff;
        }
        .upload-area.dragover {
            border-color: #28a745;
            background: #d4edda;
        }
        .media-preview {
            max-width: 100%;
            max-height: 300px;
            margin-top: 10px;
            border-radius: 8px;
        }
        .upload-progress {
            display: none;
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
                    <a href="admin_questions.php" class="active"><i class="bi bi-list-check"></i> Semua Soal</a>
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
                    <!-- List Questions -->
                    <div class="d-flex justify-content-between align-items-center mb-4">
                        <div>
                            <h2><i class="bi bi-list-check"></i> Kelola Soal</h2>
                            <?php if ($currentAssessment): ?>
                                <p class="text-muted mb-0">
                                    <span class="badge bg-<?= $currentAssessment['type'] === 'quiz' ? 'primary' : 'danger' ?>">
                                        <?= ucfirst($currentAssessment['type']) ?>
                                    </span>
                                    <?= htmlspecialchars($currentAssessment['title']) ?>
                                </p>
                            <?php endif; ?>
                        </div>
                        <div>
                            <?php if ($assessment_id): ?>
                                <a href="admin_questions.php" class="btn btn-outline-secondary me-2">
                                    <i class="bi bi-list"></i> Lihat Semua
                                </a>
                            <?php endif; ?>
                            <a href="?action=add<?= $assessment_id ? '&assessment_id=' . $assessment_id : '' ?>" class="btn btn-success">
                                <i class="bi bi-plus-circle"></i> Tambah Soal
                            </a>
                        </div>
                    </div>

                    <!-- Filter by assessment -->
                    <div class="card mb-4">
                        <div class="card-body">
                            <form method="GET" class="row g-3">
                                <div class="col-md-8">
                                    <label class="form-label">Filter berdasarkan Quiz/Ujian:</label>
                                    <select name="assessment_id" class="form-select" onchange="this.form.submit()">
                                        <option value="">-- Semua --</option>
                                        <optgroup label="Quiz">
                                            <?php foreach ($assessments as $a): ?>
                                                <?php if ($a['type'] === 'quiz'): ?>
                                                    <option value="<?= $a['id'] ?>" <?= $assessment_id == $a['id'] ? 'selected' : '' ?>>
                                                        <?= htmlspecialchars($a['title']) ?>
                                                    </option>
                                                <?php endif; ?>
                                            <?php endforeach; ?>
                                        </optgroup>
                                        <optgroup label="Ujian/UBT">
                                            <?php foreach ($assessments as $a): ?>
                                                <?php if ($a['type'] === 'exam'): ?>
                                                    <option value="<?= $a['id'] ?>" <?= $assessment_id == $a['id'] ? 'selected' : '' ?>>
                                                        <?= htmlspecialchars($a['title']) ?>
                                                    </option>
                                                <?php endif; ?>
                                            <?php endforeach; ?>
                                        </optgroup>
                                    </select>
                                </div>
                                <div class="col-md-4 d-flex align-items-end">
                                    <span class="badge bg-info fs-6">
                                        Total: <?= count($questions) ?> soal
                                    </span>
                                </div>
                            </form>
                        </div>
                    </div>

                    <!-- Questions List -->
                    <?php if (empty($questions)): ?>
                        <div class="alert alert-info">
                            <i class="bi bi-info-circle"></i> Belum ada soal.
                            <?php if ($assessment_id): ?>
                                <a href="?action=add&assessment_id=<?= $assessment_id ?>" class="alert-link">Tambah soal pertama</a>
                            <?php endif; ?>
                        </div>
                    <?php else: ?>
                        <?php foreach ($questions as $index => $q): ?>
                            <div class="card question-card">
                                <div class="card-header d-flex justify-content-between align-items-center">
                                    <div>
                                        <strong>Soal #<?= $index + 1 ?></strong>
                                        <span class="badge bg-<?= $q['assessment_type'] === 'quiz' ? 'primary' : 'danger' ?> ms-2">
                                            <?= ucfirst($q['assessment_type']) ?>
                                        </span>
                                        <small class="text-muted ms-2"><?= htmlspecialchars($q['assessment_title']) ?></small>
                                    </div>
                                    <div>
                                        <a href="?action=edit&id=<?= $q['id'] ?>" class="btn btn-sm btn-warning">
                                            <i class="bi bi-pencil"></i> Edit
                                        </a>
                                        <a href="?action=delete&id=<?= $q['id'] ?><?= $assessment_id ? '&assessment_id=' . $assessment_id : '' ?>"
                                           class="btn btn-sm btn-danger"
                                           onclick="return confirm('Yakin ingin menghapus soal ini?')">
                                            <i class="bi bi-trash"></i> Hapus
                                        </a>
                                    </div>
                                </div>
                                <div class="card-body">
                                    <p class="card-text mb-3"><?= nl2br(htmlspecialchars($q['question_text'])) ?></p>

                                    <?php if ($q['media_url']): ?>
                                        <div class="mb-3">
                                            <small class="text-muted d-block mb-2">Media:</small>
                                            <?php
                                            $mediaUrl = $q['media_url'];
                                            $isImage = preg_match('/\.(jpg|jpeg|png|gif|webp)$/i', $mediaUrl);
                                            $isAudio = preg_match('/\.(mp3|wav|ogg|webm)$/i', $mediaUrl);
                                            $isVideo = preg_match('/\.(mp4|webm|ogv)$/i', $mediaUrl);
                                            ?>
                                            <?php if ($isImage): ?>
                                                <img src="<?= htmlspecialchars($mediaUrl) ?>" class="img-thumbnail" style="max-height: 200px;">
                                            <?php elseif ($isAudio): ?>
                                                <audio controls class="w-100">
                                                    <source src="<?= htmlspecialchars($mediaUrl) ?>">
                                                </audio>
                                            <?php elseif ($isVideo): ?>
                                                <video controls style="max-height: 200px;">
                                                    <source src="<?= htmlspecialchars($mediaUrl) ?>">
                                                </video>
                                            <?php else: ?>
                                                <a href="<?= htmlspecialchars($mediaUrl) ?>" target="_blank" class="btn btn-sm btn-outline-primary">
                                                    <i class="bi bi-link-45deg"></i> Lihat Media
                                                </a>
                                            <?php endif; ?>
                                            <div class="mt-1">
                                                <small><a href="<?= htmlspecialchars($mediaUrl) ?>" target="_blank" class="text-muted"><?= htmlspecialchars($mediaUrl) ?></a></small>
                                            </div>
                                        </div>
                                    <?php endif; ?>

                                    <div class="row">
                                        <div class="col-md-6">
                                            <div class="p-2 rounded mb-1 <?= $q['correct_answer'] === 'A' ? 'correct-answer' : 'bg-light' ?>">
                                                <strong>A.</strong> <?= htmlspecialchars($q['option_a']) ?>
                                                <?php if ($q['correct_answer'] === 'A'): ?>
                                                    <i class="bi bi-check-circle-fill text-success"></i>
                                                <?php endif; ?>
                                            </div>
                                            <div class="p-2 rounded mb-1 <?= $q['correct_answer'] === 'B' ? 'correct-answer' : 'bg-light' ?>">
                                                <strong>B.</strong> <?= htmlspecialchars($q['option_b']) ?>
                                                <?php if ($q['correct_answer'] === 'B'): ?>
                                                    <i class="bi bi-check-circle-fill text-success"></i>
                                                <?php endif; ?>
                                            </div>
                                        </div>
                                        <div class="col-md-6">
                                            <div class="p-2 rounded mb-1 <?= $q['correct_answer'] === 'C' ? 'correct-answer' : 'bg-light' ?>">
                                                <strong>C.</strong> <?= htmlspecialchars($q['option_c']) ?>
                                                <?php if ($q['correct_answer'] === 'C'): ?>
                                                    <i class="bi bi-check-circle-fill text-success"></i>
                                                <?php endif; ?>
                                            </div>
                                            <div class="p-2 rounded mb-1 <?= $q['correct_answer'] === 'D' ? 'correct-answer' : 'bg-light' ?>">
                                                <strong>D.</strong> <?= htmlspecialchars($q['option_d']) ?>
                                                <?php if ($q['correct_answer'] === 'D'): ?>
                                                    <i class="bi bi-check-circle-fill text-success"></i>
                                                <?php endif; ?>
                                            </div>
                                        </div>
                                    </div>

                                    <?php if ($q['explanation']): ?>
                                        <div class="mt-3 p-2 bg-warning bg-opacity-10 rounded">
                                            <small><strong>Penjelasan:</strong> <?= htmlspecialchars($q['explanation']) ?></small>
                                        </div>
                                    <?php endif; ?>
                                </div>
                            </div>
                        <?php endforeach; ?>
                    <?php endif; ?>

                <?php elseif ($action === 'add' || $action === 'edit'): ?>
                    <!-- Add/Edit Question Form -->
                    <div class="d-flex justify-content-between align-items-center mb-4">
                        <h2>
                            <i class="bi bi-<?= $action === 'add' ? 'plus-circle' : 'pencil' ?>"></i>
                            <?= $action === 'add' ? 'Tambah' : 'Edit' ?> Soal
                        </h2>
                        <a href="admin_questions.php<?= $assessment_id ? '?assessment_id=' . $assessment_id : '' ?>" class="btn btn-secondary">
                            <i class="bi bi-arrow-left"></i> Kembali
                        </a>
                    </div>

                    <div class="card">
                        <div class="card-body">
                            <form method="POST">
                                <input type="hidden" name="id" value="<?= $editQuestion['id'] ?? '' ?>">

                                <div class="mb-3">
                                    <label class="form-label">Quiz/Ujian *</label>
                                    <select name="assessment_id" class="form-select" required>
                                        <option value="">-- Pilih Quiz/Ujian --</option>
                                        <optgroup label="Quiz">
                                            <?php foreach ($assessments as $a): ?>
                                                <?php if ($a['type'] === 'quiz'): ?>
                                                    <option value="<?= $a['id'] ?>" <?= ($editQuestion['assessment_id'] ?? $assessment_id) == $a['id'] ? 'selected' : '' ?>>
                                                        <?= htmlspecialchars($a['title']) ?>
                                                    </option>
                                                <?php endif; ?>
                                            <?php endforeach; ?>
                                        </optgroup>
                                        <optgroup label="Ujian/UBT">
                                            <?php foreach ($assessments as $a): ?>
                                                <?php if ($a['type'] === 'exam'): ?>
                                                    <option value="<?= $a['id'] ?>" <?= ($editQuestion['assessment_id'] ?? $assessment_id) == $a['id'] ? 'selected' : '' ?>>
                                                        <?= htmlspecialchars($a['title']) ?>
                                                    </option>
                                                <?php endif; ?>
                                            <?php endforeach; ?>
                                        </optgroup>
                                    </select>
                                </div>

                                <div class="mb-3">
                                    <label class="form-label">Teks Soal *</label>
                                    <textarea name="question_text" class="form-control" rows="4" required
                                              placeholder="Tuliskan pertanyaan di sini..."><?= htmlspecialchars($editQuestion['question_text'] ?? '') ?></textarea>
                                </div>

                                <div class="row">
                                    <div class="col-md-6">
                                        <div class="mb-3">
                                            <label class="form-label">Tipe Soal</label>
                                            <select name="question_type" id="question_type" class="form-select">
                                                <option value="text" <?= ($editQuestion['question_type'] ?? 'text') === 'text' ? 'selected' : '' ?>>Teks</option>
                                                <option value="image" <?= ($editQuestion['question_type'] ?? '') === 'image' ? 'selected' : '' ?>>Dengan Gambar</option>
                                                <option value="audio" <?= ($editQuestion['question_type'] ?? '') === 'audio' ? 'selected' : '' ?>>Dengan Audio</option>
                                                <option value="video" <?= ($editQuestion['question_type'] ?? '') === 'video' ? 'selected' : '' ?>>Dengan Video</option>
                                            </select>
                                        </div>
                                    </div>
                                    <div class="col-md-6">
                                        <div class="mb-3">
                                            <label class="form-label">URL Media</label>
                                            <input type="url" name="media_url" id="media_url" class="form-control"
                                                   value="<?= htmlspecialchars($editQuestion['media_url'] ?? '') ?>"
                                                   placeholder="Upload file atau masukkan URL manual">
                                            <small class="text-muted">URL akan terisi otomatis setelah upload</small>
                                        </div>
                                    </div>
                                </div>

                                <!-- Media Upload Section -->
                                <div class="card bg-light mb-3" id="mediaUploadSection">
                                    <div class="card-body">
                                        <h5 class="card-title"><i class="bi bi-cloud-upload"></i> Upload Media</h5>
                                        <div class="upload-area" id="uploadArea">
                                            <i class="bi bi-cloud-arrow-up fs-1 text-muted"></i>
                                            <p class="mb-1">Drag & drop file di sini atau <strong>klik untuk pilih file</strong></p>
                                            <small class="text-muted">Mendukung: JPG, PNG, GIF, WEBP, MP3, WAV, MP4 (Max 50MB)</small>
                                            <input type="file" id="mediaFileInput" class="d-none"
                                                   accept="image/*,audio/*,video/*">
                                        </div>

                                        <!-- Upload Progress -->
                                        <div class="upload-progress mt-3" id="uploadProgress">
                                            <div class="d-flex align-items-center">
                                                <div class="spinner-border spinner-border-sm text-primary me-2" role="status">
                                                    <span class="visually-hidden">Uploading...</span>
                                                </div>
                                                <span>Sedang mengupload...</span>
                                            </div>
                                            <div class="progress mt-2">
                                                <div class="progress-bar progress-bar-striped progress-bar-animated"
                                                     id="progressBar" role="progressbar" style="width: 0%"></div>
                                            </div>
                                        </div>

                                        <!-- Media Preview -->
                                        <div id="mediaPreview" class="mt-3" style="display: none;">
                                            <div class="d-flex justify-content-between align-items-center mb-2">
                                                <strong>Preview:</strong>
                                                <button type="button" class="btn btn-sm btn-outline-danger" id="removeMedia">
                                                    <i class="bi bi-trash"></i> Hapus Media
                                                </button>
                                            </div>
                                            <div id="previewContent"></div>
                                        </div>

                                        <!-- Upload Status -->
                                        <div id="uploadStatus" class="mt-2"></div>
                                    </div>
                                </div>

                                <div class="card bg-light mb-3">
                                    <div class="card-body">
                                        <h5 class="card-title">Pilihan Jawaban</h5>
                                        <div class="row">
                                            <div class="col-md-6">
                                                <div class="mb-3">
                                                    <label class="form-label">Pilihan A *</label>
                                                    <input type="text" name="option_a" class="form-control" required
                                                           value="<?= htmlspecialchars($editQuestion['option_a'] ?? '') ?>">
                                                </div>
                                            </div>
                                            <div class="col-md-6">
                                                <div class="mb-3">
                                                    <label class="form-label">Pilihan B *</label>
                                                    <input type="text" name="option_b" class="form-control" required
                                                           value="<?= htmlspecialchars($editQuestion['option_b'] ?? '') ?>">
                                                </div>
                                            </div>
                                        </div>
                                        <div class="row">
                                            <div class="col-md-6">
                                                <div class="mb-3">
                                                    <label class="form-label">Pilihan C *</label>
                                                    <input type="text" name="option_c" class="form-control" required
                                                           value="<?= htmlspecialchars($editQuestion['option_c'] ?? '') ?>">
                                                </div>
                                            </div>
                                            <div class="col-md-6">
                                                <div class="mb-3">
                                                    <label class="form-label">Pilihan D *</label>
                                                    <input type="text" name="option_d" class="form-control" required
                                                           value="<?= htmlspecialchars($editQuestion['option_d'] ?? '') ?>">
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                <div class="row">
                                    <div class="col-md-6">
                                        <div class="mb-3">
                                            <label class="form-label">Jawaban Benar *</label>
                                            <select name="correct_answer" class="form-select" required>
                                                <option value="A" <?= ($editQuestion['correct_answer'] ?? 'A') === 'A' ? 'selected' : '' ?>>A</option>
                                                <option value="B" <?= ($editQuestion['correct_answer'] ?? '') === 'B' ? 'selected' : '' ?>>B</option>
                                                <option value="C" <?= ($editQuestion['correct_answer'] ?? '') === 'C' ? 'selected' : '' ?>>C</option>
                                                <option value="D" <?= ($editQuestion['correct_answer'] ?? '') === 'D' ? 'selected' : '' ?>>D</option>
                                            </select>
                                        </div>
                                    </div>
                                    <div class="col-md-6">
                                        <div class="mb-3">
                                            <label class="form-label">Urutan Soal</label>
                                            <input type="number" name="order_index" class="form-control" min="0"
                                                   value="<?= $editQuestion['order_index'] ?? 0 ?>">
                                        </div>
                                    </div>
                                </div>

                                <div class="mb-3">
                                    <label class="form-label">Penjelasan (Opsional)</label>
                                    <textarea name="explanation" class="form-control" rows="3"
                                              placeholder="Penjelasan mengapa jawaban tersebut benar..."><?= htmlspecialchars($editQuestion['explanation'] ?? '') ?></textarea>
                                </div>

                                <button type="submit" name="save_question" class="btn btn-primary">
                                    <i class="bi bi-save"></i> Simpan Soal
                                </button>

                                <?php if ($action === 'add'): ?>
                                    <button type="submit" name="save_question" class="btn btn-success" onclick="this.form.action='?action=add<?= $assessment_id ? '&assessment_id=' . $assessment_id : '' ?>'">
                                        <i class="bi bi-plus"></i> Simpan & Tambah Lagi
                                    </button>
                                <?php endif; ?>
                            </form>
                        </div>
                    </div>
                <?php endif; ?>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script>
    document.addEventListener('DOMContentLoaded', function() {
        const uploadArea = document.getElementById('uploadArea');
        const mediaFileInput = document.getElementById('mediaFileInput');
        const mediaUrlInput = document.getElementById('media_url');
        const questionTypeSelect = document.getElementById('question_type');
        const uploadProgress = document.getElementById('uploadProgress');
        const progressBar = document.getElementById('progressBar');
        const mediaPreview = document.getElementById('mediaPreview');
        const previewContent = document.getElementById('previewContent');
        const uploadStatus = document.getElementById('uploadStatus');
        const removeMediaBtn = document.getElementById('removeMedia');

        if (!uploadArea) return; // Only run on add/edit page

        // Click to upload
        uploadArea.addEventListener('click', () => mediaFileInput.click());

        // Drag and drop
        uploadArea.addEventListener('dragover', (e) => {
            e.preventDefault();
            uploadArea.classList.add('dragover');
        });

        uploadArea.addEventListener('dragleave', () => {
            uploadArea.classList.remove('dragover');
        });

        uploadArea.addEventListener('drop', (e) => {
            e.preventDefault();
            uploadArea.classList.remove('dragover');
            const files = e.dataTransfer.files;
            if (files.length > 0) {
                handleFileUpload(files[0]);
            }
        });

        // File input change
        mediaFileInput.addEventListener('change', (e) => {
            if (e.target.files.length > 0) {
                handleFileUpload(e.target.files[0]);
            }
        });

        // Remove media button
        if (removeMediaBtn) {
            removeMediaBtn.addEventListener('click', () => {
                mediaUrlInput.value = '';
                mediaPreview.style.display = 'none';
                previewContent.innerHTML = '';
                uploadStatus.innerHTML = '';
                questionTypeSelect.value = 'text';
            });
        }

        // Load existing preview if URL exists
        if (mediaUrlInput && mediaUrlInput.value) {
            showPreview(mediaUrlInput.value, questionTypeSelect.value);
        }

        function handleFileUpload(file) {
            // Validate file size (50MB)
            if (file.size > 50 * 1024 * 1024) {
                showError('File terlalu besar. Maksimal 50MB');
                return;
            }

            // Validate file type
            const allowedTypes = [
                'image/jpeg', 'image/png', 'image/gif', 'image/webp',
                'audio/mpeg', 'audio/mp3', 'audio/wav', 'audio/ogg', 'audio/webm',
                'video/mp4', 'video/webm', 'video/ogg'
            ];

            if (!allowedTypes.includes(file.type)) {
                showError('Tipe file tidak didukung');
                return;
            }

            // Show progress
            uploadProgress.style.display = 'block';
            uploadStatus.innerHTML = '';
            progressBar.style.width = '0%';

            // Create FormData
            const formData = new FormData();
            formData.append('media_file', file);

            // Upload via AJAX
            const xhr = new XMLHttpRequest();
            xhr.open('POST', 'admin_upload_media.php', true);

            // Progress tracking
            xhr.upload.addEventListener('progress', (e) => {
                if (e.lengthComputable) {
                    const percentComplete = (e.loaded / e.total) * 100;
                    progressBar.style.width = percentComplete + '%';
                }
            });

            xhr.onload = function() {
                uploadProgress.style.display = 'none';
                progressBar.style.width = '0%';

                try {
                    const response = JSON.parse(xhr.responseText);
                    if (response.success) {
                        // Set URL field
                        mediaUrlInput.value = response.url;

                        // Auto-select question type
                        if (response.type === 'image') {
                            questionTypeSelect.value = 'image';
                        } else if (response.type === 'audio') {
                            questionTypeSelect.value = 'audio';
                        } else if (response.type === 'video') {
                            questionTypeSelect.value = 'video';
                        }

                        // Show preview
                        showPreview(response.url, response.type);

                        // Show success message
                        const fileSize = (response.size / 1024 / 1024).toFixed(2);
                        uploadStatus.innerHTML = `
                            <div class="alert alert-success py-2">
                                <i class="bi bi-check-circle"></i>
                                File berhasil diupload (${fileSize} MB)
                            </div>
                        `;
                    } else {
                        showError(response.message || 'Upload gagal');
                    }
                } catch (e) {
                    showError('Terjadi kesalahan saat upload');
                }
            };

            xhr.onerror = function() {
                uploadProgress.style.display = 'none';
                showError('Network error. Pastikan koneksi internet stabil');
            };

            xhr.send(formData);
        }

        function showPreview(url, type) {
            previewContent.innerHTML = '';
            mediaPreview.style.display = 'block';

            if (type === 'image' || url.match(/\.(jpg|jpeg|png|gif|webp)$/i)) {
                previewContent.innerHTML = `
                    <img src="${url}" class="media-preview img-thumbnail" alt="Preview">
                `;
            } else if (type === 'audio' || url.match(/\.(mp3|wav|ogg|webm)$/i)) {
                previewContent.innerHTML = `
                    <audio controls class="w-100">
                        <source src="${url}">
                        Browser tidak mendukung audio player.
                    </audio>
                `;
            } else if (type === 'video' || url.match(/\.(mp4|webm|ogv)$/i)) {
                previewContent.innerHTML = `
                    <video controls class="media-preview">
                        <source src="${url}">
                        Browser tidak mendukung video player.
                    </video>
                `;
            } else {
                previewContent.innerHTML = `
                    <a href="${url}" target="_blank" class="btn btn-outline-primary">
                        <i class="bi bi-link-45deg"></i> Lihat Media
                    </a>
                `;
            }
        }

        function showError(message) {
            uploadStatus.innerHTML = `
                <div class="alert alert-danger py-2">
                    <i class="bi bi-exclamation-circle"></i> ${message}
                </div>
            `;
        }
    });
    </script>
</body>
</html>
