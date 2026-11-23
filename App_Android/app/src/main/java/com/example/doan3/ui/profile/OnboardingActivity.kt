package com.example.doan3.ui.profile

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.doan3.MainActivity
import com.example.doan3.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator

class OnboardingActivity : AppCompatActivity() {

    private var currentStep = 0
    private var selectedIndex = 0

    private var chosenGoal = "IELTS"
    private var chosenLevel = "Trung cấp (B1-B2)"
    private var chosenDailyMinutes = 10

    private lateinit var tvStepIndicator: TextView
    private lateinit var progressOnboarding: LinearProgressIndicator
    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var btnNextStep: MaterialButton
    private lateinit var cards: List<MaterialCardView>
    private lateinit var titles: List<TextView>
    private lateinit var descs: List<TextView>

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        tvStepIndicator = findViewById(R.id.tvStepIndicator)
        progressOnboarding = findViewById(R.id.progressOnboarding)
        tvTitle = findViewById(R.id.tvOnboardingTitle)
        tvSubtitle = findViewById(R.id.tvOnboardingSubtitle)
        btnNextStep = findViewById(R.id.btnNextStep)

        cards = listOf(
            findViewById(R.id.cardOpt1),
            findViewById(R.id.cardOpt2),
            findViewById(R.id.cardOpt3)
        )
        titles = listOf(
            findViewById(R.id.tvOpt1Title),
            findViewById(R.id.tvOpt2Title),
            findViewById(R.id.tvOpt3Title)
        )
        descs = listOf(
            findViewById(R.id.tvOpt1Desc),
            findViewById(R.id.tvOpt2Desc),
            findViewById(R.id.tvOpt3Desc)
        )

        cards.forEachIndexed { idx, card ->
            card.setOnClickListener {
                selectedIndex = idx
                highlightCard(idx)
            }
        }

        findViewById<TextView>(R.id.tvSkipOnboarding).setOnClickListener {
            finishOnboarding()
        }

        btnNextStep.setOnClickListener {
            saveCurrentStepChoice()
            if (currentStep < 2) {
                currentStep++
                selectedIndex = 0
                renderStep()
            } else {
                finishOnboarding()
            }
        }

        renderStep()
    }

    private fun renderStep() {
        tvStepIndicator.text = "Bước ${currentStep + 1} / 3"
        progressOnboarding.progress = ((currentStep + 1) * 100) / 3
        highlightCard(selectedIndex)

        when (currentStep) {
            0 -> {
                tvTitle.text = "Mục tiêu học tiếng Anh của bạn là gì?"
                tvSubtitle.text = "LexiVT XAI sẽ cá nhân hóa lộ trình và bộ từ vựng ngay từ ngày đầu."
                titles[0].text = "🎯 Luyện thi IELTS"
                descs[0].text = "Tập trung từ vựng học thuật và đề thi chuẩn band 6.5–8.0+"
                titles[1].text = "💼 Tiếng Anh công việc"
                descs[1].text = "Từ vựng văn phòng, hợp đồng, họp hành và email chuyên nghiệp"
                titles[2].text = "🗣️ Giao tiếp & Du lịch"
                descs[2].text = "Phản xạ tự nhiên, phát âm chuẩn và từ vựng đời sống hằng ngày"
                btnNextStep.text = "Tiếp tục"
            }
            1 -> {
                tvTitle.text = "Trình độ hiện tại của bạn?"
                tvSubtitle.text = "Giúp hệ thống XAI chọn độ khó khởi điểm phù hợp để tránh quá dễ hoặc quá khó."
                titles[0].text = "🌱 Mới bắt đầu (A1 – A2)"
                descs[0].text = "Củng cố nền tảng từ vựng cơ bản và cách phát âm chuẩn"
                titles[1].text = "🚀 Trung cấp (B1 – B2)"
                descs[1].text = "Mở rộng vốn từ học thuật, đọc hiểu nhanh và làm Quiz phản xạ"
                titles[2].text = "💎 Nâng cao (C1+)"
                descs[2].text = "Tối ưu từ vựng chuyên sâu, thành ngữ và ôn tập chống quên XAI"
                btnNextStep.text = "Tiếp tục"
            }
            2 -> {
                tvTitle.text = "Mục tiêu luyện tập mỗi ngày?"
                tvSubtitle.text = "Duy trì đều đặn mỗi ngày giúp đường cong trí nhớ Ebbinghaus bền vững gấp 3 lần."
                titles[0].text = "⚡ Nhẹ nhàng • 5 phút / ngày"
                descs[0].text = "Phù hợp người bận rộn, giữ chuỗi Streak dễ dàng"
                titles[1].text = "🔥 Tiêu chuẩn • 10 phút / ngày"
                descs[1].text = "Cân bằng giữa học từ mới và ôn tập Flashcard thông minh"
                titles[2].text = "🏆 Tăng tốc • 20 phút / ngày"
                descs[2].text = "Bứt phá vốn từ nhanh chóng cho kỳ thi sắp tới"
                btnNextStep.text = "Bắt đầu hành trình 🚀"
            }
        }
    }

    private fun saveCurrentStepChoice() {
        when (currentStep) {
            0 -> chosenGoal = listOf("IELTS", "Công việc", "Giao tiếp")[selectedIndex]
            1 -> chosenLevel = listOf("Mới bắt đầu (A1-A2)", "Trung cấp (B1-B2)", "Nâng cao (C1+)")[selectedIndex]
            2 -> chosenDailyMinutes = listOf(5, 10, 20)[selectedIndex]
        }
    }

    private fun highlightCard(activeIdx: Int) {
        cards.forEachIndexed { idx, card ->
            if (idx == activeIdx) {
                card.setCardBackgroundColor(Color.parseColor("#EFF6FF"))
                card.strokeColor = Color.parseColor("#2563EB")
                card.strokeWidth = (2 * resources.displayMetrics.density).toInt()
            } else {
                card.setCardBackgroundColor(Color.WHITE)
                card.strokeColor = Color.parseColor("#E2E8F0")
                card.strokeWidth = (1.5f * resources.displayMetrics.density).toInt()
            }
        }
    }

    private fun finishOnboarding() {
        val prefs = getSharedPreferences("DoAn3Prefs", MODE_PRIVATE)
        val userId = prefs.getString("USER_ID", "") ?: ""
        prefs.edit().apply {
            putBoolean("ONBOARDING_DONE_$userId", true)
            putString("USER_GOAL", chosenGoal)
            putString("USER_LEVEL_GOAL", chosenLevel)
            putInt("DAILY_GOAL_MINUTES", chosenDailyMinutes)
            apply()
        }
        ReminderScheduler.scheduleDailyReminder(this)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
