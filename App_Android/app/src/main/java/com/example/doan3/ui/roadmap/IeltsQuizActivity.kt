package com.example.doan3.ui.roadmap

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.doan3.R
import com.example.doan3.databinding.IeltsQuizBinding
import com.example.doan3.ui.profile.RetrofitClient
import com.example.doan3.ui.profile.SubmitQuizRequest
import com.example.doan3.ui.profile.AiReport
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import android.os.CountDownTimer
import kotlin.math.max

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctAnswer: String,
    val pronounceText: String,
    val hint: String,
    // --- BỔ SUNG CHO XAI ---
    val is_high_risk: Boolean = false,
    val reward_multiplier: Int = 1,
    val challenge_message: String? = null,
    val ai_advice: String? = null
)

class IeltsQuizActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: IeltsQuizBinding
    private var ttsEngine: TextToSpeech? = null

    private var quizList = listOf<QuizQuestion>()
    private var currentQuestionIndex = 0
    private var score = 0

    private var countDownTimer: CountDownTimer? = null
    private val QUIZ_TIME_MILLIS: Long = 15 * 60 * 1000

    // 🚀 CÁC BIẾN CHO HỆ THỐNG XAI & THEO DÕI HỌC TẬP
    private var totalEarnedXp = 0
    private var questionStartTime: Long = 0 // Radar đo thời gian từng câu
    private var hasShownPacingAlert = false
    private var startTimeMillis: Long = 0 // Đo tổng thời gian bài học
    private val wrongWordsList = mutableListOf<String>() // Lưu danh sách từ sai để gửi về Sổ tay

    private var selectedOptionIndex: Int = -1
    private var isAnswerChecked: Boolean = false

    private val PREFS_NAME = "QuizProgressPrefs"
    private var currentStepTitle = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = IeltsQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ttsEngine = TextToSpeech(this, this)

        currentStepTitle = intent.getStringExtra("STEP_TITLE") ?: "Giao tiếp cơ bản"
        val stepDesc = intent.getStringExtra("STEP_DESC") ?: ""
        val questionCount = intent.getIntExtra("QUESTION_COUNT", 5)

        binding.tvCategory.text = currentStepTitle.uppercase()

        setupListeners()

        // Bắt đầu tính giờ tổng bài làm
        startTimeMillis = System.currentTimeMillis()

        if (loadProgress(currentStepTitle)) {
            Toast.makeText(this, "Đang tiếp tục bài học dang dở...", Toast.LENGTH_SHORT).show()
            displayQuestion()
            startTimer()
        } else {
            loadQuizFromAi(currentStepTitle, stepDesc, questionCount)
        }
    }

    override fun onPause() {
        super.onPause()
        saveProgress()
    }

    private fun saveProgress() {
        if (quizList.isEmpty() || currentQuestionIndex >= quizList.size) return
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val gson = Gson()
        val jsonList = gson.toJson(quizList)

        editor.putString("${currentStepTitle}_list", jsonList)
        editor.putInt("${currentStepTitle}_index", currentQuestionIndex)
        editor.putInt("${currentStepTitle}_score", score)
        editor.putInt("${currentStepTitle}_xp", totalEarnedXp)
        editor.apply()
    }

    private fun startTimer() {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(QUIZ_TIME_MILLIS, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                binding.tvTimer.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
            }
            override fun onFinish() {
                binding.tvTimer.text = "00:00"
                Toast.makeText(this@IeltsQuizActivity, "Hết giờ làm bài!", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    private fun loadProgress(stepTitle: String): Boolean {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonList = prefs.getString("${stepTitle}_list", null)
        if (jsonList != null) {
            val gson = Gson()
            val type = object : TypeToken<List<QuizQuestion>>() {}.type
            quizList = gson.fromJson(jsonList, type)
            currentQuestionIndex = prefs.getInt("${stepTitle}_index", 0)
            score = prefs.getInt("${stepTitle}_score", 0)
            totalEarnedXp = prefs.getInt("${stepTitle}_xp", 0)
            return true
        }
        return false
    }

    private fun clearProgress(stepTitle: String) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove("${stepTitle}_list").remove("${stepTitle}_index").remove("${stepTitle}_score").remove("${stepTitle}_xp").apply()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }
        binding.tvSkipQuestion.setOnClickListener { skipQuestion() }
        binding.tvGetHint.setOnClickListener {
            if (quizList.isNotEmpty()) {
                val currentQuestion = quizList[currentQuestionIndex]
                if (currentQuestion.ai_advice != null) {
                    binding.cardAiTutor.visibility = View.VISIBLE
                    binding.tvAiTutorMessage.text = "🤖 AI phân tích: ${currentQuestion.ai_advice}"
                } else {
                    binding.tvExplanation.visibility = View.VISIBLE
                    binding.tvExplanation.text = "💡 Dịch nghĩa: ${currentQuestion.hint}"
                    binding.tvExplanation.setTextColor(Color.parseColor("#D97706"))
                    binding.tvExplanation.setBackgroundColor(Color.parseColor("#FEF3C7"))
                }
            }
        }
        binding.btnOptionA.setOnClickListener { selectOption(0) }
        binding.btnOptionB.setOnClickListener { selectOption(1) }
        binding.btnOptionC.setOnClickListener { selectOption(2) }
        binding.btnOptionD.setOnClickListener { selectOption(3) }
        binding.btnNopBai.setOnClickListener { checkAnswerAndSubmit() }
    }

    private fun skipQuestion() {
        if (quizList.isNotEmpty() && currentQuestionIndex < quizList.size) {
            val currentQuiz = quizList[currentQuestionIndex]
            val mutableList = quizList.toMutableList()
            mutableList.add(currentQuiz)
            quizList = mutableList
            Toast.makeText(this, "Đã bỏ qua! Câu hỏi sẽ lặp lại ở cuối bài.", Toast.LENGTH_SHORT).show()
            goToNextQuestion()
        }
    }

    private fun resetOptionsUI() {
        val defaultBg = Color.parseColor("#FFFFFF")
        val defaultStroke = Color.parseColor("#CBD5E1")
        val defaultLetterBg = R.drawable.bg_circle_gray
        val defaultTextColor = Color.parseColor("#0F172A")

        val cards = listOf(binding.btnOptionA, binding.btnOptionB, binding.btnOptionC, binding.btnOptionD)
        val letters = listOf(binding.tvLetterA, binding.tvLetterB, binding.tvLetterC, binding.tvLetterD)
        val answers = listOf(binding.tvAnswerA, binding.tvAnswerB, binding.tvAnswerC, binding.tvAnswerD)
        val ticks = listOf(binding.imgTickA, binding.imgTickB, binding.imgTickC, binding.imgTickD)

        for (i in 0..3) {
            cards[i].setCardBackgroundColor(defaultBg)
            cards[i].strokeColor = defaultStroke
            letters[i].setBackgroundResource(defaultLetterBg)
            letters[i].setTextColor(defaultTextColor)
            answers[i].setTextColor(defaultTextColor)
            ticks[i].visibility = View.INVISIBLE
            cards[i].isClickable = true
        }
    }

    private fun selectOption(index: Int) {
        if (isAnswerChecked) return
        selectedOptionIndex = index
        resetOptionsUI()

        val cards = listOf(binding.btnOptionA, binding.btnOptionB, binding.btnOptionC, binding.btnOptionD)
        val letters = listOf(binding.tvLetterA, binding.tvLetterB, binding.tvLetterC, binding.tvLetterD)
        val answers = listOf(binding.tvAnswerA, binding.tvAnswerB, binding.tvAnswerC, binding.tvAnswerD)
        val ticks = listOf(binding.imgTickA, binding.imgTickB, binding.imgTickC, binding.imgTickD)

        cards[index].setCardBackgroundColor(Color.parseColor("#F0F7FF"))
        cards[index].strokeColor = Color.parseColor("#0056D2")
        letters[index].setBackgroundResource(R.drawable.bg_circle_blue)
        letters[index].setTextColor(Color.WHITE)
        answers[index].setTextColor(Color.parseColor("#0056D2"))
        ticks[index].visibility = View.VISIBLE
    }

    private fun loadQuizFromAi(stepTitle: String, stepDesc: String, questionCount: Int) {
        binding.btnNopBai.isEnabled = false
        binding.btnNopBai.text = "Đang tải bài học AI..."

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val apiService = RetrofitClient.getInstance(applicationContext)
                val response = apiService.getStepQuiz(stepTitle, stepDesc, questionCount)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                        quizList = response.body()!!
                        currentQuestionIndex = 0
                        score = 0
                        totalEarnedXp = 0
                        displayQuestion()
                        binding.btnNopBai.isEnabled = true
                        startTimeMillis = System.currentTimeMillis() // Reset đo thời gian tổng
                        startTimer()
                    } else {
                        loadMockQuiz()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadMockQuiz()
                }
            }
        }
    }

    private fun loadMockQuiz() {
        quizList = listOf(
            QuizQuestion(
                question = "We need to sign the ___ before starting the project.",
                options = listOf("contract", "contact", "compact", "contrast"),
                correctAnswer = "contract",
                pronounceText = "contract",
                hint = "Hợp đồng (văn bản pháp lý)",
                is_high_risk = true,
                challenge_message = "Câu hỏi thường bị nhầm lẫn!"
            )
        )
        currentQuestionIndex = 0
        score = 0
        totalEarnedXp = 0
        displayQuestion()
        binding.btnNopBai.isEnabled = true
        startTimeMillis = System.currentTimeMillis()
        startTimer()
    }

    private fun displayQuestion() {
        if (quizList.isEmpty() || currentQuestionIndex >= quizList.size) return
        isAnswerChecked = false
        selectedOptionIndex = -1

        // 🚀 BẮT ĐẦU BẤM GIỜ XAI KHI CÂU HỎI XUẤT HIỆN
        questionStartTime = System.currentTimeMillis()
        hasShownPacingAlert = false

        binding.btnNopBai.text = "KIỂM TRA"
        binding.tvExplanation.visibility = View.GONE
        binding.cardAiTutor.visibility = View.GONE
        resetOptionsUI()

        val currentQuiz = quizList[currentQuestionIndex]
        val currentNum = currentQuestionIndex + 1
        val totalNum = quizList.size
        val percent = ((currentNum.toFloat() / totalNum.toFloat()) * 100).toInt()

        binding.tvQuestionCount.text = "Question $currentNum / $totalNum"
        binding.tvProgressPercent.text = "$percent% Complete"
        binding.progressBar.max = totalNum
        binding.progressBar.progress = currentNum

        binding.tvQuestionKeyword.text = currentQuiz.pronounceText.uppercase()
        binding.tvQuestionSentence.text = currentQuiz.question
        binding.tvAnswerA.text = currentQuiz.options.getOrNull(0) ?: ""
        binding.tvAnswerB.text = currentQuiz.options.getOrNull(1) ?: ""
        binding.tvAnswerC.text = currentQuiz.options.getOrNull(2) ?: ""
        binding.tvAnswerD.text = currentQuiz.options.getOrNull(3) ?: ""

        if (currentQuiz.is_high_risk) {
            binding.cardQuestion.strokeColor = Color.parseColor("#EF4444")
            binding.cardQuestion.strokeWidth = 4
            binding.tvChallengeAlert.visibility = View.VISIBLE
            binding.tvChallengeAlert.text = currentQuiz.challenge_message
            binding.tvChallengeAlert.setBackgroundColor(Color.parseColor("#FEF2F2"))
            binding.tvChallengeAlert.setTextColor(Color.parseColor("#DC2626"))
        } else {
            binding.cardQuestion.strokeColor = Color.parseColor("#E2E8F0")
            binding.cardQuestion.strokeWidth = 2
            binding.tvChallengeAlert.visibility = View.GONE
        }

        if (currentQuiz.ai_advice != null) {
            binding.cardAiTutor.visibility = View.VISIBLE
            binding.tvAiTutorMessage.text = "🤖 ${currentQuiz.ai_advice}"
        }

        speakOut(currentQuiz.pronounceText)
    }

    private fun checkAnswerAndSubmit() {
        if (selectedOptionIndex == -1) {
            Toast.makeText(this, "Vui lòng chọn một đáp án!", Toast.LENGTH_SHORT).show()
            return
        }

        if (isAnswerChecked) {
            goToNextQuestion()
            return
        }

        val currentQuiz = quizList[currentQuestionIndex]
        val userAnswer = currentQuiz.options[selectedOptionIndex]
        val isCorrect = (userAnswer == currentQuiz.correctAnswer)

        // ==========================================
        // 🚀 XAI - RADAR THỜI GIAN
        // ==========================================
        val timeSpent = System.currentTimeMillis() - questionStartTime
        val prefs = getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
        val isPacingAlertEnabled = prefs.getBoolean("PREF_PACING_ALERT", true)

        if (isPacingAlertEnabled && timeSpent < 2000 && !isCorrect && !hasShownPacingAlert) {
            hasShownPacingAlert = true
            binding.cardAiTutor.visibility = View.VISIBLE
            binding.tvAiTutorMessage.text = "⏳ Radar XAI: Bạn trả lời dưới 2 giây và có dấu hiệu sai! Hãy đọc kỹ lại, bạn còn 1 cơ hội."
            binding.cardAiTutor.setCardBackgroundColor(Color.parseColor("#FEF3C7"))
            binding.tvAiTutorMessage.setTextColor(Color.parseColor("#D97706"))

            Toast.makeText(this, "⚠️ AI phát hiện bạn đang vội!", Toast.LENGTH_SHORT).show()

            selectedOptionIndex = -1
            resetOptionsUI()
            questionStartTime = System.currentTimeMillis()
            return
        }

        binding.cardAiTutor.setCardBackgroundColor(Color.parseColor("#F0F7FF"))
        binding.tvAiTutorMessage.setTextColor(Color.parseColor("#0056D2"))

        isAnswerChecked = true
        val correctIndex = currentQuiz.options.indexOf(currentQuiz.correctAnswer)

        val cards = listOf(binding.btnOptionA, binding.btnOptionB, binding.btnOptionC, binding.btnOptionD)
        val letters = listOf(binding.tvLetterA, binding.tvLetterB, binding.tvLetterC, binding.tvLetterD)
        val answers = listOf(binding.tvAnswerA, binding.tvAnswerB, binding.tvAnswerC, binding.tvAnswerD)

        for (card in cards) card.isClickable = false

        if (isCorrect) {
            score++
            val baseXP = 10
            val safeMultiplier = if (currentQuiz.reward_multiplier > 0) currentQuiz.reward_multiplier else 1
            val gainedXp = baseXP * safeMultiplier
            totalEarnedXp += gainedXp

            cards[selectedOptionIndex].setCardBackgroundColor(Color.parseColor("#F0FDF4"))
            cards[selectedOptionIndex].strokeColor = Color.parseColor("#16A34A")
            letters[selectedOptionIndex].setBackgroundResource(R.drawable.bg_circle_green)
            letters[selectedOptionIndex].setTextColor(Color.WHITE)
            answers[selectedOptionIndex].setTextColor(Color.parseColor("#16A34A"))

            binding.btnNopBai.text = "TUYỆT VỜI! +$gainedXp XP"
            binding.btnNopBai.setBackgroundColor(Color.parseColor("#16A34A"))

            if (currentQuiz.is_high_risk) {
                Toast.makeText(this, "🔥 Kẻ Phá Vỡ Giới Hạn! Nhận x3 XP", Toast.LENGTH_SHORT).show()
            }
        } else {
            // 🚀 BẮT LỖI TỪ SAI VÀO MẢNG
            if (!wrongWordsList.contains(currentQuiz.correctAnswer)) {
                wrongWordsList.add(currentQuiz.correctAnswer)
            }

            cards[selectedOptionIndex].setCardBackgroundColor(Color.parseColor("#FEF2F2"))
            cards[selectedOptionIndex].strokeColor = Color.parseColor("#DC2626")
            letters[selectedOptionIndex].setBackgroundResource(R.drawable.bg_circle_red)
            letters[selectedOptionIndex].setTextColor(Color.WHITE)
            answers[selectedOptionIndex].setTextColor(Color.parseColor("#DC2626"))

            if (correctIndex != -1) {
                cards[correctIndex].setCardBackgroundColor(Color.parseColor("#F0FDF4"))
                cards[correctIndex].strokeColor = Color.parseColor("#16A34A")
                letters[correctIndex].setBackgroundResource(R.drawable.bg_circle_green)
                letters[correctIndex].setTextColor(Color.WHITE)
                answers[correctIndex].setTextColor(Color.parseColor("#16A34A"))
            }

            binding.tvExplanation.visibility = View.VISIBLE
            binding.tvExplanation.text = "Chưa chính xác!\nGiải thích: ${currentQuiz.hint}"
            binding.tvExplanation.setTextColor(Color.parseColor("#DC2626"))
            binding.tvExplanation.setBackgroundColor(Color.parseColor("#FEF2F2"))

            binding.btnNopBai.text = "ĐÃ HIỂU! TIẾP TỤC"
            binding.btnNopBai.setBackgroundColor(Color.parseColor("#DC2626"))

            val mutableList = quizList.toMutableList()
            mutableList.add(currentQuiz)
            quizList = mutableList
        }
    }

    private fun goToNextQuestion() {
        binding.btnNopBai.setBackgroundColor(Color.parseColor("#6200EE"))
        currentQuestionIndex++
        if (currentQuestionIndex < quizList.size) {
            displayQuestion()
        } else {
            submitQuizAndShowReport()
        }
    }

    private fun submitQuizAndShowReport() {
        countDownTimer?.cancel()
        clearProgress(currentStepTitle)

        binding.btnNopBai.text = "Đang phân tích kết quả..."
        binding.btnNopBai.isEnabled = false

        // 🚀 TÍNH THỜI GIAN THỰC TẾ
        val timeSpentMillis = System.currentTimeMillis() - startTimeMillis
        val calculatedMinutes = max(1, (timeSpentMillis / 60000).toInt())

        val finalXp = 50 + totalEarnedXp
        val request = SubmitQuizRequest(
            topic = currentStepTitle,
            score = score,
            totalQuestions = quizList.size,
            earnedXp = finalXp,
            learnedWords = score,
            timeSpentMinutes = calculatedMinutes,
            difficulty = 3,
            wrongWords = wrongWordsList
        )

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getInstance(applicationContext)
                val response = api.submitQuiz(request).execute()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val submitData = response.body()!!
                        if (submitData.aiReport != null) {
                            showAiPostMortemDialog(submitData.aiReport, finalXp)
                        } else {
                            showBasicResultDialog(finalXp)
                        }
                    } else {
                        Toast.makeText(this@IeltsQuizActivity, "Lỗi nộp bài: ${response.code()}", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@IeltsQuizActivity, "Lỗi kết nối: ${e.message}", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    // 🚀 DỊCH LẠI LOGIC TEXT ĐỂ UI SẠCH SẼ HƠN (ĐƯỢC GỘP TỪ CODE CỦA BẠN BẠN)
    private fun formatFactorText(rawText: String): String {
        val regex = Regex("""\(Mức:\s*([\d.]+)\)""")
        return regex.replace(rawText) { matchResult ->
            val numberStr = matchResult.groupValues[1]
            val number = numberStr.toDoubleOrNull()
            if (number != null) {
                if (number > 1000) {
                    val seconds = number / 1000.0
                    "(${String.format(Locale.US, "%.1f", seconds)} giây)"
                } else {
                    "(Điểm: $numberStr)"
                }
            } else ""
        }
    }

    private fun showAiPostMortemDialog(aiReport: AiReport, finalXp: Int) {
        val bottomSheet = BottomSheetDialog(this)
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(60, 60, 60, 60)
            setBackgroundColor(Color.WHITE)
        }

        val titleText = TextView(this).apply {
            text = "BÁO CÁO PHÂN TÍCH NĂNG LỰC"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 40)
        }
        layout.addView(titleText)

        val accText = TextView(this).apply {
            val isGood = aiReport.predictedAccuracy >= 50.0
            val colorStr = if(isGood) "#16A34A" else "#EA580C"
            val status = if(isGood) "Duy trì ổn định 🚀" else "Cần cải thiện thêm ⚠️"

            text = "Mức độ thành thạo dự kiến: ${aiReport.predictedAccuracy}%\n($status)"
            textSize = 16f
            setTextColor(Color.parseColor(colorStr))
            setPadding(0, 0, 0, 40)
            gravity = android.view.Gravity.CENTER
        }
        layout.addView(accText)

        val posTitle = TextView(this).apply {
            text = "🌟 Nhân tố tích cực (Ưu điểm):"
            setTextColor(Color.parseColor("#059669"))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        layout.addView(posTitle)

        aiReport.xaiExplanation.positiveFactors.forEach { factor ->
            val pt = TextView(this).apply {
                text = "• ${formatFactorText(factor)}"
                setPadding(20, 15, 0, 20)
                setTextColor(Color.parseColor("#334155"))
            }
            layout.addView(pt)
        }

        val negTitle = TextView(this).apply {
            text = "⚠️ Nhân tố rủi ro (Hạn chế):"
            setTextColor(Color.parseColor("#DC2626"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 20, 0, 0)
        }
        layout.addView(negTitle)

        aiReport.xaiExplanation.negativeFactors.forEach { factor ->
            val nt = TextView(this).apply {
                text = "• ${formatFactorText(factor)}"
                setPadding(20, 15, 0, 20)
                setTextColor(Color.parseColor("#334155"))
            }
            layout.addView(nt)
        }

        val btnClose = Button(this).apply {
            text = "NHẬN +$finalXp XP & HOÀN THÀNH"
            setBackgroundColor(Color.parseColor("#0056D2"))
            setTextColor(Color.WHITE)
            setPadding(0, 30, 0, 30)
            setOnClickListener {
                bottomSheet.dismiss()
                finish()
            }
        }
        val btnParams = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 50, 0, 0) }

        layout.addView(btnClose, btnParams)

        bottomSheet.setContentView(layout)
        bottomSheet.setCancelable(false)
        bottomSheet.show()
    }

    private fun showBasicResultDialog(finalXp: Int) {
        AlertDialog.Builder(this)
            .setTitle("Hoàn Thành Bài Học! 🎉")
            .setMessage("Chúc mừng bạn đã hoàn thành xuất sắc.\nSố câu đúng: $score / ${quizList.size}\nBạn nhận được: +$finalXp XP!")
            .setCancelable(false)
            .setPositiveButton("Tuyệt vời") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .create()
            .show()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) ttsEngine?.setLanguage(Locale.US)
    }

    private fun speakOut(text: String) {
        if (text.isNotEmpty()) ttsEngine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
    }

    override fun onDestroy() {
        ttsEngine?.stop()
        ttsEngine?.shutdown()
        super.onDestroy()
    }
}