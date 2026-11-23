package com.example.doan3.ui.roadmap

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.doan3.R
import com.example.doan3.ui.profile.KhamPhaActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlin.math.roundToInt

class QuizSummaryActivity : AppCompatActivity() {

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz_summary)

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

        if (wrongWords.isNotEmpty()) {
            findViewById<TextView>(R.id.tvWrongSectionTitle).visibility = View.VISIBLE
            btnReviewWrong.visibility = View.VISIBLE
            btnReviewWrong.text = "Ôn lại ${wrongWords.size} từ vừa sai 🔥"

            wrongWords.distinct().forEach { word ->
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(16.dp(), 14.dp(), 16.dp(), 14.dp())
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
                val tv = TextView(this).apply {
                    text = "• $word"
                    textSize = 15f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(Color.parseColor("#B91C1C"))
                }
                card.addView(tv)
                container.addView(card)
            }

            btnReviewWrong.setOnClickListener {
                val reviewIntent = Intent(this, KhamPhaActivity::class.java).apply {
                    putStringArrayListExtra("PREDICTIVE_WORDS", ArrayList(wrongWords.distinct()))
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
}
