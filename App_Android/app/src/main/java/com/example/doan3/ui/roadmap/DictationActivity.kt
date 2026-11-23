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
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.doan3.R
import com.example.doan3.ui.profile.RetrofitClient
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

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
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

        if (userInput.equals(item.word.trim(), ignoreCase = true)) {
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
            val intent = Intent(this, QuizSummaryActivity::class.java).apply {
                putExtra("SCORE", score)
                putExtra("TOTAL", words.size)
                putExtra("EARNED_XP", score * 10)
                putExtra("STEP_TITLE", "Luyện Nghe & Chép chính tả (Dictation)")
                putExtra("XAI_FEEDBACK", "Bạn đã hoàn thành bài luyện kỹ năng Nghe – Viết! Luyện tập đều đặn giúp tăng phản xạ chính tả nhanh hơn.")
                putStringArrayListExtra("WRONG_WORDS", wrongWords)
            }
            startActivity(intent)
            finish()
        }
    }

    override fun onDestroy() {
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }
}
