package com.example.doan3.ui.profile

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.doan3.MainActivity
import com.example.doan3.R
import com.example.doan3.ui.vocabulary.Vocabulary
import com.google.android.material.card.MaterialCardView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class KhamPhaActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var isFront = true
    private var isSubmittingScore = false

    private var studyList: List<Vocabulary> = emptyList()
    private var currentIndex = 0
    private var currentCategory: String = "Tổng hợp"
    private var predictiveWords: ArrayList<String>? = null
    private val BATCH_SIZE = 10

    private var secondsElapsed = 0
    private var isTimerRunning = false
    private val timerHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isTimerRunning) {
                secondsElapsed++
                updateTimerUI()
                timerHandler.postDelayed(this, 1000)
            }
        }
    }

    private lateinit var tts: TextToSpeech

    private lateinit var tvWord: TextView
    private lateinit var tvPronunciation: TextView
    private lateinit var tvMeaning: TextView
    private lateinit var cardTuVung: MaterialCardView
    private lateinit var btnSpeaker: ImageView
    private lateinit var tagIelts: MaterialCardView
    private lateinit var tvTimer: TextView

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.khampha)

        tts = TextToSpeech(this, this)

        tvWord = findViewById(R.id.tvWord)
        tvPronunciation = findViewById(R.id.tvPronunciation)
        tvMeaning = findViewById(R.id.tvMeaning)
        cardTuVung = findViewById(R.id.cardTuVung)
        btnSpeaker = findViewById(R.id.btnSpeaker)
        tagIelts = findViewById(R.id.tagIelts)
        tvTimer = findViewById(R.id.tvTimer)

        val categoryFromIntent = intent.getStringExtra("CATEGORY_NAME")
        if (categoryFromIntent != null) {
            currentCategory = categoryFromIntent
            val tvTag = tagIelts.getChildAt(0) as? TextView
            tvTag?.text = currentCategory.uppercase()
        }

        predictiveWords = intent.getStringArrayListExtra("PREDICTIVE_WORDS")

        setupWindowInsets()
        setupBottomNav()
        setupFlashcardAnimation()
        setupActionButtons()
        setupBreathingEffect()
        setupAudioButton()

        fetchStudyWords()
    }

    private fun updateTimerUI() {
        val minutes = secondsElapsed / 60
        val seconds = secondsElapsed % 60
        tvTimer.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    private fun startTimer() {
        if (!isTimerRunning) {
            isTimerRunning = true
            timerHandler.post(timerRunnable)
        }
    }

    private fun stopTimer() {
        isTimerRunning = false
        timerHandler.removeCallbacks(timerRunnable)
    }

    override fun onResume() {
        super.onResume()
        startTimer()
    }

    override fun onPause() {
        super.onPause()
        stopTimer()

        val sharedPref = getSharedPreferences("StudyData", MODE_PRIVATE)
        val savedSeconds = sharedPref.getInt("totalSeconds", 0)
        sharedPref.edit().putInt("totalSeconds", savedSeconds + secondsElapsed).apply()
        secondsElapsed = 0
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
        }
    }

    private fun speakCurrentWord(rate: Float = 1.0f) {
        if (currentIndex < studyList.size) {
            val wordToSpeak = studyList[currentIndex].word
            tts.setSpeechRate(rate)
            tts.speak(wordToSpeak, TextToSpeech.QUEUE_FLUSH, null, "")
        }
    }

    private fun setupAudioButton() {
        btnSpeaker.setOnClickListener { speakCurrentWord(1.0f) }
        findViewById<TextView>(R.id.btnSpeakerSlow)?.setOnClickListener {
            speakCurrentWord(0.7f)
            Toast.makeText(this, "Đang đọc chậm (0.7x)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchStudyWords() {
        val apiService = RetrofitClient.getInstance(this)
        apiService.getAllVocabularies().enqueue(object : Callback<List<Vocabulary>> {
            override fun onResponse(call: Call<List<Vocabulary>>, response: Response<List<Vocabulary>>) {
                if (isFinishing || isDestroyed) return
                if (response.isSuccessful) {
                    val allWords = response.body() ?: emptyList()

                    if (!predictiveWords.isNullOrEmpty()) {
                        val targetSet = predictiveWords!!.map { it.trim().lowercase(Locale.ROOT) }.toSet()
                        studyList = allWords.filter { targetSet.contains(it.word.trim().lowercase(Locale.ROOT)) }
                    } else {
                        studyList = allWords
                            .filter { it.proficiency < 80 && it.category == currentCategory }
                            .take(BATCH_SIZE)
                    }

                    if (studyList.isNotEmpty()) {
                        currentIndex = 0
                        loadWordToUI()
                    } else {
                        tvWord.text = "Hoàn thành!"
                        tvPronunciation.text = ""
                        tvMeaning.text = "Bạn đã học hết từ vựng của chủ đề này."
                        Toast.makeText(this@KhamPhaActivity, "Chủ đề này không còn từ mới!", Toast.LENGTH_LONG).show()
                    }
                }
            }

            override fun onFailure(call: Call<List<Vocabulary>>, t: Throwable) {
                if (isFinishing || isDestroyed) return
                Toast.makeText(this@KhamPhaActivity, "Lỗi kết nối mạng", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadWordToUI() {
        if (currentIndex < studyList.size) {
            val currentWord = studyList[currentIndex]
            tvWord.text = currentWord.word
            tvPronunciation.text = currentWord.pronunciation
            tvMeaning.text = currentWord.meaning
            resetCardToFront()
        } else {
            showSessionCompleteDialog()
        }
    }

    private fun showSessionCompleteDialog() {
        if (isFinishing || isDestroyed) return
        try {
            val dialog = Dialog(this)
            dialog.setContentView(R.layout.dialog_session_complete)
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            dialog.setCancelable(false)

            val uniqueWordCount = studyList.map { it.id }.distinct().size
            val tvMessage = dialog.findViewById<TextView>(R.id.tvDialogMessage)
            tvMessage?.text = "Bạn đã hoàn thành $uniqueWordCount từ vựng của phiên này. Bạn muốn tiếp tục nạp thêm kiến thức hay nghỉ ngơi một chút?"

            val btnHocTiep = dialog.findViewById<View>(R.id.btnHocTiep)
            btnHocTiep?.setOnClickListener {
                dialog.dismiss()
                if (!predictiveWords.isNullOrEmpty()) {
                    // Đã ôn xong danh sách AI dự đoán -> chuyển về học chủ đề thường hoặc thoát
                    predictiveWords = null
                    finish()
                } else {
                    fetchStudyWords()
                }
            }

            val btnNghiNgoi = dialog.findViewById<View>(R.id.btnNghiNgoi)
            btnNghiNgoi?.setOnClickListener {
                dialog.dismiss()
                finish()
            }

            dialog.show()
        } catch (e: Exception) {
            Toast.makeText(this, "Hoàn thành bài học!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun handleScore(scoreType: String) {
        if (isSubmittingScore || currentIndex >= studyList.size) return
        try {
            val currentWord = studyList[currentIndex]
            var newProficiency = currentWord.proficiency

            when (scoreType) {
                "AGAIN" -> newProficiency = 0
                "GOOD" -> newProficiency = (newProficiency + 30).coerceAtMost(90)
                "EASY" -> newProficiency = 100
            }

            val sharedPreferences = getSharedPreferences("DoAn3Prefs", MODE_PRIVATE)
            val currentUserId = sharedPreferences.getString("USER_ID", "") ?: ""
            if (currentUserId.isEmpty()) {
                Toast.makeText(this, "Lỗi: Không tìm thấy ID người dùng!", Toast.LENGTH_LONG).show()
                return
            }

            isSubmittingScore = true
            val request = ProficiencyRequest(proficiency = newProficiency, userId = currentUserId)
            RetrofitClient.getInstance(this).updateProficiency(currentWord.id, request)
                .enqueue(object : Callback<ProficiencyResponse> {
                    override fun onResponse(call: Call<ProficiencyResponse>, response: Response<ProficiencyResponse>) {
                        isSubmittingScore = false
                        if (isFinishing || isDestroyed) return
                        if (response.isSuccessful) {
                            currentIndex++
                            loadWordToUI()
                        } else {
                            Toast.makeText(this@KhamPhaActivity, "Lỗi server: Code ${response.code()}", Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onFailure(call: Call<ProficiencyResponse>, t: Throwable) {
                        isSubmittingScore = false
                        if (isFinishing || isDestroyed) return
                        Toast.makeText(this@KhamPhaActivity, "Lỗi mạng: ${t.message}", Toast.LENGTH_LONG).show()
                    }
                })
        } catch (e: Exception) {
            isSubmittingScore = false
            Toast.makeText(this, "Lỗi code Android: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupActionButtons() {
        findViewById<MaterialCardView>(R.id.btnAgain).setOnClickListener { handleScore("AGAIN") }
        findViewById<MaterialCardView>(R.id.btnGood).setOnClickListener { handleScore("GOOD") }
        findViewById<MaterialCardView>(R.id.btnEasy).setOnClickListener { handleScore("EASY") }
    }

    private fun setupFlashcardAnimation() {
        cardTuVung.setOnClickListener {
            cardTuVung.animate().scaleX(0f).setDuration(150).withEndAction {
                isFront = !isFront
                if (isFront) {
                    tvWord.visibility = View.VISIBLE
                    tvPronunciation.visibility = View.VISIBLE
                    tvMeaning.visibility = View.GONE
                } else {
                    tvWord.visibility = View.GONE
                    tvPronunciation.visibility = View.GONE
                    tvMeaning.visibility = View.VISIBLE
                }
                cardTuVung.animate().scaleX(1f).setDuration(150).start()
            }.start()
        }
    }

    private fun resetCardToFront() {
        isFront = true
        tvWord.visibility = View.VISIBLE
        tvPronunciation.visibility = View.VISIBLE
        tvMeaning.visibility = View.GONE
    }

    private fun setupBreathingEffect() {
        val btnHanhDong = findViewById<MaterialCardView>(R.id.btnHanhDongVuot)
        if (btnHanhDong != null) {
            val nhapNhay = ObjectAnimator.ofFloat(btnHanhDong, "alpha", 1f, 0.6f, 1f)
            nhapNhay.duration = 1500
            nhapNhay.repeatCount = ObjectAnimator.INFINITE
            nhapNhay.start()
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun navigateToMainTab(tabName: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("OPEN_FRAGMENT", tabName)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
    }

    private fun setupBottomNav() {
        findViewById<View>(R.id.btnTrangChu)?.setOnClickListener { navigateToMainTab("HOME") }
        findViewById<View>(R.id.btnKhamPha)?.setOnClickListener { navigateToMainTab("LEARNING") }
        findViewById<View>(R.id.btnTroLyAI)?.setOnClickListener { navigateToMainTab("AI_ASSISTANT") }
        findViewById<View>(R.id.btnHoSo)?.setOnClickListener { navigateToMainTab("PROFILE") }
        findViewById<View>(R.id.btnTaiKhoan)?.setOnClickListener { navigateToMainTab("ACCOUNT") }
    }

    override fun onDestroy() {
        stopTimer()
        if (this::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}