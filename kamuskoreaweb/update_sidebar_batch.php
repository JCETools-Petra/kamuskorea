<?php
/**
 * Script untuk update sidebar di semua file admin
 * Run script ini sekali untuk update semua file
 */

$adminFiles = [
    'admin_quizzes.php',
    'admin_exams.php',
    'admin_questions.php',
    'admin_profile.php'
];

$oldSidebar = <<<'HTML'
                <nav>
                    <a href="admin_dashboard.php"><i class="bi bi-speedometer2"></i> Dashboard</a>
                    <a href="admin_categories.php"><i class="bi bi-folder"></i> Kategori</a>
                    <a href="admin_quizzes.php"><i class="bi bi-patch-question"></i> Quiz</a>
                    <a href="admin_exams.php"><i class="bi bi-file-earmark-text"></i> Ujian/UBT</a>
                    <a href="admin_questions.php"><i class="bi bi-list-check"></i> Semua Soal</a>
                    <hr class="text-white">
                    <a href="admin_profile.php"><i class="bi bi-person-circle"></i> Profil</a>
                    <a href="admin_logout.php"><i class="bi bi-box-arrow-right"></i> Logout</a>
                </nav>
HTML;

$newSidebar = <<<'HTML'
                <nav>
                    <a href="admin_dashboard.php"><i class="bi bi-speedometer2"></i> Dashboard</a>
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
HTML;

echo "🔧 Starting sidebar update...\n\n";

$updatedCount = 0;

foreach ($adminFiles as $file) {
    if (!file_exists($file)) {
        echo "⚠️  File not found: $file\n";
        continue;
    }

    $content = file_get_contents($file);

    // Check if sidebar needs update
    if (strpos($content, 'admin_pdfs.php') !== false) {
        echo "✓ $file - Already updated\n";
        continue;
    }

    // Replace old sidebar dengan new sidebar, tapi keep "active" class
    $pattern = '/<nav>.*?<\/nav>/s';

    if (preg_match($pattern, $content, $matches)) {
        $oldNav = $matches[0];

        // Determine active page untuk file ini
        $activePage = '';
        if (strpos($file, 'quizzes') !== false) $activePage = 'quizzes';
        elseif (strpos($file, 'exams') !== false) $activePage = 'exams';
        elseif (strpos($file, 'questions') !== false) $activePage = 'questions';
        elseif (strpos($file, 'profile') !== false) $activePage = 'profile';

        // Build new nav dengan active class
        $newNav = $newSidebar;

        // Add active class ke menu yang sesuai
        if ($activePage) {
            $newNav = str_replace(
                "admin_{$activePage}.php\"><i",
                "admin_{$activePage}.php\" class=\"active\"><i",
                $newNav
            );
        }

        // Replace
        $newContent = str_replace($oldNav, $newNav, $content);

        // Backup original
        $backupFile = $file . '.backup';
        file_put_contents($backupFile, $content);

        // Write new content
        file_put_contents($file, $newContent);

        echo "✅ $file - Updated successfully (backup: $backupFile)\n";
        $updatedCount++;
    } else {
        echo "❌ $file - Nav pattern not found\n";
    }
}

echo "\n🎉 Update complete! $updatedCount file(s) updated.\n";
echo "\nℹ️  Note: Backup files created with .backup extension\n";
echo "   If something goes wrong, you can restore from backup.\n";
?>
