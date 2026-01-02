<?php
/**
 * Test Leaderboard Endpoint
 * URL: https://webtechsolution.my.id/kamuskorea/test_leaderboard.php
 */

header("Content-Type: text/plain; charset=UTF-8");

echo "===========================================\n";
echo "TESTING LEADERBOARD ENDPOINT\n";
echo "===========================================\n\n";

// Test 1: Get leaderboard for all exams (UBT)
echo "Test 1: Leaderboard for all UBT (type=exam)\n";
echo str_repeat("-", 60) . "\n";

$url1 = 'https://webtechsolution.my.id/kamuskorea/api.php/assessments/leaderboard?type=exam';
$ch1 = curl_init($url1);
curl_setopt($ch1, CURLOPT_RETURNTRANSFER, true);
curl_setopt($ch1, CURLOPT_HEADER, true);
$response1 = curl_exec($ch1);
$httpCode1 = curl_getinfo($ch1, CURLINFO_HTTP_CODE);
$headerSize1 = curl_getinfo($ch1, CURLINFO_HEADER_SIZE);
curl_close($ch1);

$headers1 = substr($response1, 0, $headerSize1);
$body1 = substr($response1, $headerSize1);

echo "HTTP Status: $httpCode1\n";
echo "Response Body:\n$body1\n\n";

// Decode JSON to count entries
$data1 = json_decode($body1, true);
if ($data1 !== null) {
    echo "Entries count: " . count($data1) . "\n";
    if (count($data1) > 0) {
        echo "✅ Leaderboard has data\n";
        echo "First entry: " . json_encode($data1[0], JSON_PRETTY_PRINT) . "\n";
    } else {
        echo "❌ Leaderboard is EMPTY!\n";
    }
} else {
    echo "❌ Invalid JSON response\n";
    echo "JSON Error: " . json_last_error_msg() . "\n";
}

echo "\n" . str_repeat("=", 60) . "\n\n";

// Test 2: Check if there are any assessment results in database
echo "Test 2: Checking database for assessment results\n";
echo str_repeat("-", 60) . "\n";

require_once __DIR__ . '/api.php';

try {
    $stmt = $pdo->query("
        SELECT COUNT(*) as total
        FROM assessment_results ar
        INNER JOIN assessments a ON ar.assessment_id = a.id
        WHERE a.type = 'exam'
    ");
    $result = $stmt->fetch();
    echo "Total UBT results in database: " . $result['total'] . "\n";

    if ($result['total'] > 0) {
        echo "✅ Database has UBT results\n\n";

        // Show sample data
        $stmt2 = $pdo->query("
            SELECT
                ar.id,
                ar.user_id,
                u.name as user_name,
                a.title as assessment_title,
                ar.score,
                ar.time_taken_seconds,
                ar.created_at
            FROM assessment_results ar
            INNER JOIN assessments a ON ar.assessment_id = a.id
            LEFT JOIN users u ON ar.user_id = u.firebase_uid
            WHERE a.type = 'exam'
            ORDER BY ar.created_at DESC
            LIMIT 5
        ");
        $samples = $stmt2->fetchAll();

        echo "Sample results (last 5):\n";
        foreach ($samples as $sample) {
            echo "  - ID: {$sample['id']}, User: {$sample['user_name']}, ";
            echo "Assessment: {$sample['assessment_title']}, Score: {$sample['score']}/100, ";
            echo "Time: {$sample['time_taken_seconds']}s\n";
        }
    } else {
        echo "❌ NO UBT results in database - This is why leaderboard is empty!\n";
    }
} catch (Exception $e) {
    echo "Error querying database: " . $e->getMessage() . "\n";
}

echo "\n" . str_repeat("=", 60) . "\n\n";

// Test 3: Check assessments table
echo "Test 3: Checking assessments table\n";
echo str_repeat("-", 60) . "\n";

try {
    $stmt = $pdo->query("
        SELECT id, title, type, category_id
        FROM assessments
        WHERE type = 'exam'
        LIMIT 5
    ");
    $assessments = $stmt->fetchAll();

    echo "UBT Assessments in database: " . count($assessments) . "\n";
    if (count($assessments) > 0) {
        echo "✅ Assessments exist\n";
        foreach ($assessments as $assessment) {
            echo "  - ID: {$assessment['id']}, Title: {$assessment['title']}\n";
        }
    } else {
        echo "❌ NO UBT assessments found!\n";
    }
} catch (Exception $e) {
    echo "Error: " . $e->getMessage() . "\n";
}
