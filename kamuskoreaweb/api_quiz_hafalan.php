<?php
/**
 * ========================================
 * API QUIZ HAFALAN AUTO-GENERATE
 * ========================================
 * Generates automatic quiz from vocabulary database
 * - Random word selection
 * - Auto-generate 3 wrong answers + 1 correct answer
 * - Infinite quiz mode
 * - Track results
 */

header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: GET, POST, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Authorization");
header("Content-Type: application/json; charset=UTF-8");

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

require_once __DIR__ . '/config.php';

try {
    $pdo = new PDO(
        "mysql:host=$db_host;dbname=$db_name;charset=utf8mb4",
        $db_user,
        $db_pass,
        [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC
        ]
    );
} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode([
        'success' => false,
        'message' => 'Database connection failed'
    ]);
    exit();
}

$method = $_SERVER['REQUEST_METHOD'];
$action = $_GET['action'] ?? '';

// ========================================
// GET RANDOM QUIZ
// ========================================
if ($method === 'GET' && $action === 'get_quiz') {
    $category = $_GET['category'] ?? null;
    $level = $_GET['level'] ?? null;
    $quiz_mode = $_GET['mode'] ?? 'korean_to_indonesian'; // or 'indonesian_to_korean'

    try {
        // Build query to get random word
        $sql = "SELECT * FROM vocabulary WHERE 1=1";
        $params = [];

        if ($category) {
            $sql .= " AND category = ?";
            $params[] = $category;
        }

        if ($level) {
            $sql .= " AND level = ?";
            $params[] = $level;
        }

        $sql .= " ORDER BY RAND() LIMIT 1";

        $stmt = $pdo->prepare($sql);
        $stmt->execute($params);
        $word = $stmt->fetch();

        if (!$word) {
            echo json_encode([
                'success' => false,
                'message' => 'No vocabulary found in database'
            ]);
            exit();
        }

        // Determine question and correct answer based on mode
        if ($quiz_mode === 'korean_to_indonesian') {
            $question = $word['korean'];
            $question_romaji = $word['romaji'];
            $correct_answer = $word['indonesian'];
            $question_type = 'korean';
        } else {
            $question = $word['indonesian'];
            $question_romaji = null;
            $correct_answer = $word['korean'];
            $question_type = 'indonesian';
        }

        // Generate 3 wrong answers from other words
        $wrongSql = "SELECT " . ($quiz_mode === 'korean_to_indonesian' ? 'indonesian' : 'korean') . " as answer
                     FROM vocabulary
                     WHERE id != ?
                     ORDER BY RAND()
                     LIMIT 3";
        $wrongStmt = $pdo->prepare($wrongSql);
        $wrongStmt->execute([$word['id']]);
        $wrongAnswers = $wrongStmt->fetchAll(PDO::FETCH_COLUMN);

        // If not enough words, generate random Korean-sounding wrong answers
        while (count($wrongAnswers) < 3) {
            if ($quiz_mode === 'korean_to_indonesian') {
                $wrongAnswers[] = generateRandomIndonesian();
            } else {
                $wrongAnswers[] = generateRandomKorean();
            }
        }

        // Combine and shuffle answers
        $allAnswers = array_merge([$correct_answer], $wrongAnswers);
        shuffle($allAnswers);

        // Find correct answer position (1-4)
        $correctPosition = array_search($correct_answer, $allAnswers) + 1;

        echo json_encode([
            'success' => true,
            'quiz' => [
                'vocabulary_id' => $word['id'],
                'question' => $question,
                'question_romaji' => $question_romaji,
                'question_type' => $question_type,
                'image_url' => $word['image_url'],
                'audio_url' => $word['audio_url'],
                'options' => [
                    '1' => $allAnswers[0],
                    '2' => $allAnswers[1],
                    '3' => $allAnswers[2],
                    '4' => $allAnswers[3]
                ],
                'correct_answer' => $correctPosition,
                'correct_answer_text' => $correct_answer,
                'explanation' => $word['example_sentence'] ?? null,
                'category' => $word['category'],
                'level' => $word['level']
            ]
        ]);

    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode([
            'success' => false,
            'message' => 'Failed to generate quiz: ' . $e->getMessage()
        ]);
    }
}

// ========================================
// SUBMIT ANSWER
// ========================================
elseif ($method === 'POST' && $action === 'submit_answer') {
    $input = json_decode(file_get_contents('php://input'), true);

    $user_id = $input['user_id'] ?? null;
    $vocabulary_id = $input['vocabulary_id'] ?? null;
    $selected_answer = $input['selected_answer'] ?? null; // 1,2,3,4
    $correct_answer = $input['correct_answer'] ?? null;   // 1,2,3,4
    $time_spent = $input['time_spent'] ?? 0;

    if (!$user_id || !$vocabulary_id || !$selected_answer || !$correct_answer) {
        echo json_encode([
            'success' => false,
            'message' => 'Missing required fields'
        ]);
        exit();
    }

    $is_correct = ($selected_answer == $correct_answer);

    try {
        $stmt = $pdo->prepare("
            INSERT INTO quiz_hafalan_results
            (user_id, vocabulary_id, is_correct, selected_answer, correct_answer, time_spent)
            VALUES (?, ?, ?, ?, ?, ?)
        ");
        $stmt->execute([
            $user_id,
            $vocabulary_id,
            $is_correct ? 1 : 0,
            $selected_answer,
            $correct_answer,
            $time_spent
        ]);

        echo json_encode([
            'success' => true,
            'is_correct' => $is_correct,
            'message' => $is_correct ? 'Jawaban benar!' : 'Jawaban salah!'
        ]);

    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode([
            'success' => false,
            'message' => 'Failed to save result: ' . $e->getMessage()
        ]);
    }
}

// ========================================
// GET USER STATS
// ========================================
elseif ($method === 'GET' && $action === 'stats') {
    $user_id = $_GET['user_id'] ?? null;

    if (!$user_id) {
        echo json_encode([
            'success' => false,
            'message' => 'User ID required'
        ]);
        exit();
    }

    try {
        // Total quizzes
        $total = $pdo->prepare("SELECT COUNT(*) as total FROM quiz_hafalan_results WHERE user_id = ?");
        $total->execute([$user_id]);
        $totalCount = $total->fetch()['total'];

        // Correct answers
        $correct = $pdo->prepare("SELECT COUNT(*) as correct FROM quiz_hafalan_results WHERE user_id = ? AND is_correct = 1");
        $correct->execute([$user_id]);
        $correctCount = $correct->fetch()['correct'];

        // Accuracy
        $accuracy = $totalCount > 0 ? round(($correctCount / $totalCount) * 100, 2) : 0;

        // Average time
        $avgTime = $pdo->prepare("SELECT AVG(time_spent) as avg_time FROM quiz_hafalan_results WHERE user_id = ?");
        $avgTime->execute([$user_id]);
        $avgTimeSeconds = $avgTime->fetch()['avg_time'] ?? 0;

        echo json_encode([
            'success' => true,
            'stats' => [
                'total_quizzes' => (int)$totalCount,
                'correct_answers' => (int)$correctCount,
                'wrong_answers' => (int)($totalCount - $correctCount),
                'accuracy_percentage' => $accuracy,
                'average_time_seconds' => round($avgTimeSeconds, 2)
            ]
        ]);

    } catch (Exception $e) {
        http_response_code(500);
        echo json_encode([
            'success' => false,
            'message' => 'Failed to get stats: ' . $e->getMessage()
        ]);
    }
}

// ========================================
// INVALID REQUEST
// ========================================
else {
    http_response_code(400);
    echo json_encode([
        'success' => false,
        'message' => 'Invalid request',
        'usage' => [
            'get_quiz' => 'GET /api_quiz_hafalan.php?action=get_quiz&mode=korean_to_indonesian&category=greeting&level=beginner',
            'submit_answer' => 'POST /api_quiz_hafalan.php?action=submit_answer',
            'stats' => 'GET /api_quiz_hafalan.php?action=stats&user_id=xxx'
        ]
    ]);
}

// ========================================
// HELPER FUNCTIONS
// ========================================
function generateRandomIndonesian() {
    $words = ['Halo', 'Selamat', 'Terima kasih', 'Maaf', 'Senang', 'Sedih', 'Rumah', 'Mobil', 'Buku', 'Makan'];
    return $words[array_rand($words)];
}

function generateRandomKorean() {
    $words = ['안녕', '고마워', '미안해', '사랑', '집', '차', '책', '밥', '물', '친구'];
    return $words[array_rand($words)];
}
?>
