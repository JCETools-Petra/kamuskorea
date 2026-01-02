<?php
/**
 * Test Upload Endpoint with Error Display
 * Upload file ini ke kamuskorea/, lalu test upload dari form sederhana
 * URL: https://webtechsolution.my.id/kamuskorea/test_upload.php
 */

// Enable error display
ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);

header("Content-Type: text/html; charset=UTF-8");
?>
<!DOCTYPE html>
<html>
<head>
    <title>Test Profile Picture Upload</title>
    <style>
        body { font-family: monospace; padding: 20px; }
        .success { color: green; }
        .error { color: red; }
        pre { background: #f5f5f5; padding: 10px; border: 1px solid #ddd; }
    </style>
</head>
<body>
    <h1>Test Profile Picture Upload</h1>

    <form method="POST" enctype="multipart/form-data">
        <p>
            <label>Select Image:</label><br>
            <input type="file" name="image" accept="image/*" required>
        </p>
        <p>
            <label>Firebase UID (test):</label><br>
            <input type="text" name="uid" value="TEST_UID_123" required>
        </p>
        <p>
            <button type="submit">Upload</button>
        </p>
    </form>

    <?php
    if ($_SERVER['REQUEST_METHOD'] === 'POST' && isset($_FILES['image'])) {
        echo "<h2>Upload Test Results:</h2>";
        echo "<pre>";

        try {
            // Simulate the upload logic from api.php
            $file = $_FILES['image'];
            $uid = $_POST['uid'] ?? 'unknown';

            echo "1. File info:\n";
            echo "   Name: " . $file['name'] . "\n";
            echo "   Type: " . $file['type'] . "\n";
            echo "   Size: " . $file['size'] . " bytes\n";
            echo "   Tmp: " . $file['tmp_name'] . "\n";
            echo "   Error: " . $file['error'] . "\n\n";

            if ($file['error'] !== UPLOAD_ERR_OK) {
                throw new Exception("Upload error code: " . $file['error']);
            }

            $uploadDir = __DIR__ . '/uploads/profiles/';
            echo "2. Upload directory: $uploadDir\n";
            echo "   Exists: " . (is_dir($uploadDir) ? "YES" : "NO") . "\n";
            echo "   Writable: " . (is_writable($uploadDir) ? "YES" : "NO") . "\n\n";

            if (!is_dir($uploadDir)) {
                echo "   Creating directory...\n";
                mkdir($uploadDir, 0755, true);
            }

            $extension = pathinfo($file['name'], PATHINFO_EXTENSION);
            $filename = $uid . '_' . time() . '.' . $extension;
            $uploadPath = $uploadDir . $filename;

            echo "3. Target file: $uploadPath\n\n";

            echo "4. Moving uploaded file...\n";
            if (move_uploaded_file($file['tmp_name'], $uploadPath)) {
                echo "   ✅ SUCCESS!\n\n";

                $url = "https://webtechsolution.my.id/kamuskorea/uploads/profiles/$filename";
                echo "5. File URL:\n";
                echo "   <a href='$url' target='_blank'>$url</a>\n\n";

                echo "6. Database update would execute:\n";
                echo "   UPDATE users SET profile_picture_url = '$url' WHERE firebase_uid = '$uid'\n\n";

                echo "<span class='success'>✅ UPLOAD TEST SUCCESSFUL!</span>\n";
                echo "\nThis means the upload mechanism works.\n";
                echo "If Android app still fails, the issue is in:\n";
                echo "- Firebase Auth verification\n";
                echo "- App Check verification\n";
                echo "- Database connection/query\n";

            } else {
                throw new Exception("move_uploaded_file() failed");
            }

        } catch (Exception $e) {
            echo "\n<span class='error'>❌ ERROR: " . $e->getMessage() . "</span>\n";
            echo "\nStack trace:\n";
            echo $e->getTraceAsString();
        }

        echo "</pre>";
    }
    ?>

    <hr>
    <p><small>⚠️ Delete this file after testing for security!</small></p>
</body>
</html>
