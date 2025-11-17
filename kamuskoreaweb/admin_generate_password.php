<?php
/**
 * Generate Password Hash for Admin
 * Jalankan file ini sekali untuk mendapatkan hash yang benar
 * HAPUS FILE INI SETELAH SELESAI!
 */

$password = 'kamuskorea2024';
$hash = password_hash($password, PASSWORD_DEFAULT);

echo "<h2>Password Hash Generator</h2>";
echo "<p><strong>Password:</strong> " . htmlspecialchars($password) . "</p>";
echo "<p><strong>Generated Hash:</strong><br><code>" . htmlspecialchars($hash) . "</code></p>";

echo "<hr>";
echo "<h3>SQL untuk Update Password:</h3>";
echo "<pre>";
echo "UPDATE admin_users SET password = '" . $hash . "' WHERE username = 'admin';";
echo "</pre>";

echo "<hr>";
echo "<h3>Atau INSERT baru:</h3>";
echo "<pre>";
echo "-- Hapus dulu jika sudah ada\n";
echo "DELETE FROM admin_users WHERE username = 'admin';\n\n";
echo "-- Insert dengan hash baru\n";
echo "INSERT INTO admin_users (username, password, email, full_name) VALUES\n";
echo "('admin', '" . $hash . "', 'admin@kamuskorea.com', 'Administrator');";
echo "</pre>";

echo "<hr>";
echo "<p style='color:red;'><strong>PENTING:</strong> Hapus file ini setelah selesai untuk keamanan!</p>";
?>
