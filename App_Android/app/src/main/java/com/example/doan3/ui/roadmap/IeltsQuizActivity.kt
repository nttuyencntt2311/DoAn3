package com.example.doan3.ui.roadmap

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.doan3.R
import com.example.doan3.databinding.IeltsQuizBinding
import com.example.doan3.ui.profile.AiReport
import com.example.doan3.ui.profile.QuestionLog
import com.example.doan3.ui.profile.RetrofitClient
import com.example.doan3.ui.profile.SubmitQuizRequest
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.max

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctAnswer: String,
    val pronounceText: String,
    val hint: String,
    val is_high_risk: Boolean = false,
    val reward_multiplier: Int = 1,
    val challenge_message: String? = null,
    val ai_advice: String? = null
)

class IeltsQuizActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: IeltsQuizBinding
    private var ttsEngine: TextToSpeech? = null
    private var isTtsReady = false
    private var pendingSpeechText: String? = null

    private var quizList = listOf<QuizQuestion>()
    private var currentQuestionIndex = 0
    private var score = 0
    private var difficultyLevel = 3

    private var countDownTimer: CountDownTimer? = null
    private val QUIZ_TIME_MILLIS: Long = 15 * 60 * 1000
    private var remainingTimeMillis: Long = QUIZ_TIME_MILLIS

    private var totalEarnedXp = 0
    private var questionStartTime: Long = 0L
    private var hasShownPacingAlert = false
    private var accumulatedElapsedMillis: Long = 0L
    private var sessionResumeTimestamp: Long = 0L
    private val wrongWordsList = mutableListOf<String>()
    private val questionLogsList = mutableListOf<QuestionLog>()

    private var selectedOptionIndex: Int = -1
    private var isAnswerChecked: Boolean = false
    private var isSubmitted: Boolean = false

    private val PREFS_NAME = "QuizProgressPrefs"
    private var currentStepId = ""
    private var currentStepTitle = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = IeltsQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ttsEngine = TextToSpeech(this, this)

        currentStepId = intent.getStringExtra("STEP_ID") ?: ""
        currentStepTitle = intent.getStringExtra("STEP_TITLE") ?: "Giao tiếp cơ bản"
        val stepDesc = intent.getStringExtra("STEP_DESC") ?: ""
        val questionCount = intent.getIntExtra("QUESTION_COUNT", 5)
        difficultyLevel = intent.getIntExtra("DIFFICULTY_LEVEL", 3)

        binding.tvCategory.text = currentStepTitle.uppercase()

        setupListeners()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                confirmExitQuiz()
            }
        })

        sessionResumeTimestamp = System.currentTimeMillis()

        if (loadProgress(currentStepTitle)) {
            Toast.makeText(this, "Đang tiếp tục bài học dang dở...", Toast.LENGTH_SHORT).show()
            displayQuestion()
            startTimer(remainingTimeMillis)
        } else {
            loadQuizFromAi(currentStepTitle, stepDesc, questionCount)
        }
    }

    override fun onPause() {
        super.onPause()
        countDownTimer?.cancel()
        if (!isSubmitted) {
            if (sessionResumeTimestamp > 0L) {
                accumulatedElapsedMillis += (System.currentTimeMillis() - sessionResumeTimestamp).coerceAtLeast(0L)
                sessionResumeTimestamp = System.currentTimeMillis()
            }
            saveProgress()
        }
    }

    override fun onResume() {
        super.onResume()
        sessionResumeTimestamp = System.currentTimeMillis()
        if (quizList.isNotEmpty() && !isSubmitted && remainingTimeMillis > 0L && currentQuestionIndex < quizList.size) {
            startTimer(remainingTimeMillis)
        }
    }

    private fun saveProgress() {
        if (quizList.isEmpty() || currentQuestionIndex >= quizList.size || isSubmitted) return
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val gson = Gson()
        prefs.edit().apply {
            putString("${currentStepTitle}_list", gson.toJson(quizList))
            putInt("${currentStepTitle}_index", currentQuestionIndex)
            putInt("${currentStepTitle}_score", score)
            putInt("${currentStepTitle}_xp", totalEarnedXp)
            putLong("${currentStepTitle}_remaining_ms", remainingTimeMillis)
            putLong("${currentStepTitle}_elapsed_ms", accumulatedElapsedMillis)
            putString("${currentStepTitle}_wrong_words", gson.toJson(wrongWordsList))
            putString("${currentStepTitle}_question_logs", gson.toJson(questionLogsList))
            apply()
        }
    }

    private fun startTimer(durationMillis: Long) {
        countDownTimer?.cancel()
        remainingTimeMillis = durationMillis.coerceAtLeast(0L)
        if (remainingTimeMillis == 0L) {
            handleTimeUp()
            return
        }

        countDownTimer = object : CountDownTimer(remainingTimeMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingTimeMillis = millisUntilFinished
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                binding.tvTimer.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                remainingTimeMillis = 0L
                binding.tvTimer.text = "00:00"
                handleTimeUp()
            }
        }.start()
    }

    private fun handleTimeUp() {
        if (isSubmitted || isFinishing || isDestroyed) return
        lockAllInputs()
        Toast.makeText(this, "Hết giờ làm bài! Đang tự động nộp bài...", Toast.LENGTH_SHORT).show()
        submitQuizAndShowReport()
    }

    private fun lockAllInputs() {
        binding.btnOptionA.isClickable = false
        binding.btnOptionB.isClickable = false
        binding.btnOptionC.isClickable = false
        binding.btnOptionD.isClickable = false
        binding.btnNopBai.isEnabled = false
        binding.tvSkipQuestion.isEnabled = false
        binding.tvGetHint.isEnabled = false
    }

    private fun loadProgress(stepTitle: String): Boolean {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonList = prefs.getString("${stepTitle}_list", null) ?: return false
        return try {
            val gson = Gson()
            val listType = object : TypeToken<List<QuizQuestion>>() {}.type
            val loadedList: List<QuizQuestion> = gson.fromJson(jsonList, listType)
            val savedIndex = prefs.getInt("${stepTitle}_index", 0)
            if (loadedList.isEmpty() || savedIndex >= loadedList.size) {
                clearProgress(stepTitle)
                return false
            }

            quizList = loadedList
            currentQuestionIndex = savedIndex
            score = prefs.getInt("${stepTitle}_score", 0)
            totalEarnedXp = prefs.getInt("${stepTitle}_xp", 0)
            remainingTimeMillis = prefs.getLong("${stepTitle}_remaining_ms", QUIZ_TIME_MILLIS)
                .coerceIn(1000L, QUIZ_TIME_MILLIS)
            accumulatedElapsedMillis = prefs.getLong("${stepTitle}_elapsed_ms", 0L)

            wrongWordsList.clear()
            prefs.getString("${stepTitle}_wrong_words", null)?.let { json ->
                val wrongType = object : TypeToken<List<String>>() {}.type
                wrongWordsList.addAll(gson.fromJson<List<String>>(json, wrongType) ?: emptyList())
            }

            questionLogsList.clear()
            prefs.getString("${stepTitle}_question_logs", null)?.let { json ->
                val logsType = object : TypeToken<List<QuestionLog>>() {}.type
                questionLogsList.addAll(gson.fromJson<List<QuestionLog>>(json, logsType) ?: emptyList())
            }
            true
        } catch (e: Exception) {
            clearProgress(stepTitle)
            false
        }
    }

    private fun clearProgress(stepTitle: String) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove("${stepTitle}_list")
            .remove("${stepTitle}_index")
            .remove("${stepTitle}_score")
            .remove("${stepTitle}_xp")
            .remove("${stepTitle}_remaining_ms")
            .remove("${stepTitle}_elapsed_ms")
            .remove("${stepTitle}_wrong_words")
            .remove("${stepTitle}_question_logs")
            .apply()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { confirmExitQuiz() }
        binding.tvSkipQuestion.setOnClickListener { skipQuestion() }
        binding.tvGetHint.setOnClickListener {
            if (quizList.isNotEmpty() && currentQuestionIndex < quizList.size) {
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

    /** Bỏ qua câu hỏi: ghi nhận log chưa trả lời đúng và chuyển câu kế tiếp, không phình quizList */
    private fun skipQuestion() {
        if (isSubmitted || isAnswerChecked) return
        if (quizList.isNotEmpty() && currentQuestionIndex < quizList.size) {
            val currentQuiz = quizList[currentQuestionIndex]
            val timeSpent = (System.currentTimeMillis() - questionStartTime).coerceAtLeast(0L)

            if (!wrongWordsList.contains(currentQuiz.correctAnswer)) {
                wrongWordsList.add(currentQuiz.correctAnswer)
            }
            questionLogsList.add(
                QuestionLog(
                    question = currentQuiz.question,
                    topic = currentStepTitle,
                    isCorrect = false,
                    elapsedTimeMs = timeSpent
                )
            )
            Toast.makeText(this, "Đã bỏ qua câu hỏi này.", Toast.LENGTH_SHORT).show()
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
        if (isAnswerChecked || isSubmitted) return
        if (currentQuestionIndex >= quizList.size) return
        val currentQuiz = quizList[currentQuestionIndex]
        if (index !in currentQuiz.options.indices) return

        selectedOptionIndex = index
        resetOptionsUI()

        val cards = listOf(binding.btnOptionA, binding.btnOptionB, binding.btnOptionC, binding.btnOptionD)
        val letters = listOf(binding.tvLetterA, binding.tvLetterB, binding.tvLetterC, binding.tvLetterD)
        val answers = listOf(binding.tvAnswerA, binding.tvAnswerB, binding.tvAnswerC, binding.tvAnswerD)
        val ticks = listOf(binding.imgTickA, binding.imgTickB, binding.imgTickC, binding.imgTickD)

        cards[index].setCardBackgroundColor(Color.parseColor("#F0F7FF"))
        cards[index].strokeColor = Color.parseColor("#2563EB")
        letters[index].setBackgroundResource(R.drawable.bg_circle_blue)
        letters[index].setTextColor(Color.WHITE)
        answers[index].setTextColor(Color.parseColor("#2563EB"))
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
                    if (isFinishing || isDestroyed) return@withContext
                    if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                        quizList = response.body()!!
                        currentQuestionIndex = 0
                        score = 0
                        totalEarnedXp = 0
                        accumulatedElapsedMillis = 0L
                        remainingTimeMillis = QUIZ_TIME_MILLIS
                        sessionResumeTimestamp = System.currentTimeMillis()
                        displayQuestion()
                        binding.btnNopBai.isEnabled = true
                        startTimer(remainingTimeMillis)
                    } else {
                        Toast.makeText(this@IeltsQuizActivity, "Không tải được bài từ AI, đang mở câu mẫu.", Toast.LENGTH_SHORT).show()
                        loadMockQuiz()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (isFinishing || isDestroyed) return@withContext
                    Toast.makeText(this@IeltsQuizActivity, "Mất kết nối máy chủ, đang mở câu mẫu.", Toast.LENGTH_SHORT).show()
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
        accumulatedElapsedMillis = 0L
        remainingTimeMillis = QUIZ_TIME_MILLIS
        sessionResumeTimestamp = System.currentTimeMillis()
        displayQuestion()
        binding.btnNopBai.isEnabled = true
        startTimer(remainingTimeMillis)
    }

    private fun displayQuestion() {
        if (quizList.isEmpty() || currentQuestionIndex >= quizList.size) return
        isAnswerChecked = false
        selectedOptionIndex = -1

        questionStartTime = System.currentTimeMillis()
        hasShownPacingAlert = false

        binding.btnNopBai.text = "KIỂM TRA"
        binding.btnNopBai.setBackgroundColor(Color.parseColor("#6200EE"))
        binding.tvExplanation.visibility = View.GONE
        binding.cardAiTutor.visibility = View.GONE
        resetOptionsUI()

        val currentQuiz = quizList[currentQuestionIndex]
        val currentNum = currentQuestionIndex + 1
        val totalNum = quizList.size
        val percent = ((currentNum.toFloat() / totalNum.toFloat()) * 100).toInt()

        binding.tvQuestionCount.text = "Câu $currentNum / $totalNum"
        binding.tvProgressPercent.text = "$percent% Hoàn thành"
        binding.progressBar.max = totalNum
        binding.progressBar.progress = currentNum

        binding.tvQuestionKeyword.text = currentQuiz.pronounceText.uppercase()
        binding.tvQuestionSentence.text = currentQuiz.question

        val cards = listOf(binding.btnOptionA, binding.btnOptionB, binding.btnOptionC, binding.btnOptionD)
        val answers = listOf(binding.tvAnswerA, binding.tvAnswerB, binding.tvAnswerC, binding.tvAnswerD)
        for (i in 0..3) {
            val opt = currentQuiz.options.getOrNull(i)
            if (opt.isNullOrBlank()) {
                cards[i].visibility = View.GONE
                cards[i].isClickable = false
            } else {
                cards[i].visibility = View.VISIBLE
                cards[i].isClickable = true
                answers[i].text = opt
            }
        }

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
        if (isSubmitted || currentQuestionIndex >= quizList.size) return

        if (isAnswerChecked) {
            goToNextQuestion()
            return
        }

        if (selectedOptionIndex == -1) {
            Toast.makeText(this, "Vui lòng chọn một đáp án!", Toast.LENGTH_SHORT).show()
            return
        }

        val currentQuiz = quizList[currentQuestionIndex]
        val userAnswer = currentQuiz.options.getOrNull(selectedOptionIndex) ?: return
        val isCorrect = (userAnswer == currentQuiz.correctAnswer)

        val timeSpent = (System.currentTimeMillis() - questionStartTime).coerceAtLeast(0L)
        val prefs = getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
        val isPacingAlertEnabled = prefs.getBoolean("PREF_PACING_ALERT", true)
        val savedAvgTimeMs = prefs.getLong("USER_AVG_TIME_MS", 12000L)
        val personalThresholdMs = (savedAvgTimeMs * 0.3).toLong().coerceIn(1500L, 6000L)

        if (isPacingAlertEnabled && timeSpent < personalThresholdMs && !isCorrect && !hasShownPacingAlert) {
            hasShownPacingAlert = true
            val secThreshold = String.format(Locale.US, "%.1f", personalThresholdMs / 1000.0)
            binding.cardAiTutor.visibility = View.VISIBLE
            binding.tvAiTutorMessage.text = "⏳ Radar XAI cá nhân hóa: Bạn trả lời nhanh hơn ngưỡng ${secThreshold}s (30% tốc độ TB của bạn) và có dấu hiệu sai! Hãy đọc kỹ lại, bạn còn 1 cơ hội."
            binding.cardAiTutor.setCardBackgroundColor(Color.parseColor("#FEF3C7"))
            binding.tvAiTutorMessage.setTextColor(Color.parseColor("#D97706"))

            Toast.makeText(this, "⚠️ AI phát hiện bạn đang vội!", Toast.LENGTH_SHORT).show()

            selectedOptionIndex = -1
            resetOptionsUI()
            questionStartTime = System.currentTimeMillis()
            return
        }

        binding.cardAiTutor.setCardBackgroundColor(Color.parseColor("#F0F7FF"))
        binding.tvAiTutorMessage.setTextColor(Color.parseColor("#2563EB"))

        isAnswerChecked = true
        questionLogsList.add(
            QuestionLog(
                question = currentQuiz.question,
                topic = currentStepTitle,
                isCorrect = isCorrect,
                elapsedTimeMs = timeSpent
            )
        )

        // Nhãn giải thích tốc độ phản xạ trực tiếp (Live XAI Tag)
        val secSpent = String.format(Locale.US, "%.1f", timeSpent / 1000.0)
        when {
            timeSpent < 2500L -> {
                binding.cardAiTutor.visibility = View.VISIBLE
                binding.cardAiTutor.setCardBackgroundColor(Color.parseColor("#FFFBEB"))
                binding.tvAiTutorMessage.setTextColor(Color.parseColor("#B45309"))
                binding.tvAiTutorMessage.text = if (isCorrect) {
                    "⚡ XAI Live: Phản xạ siêu nhanh (${secSpent}s) và chính xác! Bạn đã ghi nhớ rất chắc từ này."
                } else {
                    "⚡ XAI Live: Bạn chọn khá vội (${secSpent}s) — dấu hiệu đoán nhanh. Đã đánh dấu từ này để ôn lại sớm!"
                }
            }
            timeSpent > 25000L -> {
                binding.cardAiTutor.visibility = View.VISIBLE
                binding.cardAiTutor.setCardBackgroundColor(Color.parseColor("#EFF6FF"))
                binding.tvAiTutorMessage.setTextColor(Color.parseColor("#1D4ED8"))
                binding.tvAiTutorMessage.text =
                    "⏱️ XAI Live: Bạn suy nghĩ ${secSpent}s ở câu này (dấu hiệu còn do dự) ➝ ưu tiên đưa vào Sổ tay Tiên tri để củng cố phản xạ."
            }
        }
        if (isCorrect) {
            com.example.doan3.ui.profile.SoundHapticHelper.playCorrect(binding.btnNopBai)
        } else {
            com.example.doan3.ui.profile.SoundHapticHelper.playWrong(binding.btnNopBai)
        }
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
                Toast.makeText(this, "🔥 Kẻ Phá Vỡ Giới Hạn! Nhận x$safeMultiplier XP", Toast.LENGTH_SHORT).show()
            }
        } else {
            if (!wrongWordsList.contains(currentQuiz.correctAnswer)) {
                wrongWordsList.add(currentQuiz.correctAnswer)
            }

            cards[selectedOptionIndex].setCardBackgroundColor(Color.parseColor("#FEF2F2"))
            cards[selectedOptionIndex].strokeColor = Color.parseColor("#DC2626")
            letters[selectedOptionIndex].setBackgroundResource(R.drawable.bg_circle_red)
            letters[selectedOptionIndex].setTextColor(Color.WHITE)
            answers[selectedOptionIndex].setTextColor(Color.parseColor("#DC2626"))

            if (correctIndex in 0..3) {
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
        if (isSubmitted) return
        isSubmitted = true
        countDownTimer?.cancel()
        clearProgress(currentStepTitle)

        binding.btnNopBai.text = "Đang phân tích kết quả..."
        binding.btnNopBai.isEnabled = false

        val finalElapsedMillis = accumulatedElapsedMillis + (System.currentTimeMillis() - sessionResumeTimestamp).coerceAtLeast(0L)
        val calculatedMinutes = (finalElapsedMillis / 60000).toInt().coerceIn(1, 180)

        val exactAvgMs = if (questionLogsList.isNotEmpty()) {
            questionLogsList.map { it.elapsedTimeMs }.average().toLong()
        } else {
            (finalElapsedMillis / max(1, quizList.size))
        }
        val prefs = getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
        val todayKey = "TODAY_STUDY_MINS_" + java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())
        val updatedTodayMins = prefs.getInt(todayKey, 0) + calculatedMinutes
        prefs.edit()
            .putLong("USER_AVG_TIME_MS", exactAvgMs)
            .putInt(todayKey, updatedTodayMins)
            .apply()

        val completionBonus = if (quizList.isNotEmpty()) (50 * score) / quizList.size else 0
        val finalXp = completionBonus + totalEarnedXp
        val request = SubmitQuizRequest(
            topic = currentStepTitle,
            score = score,
            totalQuestions = quizList.size,
            earnedXp = finalXp,
            learnedWords = score,
            timeSpentMinutes = calculatedMinutes,
            difficulty = difficultyLevel,
            wrongWords = wrongWordsList,
            avgElapsedTimeMs = exactAvgMs,
            questionLogs = questionLogsList
        )

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val api = RetrofitClient.getInstance(applicationContext)
                val response = api.submitQuiz(request).execute()

                // Nếu bài Quiz thuộc một chặng Lộ trình AI và học viên đạt >= 50%, gọi API mở khóa chặng tiếp theo
                if (response.isSuccessful && currentStepId.isNotBlank() && quizList.isNotEmpty() && score * 2 >= quizList.size) {
                    try {
                        api.completeRoadmapStep(
                            mapOf(
                                "stepId" to currentStepId,
                                "score" to score.toString(),
                                "totalQuestions" to quizList.size.toString()
                            )
                        )
                    } catch (stepErr: Exception) {
                        stepErr.printStackTrace()
                    }
                }

                withContext(Dispatchers.Main) {
                    if (isFinishing || isDestroyed) return@withContext
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
                    if (isFinishing || isDestroyed) return@withContext
                    Toast.makeText(this@IeltsQuizActivity, "Lỗi kết nối: ${e.message}", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

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

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    private fun friendlyShapSummary(item: com.example.doan3.ui.profile.ShapDetail): Pair<String, String> {
        val pos = item.shapScore >= 0.0
        val impact = when {
            kotlin.math.abs(item.shapScore) >= 0.35 -> if (pos) "Hỗ trợ rất tốt" else "Ảnh hưởng lớn"
            kotlin.math.abs(item.shapScore) >= 0.15 -> if (pos) "Hỗ trợ tốt" else "Cần chú ý"
            else -> if (pos) "Tích cực nhẹ" else "Lưu ý nhỏ"
        }
        val label = when (item.feature) {
            "question_difficulty" -> if (pos) "Độ khó bài học vừa sức với bạn" else "Độ khó bài học khá thử thách"
            "elapsed_time" -> if (pos) "Tốc độ đọc đề & suy nghĩ ổn định" else "Thời gian suy nghĩ chưa đều"
            "user_avg_elapsed_time" -> if (pos) "Nhịp làm bài trung bình rất tốt" else "Cần giữ nhịp suy nghĩ ổn định hơn"
            "log_time_diff" -> if (pos) "Lịch ôn tập đều đặn" else "Khoảng nghỉ giữa các buổi hơi lâu"
            "user_history_acc" -> if (pos) "Nền tảng kiến thức lịch sử vững" else "Nền tảng từ vựng cần củng cố thêm"
            "rolling_acc_5" -> if (pos) "Phong độ các câu gần đây đang lên" else "Phong độ các câu gần đây hơi chững"
            "user_questions_attempted" -> if (pos) "Kinh nghiệm luyện tập phong phú" else "Cần luyện thêm để quen dạng bài"
            else -> item.viDescription.substringBefore(" (")
        }
        return Pair(label, impact)
    }

    private fun showAiPostMortemDialog(aiReport: AiReport, finalXp: Int) {
        if (isFinishing || isDestroyed) return
        val bottomSheet = BottomSheetDialog(this)
        val scrollView = android.widget.ScrollView(this)
        val layout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(20.dp(), 20.dp(), 20.dp(), 20.dp())
            setBackgroundColor(Color.WHITE)
        }

        val accuracyPct = if (quizList.isNotEmpty()) (score * 100) / quizList.size else 0
        val isRoadmapQuiz = currentStepId.isNotBlank()
        val passedStep = !isRoadmapQuiz || (quizList.isNotEmpty() && score * 2 >= quizList.size)

        val titleText = TextView(this).apply {
            text = "BÁO CÁO PHÂN TÍCH HỌC TẬP"
            textSize = 19f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 6.dp())
        }
        layout.addView(titleText)

        val accText = TextView(this).apply {
            val isGood = accuracyPct >= 60
            val colorStr = if (isGood) "#059669" else "#D97706"
            val status = when {
                accuracyPct >= 80 -> "Xuất sắc 🚀"
                accuracyPct >= 50 -> "Đạt yêu cầu 👍"
                else -> "Cần ôn thêm một chút 🌱"
            }
            text = "Kết quả: $score / ${quizList.size} câu đúng ($accuracyPct%) • $status"
            textSize = 15.5f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(Color.parseColor(colorStr))
            setPadding(0, 0, 0, 10.dp())
            gravity = android.view.Gravity.CENTER
        }
        layout.addView(accText)

        // Banner trạng thái qua chặng Lộ trình AI (rõ ràng cho người học)
        if (isRoadmapQuiz) {
            val stepBanner = TextView(this).apply {
                text = if (passedStep) {
                    "🎉 Chúc mừng! Bạn đã hoàn thành chặng này và mở khóa chặng tiếp theo."
                } else {
                    "📌 Bạn cần đạt tối thiểu 50% số câu đúng để hoàn thành và mở khóa chặng tiếp theo. Ôn từ sai rồi thử lại nhé!"
                }
                textSize = 13f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(Color.parseColor(if (passedStep) "#065F46" else "#9A3412"))
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(Color.parseColor(if (passedStep) "#ECFDF5" else "#FFF7ED"))
                    cornerRadius = 12.dp().toFloat()
                    setStroke(1.dp(), Color.parseColor(if (passedStep) "#A7F3D0" else "#FED7AA"))
                }
                setPadding(12.dp(), 10.dp(), 12.dp(), 10.dp())
            }
            val bannerLp = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12.dp() }
            layout.addView(stepBanner, bannerLp)
        }

        // 1. ĐƯA NHẬN XÉT GIA SƯ AI LÊN ĐẦU (DỄ HIỂU NHẤT CHO NGƯỜI HỌC)
        val pedFeedback = aiReport.pedagogicalFeedback
        if (!pedFeedback.isNullOrBlank()) {
            val fbBox = TextView(this).apply {
                text = "🎓 Nhận xét từ gia sư AI: $pedFeedback"
                textSize = 14f
                setTextColor(Color.parseColor("#065F46"))
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(Color.parseColor("#ECFDF5"))
                    cornerRadius = 14.dp().toFloat()
                }
                setPadding(14.dp(), 12.dp(), 14.dp(), 12.dp())
            }
            val fbParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 10.dp() }
            layout.addView(fbBox, fbParams)
        }

        // 2. GỢI Ý HÀNH ĐỘNG (WHAT-IF)
        val cfSummary = aiReport.counterfactual?.summaryVi
        if (!cfSummary.isNullOrBlank()) {
            val cfBox = TextView(this).apply {
                text = "💡 Gợi ý cải thiện ngay: $cfSummary"
                textSize = 13.5f
                setTextColor(Color.parseColor("#1E3A8A"))
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(Color.parseColor("#EFF6FF"))
                    cornerRadius = 14.dp().toFloat()
                }
                setPadding(14.dp(), 12.dp(), 14.dp(), 12.dp())
            }
            val cfParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12.dp() }
            layout.addView(cfBox, cfParams)
        }

        // 3. TÓM TẮT ĐIỂM MẠNH & ĐIỂM CẦN CẢI THIỆN BẰNG NGÔN NGỮ TỰ NHIÊN
        val detailedShap = aiReport.xaiExplanation.detailedShap
        if (!detailedShap.isNullOrEmpty()) {
            val summaryTitle = TextView(this).apply {
                text = "✨ Tóm tắt thói quen làm bài của bạn"
                textSize = 14.5f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(Color.parseColor("#0F172A"))
                setPadding(0, 2.dp(), 0, 6.dp())
            }
            layout.addView(summaryTitle)

            detailedShap.take(4).forEach { item ->
                val isPositive = item.shapScore >= 0.0
                val (friendlyLabel, impactBadge) = friendlyShapSummary(item)
                val row = android.widget.LinearLayout(this).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(10.dp(), 8.dp(), 10.dp(), 8.dp())
                    background = android.graphics.drawable.GradientDrawable().apply {
                        setColor(Color.parseColor("#F8FAFC"))
                        cornerRadius = 10.dp().toFloat()
                    }
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = 6.dp() }
                }
                val txt = TextView(this).apply {
                    text = "${if (isPositive) "✅" else "⚠️"} $friendlyLabel"
                    textSize = 13f
                    setTextColor(Color.parseColor("#334155"))
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                    )
                }
                val badge = TextView(this).apply {
                    text = impactBadge
                    textSize = 11.5f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(Color.parseColor(if (isPositive) "#047857" else "#B91C1C"))
                }
                row.addView(txt)
                row.addView(badge)
                layout.addView(row)
            }

            if (wrongWordsList.isNotEmpty()) {
                val wrongTv = TextView(this).apply {
                    text = "📌 Từ vựng cần ôn lại (${wrongWordsList.distinct().size} từ): ${wrongWordsList.distinct().joinToString(", ")}"
                    textSize = 13f
                    setTextColor(Color.parseColor("#B91C1C"))
                    setPadding(4.dp(), 4.dp(), 4.dp(), 8.dp())
                }
                layout.addView(wrongTv)
            }

            // 4. MỤC THU GỌN BIỂU ĐỒ KỸ THUẬT SHAP (GIỮ NGUYÊN CHỈ SỐ ĐỂ DEMO/BẢO VỆ ĐỒ ÁN)
            val shapDetailsContainer = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                visibility = View.GONE
                setPadding(10.dp(), 10.dp(), 10.dp(), 8.dp())
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(Color.parseColor("#F8FAFC"))
                    cornerRadius = 12.dp().toFloat()
                    setStroke(1.dp(), Color.parseColor("#E2E8F0"))
                }
            }

            val toggleShapBtn = TextView(this).apply {
                text = "🔬 Xem phân tích kỹ thuật SHAP (${aiReport.predictedAccuracy}% dự kiến) ▾"
                textSize = 13f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setTextColor(Color.parseColor("#2563EB"))
                gravity = android.view.Gravity.CENTER
                setPadding(8.dp(), 10.dp(), 8.dp(), 10.dp())
                setOnClickListener {
                    if (shapDetailsContainer.visibility == View.GONE) {
                        shapDetailsContainer.visibility = View.VISIBLE
                        text = "🔬 Ẩn phân tích kỹ thuật SHAP ▴"
                    } else {
                        shapDetailsContainer.visibility = View.GONE
                        text = "🔬 Xem phân tích kỹ thuật SHAP (${aiReport.predictedAccuracy}% dự kiến) ▾"
                    }
                }
            }
            layout.addView(toggleShapBtn)

            val maxAbsShap = detailedShap.maxOfOrNull { kotlin.math.abs(it.shapScore) }
                ?.coerceAtLeast(0.05) ?: 1.0

            detailedShap.take(5).forEach { item ->
                val isPositive = item.shapScore >= 0.0
                val barColor = if (isPositive) Color.parseColor("#16A34A") else Color.parseColor("#DC2626")

                val rowContainer = android.widget.LinearLayout(this).apply {
                    orientation = android.widget.LinearLayout.VERTICAL
                    setPadding(0, 3.dp(), 0, 6.dp())
                }
                val headerRow = android.widget.LinearLayout(this).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL
                }
                val labelTv = TextView(this).apply {
                    text = item.viDescription
                    textSize = 12.5f
                    setTextColor(Color.parseColor("#334155"))
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                    )
                }
                val scoreTv = TextView(this).apply {
                    val sign = if (isPositive) "+" else ""
                    text = "$sign${String.format(Locale.US, "%.2f", item.shapScore)}"
                    textSize = 12.5f
                    setTypeface(null, android.graphics.Typeface.BOLD)
                    setTextColor(barColor)
                }
                headerRow.addView(labelTv)
                headerRow.addView(scoreTv)
                rowContainer.addView(headerRow)

                val barTrack = android.widget.LinearLayout(this).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL
                    setBackgroundColor(Color.parseColor("#E2E8F0"))
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 7.dp()
                    ).apply { setMargins(0, 4.dp(), 0, 0) }
                }
                val ratio = (kotlin.math.abs(item.shapScore) / maxAbsShap).toFloat().coerceIn(0.06f, 1.0f)
                barTrack.addView(View(this).apply {
                    setBackgroundColor(barColor)
                    layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.MATCH_PARENT, ratio)
                })
                barTrack.addView(View(this).apply {
                    setBackgroundColor(Color.TRANSPARENT)
                    layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.MATCH_PARENT, (1.0f - ratio).coerceAtLeast(0f))
                })
                rowContainer.addView(barTrack)
                shapDetailsContainer.addView(rowContainer)
            }
            layout.addView(shapDetailsContainer)
        }

        val btnClose = Button(this).apply {
            text = "NHẬN +$finalXp XP & XEM TỔNG KẾT"
            setBackgroundColor(Color.parseColor("#2563EB"))
            setTextColor(Color.WHITE)
            setPadding(0, 12.dp(), 0, 12.dp())
            setOnClickListener {
                bottomSheet.dismiss()
                val fb = listOfNotNull(aiReport.pedagogicalFeedback, aiReport.counterfactual?.summaryVi)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                openQuizSummary(finalXp, fb)
            }
        }
        val btnParams = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 14.dp(), 0, 0) }

        layout.addView(btnClose, btnParams)
        scrollView.addView(layout)

        bottomSheet.setContentView(scrollView)
        bottomSheet.setCancelable(false)
        bottomSheet.show()
    }

    private fun openQuizSummary(finalXp: Int, xaiFeedback: String = "") {
        if (isFinishing || isDestroyed) return
        val summaryIntent = android.content.Intent(this, QuizSummaryActivity::class.java).apply {
            putExtra("SCORE", score)
            putExtra("TOTAL", quizList.size)
            putExtra("EARNED_XP", finalXp)
            putExtra("STEP_TITLE", currentStepTitle.ifBlank { "Bài luyện tập Quiz" })
            putExtra("XAI_FEEDBACK", xaiFeedback)
            putStringArrayListExtra("WRONG_WORDS", ArrayList(wrongWordsList.distinct()))
        }
        startActivity(summaryIntent)
        finish()
    }

    private fun showBasicResultDialog(finalXp: Int) {
        openQuizSummary(finalXp)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            ttsEngine?.setLanguage(Locale.US)
            isTtsReady = true
            pendingSpeechText?.let {
                speakOut(it)
                pendingSpeechText = null
            }
        }
    }

    private fun speakOut(text: String) {
        if (text.isEmpty()) return
        if (isTtsReady) {
            ttsEngine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
        } else {
            pendingSpeechText = text
        }
    }

    private fun confirmExitQuiz() {
        if (isSubmitted || quizList.isEmpty()) {
            finish()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Tạm dừng bài học?")
            .setMessage("Bạn đang làm đến câu ${currentQuestionIndex + 1}/${quizList.size}. Tiến độ của bạn sẽ được tự động lưu lại để lần sau học tiếp.")
            .setPositiveButton("Ở lại học tiếp", null)
            .setNegativeButton("Tạm thoát") { _, _ -> finish() }
            .show()
    }

    override fun onDestroy() {
        countDownTimer?.cancel()
        ttsEngine?.stop()
        ttsEngine?.shutdown()
        super.onDestroy()
    }
}