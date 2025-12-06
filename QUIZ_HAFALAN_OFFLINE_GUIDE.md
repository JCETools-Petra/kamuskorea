# 🎯 Quiz Hafalan Offline - Updated for Vocabulary Table

## 📋 Database Schema (Actual)

```sql
CREATE TABLE "Vocabulary" (
	"id"	INTEGER,
	"chapter_number"	INTEGER,
	"chapter_title_korean"	TEXT,
	"chapter_title_indonesian"	TEXT,
	"korean_word"	TEXT,
	"indonesian_meaning"	TEXT,
	PRIMARY KEY("id" AUTOINCREMENT)
);

-- Tabel hasil quiz (buat manual di hafalan.db)
CREATE TABLE IF NOT EXISTS "quiz_results" (
    "id" INTEGER PRIMARY KEY AUTOINCREMENT,
    "word_id" INTEGER NOT NULL,
    "is_correct" INTEGER NOT NULL,
    "selected_answer" TEXT,
    "correct_answer" TEXT,
    "timestamp" INTEGER,
    FOREIGN KEY (word_id) REFERENCES Vocabulary(id)
);
```

---

## 📱 Flutter Implementation (Updated)

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

    final db = await openDatabase(path, version: 1);

    // Create quiz_results table if not exists
    await db.execute('''
      CREATE TABLE IF NOT EXISTS quiz_results (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        word_id INTEGER NOT NULL,
        is_correct INTEGER NOT NULL,
        selected_answer TEXT,
        correct_answer TEXT,
        timestamp INTEGER,
        FOREIGN KEY (word_id) REFERENCES Vocabulary(id)
      )
    ''');

    return db;
  }

  /// Get random word from Vocabulary table
  Future<Map<String, dynamic>?> getRandomWord({
    int? chapterNumber,
  }) async {
    final db = await database;

    String query;
    List<dynamic> args = [];

    if (chapterNumber != null) {
      query = '''
        SELECT * FROM Vocabulary
        WHERE chapter_number = ?
        ORDER BY RANDOM()
        LIMIT 1
      ''';
      args.add(chapterNumber);
    } else {
      query = 'SELECT * FROM Vocabulary ORDER BY RANDOM() LIMIT 1';
    }

    final result = await db.rawQuery(query, args);
    return result.isNotEmpty ? result.first : null;
  }

  /// Get wrong answers (3 random different words)
  Future<List<String>> getWrongAnswers({
    required int excludeId,
    required String answerField, // 'indonesian_meaning' or 'korean_word'
    int count = 3,
  }) async {
    final db = await database;

    final result = await db.rawQuery('''
      SELECT $answerField
      FROM Vocabulary
      WHERE id != ?
      ORDER BY RANDOM()
      LIMIT ?
    ''', [excludeId, count]);

    return result.map((row) => row[answerField] as String).toList();
  }

  /// Save quiz result
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

  /// Get user stats
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

  /// Get all chapters
  Future<List<Map<String, dynamic>>> getChapters() async {
    final db = await database;

    final result = await db.rawQuery('''
      SELECT DISTINCT
        chapter_number,
        chapter_title_korean,
        chapter_title_indonesian,
        COUNT(*) as word_count
      FROM Vocabulary
      GROUP BY chapter_number
      ORDER BY chapter_number ASC
    ''');

    return result;
  }

  /// Get stats by chapter
  Future<Map<String, dynamic>> getChapterStats(int chapterNumber) async {
    final db = await database;

    final totalResult = await db.rawQuery('''
      SELECT COUNT(*) as total
      FROM quiz_results qr
      JOIN Vocabulary v ON qr.word_id = v.id
      WHERE v.chapter_number = ?
    ''', [chapterNumber]);
    final total = totalResult.first['total'] as int;

    final correctResult = await db.rawQuery('''
      SELECT COUNT(*) as correct
      FROM quiz_results qr
      JOIN Vocabulary v ON qr.word_id = v.id
      WHERE v.chapter_number = ? AND qr.is_correct = 1
    ''', [chapterNumber]);
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
  final QuizMode mode;
  final List<String> options; // 4 options
  final int correctAnswerIndex; // 0-3
  final String correctAnswerText;
  final int? chapterNumber;
  final String? chapterTitleKorean;
  final String? chapterTitleIndonesian;

  QuizHafalan({
    required this.wordId,
    required this.question,
    required this.mode,
    required this.options,
    required this.correctAnswerIndex,
    required this.correctAnswerText,
    this.chapterNumber,
    this.chapterTitleKorean,
    this.chapterTitleIndonesian,
  });
}

enum QuizMode {
  koreanToIndonesian,  // Show korean_word, answer indonesian_meaning
  indonesiaToKorean,   // Show indonesian_meaning, answer korean_word
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

  /// Generate quiz from Vocabulary table
  Future<QuizHafalan?> generateQuiz({
    QuizMode mode = QuizMode.koreanToIndonesian,
    int? chapterNumber,
  }) async {
    // 1. Get random word
    final word = await _dbHelper.getRandomWord(
      chapterNumber: chapterNumber,
    );

    if (word == null) return null;

    final wordId = word['id'] as int;
    final koreanWord = word['korean_word'] as String;
    final indonesianMeaning = word['indonesian_meaning'] as String;
    final chapterNum = word['chapter_number'] as int?;
    final chapterTitleKr = word['chapter_title_korean'] as String?;
    final chapterTitleId = word['chapter_title_indonesian'] as String?;

    // 2. Determine question and answer based on mode
    String question;
    String correctAnswer;
    String answerField;

    if (mode == QuizMode.koreanToIndonesian) {
      question = koreanWord;
      correctAnswer = indonesianMeaning;
      answerField = 'indonesian_meaning';
    } else {
      question = indonesianMeaning;
      correctAnswer = koreanWord;
      answerField = 'korean_word';
    }

    // 3. Get 3 wrong answers
    final wrongAnswers = await _dbHelper.getWrongAnswers(
      excludeId: wordId,
      answerField: answerField,
      count: 3,
    );

    // If not enough words, generate fallback wrong answers
    while (wrongAnswers.length < 3) {
      if (mode == QuizMode.koreanToIndonesian) {
        wrongAnswers.add(_generateRandomIndonesian());
      } else {
        wrongAnswers.add(_generateRandomKorean());
      }
    }

    // 4. Combine and shuffle
    final allOptions = [correctAnswer, ...wrongAnswers];
    allOptions.shuffle(Random());

    // 5. Find correct answer index
    final correctIndex = allOptions.indexOf(correctAnswer);

    return QuizHafalan(
      wordId: wordId,
      question: question,
      mode: mode,
      options: allOptions,
      correctAnswerIndex: correctIndex,
      correctAnswerText: correctAnswer,
      chapterNumber: chapterNum,
      chapterTitleKorean: chapterTitleKr,
      chapterTitleIndonesian: chapterTitleId,
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

  /// Get all chapters
  Future<List<Map<String, dynamic>>> getChapters() async {
    return await _dbHelper.getChapters();
  }

  /// Get chapter stats
  Future<Map<String, dynamic>> getChapterStats(int chapterNumber) async {
    return await _dbHelper.getChapterStats(chapterNumber);
  }

  // Fallback generators
  String _generateRandomIndonesian() {
    final words = ['Halo', 'Selamat', 'Terima kasih', 'Maaf', 'Senang',
                   'Sedih', 'Rumah', 'Mobil', 'Buku', 'Makan'];
    return words[Random().nextInt(words.length)];
  }

  String _generateRandomKorean() {
    final words = ['안녕', '고마워', '미안해', '사랑', '집',
                   '차', '책', '밥', '물', '친구'];
    return words[Random().nextInt(words.length)];
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
  final int? chapterNumber;

  const QuizHafalanScreen({
    Key? key,
    this.mode = QuizMode.koreanToIndonesian,
    this.chapterNumber,
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
      chapterNumber: widget.chapterNumber,
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
            // Chapter info
            if (_currentQuiz!.chapterNumber != null)
              Card(
                color: Colors.blue[50],
                child: Padding(
                  padding: const EdgeInsets.all(12.0),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      const Icon(Icons.book, size: 20, color: Colors.blue),
                      const SizedBox(width: 8),
                      Text(
                        'Chapter ${_currentQuiz!.chapterNumber}: ${_currentQuiz!.chapterTitleIndonesian}',
                        style: const TextStyle(
                          fontSize: 14,
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                    ],
                  ),
                ),
              ),

            const SizedBox(height: 20),

            // Question
            Card(
              elevation: 4,
              child: Padding(
                padding: const EdgeInsets.all(24.0),
                child: Text(
                  _currentQuiz!.question,
                  style: const TextStyle(
                    fontSize: 32,
                    fontWeight: FontWeight.bold,
                  ),
                  textAlign: TextAlign.center,
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
                    Expanded(
                      child: Text(
                        _isCorrect
                            ? 'Benar! 🎉'
                            : 'Jawaban Benar: ${_currentQuiz!.correctAnswerText}',
                        style: TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                          color: _isCorrect ? Colors.green : Colors.red,
                        ),
                        textAlign: TextAlign.center,
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

### 5. Chapter Selection Screen (Bonus)

```dart
// lib/screens/chapter_selection_screen.dart
import 'package:flutter/material.dart';
import 'package:your_app/models/quiz_hafalan_model.dart';
import 'package:your_app/screens/quiz_hafalan_screen.dart';
import 'package:your_app/services/quiz_hafalan_generator.dart';

class ChapterSelectionScreen extends StatefulWidget {
  final QuizMode mode;

  const ChapterSelectionScreen({
    Key? key,
    this.mode = QuizMode.koreanToIndonesian,
  }) : super(key: key);

  @override
  State<ChapterSelectionScreen> createState() => _ChapterSelectionScreenState();
}

class _ChapterSelectionScreenState extends State<ChapterSelectionScreen> {
  final QuizHafalanGenerator _generator = QuizHafalanGenerator();
  List<Map<String, dynamic>> _chapters = [];
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _loadChapters();
  }

  Future<void> _loadChapters() async {
    final chapters = await _generator.getChapters();
    setState(() {
      _chapters = chapters;
      _loading = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Pilih Chapter'),
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: _chapters.length + 1,
              itemBuilder: (context, index) {
                if (index == 0) {
                  // All chapters option
                  return Card(
                    elevation: 2,
                    margin: const EdgeInsets.only(bottom: 12),
                    child: ListTile(
                      leading: const CircleAvatar(
                        backgroundColor: Colors.blue,
                        child: Icon(Icons.all_inclusive, color: Colors.white),
                      ),
                      title: const Text(
                        'Semua Chapter',
                        style: TextStyle(fontWeight: FontWeight.bold),
                      ),
                      subtitle: const Text('Quiz dari semua vocabulary'),
                      trailing: const Icon(Icons.arrow_forward_ios),
                      onTap: () {
                        Navigator.push(
                          context,
                          MaterialPageRoute(
                            builder: (context) => QuizHafalanScreen(
                              mode: widget.mode,
                              chapterNumber: null,
                            ),
                          ),
                        );
                      },
                    ),
                  );
                }

                final chapter = _chapters[index - 1];
                final chapterNum = chapter['chapter_number'] as int;
                final titleId = chapter['chapter_title_indonesian'] as String;
                final titleKr = chapter['chapter_title_korean'] as String;
                final wordCount = chapter['word_count'] as int;

                return Card(
                  elevation: 2,
                  margin: const EdgeInsets.only(bottom: 12),
                  child: ListTile(
                    leading: CircleAvatar(
                      child: Text('$chapterNum'),
                    ),
                    title: Text(
                      titleId,
                      style: const TextStyle(fontWeight: FontWeight.bold),
                    ),
                    subtitle: Text('$titleKr • $wordCount kata'),
                    trailing: const Icon(Icons.arrow_forward_ios),
                    onTap: () {
                      Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (context) => QuizHafalanScreen(
                            mode: widget.mode,
                            chapterNumber: chapterNum,
                          ),
                        ),
                      );
                    },
                  ),
                );
              },
            ),
    );
  }
}
```

---

## 🚀 Cara Menggunakan

### 1. Langsung ke Quiz (All Chapters)
```dart
Navigator.push(
  context,
  MaterialPageRoute(
    builder: (context) => const QuizHafalanScreen(
      mode: QuizMode.koreanToIndonesian,
      chapterNumber: null, // All chapters
    ),
  ),
);
```

### 2. Quiz Specific Chapter
```dart
Navigator.push(
  context,
  MaterialPageRoute(
    builder: (context) => const QuizHafalanScreen(
      mode: QuizMode.koreanToIndonesian,
      chapterNumber: 1, // Chapter 1 only
    ),
  ),
);
```

### 3. Chapter Selection Screen
```dart
Navigator.push(
  context,
  MaterialPageRoute(
    builder: (context) => const ChapterSelectionScreen(
      mode: QuizMode.koreanToIndonesian,
    ),
  ),
);
```

---

## 📊 Get Statistics

```dart
// Overall stats
final stats = await QuizHafalanGenerator().getStats();
print('Total: ${stats['total']}');
print('Accuracy: ${stats['accuracy']}%');

// Chapter specific stats
final chapterStats = await QuizHafalanGenerator().getChapterStats(1);
print('Chapter 1 Accuracy: ${chapterStats['accuracy']}%');

// Get all chapters
final chapters = await QuizHafalanGenerator().getChapters();
for (var chapter in chapters) {
  print('Chapter ${chapter['chapter_number']}: ${chapter['chapter_title_indonesian']}');
  print('Words: ${chapter['word_count']}');
}
```

---

## ✅ Fitur yang Tersedia

✅ **Auto-generate** 3 jawaban salah + 1 benar
✅ **Random selection** dari Vocabulary table
✅ **Auto-next** setelah 4 detik
✅ **Infinite loop** mode
✅ **Show correct answer** jika salah
✅ **Track score** real-time
✅ **Filter by chapter** (optional)
✅ **Chapter selection screen**
✅ **Statistics** per chapter
✅ **100% offline** - tidak perlu internet

---

## 🔧 Dependencies

```yaml
# pubspec.yaml
dependencies:
  flutter:
    sdk: flutter
  sqflite: ^2.3.0
  path: ^1.8.3

flutter:
  assets:
    - assets/database/hafalan.db
```

---

**Sudah disesuaikan dengan schema Vocabulary table Anda! 🎉**
