package com.example.doan3.ui.roadmap

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.doan3.R
import com.example.doan3.databinding.IeltsQuizBinding
import com.example.doan3.ui.profile.RetrofitClient
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctAnswer: String,
    val pronounceText: String,
    val hint: String
)

class IeltsQuizActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: IeltsQuizBinding
    private var ttsEngine: TextToSpeech? = null

    private var quizList = listOf<QuizQuestion>()
    private var currentQuestionIndex = 0
    private var score = 0

    private var selectedOptionIndex: Int = -1
    private var isAnswerChecked: Boolean = false

    // Tên file lưu trữ tiến độ
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

        // 🚀 KIỂM TRA XEM CÓ TIẾN ĐỘ CŨ KHÔNG
        if (loadProgress(currentStepTitle)) {
            Toast.makeText(this, "Đang tiếp tục bài học dang dở...", Toast.LENGTH_SHORT).show()
            displayQuestion()
        } else {
            // Nếu không có tiến độ cũ thì mới gọi AI tải mới
            loadQuizFromAi(currentStepTitle, stepDesc, questionCount)
        }
    }

    // 🚀 LƯU TIẾN ĐỘ KHI NGƯỜI DÙNG THOÁT MÀN HÌNH (Bấm Home, Back, Đa nhiệm...)
    override fun onPause() {
        super.onPause()
        saveProgress()
    }

    // --- CÁC HÀM XỬ LÝ LƯU TRỮ (SHARED PREFERENCES) ---
    private fun saveProgress() {
        // Chỉ lưu khi đã có câu hỏi và chưa hoàn thành bài
        if (quizList.isEmpty() || currentQuestionIndex >= quizList.size) return

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        val gson = Gson()
        val jsonList = gson.toJson(quizList)

        editor.putString("${currentStepTitle}_list", jsonList)
        editor.putInt("${currentStepTitle}_index", currentQuestionIndex)
        editor.putInt("${currentStepTitle}_score", score)
        editor.apply()
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
            return true // Trả về true nếu nạp dữ liệu cũ thành công
        }
        return false // Trả về false nếu chưa có dữ liệu lưu trước đó
    }

    private fun clearProgress(stepTitle: String) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove("${stepTitle}_list")
            .remove("${stepTitle}_index")
            .remove("${stepTitle}_score")
            .apply()
    }
    // ---------------------------------------------------

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }
        binding.tvSkipQuestion.setOnClickListener { skipQuestion() }

        binding.tvGetHint.setOnClickListener {
            if (quizList.isNotEmpty()) {
                val currentQuestion = quizList[currentQuestionIndex]
                binding.tvExplanation.visibility = View.VISIBLE
                binding.tvExplanation.text = "💡 Gợi ý: ${currentQuestion.hint}"
                binding.tvExplanation.setTextColor(Color.parseColor("#D97706"))
                binding.tvExplanation.setBackgroundColor(Color.parseColor("#FEF3C7"))
            }
        }

        binding.btnOptionA.setOnClickListener { selectOption(0) }
        binding.btnOptionB.setOnClickListener { selectOption(1) }
        binding.btnOptionC.setOnClickListener { selectOption(2) }
        binding.btnOptionD.setOnClickListener { selectOption(3) }

        binding.btnNopBai.setOnClickListener {
            checkAnswerAndSubmit()
        }
    }

    private fun skipQuestion() {
        if (quizList.isNotEmpty() && currentQuestionIndex < quizList.size) {
            val currentQuiz = quizList[currentQuestionIndex]
            val mutableList = quizList.toMutableList()
            mutableList.add(currentQuiz)
            quizList = mutableList

            Toast.makeText(this, "Đã bỏ qua! Câu hỏi sẽ được lặp lại ở cuối bài.", Toast.LENGTH_SHORT).show()
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

        val selectedBg = Color.parseColor("#F0F7FF")
        val selectedStroke = Color.parseColor("#0056D2")
        val selectedLetterBg = R.drawable.bg_circle_blue
        val selectedTextColor = Color.parseColor("#0056D2")

        val cards = listOf(binding.btnOptionA, binding.btnOptionB, binding.btnOptionC, binding.btnOptionD)
        val letters = listOf(binding.tvLetterA, binding.tvLetterB, binding.tvLetterC, binding.tvLetterD)
        val answers = listOf(binding.tvAnswerA, binding.tvAnswerB, binding.tvAnswerC, binding.tvAnswerD)
        val ticks = listOf(binding.imgTickA, binding.imgTickB, binding.imgTickC, binding.imgTickD)

        cards[index].setCardBackgroundColor(selectedBg)
        cards[index].strokeColor = selectedStroke
        letters[index].setBackgroundResource(selectedLetterBg)
        letters[index].setTextColor(Color.WHITE)
        answers[index].setTextColor(selectedTextColor)
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
                        displayQuestion()
                        binding.btnNopBai.isEnabled = true
                    } else {
                        Log.e("IeltsQuiz", "Lỗi API Backend")
                        Toast.makeText(this@IeltsQuizActivity, "Lỗi Server. Xem Logcat!", Toast.LENGTH_LONG).show()
                        loadMockQuiz()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("IeltsQuiz", "Lỗi tải câu hỏi AI: ${e.message}", e)
                    Toast.makeText(this@IeltsQuizActivity, "Lỗi mạng. Tải bộ mặc định!", Toast.LENGTH_SHORT).show()
                    loadMockQuiz()
                }
            }
        }
    }

    private fun loadMockQuiz() {
        quizList = listOf(
            QuizQuestion(
                question = "The wording of the agreement was so ... that both parties interpreted it differently.",
                options = listOf("Clear", "Uncertain", "Obvious", "Specific"),
                correctAnswer = "Uncertain",
                pronounceText = "Uncertain",
                hint = "Không chắc chắn, mơ hồ"
            )
        )
        currentQuestionIndex = 0
        score = 0
        displayQuestion()
        binding.btnNopBai.isEnabled = true
    }

    private fun displayQuestion() {
        if (quizList.isEmpty() || currentQuestionIndex >= quizList.size) return

        isAnswerChecked = false
        selectedOptionIndex = -1
        binding.btnNopBai.text = "KIỂM TRA"
        binding.tvExplanation.visibility = View.GONE

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

        isAnswerChecked = true
        val currentQuiz = quizList[currentQuestionIndex]
        val userAnswer = currentQuiz.options[selectedOptionIndex]
        val correctIndex = currentQuiz.options.indexOf(currentQuiz.correctAnswer)

        val cards = listOf(binding.btnOptionA, binding.btnOptionB, binding.btnOptionC, binding.btnOptionD)
        val letters = listOf(binding.tvLetterA, binding.tvLetterB, binding.tvLetterC, binding.tvLetterD)
        val answers = listOf(binding.tvAnswerA, binding.tvAnswerB, binding.tvAnswerC, binding.tvAnswerD)

        for (card in cards) card.isClickable = false

        if (userAnswer == currentQuiz.correctAnswer) {
            score++
            cards[selectedOptionIndex].setCardBackgroundColor(Color.parseColor("#F0FDF4"))
            cards[selectedOptionIndex].strokeColor = Color.parseColor("#16A34A")
            letters[selectedOptionIndex].setBackgroundResource(R.drawable.bg_circle_green)
            letters[selectedOptionIndex].setTextColor(Color.WHITE)
            answers[selectedOptionIndex].setTextColor(Color.parseColor("#16A34A"))

            binding.btnNopBai.text = "TUYỆT VỜI! TIẾP TỤC"
            binding.btnNopBai.setBackgroundColor(Color.parseColor("#16A34A"))
        } else {
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
            showResultDialog()
        }
    }

    private fun showResultDialog() {
        val stepId = intent.getStringExtra("STEP_ID") ?: "1"
        val displayXp = 50 + (score * 10)

        clearProgress(currentStepTitle)

        AlertDialog.Builder(this)
            .setTitle("Hoàn Thành Bài Học! 🎉")
            .setMessage("Chúc mừng bạn đã hoàn thành xuất sắc.\nSố câu đúng: $score / ${quizList.size}\nBạn nhận được: +$displayXp XP!")
            .setCancelable(false)
            .setPositiveButton("Tiếp tục lộ trình") { dialog, _ ->
                dialog.dismiss()

                // BẮT ĐẦU ĐOẠN CODE ĐÃ ĐƯỢC CHỈNH SỬA ĐỂ BẮT LỖI
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val api = RetrofitClient.getInstance(applicationContext)
                        val response = api.completeRoadmapStep(mapOf(
                            "stepId" to stepId,
                            "score" to score.toString(),
                            "totalQuestions" to quizList.size.toString()
                        ))

                        withContext(Dispatchers.Main) {
                            // Ép Android phải kiểm tra xem Backend trả về mã gì
                            if (response.isSuccessful) {
                                Toast.makeText(this@IeltsQuizActivity, "🔥 Tiến độ đã lưu! Giữ vững phong độ nhé!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@IeltsQuizActivity, "❌ Lỗi Server: Mã ${response.code()}", Toast.LENGTH_LONG).show()
                            }
                            finish()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@IeltsQuizActivity, "❌ Lỗi mạng/Hệ thống: ${e.message}", Toast.LENGTH_LONG).show()
                            finish()
                        }
                    }
                }
                // KẾT THÚC ĐOẠN SỬA
            }
            .create()
            .show()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            ttsEngine?.setLanguage(Locale.US)
        }
    }

    private fun speakOut(text: String) {
        if (text.isNotEmpty()) {
            ttsEngine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
        }
    }

    override fun onDestroy() {
        if (ttsEngine != null) {
            ttsEngine?.stop()
            ttsEngine?.shutdown()
        }
        super.onDestroy()
    }
}