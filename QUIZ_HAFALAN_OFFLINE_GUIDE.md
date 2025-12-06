# 🎯 Quiz Hafalan Offline - Implementation Guide

## 📋 Overview

Quiz hafalan akan berjalan **100% offline** di mobile app menggunakan database lokal `hafalan.db` yang ada di `app/src/main/assets/database/`.

**Tidak perlu server/API untuk quiz hafalan!** ✅

---

## 🗄️ Database Schema (hafalan.db)

Pastikan `hafalan.db` punya struktur seperti ini:

```sql
-- Tabel hafalan
CREATE TABLE hafalan (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    korean TEXT NOT NULL,
    indonesia TEXT NOT NULL,
    romaji TEXT,
    category TEXT,
    level TEXT
);

-- Tabel hasil quiz (optional, untuk tracking)
CREATE TABLE quiz_results (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    word_id INTEGER NOT NULL,
    is_correct INTEGER NOT NULL,
    selected_answer TEXT,
    correct_answer TEXT,
    timestamp INTEGER,
    FOREIGN KEY (word_id) REFERENCES hafalan(id)
);
```

---

## 📱 Flutter Implementation

### 1. Database Helper

```dart
// lib/services/hafalan_database_helper.dart
import 'dart:io';
import 'dart:math';
import 'package:flutter/services.dart';
import 'package:path/path.dart';
import 'package:sqflite/sqflite.dart';

class HafalanDatabaseHelper {
  static final HafalanDatabaseHelper instance = HafalanDatabaseHelper._init();
  static Database? _database;

  HafalanDatabaseHelper._init();

  Future<Database> get database async {
    if (_database != null) return _database!;
    _database = await _initDB();
    return _database!;
  }

  Future<Database> _initDB() async {
    final dbPath = await getDatabasesPath();
    final path = join(dbPath, 'hafalan.db');

    // Copy from assets if not exists
    if (!await File(path).exists()) {
      ByteData data = await rootBundle.load('assets/database/hafalan.db');
      List<int> bytes = data.buffer.asUint8List();
      await File(path).writeAsBytes(bytes, flush: true);
    }

    return await openDatabase(path, version: 1);
  }

  // Get random word
  Future<Map<String, dynamic>?> getRandomWord({
    String? category,
    String? level,
  }) async {
    final db = await database;

    String whereClause = '';
    List<dynamic> whereArgs = [];

    if (category != null) {
      whereClause = 'category = ?';
      whereArgs.add(category);
    }

    if (level != null) {
      if (whereClause.isNotEmpty) whereClause += ' AND ';
      whereClause += 'level = ?';
      whereArgs.add(level);
    }

    final query = whereClause.isEmpty
        ? 'SELECT * FROM hafalan ORDER BY RANDOM() LIMIT 1'
        : 'SELECT * FROM hafalan WHERE $whereClause ORDER BY RANDOM() LIMIT 1';

    final result = await db.rawQuery(query, whereArgs);
    return result.isNotEmpty ? result.first : null;
  }

  // Get wrong answers (3 random different words)
  Future<List<String>> getWrongAnswers({
    required int excludeId,
    required String answerField, // 'indonesia' or 'korean'
    int count = 3,
  }) async {
    final db = await database;

    final result = await db.rawQuery('''
      SELECT $answerField
      FROM hafalan
      WHERE id != ?
      ORDER BY RANDOM()
      LIMIT ?
    ''', [excludeId, count]);

    return result.map((row) => row[answerField] as String).toList();
  }

  // Save quiz result
  Future<void> saveQuizResult({
    required int wordId,
    required bool isCorrect,
    required String selectedAnswer,
    required String correctAnswer,
  }) async {
    final db = await database;

    await db.insert('quiz_results', {
      'word_id': wordId,
      'is_correct': isCorrect ? 1 : 0,
      'selected_answer': selectedAnswer,
      'correct_answer': correctAnswer,
      'timestamp': DateTime.now().millisecondsSinceEpoch,
    });
  }

  // Get user stats
  Future<Map<String, dynamic>> getUserStats() async {
    final db = await database;

    final totalResult = await db.rawQuery(
      'SELECT COUNT(*) as total FROM quiz_results'
    );
    final total = totalResult.first['total'] as int;

    final correctResult = await db.rawQuery(
      'SELECT COUNT(*) as correct FROM quiz_results WHERE is_correct = 1'
    );
    final correct = correctResult.first['correct'] as int;

    final accuracy = total > 0 ? (correct / total * 100) : 0.0;

    return {
      'total': total,
      'correct': correct,
      'wrong': total - correct,
      'accuracy': accuracy,
    };
  }

  Future<void> close() async {
    final db = await database;
    db.close();
  }
}
```

---

### 2. Quiz Model

```dart
// lib/models/quiz_hafalan_model.dart
class QuizHafalan {
  final int wordId;
  final String question;
  final String? questionRomaji;
  final QuizMode mode;
  final List<String> options; // 4 options
  final int correctAnswerIndex; // 0-3
  final String correctAnswerText;
  final String? category;
  final String? level;

  QuizHafalan({
    required this.wordId,
    required this.question,
    this.questionRomaji,
    required this.mode,
    required this.options,
    required this.correctAnswerIndex,
    required this.correctAnswerText,
    this.category,
    this.level,
  });
}

enum QuizMode {
  koreanToIndonesia,
  indonesiaToKorean,
}
```

---

### 3. Quiz Generator Service

```dart
// lib/services/quiz_hafalan_generator.dart
import 'dart:math';
import 'package:your_app/models/quiz_hafalan_model.dart';
import 'package:your_app/services/hafalan_database_helper.dart';

class QuizHafalanGenerator {
  final HafalanDatabaseHelper _dbHelper = HafalanDatabaseHelper.instance;

  /// Generate quiz from hafalan database
  Future<QuizHafalan?> generateQuiz({
    QuizMode mode = QuizMode.koreanToIndonesia,
    String? category,
    String? level,
  }) async {
    // 1. Get random word
    final word = await _dbHelper.getRandomWord(
      category: category,
      level: level,
    );

    if (word == null) return null;

    final wordId = word['id'] as int;
    final korean = word['korean'] as String;
    final indonesia = word['indonesia'] as String;
    final romaji = word['romaji'] as String?;

    // 2. Determine question and answer based on mode
    String question;
    String correctAnswer;
    String answerField;

    if (mode == QuizMode.koreanToIndonesia) {
      question = korean;
      correctAnswer = indonesia;
      answerField = 'indonesia';
    } else {
      question = indonesia;
      correctAnswer = korean;
      answerField = 'korean';
    }

    // 3. Get 3 wrong answers
    final wrongAnswers = await _dbHelper.getWrongAnswers(
      excludeId: wordId,
      answerField: answerField,
      count: 3,
    );

    // 4. Combine and shuffle
    final allOptions = [correctAnswer, ...wrongAnswers];
    allOptions.shuffle(Random());

    // 5. Find correct answer index
    final correctIndex = allOptions.indexOf(correctAnswer);

    return QuizHafalan(
      wordId: wordId,
      question: question,
      questionRomaji: mode == QuizMode.koreanToIndonesia ? romaji : null,
      mode: mode,
      options: allOptions,
      correctAnswerIndex: correctIndex,
      correctAnswerText: correctAnswer,
      category: word['category'] as String?,
      level: word['level'] as String?,
    );
  }

  /// Submit answer and save result
  Future<bool> submitAnswer({
    required QuizHafalan quiz,
    required int selectedIndex,
  }) async {
    final isCorrect = selectedIndex == quiz.correctAnswerIndex;

    await _dbHelper.saveQuizResult(
      wordId: quiz.wordId,
      isCorrect: isCorrect,
      selectedAnswer: quiz.options[selectedIndex],
      correctAnswer: quiz.correctAnswerText,
    );

    return isCorrect;
  }

  /// Get user statistics
  Future<Map<String, dynamic>> getStats() async {
    return await _dbHelper.getUserStats();
  }
}
```

---

### 4. Quiz Screen (UI)

```dart
// lib/screens/quiz_hafalan_screen.dart
import 'dart:async';
import 'package:flutter/material.dart';
import 'package:your_app/models/quiz_hafalan_model.dart';
import 'package:your_app/services/quiz_hafalan_generator.dart';

class QuizHafalanScreen extends StatefulWidget {
  final QuizMode mode;
  final String? category;
  final String? level;

  const QuizHafalanScreen({
    Key? key,
    this.mode = QuizMode.koreanToIndonesia,
    this.category,
    this.level,
  }) : super(key: key);

  @override
  State<QuizHafalanScreen> createState() => _QuizHafalanScreenState();
}

class _QuizHafalanScreenState extends State<QuizHafalanScreen> {
  final QuizHafalanGenerator _generator = QuizHafalanGenerator();

  QuizHafalan? _currentQuiz;
  int? _selectedAnswer;
  bool _isAnswered = false;
  bool _isCorrect = false;
  int _score = 0;
  int _totalQuestions = 0;

  @override
  void initState() {
    super.initState();
    _loadNextQuiz();
  }

  Future<void> _loadNextQuiz() async {
    final quiz = await _generator.generateQuiz(
      mode: widget.mode,
      category: widget.category,
      level: widget.level,
    );

    setState(() {
      _currentQuiz = quiz;
      _selectedAnswer = null;
      _isAnswered = false;
    });
  }

  Future<void> _submitAnswer(int index) async {
    if (_isAnswered || _currentQuiz == null) return;

    setState(() {
      _selectedAnswer = index;
      _isAnswered = true;
      _totalQuestions++;
    });

    // Submit to database
    final isCorrect = await _generator.submitAnswer(
      quiz: _currentQuiz!,
      selectedIndex: index,
    );

    setState(() {
      _isCorrect = isCorrect;
      if (isCorrect) _score++;
    });

    // Auto-next after 4 seconds
    Timer(const Duration(seconds: 4), () {
      if (mounted) {
        _loadNextQuiz();
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    if (_currentQuiz == null) {
      return const Scaffold(
        body: Center(child: CircularProgressIndicator()),
      );
    }

    return Scaffold(
      appBar: AppBar(
        title: const Text('Quiz Hafalan'),
        actions: [
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: Center(
              child: Text(
                'Score: $_score/$_totalQuestions',
                style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
              ),
            ),
          ),
        ],
      ),
      body: Padding(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Question
            Card(
              elevation: 4,
              child: Padding(
                padding: const EdgeInsets.all(24.0),
                child: Column(
                  children: [
                    Text(
                      _currentQuiz!.question,
                      style: const TextStyle(
                        fontSize: 32,
                        fontWeight: FontWeight.bold,
                      ),
                      textAlign: TextAlign.center,
                    ),
                    if (_currentQuiz!.questionRomaji != null)
                      Padding(
                        padding: const EdgeInsets.only(top: 8.0),
                        child: Text(
                          _currentQuiz!.questionRomaji!,
                          style: TextStyle(
                            fontSize: 16,
                            color: Colors.grey[600],
                            fontStyle: FontStyle.italic,
                          ),
                        ),
                      ),
                  ],
                ),
              ),
            ),

            const SizedBox(height: 30),

            // Options
            Expanded(
              child: ListView.builder(
                itemCount: 4,
                itemBuilder: (context, index) {
                  final option = _currentQuiz!.options[index];
                  final isSelected = _selectedAnswer == index;
                  final isCorrectOption = index == _currentQuiz!.correctAnswerIndex;

                  Color? backgroundColor;
                  if (_isAnswered) {
                    if (isCorrectOption) {
                      backgroundColor = Colors.green[100];
                    } else if (isSelected && !_isCorrect) {
                      backgroundColor = Colors.red[100];
                    }
                  }

                  return Padding(
                    padding: const EdgeInsets.only(bottom: 12.0),
                    child: ElevatedButton(
                      style: ElevatedButton.styleFrom(
                        backgroundColor: backgroundColor ?? Colors.white,
                        foregroundColor: Colors.black87,
                        padding: const EdgeInsets.all(20),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12),
                          side: BorderSide(
                            color: isSelected ? Colors.blue : Colors.grey[300]!,
                            width: isSelected ? 3 : 1,
                          ),
                        ),
                      ),
                      onPressed: _isAnswered ? null : () => _submitAnswer(index),
                      child: Row(
                        children: [
                          Text(
                            '${index + 1}.',
                            style: const TextStyle(
                              fontSize: 18,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Text(
                              option,
                              style: const TextStyle(fontSize: 18),
                            ),
                          ),
                          if (_isAnswered && isCorrectOption)
                            const Icon(Icons.check_circle, color: Colors.green),
                          if (_isAnswered && isSelected && !_isCorrect)
                            const Icon(Icons.cancel, color: Colors.red),
                        ],
                      ),
                    ),
                  );
                },
              ),
            ),

            // Result message
            if (_isAnswered)
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: _isCorrect ? Colors.green[50] : Colors.red[50],
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Icon(
                      _isCorrect ? Icons.check_circle : Icons.cancel,
                      color: _isCorrect ? Colors.green : Colors.red,
                      size: 32,
                    ),
                    const SizedBox(width: 12),
                    Text(
                      _isCorrect ? 'Benar! 🎉' : 'Jawaban: ${_currentQuiz!.correctAnswerText}',
                      style: TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                        color: _isCorrect ? Colors.green : Colors.red,
                      ),
                    ),
                  ],
                ),
              ),

            const SizedBox(height: 20),
          ],
        ),
      ),
    );
  }
}
```

---

### 5. Launch Quiz

```dart
// Cara menggunakan:
Navigator.push(
  context,
  MaterialPageRoute(
    builder: (context) => const QuizHafalanScreen(
      mode: QuizMode.koreanToIndonesia,
      category: null, // or 'greeting', 'food', etc
      level: null,    // or 'beginner', 'intermediate', 'advanced'
    ),
  ),
);
```

---

## 🎯 Fitur yang Sudah Terimplementasi

✅ **Generate otomatis** 3 jawaban salah + 1 benar
✅ **Random selection** dari database
✅ **Auto-next** setelah 4 detik
✅ **Infinite loop** mode
✅ **Show correct answer** jika salah
✅ **Track score** dan statistik
✅ **100% offline** - tidak perlu internet
✅ **Filter by category/level** (optional)

---

## 📊 Statistik User

```dart
// Get user statistics
final stats = await QuizHafalanGenerator().getStats();

print('Total: ${stats['total']}');
print('Correct: ${stats['correct']}');
print('Wrong: ${stats['wrong']}');
print('Accuracy: ${stats['accuracy']}%');
```

---

## 🔧 Dependencies Required

```yaml
# pubspec.yaml
dependencies:
  flutter:
    sdk: flutter
  sqflite: ^2.3.0
  path: ^1.8.3
```

---

## 📝 pubspec.yaml Assets

```yaml
flutter:
  assets:
    - assets/database/hafalan.db
```

---

## ✨ Keuntungan Offline Approach

1. ✅ **Tidak perlu internet** - Quiz bisa dimainkan kapan saja
2. ✅ **Lebih cepat** - Tidak ada network latency
3. ✅ **Hemat bandwidth** - Tidak konsumsi data
4. ✅ **Privacy** - Data tidak keluar dari device
5. ✅ **Scalable** - Tidak beban server
6. ✅ **Gratis** - Tidak perlu hosting untuk quiz hafalan

---

## 🚀 Next Steps

1. Copy kode di atas ke project Flutter
2. Pastikan `hafalan.db` ada di `assets/database/`
3. Update `pubspec.yaml` dengan dependencies
4. Test quiz hafalan
5. Customize UI sesuai design app

---

**Quiz Hafalan siap digunakan 100% offline! 🎉**
