<?php
require_once __DIR__ . '/admin_config.php';
requireAdminLogin();

$pdo = getAdminDB();
$message = '';
$error = '';

/**
 * Sanitize HTML content from Quill editor
 * Allows safe HTML tags while removing potentially dangerous content
 *
 * @param string|null $html The HTML content to sanitize
 * @return string Sanitized HTML
 */
function sanitizeQuillHtml($html) {
    if ($html === null || $html === '') {
        return '';
    }

    // Allowed HTML tags from Quill editor (safe subset)
    $allowedTags = '<p><br><strong><em><u><s><a><ul><ol><li><h1><h2><h3><h4><h5><h6><blockquote><code><pre><span><div>';

    // First pass: strip all tags except allowed ones
    $sanitized = strip_tags($html, $allowedTags);

    // Remove any event handlers (onclick, onerror, etc.)
    $sanitized = preg_replace('/\s*on\w+\s*=\s*["\'][^"\']*["\']/i', '', $sanitized);

    // Remove javascript: protocol
    $sanitized = preg_replace('/javascript\s*:/i', '', $sanitized);

    // Remove data: protocol (can be used for XSS)
    $sanitized = preg_replace('/data\s*:/i', '', $sanitized);

    // Clean up <a> tags to only allow href attribute with safe protocols
    $sanitized = preg_replace_callback('/<a\s+([^>]*)>/i', function($matches) {
        $attrs = $matches[1];
        // Extract href if it exists
        if (preg_match('/href\s*=\s*["\']([^"\']*)["\']/', $attrs, $hrefMatch)) {
            $href = $hrefMatch[1];
            // Only allow http, https, mailto protocols
            if (preg_match('/^(https?|mailto):/i', $href) || $href[0] === '/') {
                return '<a href="' . htmlspecialchars($href, ENT_QUOTES, 'UTF-8') . '">';
            }
        }
        return '<a>';
    }, $sanitized);

    return $sanitized;
}

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
    $media_url_2 = trim($_POST['media_url_2'] ?? '');
    $media_url_3 = trim($_POST['media_url_3'] ?? '');
    $option_a = trim($_POST['option_a'] ?? '');
    $option_b = trim($_POST['option_b'] ?? '');
    $option_c = trim($_POST['option_c'] ?? '');
    $option_d = trim($_POST['option_d'] ?? '');
    $option_a_type = $_POST['option_a_type'] ?? 'text';
    $option_b_type = $_POST['option_b_type'] ?? 'text';
    $option_c_type = $_POST['option_c_type'] ?? 'text';
    $option_d_type = $_POST['option_d_type'] ?? 'text';
    $correct_answer = $_POST['correct_answer'] ?? '1'; // Store as 1,2,3,4 instead of A,B,C,D
    $explanation = trim($_POST['explanation'] ?? '');
    $order_index = (int)($_POST['order_index'] ?? 0);

    // Box fields (prompt box)
    $box_text = trim($_POST['box_text'] ?? '');
    $box_media_url = trim($_POST['box_media_url'] ?? '');
    $box_position = $_POST['box_position'] ?? 'top';

    if (empty($question_text)) {
        $error = 'Teks soal tidak boleh kosong!';
    } elseif (empty($option_a) || empty($option_b) || empty($option_c) || empty($option_d)) {
        $error = 'Semua pilihan jawaban harus diisi!';
    } else {
        try {
            if ($id) {
                $stmt = $pdo->prepare("UPDATE questions SET assessment_id = ?, question_text = ?, question_type = ?, media_url = ?, media_url_2 = ?, media_url_3 = ?, option_a = ?, option_a_type = ?, option_b = ?, option_b_type = ?, option_c = ?, option_c_type = ?, option_d = ?, option_d_type = ?, correct_answer = ?, explanation = ?, order_index = ?, box_text = ?, box_media_url = ?, box_position = ? WHERE id = ?");
                $stmt->execute([$q_assessment_id, $question_text, $question_type, $media_url, $media_url_2, $media_url_3, $option_a, $option_a_type, $option_b, $option_b_type, $option_c, $option_c_type, $option_d, $option_d_type, $correct_answer, $explanation, $order_index, $box_text, $box_media_url, $box_position, $id]);
                $message = 'Soal berhasil diperbarui!';
            } else {
                $stmt = $pdo->prepare("INSERT INTO questions (assessment_id, question_text, question_type, media_url, media_url_2, media_url_3, option_a, option_a_type, option_b, option_b_type, option_c, option_c_type, option_d, option_d_type, correct_answer, explanation, order_index, box_text, box_media_url, box_position) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                $stmt->execute([$q_assessment_id, $question_text, $question_type, $media_url, $media_url_2, $media_url_3, $option_a, $option_a_type, $option_b, $option_b_type, $option_c, $option_c_type, $option_d, $option_d_type, $correct_answer, $explanation, $order_index, $box_text, $box_media_url, $box_position]);
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
    <!-- Quill Rich Text Editor -->
    <link href="https://cdn.quilljs.com/1.3.6/quill.snow.css" rel="stylesheet">
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
        /* Quill editor styling */
        .quill-editor {
            background: white;
            border: 1px solid #ced4da;
            border-radius: 0.375rem;
        }
        .ql-toolbar {
            border-top-left-radius: 0.375rem;
            border-top-right-radius: 0.375rem;
        }
        .ql-container {
            min-height: 100px;
            font-size: 15px;
            border-bottom-left-radius: 0.375rem;
            border-bottom-right-radius: 0.375rem;
        }
        .option-editor .ql-container {
            min-height: 60px;
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
                    <a href="admin_video_hafalan.php"><i class="bi bi-camera-video"></i> Video Hafalan</a>
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
                                    <!-- 📦 Display Box (Prompt Box) if exists -->
                                    <?php if (!empty($q['box_text']) || !empty($q['box_media_url'])): ?>
                                        <div class="alert alert-warning mb-3" style="background-color: #fff3cd; border-left: 4px solid #ffc107;">
                                            <div class="d-flex justify-content-between align-items-start mb-2">
                                                <strong>📦 Kotak Soal (<?= ucfirst($q['box_position'] ?? 'top') ?>)</strong>
                                                <span class="badge bg-warning text-dark">
                                                    <?php
                                                    $posLabel = [
                                                        'top' => '⬆️ Paling Atas',
                                                        'middle' => '📍 Tengah',
                                                        'bottom' => '⬇️ Paling Bawah'
                                                    ];
                                                    echo $posLabel[$q['box_position'] ?? 'top'] ?? 'Top';
                                                    ?>
                                                </span>
                                            </div>
                                            <?php if (!empty($q['box_text'])): ?>
                                                <div class="mb-2" style="line-height: 1.6;">
                                                    <?= sanitizeQuillHtml($q['box_text']) ?>
                                                </div>
                                            <?php endif; ?>
                                            <?php if (!empty($q['box_media_url'])): ?>
                                                <div class="mt-2">
                                                    <?php
                                                    $boxMediaUrl = $q['box_media_url'];
                                                    $isBoxImage = preg_match('/\.(jpg|jpeg|png|gif|webp)$/i', $boxMediaUrl);
                                                    $isBoxAudio = preg_match('/\.(mp3|wav|ogg|webm)$/i', $boxMediaUrl);
                                                    ?>
                                                    <?php if ($isBoxImage): ?>
                                                        <img src="<?= htmlspecialchars($boxMediaUrl) ?>" class="img-thumbnail" style="max-height: 150px;">
                                                    <?php elseif ($isBoxAudio): ?>
                                                        <audio controls class="w-100">
                                                            <source src="<?= htmlspecialchars($boxMediaUrl) ?>">
                                                        </audio>
                                                    <?php else: ?>
                                                        <a href="<?= htmlspecialchars($boxMediaUrl) ?>" target="_blank" class="btn btn-sm btn-outline-secondary">
                                                            <i class="bi bi-link-45deg"></i> Lihat Media Box
                                                        </a>
                                                    <?php endif; ?>
                                                </div>
                                            <?php endif; ?>
                                        </div>
                                    <?php endif; ?>

                                    <!-- ✅ Optimized for long paragraph/story questions with HTML support (XSS Protected) -->
                                    <div class="question-text-display mb-3" style="line-height: 1.8; font-size: 15px; max-height: 500px; overflow-y: auto; padding: 10px; background-color: #f8f9fa; border-radius: 8px;">
                                        <?= sanitizeQuillHtml($q['question_text']) ?>
                                    </div>

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

                                    <!-- ✅ Support for long answer options with HTML formatting and multiple types -->
                                    <div class="row">
                                        <div class="col-md-6">
                                            <div class="p-2 rounded mb-1 <?= $q['correct_answer'] == '1' ? 'correct-answer' : 'bg-light' ?>" style="line-height: 1.6; min-height: 40px;">
                                                <strong>1.</strong>
                                                <?php
                                                $optType = $q['option_a_type'] ?? 'text';
                                                if ($optType === 'image'): ?>
                                                    <img src="<?= htmlspecialchars($q['option_a']) ?>" class="img-thumbnail" style="max-height: 100px;">
                                                <?php elseif ($optType === 'audio'): ?>
                                                    <audio controls class="w-100"><source src="<?= htmlspecialchars($q['option_a']) ?>"></audio>
                                                <?php else: ?>
                                                    <?= $q['option_a'] ?>
                                                <?php endif; ?>
                                                <?php if ($q['correct_answer'] == '1'): ?>
                                                    <i class="bi bi-check-circle-fill text-success"></i>
                                                <?php endif; ?>
                                            </div>
                                            <div class="p-2 rounded mb-1 <?= $q['correct_answer'] == '2' ? 'correct-answer' : 'bg-light' ?>" style="line-height: 1.6; min-height: 40px;">
                                                <strong>2.</strong>
                                                <?php
                                                $optType = $q['option_b_type'] ?? 'text';
                                                if ($optType === 'image'): ?>
                                                    <img src="<?= htmlspecialchars($q['option_b']) ?>" class="img-thumbnail" style="max-height: 100px;">
                                                <?php elseif ($optType === 'audio'): ?>
                                                    <audio controls class="w-100"><source src="<?= htmlspecialchars($q['option_b']) ?>"></audio>
                                                <?php else: ?>
                                                    <?= $q['option_b'] ?>
                                                <?php endif; ?>
                                                <?php if ($q['correct_answer'] == '2'): ?>
                                                    <i class="bi bi-check-circle-fill text-success"></i>
                                                <?php endif; ?>
                                            </div>
                                        </div>
                                        <div class="col-md-6">
                                            <div class="p-2 rounded mb-1 <?= $q['correct_answer'] == '3' ? 'correct-answer' : 'bg-light' ?>" style="line-height: 1.6; min-height: 40px;">
                                                <strong>3.</strong>
                                                <?php
                                                $optType = $q['option_c_type'] ?? 'text';
                                                if ($optType === 'image'): ?>
                                                    <img src="<?= htmlspecialchars($q['option_c']) ?>" class="img-thumbnail" style="max-height: 100px;">
                                                <?php elseif ($optType === 'audio'): ?>
                                                    <audio controls class="w-100"><source src="<?= htmlspecialchars($q['option_c']) ?>"></audio>
                                                <?php else: ?>
                                                    <?= $q['option_c'] ?>
                                                <?php endif; ?>
                                                <?php if ($q['correct_answer'] == '3'): ?>
                                                    <i class="bi bi-check-circle-fill text-success"></i>
                                                <?php endif; ?>
                                            </div>
                                            <div class="p-2 rounded mb-1 <?= $q['correct_answer'] == '4' ? 'correct-answer' : 'bg-light' ?>" style="line-height: 1.6; min-height: 40px;">
                                                <strong>4.</strong>
                                                <?php
                                                $optType = $q['option_d_type'] ?? 'text';
                                                if ($optType === 'image'): ?>
                                                    <img src="<?= htmlspecialchars($q['option_d']) ?>" class="img-thumbnail" style="max-height: 100px;">
                                                <?php elseif ($optType === 'audio'): ?>
                                                    <audio controls class="w-100"><source src="<?= htmlspecialchars($q['option_d']) ?>"></audio>
                                                <?php else: ?>
                                                    <?= $q['option_d'] ?>
                                                <?php endif; ?>
                                                <?php if ($q['correct_answer'] == '4'): ?>
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

                                <!-- 📦 Kotak Soal (Prompt Box) - Opsional -->
                                <div class="card bg-warning bg-opacity-10 border-warning mb-3">
                                    <div class="card-body">
                                        <div class="d-flex justify-content-between align-items-center mb-3">
                                            <div>
                                                <h5 class="card-title mb-1">📦 Kotak Soal (Prompt Box)</h5>
                                                <small class="text-muted">Kotak ini biasanya untuk percakapan, cerita, atau instruksi.</small>
                                            </div>
                                            <div class="form-check form-switch">
                                                <input class="form-check-input" type="checkbox" id="enable_box"
                                                       <?= (!empty($editQuestion['box_text']) || !empty($editQuestion['box_media_url'])) ? 'checked' : '' ?>>
                                                <label class="form-check-label" for="enable_box">
                                                    <strong>Aktifkan Kotak</strong>
                                                </label>
                                            </div>
                                        </div>

                                        <div id="box_content_wrapper" style="display: <?= (!empty($editQuestion['box_text']) || !empty($editQuestion['box_media_url'])) ? 'block' : 'none' ?>;">
                                            <div class="mb-3">
                                                <label class="form-label">Posisi Kotak</label>
                                                <select name="box_position" id="box_position" class="form-select">
                                                    <option value="top" <?= ($editQuestion['box_position'] ?? 'top') === 'top' ? 'selected' : '' ?>>⬆️ Paling Atas (Sebelum Teks Soal)</option>
                                                    <option value="middle" <?= ($editQuestion['box_position'] ?? '') === 'middle' ? 'selected' : '' ?>>📍 Tengah (Antara Soal dan Pilihan)</option>
                                                    <option value="bottom" <?= ($editQuestion['box_position'] ?? '') === 'bottom' ? 'selected' : '' ?>>⬇️ Paling Bawah (Setelah Pilihan)</option>
                                                </select>
                                                <small class="text-muted">*Atur posisi relatif terhadap Soal dan Media</small>
                                            </div>

                                            <div class="mb-3">
                                                <label class="form-label">Isi Teks Kotak</label>
                                                <input type="hidden" name="box_text" id="box_text_input">
                                                <div id="box_text_editor" class="quill-editor"></div>
                                                <small class="text-muted">Teks yang akan muncul di dalam kotak (mendukung format HTML).</small>
                                            </div>

                                            <div class="mb-3">
                                                <label class="form-label">File Media Kotak (Audio/Gambar)</label>
                                                <input type="text" name="box_media_url" id="box_media_url" class="form-control"
                                                       value="<?= htmlspecialchars($editQuestion['box_media_url'] ?? '') ?>"
                                                       placeholder="URL file media...">
                                                <input type="file" id="box_media_file" class="form-control mt-2" accept="image/*,audio/*">
                                                <button type="button" class="btn btn-sm btn-outline-danger mt-2" id="clear_box_media" style="display: none;">
                                                    <i class="bi bi-trash"></i> Hapus
                                                </button>
                                                <small class="text-muted d-block mt-1">Choose File untuk upload atau paste URL manual</small>
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                <div class="mb-3">
                                    <label class="form-label">Teks Pertanyaan Utama *</label>
                                    <input type="hidden" name="question_text" id="question_text_input" required>
                                    <div id="question_text_editor" class="quill-editor"></div>
                                    <small class="text-muted">💡 Tip: Gunakan toolbar untuk format teks (Bold, Italic, Underline). Cocok untuk soal cerita/reading comprehension.</small>
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
                                            <label class="form-label">URL Media 1</label>
                                            <input type="url" name="media_url" id="media_url" class="form-control"
                                                   value="<?= htmlspecialchars($editQuestion['media_url'] ?? '') ?>"
                                                   placeholder="Upload file atau masukkan URL manual">
                                            <small class="text-muted">URL akan terisi otomatis setelah upload</small>
                                        </div>
                                        <div class="mb-3">
                                            <label class="form-label">URL Media 2 (Optional)</label>
                                            <input type="url" name="media_url_2" id="media_url_2" class="form-control"
                                                   value="<?= htmlspecialchars($editQuestion['media_url_2'] ?? '') ?>"
                                                   placeholder="Tambahan media 2">
                                            <small class="text-muted">Untuk soal dengan multiple media (text+image+audio)</small>
                                        </div>
                                        <div class="mb-3">
                                            <label class="form-label">URL Media 3 (Optional)</label>
                                            <input type="url" name="media_url_3" id="media_url_3" class="form-control"
                                                   value="<?= htmlspecialchars($editQuestion['media_url_3'] ?? '') ?>"
                                                   placeholder="Tambahan media 3">
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
                                        <small class="text-muted d-block mb-3">💡 Pilih tipe jawaban: Text (dengan formatting), Image, atau Audio untuk setiap pilihan!</small>

                                        <!-- Option 1 -->
                                        <div class="mb-4 p-3 bg-white rounded">
                                            <div class="row">
                                                <div class="col-md-3">
                                                    <label class="form-label">Pilihan 1 - Tipe *</label>
                                                    <select name="option_a_type" id="option_a_type" class="form-select option-type-selector" data-option="a">
                                                        <option value="text" <?= ($editQuestion['option_a_type'] ?? 'text') === 'text' ? 'selected' : '' ?>>Text</option>
                                                        <option value="image" <?= ($editQuestion['option_a_type'] ?? '') === 'image' ? 'selected' : '' ?>>Image</option>
                                                        <option value="audio" <?= ($editQuestion['option_a_type'] ?? '') === 'audio' ? 'selected' : '' ?>>Audio</option>
                                                    </select>
                                                </div>
                                                <div class="col-md-9">
                                                    <label class="form-label">Pilihan 1 - Konten *</label>
                                                    <input type="hidden" name="option_a" id="option_a_input" required>
                                                    <div id="option_a_editor" class="quill-editor option-editor option-a-text"></div>
                                                    <input type="text" name="option_a_url" id="option_a_url" class="form-control option-a-media d-none" placeholder="URL Image/Audio atau upload...">
                                                    <input type="file" id="option_a_file" class="form-control mt-2 option-a-media d-none" accept="image/*,audio/*">
                                                </div>
                                            </div>
                                        </div>

                                        <!-- Option 2 -->
                                        <div class="mb-4 p-3 bg-white rounded">
                                            <div class="row">
                                                <div class="col-md-3">
                                                    <label class="form-label">Pilihan 2 - Tipe *</label>
                                                    <select name="option_b_type" id="option_b_type" class="form-select option-type-selector" data-option="b">
                                                        <option value="text" <?= ($editQuestion['option_b_type'] ?? 'text') === 'text' ? 'selected' : '' ?>>Text</option>
                                                        <option value="image" <?= ($editQuestion['option_b_type'] ?? '') === 'image' ? 'selected' : '' ?>>Image</option>
                                                        <option value="audio" <?= ($editQuestion['option_b_type'] ?? '') === 'audio' ? 'selected' : '' ?>>Audio</option>
                                                    </select>
                                                </div>
                                                <div class="col-md-9">
                                                    <label class="form-label">Pilihan 2 - Konten *</label>
                                                    <input type="hidden" name="option_b" id="option_b_input" required>
                                                    <div id="option_b_editor" class="quill-editor option-editor option-b-text"></div>
                                                    <input type="text" name="option_b_url" id="option_b_url" class="form-control option-b-media d-none" placeholder="URL Image/Audio atau upload...">
                                                    <input type="file" id="option_b_file" class="form-control mt-2 option-b-media d-none" accept="image/*,audio/*">
                                                </div>
                                            </div>
                                        </div>

                                        <!-- Option 3 -->
                                        <div class="mb-4 p-3 bg-white rounded">
                                            <div class="row">
                                                <div class="col-md-3">
                                                    <label class="form-label">Pilihan 3 - Tipe *</label>
                                                    <select name="option_c_type" id="option_c_type" class="form-select option-type-selector" data-option="c">
                                                        <option value="text" <?= ($editQuestion['option_c_type'] ?? 'text') === 'text' ? 'selected' : '' ?>>Text</option>
                                                        <option value="image" <?= ($editQuestion['option_c_type'] ?? '') === 'image' ? 'selected' : '' ?>>Image</option>
                                                        <option value="audio" <?= ($editQuestion['option_c_type'] ?? '') === 'audio' ? 'selected' : '' ?>>Audio</option>
                                                    </select>
                                                </div>
                                                <div class="col-md-9">
                                                    <label class="form-label">Pilihan 3 - Konten *</label>
                                                    <input type="hidden" name="option_c" id="option_c_input" required>
                                                    <div id="option_c_editor" class="quill-editor option-editor option-c-text"></div>
                                                    <input type="text" name="option_c_url" id="option_c_url" class="form-control option-c-media d-none" placeholder="URL Image/Audio atau upload...">
                                                    <input type="file" id="option_c_file" class="form-control mt-2 option-c-media d-none" accept="image/*,audio/*">
                                                </div>
                                            </div>
                                        </div>

                                        <!-- Option 4 -->
                                        <div class="mb-4 p-3 bg-white rounded">
                                            <div class="row">
                                                <div class="col-md-3">
                                                    <label class="form-label">Pilihan 4 - Tipe *</label>
                                                    <select name="option_d_type" id="option_d_type" class="form-select option-type-selector" data-option="d">
                                                        <option value="text" <?= ($editQuestion['option_d_type'] ?? 'text') === 'text' ? 'selected' : '' ?>>Text</option>
                                                        <option value="image" <?= ($editQuestion['option_d_type'] ?? '') === 'image' ? 'selected' : '' ?>>Image</option>
                                                        <option value="audio" <?= ($editQuestion['option_d_type'] ?? '') === 'audio' ? 'selected' : '' ?>>Audio</option>
                                                    </select>
                                                </div>
                                                <div class="col-md-9">
                                                    <label class="form-label">Pilihan 4 - Konten *</label>
                                                    <input type="hidden" name="option_d" id="option_d_input" required>
                                                    <div id="option_d_editor" class="quill-editor option-editor option-d-text"></div>
                                                    <input type="text" name="option_d_url" id="option_d_url" class="form-control option-d-media d-none" placeholder="URL Image/Audio atau upload...">
                                                    <input type="file" id="option_d_file" class="form-control mt-2 option-d-media d-none" accept="image/*,audio/*">
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
                                                <option value="1" <?= ($editQuestion['correct_answer'] ?? '1') == '1' ? 'selected' : '' ?>>1</option>
                                                <option value="2" <?= ($editQuestion['correct_answer'] ?? '') == '2' ? 'selected' : '' ?>>2</option>
                                                <option value="3" <?= ($editQuestion['correct_answer'] ?? '') == '3' ? 'selected' : '' ?>>3</option>
                                                <option value="4" <?= ($editQuestion['correct_answer'] ?? '') == '4' ? 'selected' : '' ?>>4</option>
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
    <!-- Quill Rich Text Editor -->
    <script src="https://cdn.quilljs.com/1.3.6/quill.js"></script>
    <script>
    document.addEventListener('DOMContentLoaded', function() {
        // Initialize Quill editors if on add/edit page
        if (document.getElementById('question_text_editor')) {
            initializeQuillEditors();
        }
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

        // Handle Option Type Selector Changes
        function setupOptionTypeHandlers() {
            const optionLetters = ['a', 'b', 'c', 'd'];
            optionLetters.forEach(letter => {
                const selector = document.getElementById(`option_${letter}_type`);
                if (selector) {
                    selector.addEventListener('change', function() {
                        toggleOptionInputType(letter, this.value);
                    });
                    // Initialize on page load
                    toggleOptionInputType(letter, selector.value);
                }

                // Add file upload handler for each option
                const fileInput = document.getElementById(`option_${letter}_file`);
                if (fileInput) {
                    fileInput.addEventListener('change', function(e) {
                        if (e.target.files.length > 0) {
                            handleOptionFileUpload(letter, e.target.files[0]);
                        }
                    });
                }
            });
        }

        // Upload file for option (image/audio)
        function handleOptionFileUpload(optionLetter, file) {
            const urlInput = document.getElementById(`option_${optionLetter}_url`);

            // Validate file size (50MB)
            if (file.size > 50 * 1024 * 1024) {
                alert(`File untuk opsi ${optionLetter.toUpperCase()} terlalu besar. Maksimal 50MB`);
                return;
            }

            // Validate file type
            const allowedTypes = [
                'image/jpeg', 'image/png', 'image/gif', 'image/webp',
                'audio/mpeg', 'audio/mp3', 'audio/wav', 'audio/ogg', 'audio/webm'
            ];

            if (!allowedTypes.includes(file.type)) {
                alert(`Tipe file untuk opsi ${optionLetter.toUpperCase()} tidak didukung`);
                return;
            }

            // Create FormData
            const formData = new FormData();
            formData.append('media_file', file);

            // Upload via AJAX
            const xhr = new XMLHttpRequest();
            xhr.open('POST', 'admin_upload_media.php', true);

            xhr.onload = function() {
                try {
                    const response = JSON.parse(xhr.responseText);
                    if (response.success) {
                        // Set URL to the URL input field
                        urlInput.value = response.url;

                        // Show success message
                        const fileSize = (response.size / 1024 / 1024).toFixed(2);
                        alert(`✅ File opsi ${optionLetter.toUpperCase()} berhasil diupload (${fileSize} MB)\n${response.url}`);
                    } else {
                        alert(`Upload gagal untuk opsi ${optionLetter.toUpperCase()}: ${response.message}`);
                    }
                } catch (e) {
                    alert(`Terjadi kesalahan saat upload opsi ${optionLetter.toUpperCase()}`);
                }
            };

            xhr.onerror = function() {
                alert(`Network error saat upload opsi ${optionLetter.toUpperCase()}. Pastikan koneksi internet stabil`);
            };

            xhr.send(formData);
        }

        function toggleOptionInputType(optionLetter, type) {
            const textEditor = document.querySelector(`.option-${optionLetter}-text`);
            const mediaInputs = document.querySelectorAll(`.option-${optionLetter}-media`);

            if (type === 'text') {
                // Show text editor, hide media inputs
                if (textEditor) textEditor.classList.remove('d-none');
                mediaInputs.forEach(input => input.classList.add('d-none'));
            } else {
                // Hide text editor, show media inputs
                if (textEditor) textEditor.classList.add('d-none');
                mediaInputs.forEach(input => input.classList.remove('d-none'));
            }
        }

        // Initialize Quill Rich Text Editors
        function initializeQuillEditors() {
            const toolbarOptions = [
                ['bold', 'italic', 'underline', 'strike'],        // toggled buttons
                [{ 'list': 'ordered'}, { 'list': 'bullet' }],
                [{ 'header': [1, 2, 3, false] }],
                ['clean']                                         // remove formatting button
            ];

            // Get existing content for editing
            const editData = {
                question_text: <?= json_encode($editQuestion['question_text'] ?? '') ?>,
                box_text: <?= json_encode($editQuestion['box_text'] ?? '') ?>,
                option_a: <?= json_encode($editQuestion['option_a'] ?? '') ?>,
                option_b: <?= json_encode($editQuestion['option_b'] ?? '') ?>,
                option_c: <?= json_encode($editQuestion['option_c'] ?? '') ?>,
                option_d: <?= json_encode($editQuestion['option_d'] ?? '') ?>,
                option_a_type: <?= json_encode($editQuestion['option_a_type'] ?? 'text') ?>,
                option_b_type: <?= json_encode($editQuestion['option_b_type'] ?? 'text') ?>,
                option_c_type: <?= json_encode($editQuestion['option_c_type'] ?? 'text') ?>,
                option_d_type: <?= json_encode($editQuestion['option_d_type'] ?? 'text') ?>
            };

            // Initialize Question Text Editor
            const questionEditor = new Quill('#question_text_editor', {
                theme: 'snow',
                modules: { toolbar: toolbarOptions },
                placeholder: 'Tuliskan pertanyaan di sini...'
            });
            if (editData.question_text) {
                questionEditor.root.innerHTML = editData.question_text;
            }

            // Initialize Box Text Editor (Prompt Box)
            const boxTextEditor = new Quill('#box_text_editor', {
                theme: 'snow',
                modules: { toolbar: toolbarOptions },
                placeholder: 'Tuliskan teks kotak (percakapan/cerita/instruksi)...'
            });
            if (editData.box_text) {
                boxTextEditor.root.innerHTML = editData.box_text;
            }

            // Initialize Option A Editor
            const optionAEditor = new Quill('#option_a_editor', {
                theme: 'snow',
                modules: { toolbar: toolbarOptions },
                placeholder: 'Pilihan 1...'
            });
            if (editData.option_a_type === 'text' && editData.option_a) {
                optionAEditor.root.innerHTML = editData.option_a;
            } else if (editData.option_a) {
                // If option is image/audio, set URL to URL input
                document.getElementById('option_a_url').value = editData.option_a;
            }

            // Initialize Option B Editor
            const optionBEditor = new Quill('#option_b_editor', {
                theme: 'snow',
                modules: { toolbar: toolbarOptions },
                placeholder: 'Pilihan 2...'
            });
            if (editData.option_b_type === 'text' && editData.option_b) {
                optionBEditor.root.innerHTML = editData.option_b;
            } else if (editData.option_b) {
                document.getElementById('option_b_url').value = editData.option_b;
            }

            // Initialize Option C Editor
            const optionCEditor = new Quill('#option_c_editor', {
                theme: 'snow',
                modules: { toolbar: toolbarOptions },
                placeholder: 'Pilihan 3...'
            });
            if (editData.option_c_type === 'text' && editData.option_c) {
                optionCEditor.root.innerHTML = editData.option_c;
            } else if (editData.option_c) {
                document.getElementById('option_c_url').value = editData.option_c;
            }

            // Initialize Option D Editor
            const optionDEditor = new Quill('#option_d_editor', {
                theme: 'snow',
                modules: { toolbar: toolbarOptions },
                placeholder: 'Pilihan 4...'
            });
            if (editData.option_d_type === 'text' && editData.option_d) {
                optionDEditor.root.innerHTML = editData.option_d;
            } else if (editData.option_d) {
                document.getElementById('option_d_url').value = editData.option_d;
            }

            // Sync editors with hidden inputs on form submit
            const form = document.querySelector('form');
            if (form) {
                form.addEventListener('submit', function(e) {
                    // Sync all editors with their hidden inputs based on type
                    document.getElementById('question_text_input').value = questionEditor.root.innerHTML;
                    document.getElementById('box_text_input').value = boxTextEditor.root.innerHTML;

                    // For each option, check type and sync accordingly
                    const options = ['a', 'b', 'c', 'd'];
                    const editors = [optionAEditor, optionBEditor, optionCEditor, optionDEditor];

                    options.forEach((letter, index) => {
                        const type = document.getElementById(`option_${letter}_type`).value;
                        const input = document.getElementById(`option_${letter}_input`);

                        if (type === 'text') {
                            input.value = editors[index].root.innerHTML;
                        } else {
                            // For image/audio, use URL from the URL input field
                            const urlInput = document.getElementById(`option_${letter}_url`);
                            input.value = urlInput ? urlInput.value : '';
                        }
                    });

                    // Validate that question is not empty
                    if (questionEditor.getText().trim().length === 0) {
                        alert('Teks soal tidak boleh kosong!');
                        e.preventDefault();
                        return false;
                    }

                    // Validate that all options are filled
                    const allFilled = options.every((letter, index) => {
                        const type = document.getElementById(`option_${letter}_type`).value;
                        if (type === 'text') {
                            return editors[index].getText().trim().length > 0;
                        } else {
                            const urlInput = document.getElementById(`option_${letter}_url`);
                            return urlInput && urlInput.value.trim().length > 0;
                        }
                    });

                    if (!allFilled) {
                        alert('Semua pilihan jawaban harus diisi!');
                        e.preventDefault();
                        return false;
                    }
                });
            }

            // Setup option type handlers
            setupOptionTypeHandlers();

            // Setup Enable Box Toggle
            const enableBoxCheckbox = document.getElementById('enable_box');
            const boxContentWrapper = document.getElementById('box_content_wrapper');

            if (enableBoxCheckbox && boxContentWrapper) {
                enableBoxCheckbox.addEventListener('change', function() {
                    if (this.checked) {
                        boxContentWrapper.style.display = 'block';
                    } else {
                        boxContentWrapper.style.display = 'none';
                        // Clear box fields when disabled
                        if (confirm('Nonaktifkan kotak akan menghapus semua isi kotak. Lanjutkan?')) {
                            boxTextEditor.root.innerHTML = '';
                            document.getElementById('box_text_input').value = '';
                            document.getElementById('box_media_url').value = '';
                            document.getElementById('box_media_file').value = '';
                        } else {
                            // Revert checkbox if user cancels
                            this.checked = true;
                            boxContentWrapper.style.display = 'block';
                        }
                    }
                });
            }

            // Setup Box Media Upload Handler
            const boxMediaFile = document.getElementById('box_media_file');
            const boxMediaUrl = document.getElementById('box_media_url');
            const clearBoxMediaBtn = document.getElementById('clear_box_media');

            if (boxMediaFile) {
                boxMediaFile.addEventListener('change', function(e) {
                    if (e.target.files.length > 0) {
                        const file = e.target.files[0];

                        // Validate file size (50MB)
                        if (file.size > 50 * 1024 * 1024) {
                            alert('File terlalu besar. Maksimal 50MB');
                            return;
                        }

                        // Create FormData
                        const formData = new FormData();
                        formData.append('media_file', file);

                        // Upload via AJAX
                        const xhr = new XMLHttpRequest();
                        xhr.open('POST', 'admin_upload_media.php', true);

                        xhr.onload = function() {
                            try {
                                const response = JSON.parse(xhr.responseText);
                                if (response.success) {
                                    boxMediaUrl.value = response.url;
                                    clearBoxMediaBtn.style.display = 'inline-block';
                                    alert('✅ File berhasil diupload!\n' + response.url);
                                } else {
                                    alert('Upload gagal: ' + response.message);
                                }
                            } catch (e) {
                                alert('Terjadi kesalahan saat upload');
                            }
                        };

                        xhr.onerror = function() {
                            alert('Network error. Pastikan koneksi internet stabil');
                        };

                        xhr.send(formData);
                    }
                });
            }

            if (clearBoxMediaBtn) {
                clearBoxMediaBtn.addEventListener('click', function() {
                    boxMediaUrl.value = '';
                    boxMediaFile.value = '';
                    clearBoxMediaBtn.style.display = 'none';
                });
            }

            // Show clear button if media URL exists
            if (boxMediaUrl && boxMediaUrl.value) {
                clearBoxMediaBtn.style.display = 'inline-block';
            }
        }
    });
    </script>
</body>
</html>
