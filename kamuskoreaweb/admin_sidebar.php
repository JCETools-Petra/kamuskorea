<?php
/**
 * Admin Sidebar Component
 * Include this file in all admin pages untuk sidebar yang konsisten
 */

function renderAdminSidebar($activePage = '') {
    ?>
    <div class="col-md-2 sidebar p-0">
        <div class="text-center py-4">
            <h4 class="text-white">📚 Admin Panel</h4>
        </div>
        <nav>
            <a href="admin_dashboard.php" class="<?= $activePage === 'dashboard' ? 'active' : '' ?>">
                <i class="bi bi-speedometer2"></i> Dashboard
            </a>
            <a href="admin_categories.php" class="<?= $activePage === 'categories' ? 'active' : '' ?>">
                <i class="bi bi-folder"></i> Kategori
            </a>
            <a href="admin_quizzes.php" class="<?= $activePage === 'quizzes' ? 'active' : '' ?>">
                <i class="bi bi-patch-question"></i> Quiz
            </a>
            <a href="admin_exams.php" class="<?= $activePage === 'exams' ? 'active' : '' ?>">
                <i class="bi bi-file-earmark-text"></i> Ujian/UBT
            </a>
            <a href="admin_questions.php" class="<?= $activePage === 'questions' ? 'active' : '' ?>">
                <i class="bi bi-list-check"></i> Semua Soal
            </a>
            <a href="admin_pdfs.php" class="<?= $activePage === 'pdfs' ? 'active' : '' ?>">
                <i class="bi bi-file-pdf"></i> E-Book PDF
            </a>
            <hr class="text-white">
            <a href="https://webtechsolution.my.id/kamuskorea/send_notification.php" target="_blank">
                <i class="bi bi-bell"></i> Push Notification
            </a>
            <hr class="text-white">
            <a href="admin_profile.php" class="<?= $activePage === 'profile' ? 'active' : '' ?>">
                <i class="bi bi-person-circle"></i> Profil
            </a>
            <a href="admin_logout.php">
                <i class="bi bi-box-arrow-right"></i> Logout
            </a>
        </nav>
    </div>
    <?php
}

// CSS untuk sidebar (include di <style> tag)
function getAdminSidebarCSS() {
    return "
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
    ";
}
?>
