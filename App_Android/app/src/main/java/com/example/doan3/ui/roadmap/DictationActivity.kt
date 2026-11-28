package com.example.doan3.ui.roadmap

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.doan3.R
import com.example.doan3.ui.profile.QuestionLog
import com.example.doan3.ui.profile.RetrofitClient
import com.example.doan3.ui.profile.SubmitQuizRequest
import com.example.doan3.ui.profile.SubmitQuizResponse
import com.example.doan3.ui.profile.SoundHapticHelper
import com.example.doan3.ui.vocabulary.Vocabulary
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class DictationActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private var isTtsReady = false

    private var words: List<Vocabulary> = emptyList()
    private var currentIndex = 0
    private var score = 0
    private var isChecked = false
    private val wrongWords = arrayListOf<String>()
    private val questionLogs = mutableListOf<QuestionLog>()
    private var sessionStartMs = 0L
    private var wordStartMs = 0L
    private var isSubmitting = false

    private lateinit var tvCounter: TextView
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var tvHintMeaning: TextView
    private lateinit var edtAnswer: TextInputEditText
    private lateinit var tvFeedback: TextView
    private lateinit var btnCheck: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dictation)

        tts = TextToSpeech(this, this)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { confirmExitDictation() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                confirmExitDictation()
            }
        })
        tvCounter = findViewById(R.id.tvDictationCounter)
        progressBar = findViewById(R.id.progressDictation)
        tvHintMeaning = findViewById(R.id.tvDictationHintMeaning)
        edtAnswer = findViewById(R.id.edtDictationAnswer)
        tvFeedback = findViewById(R.id.tvDictationFeedback)
        btnCheck = findViewById(R.id.btnCheckDictation)

        findViewById<MaterialButton>(R.id.btnPlayNormal).setOnClickListener { speakCurrent(1.0f) }
        findViewById<MaterialButton>(R.id.btnPlaySlow).setOnClickListener { speakCurrent(0.7f) }

        btnCheck.setOnClickListener {
            if (words.isEmpty()) return@setOnClickListener
            if (!isChecked) {
                evaluateCurrent()
            } else {
                nextWord()
            }
        }

        loadWords()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
            isTtsReady = true
            if (words.isNotEmpty()) speakCurrent(1.0f)
        }
    }

    private fun loadWords() {
        RetrofitClient.getInstance(this).getAllVocabularies().enqueue(object : Callback<List<Vocabulary>> {
            override fun onResponse(call: Call<List<Vocabulary>>, response: Response<List<Vocabulary>>) {
                val list = response.body()?.filter { it.word.isNotBlank() } ?: emptyList()
                startSession(if (list.isNotEmpty()) list else loadOfflineFallback())
            }

            override fun onFailure(call: Call<List<Vocabulary>>, t: Throwable) {
                startSession(loadOfflineFallback())
            }
        })
    }

    private fun loadOfflineFallback(): List<Vocabulary> {
        return try {
            val json = getSharedPreferences("VocabCachePrefs", Context.MODE_PRIVATE)
                .getString("CACHED_VOCAB_LIST", null)
            if (json != null) {
                val type = object : TypeToken<List<Vocabulary>>() {}.type
                Gson().fromJson<List<Vocabulary>>(json, type) ?: emptyList()
            } else emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun startSession(pool: List<Vocabulary>) {
        if (pool.isEmpty()) {
            Toast.makeText(this, "Chưa có từ vựng để luyện nghe. Hãy kết nối mạng trước!", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        words = pool.shuffled().take(8)
        currentIndex = 0
        score = 0
        wrongWords.clear()
        questionLogs.clear()
        sessionStartMs = System.currentTimeMillis()
        showCurrentWord()
    }

    private fun showCurrentWord() {
        val item = words[currentIndex]
        isChecked = false
        tvCounter.text = "${currentIndex + 1} / ${words.size}"
        progressBar.progress = (((currentIndex + 1).toFloat() / words.size) * 100).toInt()
        tvHintMeaning.text = "Gợi ý nghĩa: ${item.meaning}"
        edtAnswer.setText("")
        edtAnswer.isEnabled = true
        tvFeedback.visibility = View.GONE
        btnCheck.text = "Kiểm tra đáp án"
        wordStartMs = System.currentTimeMillis()
        speakCurrent(1.0f)
    }

    private fun speakCurrent(rate: Float) {
        if (!isTtsReady || currentIndex >= words.size) return
        tts.setSpeechRate(rate)
        tts.speak(words[currentIndex].word, TextToSpeech.QUEUE_FLUSH, null, "")
    }

    private fun evaluateCurrent() {
        val item = words[currentIndex]
        val userInput = edtAnswer.text?.toString()?.trim() ?: ""
        if (userInput.isEmpty()) {
            Toast.makeText(this, "Hãy gõ từ bạn nghe được trước nhé!", Toast.LENGTH_SHORT).show()
            return
        }
        isChecked = true
        edtAnswer.isEnabled = false
        tvFeedback.visibility = View.VISIBLE

        val elapsedMs = (System.currentTimeMillis() - wordStartMs).coerceAtLeast(500L)
        val isCorrect = userInput.equals(item.word.trim(), ignoreCase = true)
        questionLogs.add(QuestionLog(question = item.word, topic = "Dictation", isCorrect = isCorrect, elapsedTimeMs = elapsedMs))

        if (isCorrect) {
            score++
            SoundHapticHelper.playCorrect(btnCheck)
            tvFeedback.setTextColor(Color.parseColor("#047857"))
            tvFeedback.setBackgroundColor(Color.parseColor("#ECFDF5"))
            tvFeedback.text = "✅ Chính xác! '${item.word}' (${item.pronunciation})"
        } else {
            wrongWords.add(item.word)
            SoundHapticHelper.playWrong(btnCheck)
            tvFeedback.setTextColor(Color.parseColor("#DC2626"))
            tvFeedback.setBackgroundColor(Color.parseColor("#FEF2F2"))
            tvFeedback.text = "❌ Chưa đúng! Đáp án chuẩn: '${item.word}'"
        }

        btnCheck.text = if (currentIndex == words.lastIndex) "Xem tổng kết 🏆" else "Từ tiếp theo ➝"
    }

    private fun nextWord() {
        if (currentIndex < words.lastIndex) {
            currentIndex++
            showCurrentWord()
        } else {
            if (isSubmitting) return
            isSubmitting = true
            btnCheck.isEnabled = false
            btnCheck.text = "Đang đồng bộ kết quả XAI..."

            val totalElapsed = (System.currentTimeMillis() - sessionStartMs).coerceAtLeast(1000L)
            val minutes = (totalElapsed / 60000L).toInt().coerceIn(1, 60)
            val prefs = getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
            val todayKey = "TODAY_STUDY_MINS_" + java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())
            prefs.edit().putInt(todayKey, prefs.getInt(todayKey, 0) + minutes).apply()
            val avgMs = if (questionLogs.isNotEmpty()) {
                questionLogs.map { it.elapsedTimeMs }.average().toLong()
            } else {
                totalElapsed / words.size.coerceAtLeast(1)
            }
            val earnedXp = score * 10

            val req = SubmitQuizRequest(
                topic = "Luyện Nghe & Chép chính tả (Dictation)",
                score = score,
                totalQuestions = words.size,
                earnedXp = earnedXp,
                learnedWords = score,
                timeSpentMinutes = minutes,
                difficulty = 3,
                wrongWords = wrongWords.distinct(),
                avgElapsedTimeMs = avgMs,
                questionLogs = questionLogs
            )

            val defaultFeedback = "Bạn đã hoàn thành bài luyện kỹ năng Nghe – Viết! Các từ viết sai đã được đồng bộ vào Sổ tay Tiên tri XAI để nhắc ôn đúng lúc."
            RetrofitClient.getInstance(this).submitQuiz(req).enqueue(object : Callback<SubmitQuizResponse> {
                override fun onResponse(call: Call<SubmitQuizResponse>, response: Response<SubmitQuizResponse>) {
                    if (isFinishing || isDestroyed) return
                    val aiFb = response.body()?.aiReport?.pedagogicalFeedback?.takeIf { it.isNotBlank() }
                        ?: defaultFeedback
                    openSummary(earnedXp, aiFb)
                }

                override fun onFailure(call: Call<SubmitQuizResponse>, t: Throwable) {
                    if (isFinishing || isDestroyed) return
                    openSummary(earnedXp, defaultFeedback)
                }
            })
        }
    }

    private fun openSummary(earnedXp: Int, feedback: String) {
        val intent = Intent(this, QuizSummaryActivity::class.java).apply {
            putExtra("SCORE", score)
            putExtra("TOTAL", words.size)
            putExtra("EARNED_XP", earnedXp)
            putExtra("STEP_TITLE", "Luyện Nghe & Chép chính tả (Dictation)")
            putExtra("XAI_FEEDBACK", feedback)
            putStringArrayListExtra("WRONG_WORDS", wrongWords)
        }
        startActivity(intent)
        finish()
    }

    private fun confirmExitDictation() {
        if (isSubmitting || words.isEmpty()) {
            finish()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Thoát bài luyện nghe?")
            .setMessage("Bạn đang ở từ ${currentIndex + 1}/${words.size}. Nếu thoát lúc này phiên luyện nghe hiện tại sẽ chưa được lưu điểm.")
            .setPositiveButton("Tiếp tục nghe", null)
            .setNegativeButton("Thoát") { _, _ -> finish() }
            .show()
    }

    override fun onDestroy() {
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }
}
