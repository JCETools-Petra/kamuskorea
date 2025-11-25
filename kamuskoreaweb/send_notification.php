<?php
require_once __DIR__ . '/admin_config.php';
requireAdminLogin();

require_once __DIR__ . '/vendor/autoload.php';

use Kreait\Firebase\Factory;
use Kreait\Firebase\Messaging\CloudMessage;
use Kreait\Firebase\Messaging\Notification;

// Load environment variables
$dotenv = Dotenv\Dotenv::createImmutable(__DIR__ . '/..');
$dotenv->load();

$message = '';
$error = '';
$debugInfo = '';

// Initialize Firebase Admin SDK
try {
    $firebase = (new Factory)
        ->withServiceAccount(__DIR__ . '/../firebase-service-account.json');
    $messaging = $firebase->createMessaging();
} catch (Exception $e) {
    $error = "Gagal memuat Firebase SDK: " . $e->getMessage();
}

// Handle form submission
if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_POST['send_notification'])) {
    $title = trim($_POST['title'] ?? '');
    $body = trim($_POST['body'] ?? '');
    $topic = trim($_POST['topic'] ?? 'all');
    $imageUrl = trim($_POST['image_url'] ?? '');
    $data = [];

    // Custom data (optional)
    if (!empty($_POST['data_key']) && !empty($_POST['data_value'])) {
        $data[$_POST['data_key']] = $_POST['data_value'];
    }

    if (empty($title) || empty($body)) {
        $error = 'Judul dan pesan tidak boleh kosong!';
    } else {
        try {
            // Build notification
            $notificationBuilder = Notification::create($title, $body);

            if (!empty($imageUrl)) {
                $notificationBuilder = $notificationBuilder->withImageUrl($imageUrl);
            }

            // Build message
            $messageBuilder = CloudMessage::withTarget('topic', $topic)
                ->withNotification($notificationBuilder);

            // Add custom data if provided
            if (!empty($data)) {
                $messageBuilder = $messageBuilder->withData($data);
            }

            // Add Android and iOS specific config
            $messageBuilder = $messageBuilder
                ->withAndroidConfig([
                    'priority' => 'high',
                    'notification' => [
                        'sound' => 'default',
                        'channel_id' => 'kamus_korea_channel'
                    ]
                ])
                ->withApnsConfig([
                    'payload' => [
                        'aps' => [
                            'sound' => 'default',
                            'badge' => 1
                        ]
                    ]
                ]);

            $fcmMessage = $messageBuilder;

            // Send notification
            $result = $messaging->send($fcmMessage);

            $message = "✅ Notifikasi berhasil dikirim!";
            $debugInfo = "Message ID: " . $result;

        } catch (\Kreait\Firebase\Exception\Messaging\MessagingException $e) {
            $error = "Gagal mengirim notifikasi: " . $e->getMessage();
            $debugInfo = json_encode($e->errors(), JSON_PRETTY_PRINT);
        } catch (Exception $e) {
            $error = "Error: " . $e->getMessage();
            $debugInfo = $e->getTraceAsString();
        }
    }
}

// Get Firebase project info
$projectId = 'kamus-korea-6542e'; // Default
try {
    $serviceAccountPath = __DIR__ . '/../firebase-service-account.json';
    if (file_exists($serviceAccountPath)) {
        $serviceAccount = json_decode(file_get_contents($serviceAccountPath), true);
        $projectId = $serviceAccount['project_id'] ?? $projectId;
    }
} catch (Exception $e) {
    // Ignore
}
?>
<!DOCTYPE html>
<html lang="id">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Kirim Push Notification - Kamus Korea</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.0/font/bootstrap-icons.css" rel="stylesheet">
    <style>
        body {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            padding: 20px;
        }
        .notification-card {
            max-width: 800px;
            margin: 0 auto;
            background: white;
            border-radius: 15px;
            box-shadow: 0 10px 40px rgba(0,0,0,0.2);
        }
        .card-header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            border-radius: 15px 15px 0 0 !important;
            padding: 25px;
        }
        .preview-card {
            background: #f8f9fa;
            border: 2px dashed #dee2e6;
            border-radius: 10px;
            padding: 20px;
        }
        .phone-preview {
            max-width: 350px;
            margin: 0 auto;
            background: white;
            border-radius: 25px;
            padding: 15px;
            box-shadow: 0 5px 20px rgba(0,0,0,0.1);
        }
        .notif-preview {
            background: white;
            border-radius: 10px;
            padding: 12px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
            margin-bottom: 10px;
        }
        .notif-icon {
            width: 40px;
            height: 40px;
            border-radius: 10px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            display: flex;
            align-items: center;
            justify-content: center;
            color: white;
        }
        .back-btn {
            position: absolute;
            top: 20px;
            left: 20px;
            z-index: 1000;
        }
        .debug-info {
            background: #f8f9fa;
            border-left: 4px solid #0dcaf0;
            padding: 15px;
            border-radius: 5px;
            font-family: monospace;
            font-size: 12px;
            white-space: pre-wrap;
            word-wrap: break-word;
        }
    </style>
</head>
<body>
    <a href="admin_dashboard.php" class="btn btn-light back-btn">
        <i class="bi bi-arrow-left"></i> Kembali ke Dashboard
    </a>

    <div class="notification-card">
        <div class="card-header">
            <h3 class="mb-0">
                <i class="bi bi-bell-fill"></i> Push Notification Center
            </h3>
            <small>Project: <strong><?= htmlspecialchars($projectId) ?></strong></small>
        </div>
        <div class="card-body p-4">

            <?php if ($message): ?>
                <div class="alert alert-success alert-dismissible fade show">
                    <i class="bi bi-check-circle-fill"></i> <?= htmlspecialchars($message) ?>
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            <?php endif; ?>

            <?php if ($error): ?>
                <div class="alert alert-danger alert-dismissible fade show">
                    <i class="bi bi-exclamation-triangle-fill"></i> <?= htmlspecialchars($error) ?>
                    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
                </div>
            <?php endif; ?>

            <?php if ($debugInfo): ?>
                <div class="alert alert-info">
                    <strong>Debug Info:</strong>
                    <div class="debug-info mt-2"><?= htmlspecialchars($debugInfo) ?></div>
                </div>
            <?php endif; ?>

            <div class="row">
                <div class="col-md-6">
                    <form method="POST" id="notificationForm">
                        <h5 class="mb-3"><i class="bi bi-pencil-square"></i> Compose Notification</h5>

                        <div class="mb-3">
                            <label class="form-label">Judul Notifikasi *</label>
                            <input type="text" name="title" id="notifTitle" class="form-control"
                                   placeholder="Contoh: Update Terbaru!"
                                   value="<?= htmlspecialchars($_POST['title'] ?? '') ?>"
                                   required maxlength="50">
                            <small class="text-muted">Max 50 karakter</small>
                        </div>

                        <div class="mb-3">
                            <label class="form-label">Pesan *</label>
                            <textarea name="body" id="notifBody" class="form-control" rows="4"
                                      placeholder="Tulis pesan notifikasi di sini..."
                                      required maxlength="200"><?= htmlspecialchars($_POST['body'] ?? '') ?></textarea>
                            <small class="text-muted">Max 200 karakter</small>
                        </div>

                        <div class="mb-3">
                            <label class="form-label">Target Topic</label>
                            <select name="topic" class="form-select">
                                <option value="all" selected>🌍 Semua Pengguna (topic: all)</option>
                                <option value="premium">⭐ Premium Users (topic: premium)</option>
                                <option value="android">🤖 Android Only (topic: android)</option>
                                <option value="ios">🍎 iOS Only (topic: ios)</option>
                            </select>
                            <small class="text-muted">Pastikan device sudah subscribe ke topic ini</small>
                        </div>

                        <div class="mb-3">
                            <label class="form-label">URL Gambar (Opsional)</label>
                            <input type="url" name="image_url" id="notifImage" class="form-control"
                                   placeholder="https://example.com/image.jpg"
                                   value="<?= htmlspecialchars($_POST['image_url'] ?? '') ?>">
                            <small class="text-muted">URL gambar untuk ditampilkan di notifikasi</small>
                        </div>

                        <div class="card bg-light mb-3">
                            <div class="card-body">
                                <h6 class="card-title">Custom Data (Opsional)</h6>
                                <div class="row">
                                    <div class="col-md-6">
                                        <input type="text" name="data_key" class="form-control form-control-sm"
                                               placeholder="Key (contoh: type)">
                                    </div>
                                    <div class="col-md-6">
                                        <input type="text" name="data_value" class="form-control form-control-sm"
                                               placeholder="Value (contoh: update)">
                                    </div>
                                </div>
                                <small class="text-muted d-block mt-2">Data tambahan untuk handling di aplikasi</small>
                            </div>
                        </div>

                        <button type="submit" name="send_notification" class="btn btn-primary btn-lg w-100">
                            <i class="bi bi-send-fill"></i> Kirim Notifikasi
                        </button>
                    </form>
                </div>

                <div class="col-md-6">
                    <h5 class="mb-3"><i class="bi bi-phone"></i> Preview</h5>
                    <div class="preview-card">
                        <div class="phone-preview">
                            <div class="text-center mb-2">
                                <small class="text-muted">Preview Notifikasi</small>
                            </div>
                            <div class="notif-preview">
                                <div class="d-flex">
                                    <div class="notif-icon me-3">
                                        <i class="bi bi-book-fill"></i>
                                    </div>
                                    <div class="flex-grow-1">
                                        <strong id="previewTitle" class="d-block">Kamus Korea</strong>
                                        <p id="previewBody" class="mb-1 text-muted small">Notifikasi akan muncul di sini...</p>
                                        <small class="text-muted">Baru saja</small>
                                    </div>
                                </div>
                                <div id="previewImageContainer" style="display: none;" class="mt-2">
                                    <img id="previewImage" src="" alt="Preview" class="img-fluid rounded" style="max-height: 150px; width: 100%; object-fit: cover;">
                                </div>
                            </div>
                        </div>

                        <div class="mt-4">
                            <h6><i class="bi bi-info-circle"></i> Panduan:</h6>
                            <ul class="small">
                                <li>Pastikan Firebase Cloud Messaging API sudah diaktifkan</li>
                                <li>Service account harus memiliki role <code>Firebase Cloud Messaging Admin</code></li>
                                <li>Device harus subscribe ke topic yang dipilih</li>
                                <li>Test dulu dengan topic "all" untuk semua user</li>
                            </ul>

                            <div class="alert alert-warning small mb-0">
                                <strong>⚠️ Troubleshooting Permission Error:</strong><br>
                                Jika muncul error "Permission Denied", pastikan:
                                <ol class="mb-0 mt-2">
                                    <li>Service account JSON sudah benar</li>
                                    <li>Firebase Cloud Messaging API sudah enabled di <a href="https://console.firebase.google.com/project/<?= htmlspecialchars($projectId) ?>/settings/cloudmessaging" target="_blank">Firebase Console</a></li>
                                    <li>Service account memiliki role "Firebase Admin SDK Administrator Service Agent"</li>
                                </ol>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        // Live preview
        const titleInput = document.getElementById('notifTitle');
        const bodyInput = document.getElementById('notifBody');
        const imageInput = document.getElementById('notifImage');
        const previewTitle = document.getElementById('previewTitle');
        const previewBody = document.getElementById('previewBody');
        const previewImage = document.getElementById('previewImage');
        const previewImageContainer = document.getElementById('previewImageContainer');

        function updatePreview() {
            const title = titleInput.value || 'Kamus Korea';
            const body = bodyInput.value || 'Notifikasi akan muncul di sini...';
            const image = imageInput.value;

            previewTitle.textContent = title;
            previewBody.textContent = body;

            if (image) {
                previewImage.src = image;
                previewImageContainer.style.display = 'block';
            } else {
                previewImageContainer.style.display = 'none';
            }
        }

        titleInput.addEventListener('input', updatePreview);
        bodyInput.addEventListener('input', updatePreview);
        imageInput.addEventListener('input', updatePreview);

        // Initialize preview
        updatePreview();
    </script>
</body>
</html>
