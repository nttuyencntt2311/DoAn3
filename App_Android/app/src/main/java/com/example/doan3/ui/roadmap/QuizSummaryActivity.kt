package com.example.doan3.ui.roadmap

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.doan3.R
import com.example.doan3.ui.profile.KhamPhaActivity
import com.example.doan3.ui.profile.RetrofitClient
import com.example.doan3.ui.vocabulary.Vocabulary
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale
import kotlin.math.roundToInt

class QuizSummaryActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz_summary)

        tts = TextToSpeech(this, this)

        val score = intent.getIntExtra("SCORE", 0)
        val total = intent.getIntExtra("TOTAL", 1).coerceAtLeast(1)
        val earnedXp = intent.getIntExtra("EARNED_XP", 0)
        val stepTitle = intent.getStringExtra("STEP_TITLE") ?: "Bài luyện tập Quiz"
        val xaiFeedback = intent.getStringExtra("XAI_FEEDBACK") ?: ""
        val wrongWords = intent.getStringArrayListExtra("WRONG_WORDS") ?: arrayListOf()

        val accuracy = ((score.toDouble() / total) * 100).roundToInt()

        findViewById<TextView>(R.id.tvSummaryHeadline).text = when {
            accuracy >= 85 -> "Xuất sắc quá! 🎉"
            accuracy >= 60 -> "Làm tốt lắm! 💪"
            else -> "Cố lên nhé! 🌱"
        }
        findViewById<TextView>(R.id.tvSummarySubtitle).text = stepTitle
        findViewById<TextView>(R.id.tvSummaryScore).text = "$score / $total"
        findViewById<TextView>(R.id.tvSummaryAccuracy).text = "$accuracy%"
        findViewById<TextView>(R.id.tvSummaryXp).text = "+$earnedXp XP"

        if (xaiFeedback.isNotBlank()) {
            findViewById<MaterialCardView>(R.id.cardXaiFeedback).visibility = View.VISIBLE
            findViewById<TextView>(R.id.tvXaiSummaryText).text = xaiFeedback
        }

        val container = findViewById<LinearLayout>(R.id.layoutWrongWordsContainer)
        val btnReviewWrong = findViewById<MaterialButton>(R.id.btnReviewWrongWords)

        val uniqueWrongWords = wrongWords.distinct()
        if (uniqueWrongWords.isNotEmpty()) {
            findViewById<TextView>(R.id.tvWrongSectionTitle).visibility = View.VISIBLE
            btnReviewWrong.visibility = View.VISIBLE
            btnReviewWrong.text = "Ôn lại ${uniqueWrongWords.size} từ vừa sai 🔥"

            renderWrongWordCards(container, uniqueWrongWords, emptyMap())

            // Tải thêm nghĩa & phiên âm từ Backend để hiển thị đầy đủ
            RetrofitClient.getInstance(this).getAllVocabularies()
                .enqueue(object : Callback<List<Vocabulary>> {
                    override fun onResponse(call: Call<List<Vocabulary>>, response: Response<List<Vocabulary>>) {
                        if (isFinishing || isDestroyed) return
                        if (response.isSuccessful) {
                            val map = (response.body() ?: emptyList())
                                .associateBy { it.word.trim().lowercase(Locale.ROOT) }
                            renderWrongWordCards(container, uniqueWrongWords, map)
                        }
                    }

                    override fun onFailure(call: Call<List<Vocabulary>>, t: Throwable) {}
                })

            btnReviewWrong.setOnClickListener {
                val reviewIntent = Intent(this, KhamPhaActivity::class.java).apply {
                    putStringArrayListExtra("PREDICTIVE_WORDS", ArrayList(uniqueWrongWords))
                    putExtra("CATEGORY_NAME", "Ôn từ vừa sai")
                }
                startActivity(reviewIntent)
                finish()
            }
        }

        findViewById<MaterialButton>(R.id.btnSummaryDone).setOnClickListener {
            finish()
        }
    }

    private fun renderWrongWordCards(
        container: LinearLayout,
        words: List<String>,
        vocabMap: Map<String, Vocabulary>
    ) {
        container.removeAllViews()
        words.forEach { word ->
            val vocab = vocabMap[word.trim().lowercase(Locale.ROOT)]
            val phonetic = vocab?.pronunciation?.takeIf { it.isNotBlank() } ?: ""
            val meaning = vocab?.meaning?.takeIf { it.isNotBlank() && it != "Từ vựng mới" } ?: ""

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(14.dp(), 12.dp(), 14.dp(), 12.dp())
                background = GradientDrawable().apply {
                    setColor(Color.WHITE)
                    cornerRadius = 14.dp().toFloat()
                    setStroke(1.dp(), Color.parseColor("#FECACA"))
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 8.dp() }
            }

            val textCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val wordLine = TextView(this).apply {
                text = if (phonetic.isNotBlank()) "$word  $phonetic" else word
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#B91C1C"))
            }
            textCol.addView(wordLine)

            if (meaning.isNotBlank()) {
                val meaningLine = TextView(this).apply {
                    text = meaning
                    textSize = 13f
                    setTextColor(Color.parseColor("#475569"))
                    setPadding(0, 2.dp(), 0, 0)
                }
                textCol.addView(meaningLine)
            }

            val speakBtn = TextView(this).apply {
                text = "🔊"
                textSize = 18f
                gravity = Gravity.CENTER
                setPadding(10.dp(), 6.dp(), 10.dp(), 6.dp())
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#FEF2F2"))
                    cornerRadius = 10.dp().toFloat()
                }
                setOnClickListener {
                    if (isTtsReady) {
                        tts?.speak(word, TextToSpeech.QUEUE_FLUSH, null, "")
                    }
                }
            }

            card.addView(textCol)
            card.addView(speakBtn)
            container.addView(card)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            isTtsReady = true
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
