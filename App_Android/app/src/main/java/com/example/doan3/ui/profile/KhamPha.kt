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

    // --- CÁC BIẾN LOGIC DỮ LIỆU ---
    private var studyList: List<Vocabulary> = emptyList()
    private var currentIndex = 0
    private var currentCategory: String = "Tổng hợp"
    private val BATCH_SIZE = 10

    // --- MỚI THÊM: CÁC BIẾN ĐẾM THỜI GIAN ---
    private var secondsElapsed = 0
    private var isTimerRunning = false
    private val timerHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isTimerRunning) {
                secondsElapsed++
                updateTimerUI()
                timerHandler.postDelayed(this, 1000) // Lặp lại sau mỗi 1 giây
            }
        }
    }

    private lateinit var tts: TextToSpeech

    // Khai báo views
    private lateinit var tvWord: TextView
    private lateinit var tvPronunciation: TextView
    private lateinit var tvMeaning: TextView
    private lateinit var cardTuVung: MaterialCardView
    private lateinit var btnSpeaker: ImageView
    private lateinit var tagIelts: MaterialCardView

    // Ánh xạ đồng hồ
    private lateinit var tvTimer: TextView

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.khampha)

        tts = TextToSpeech(this, this)

        // Ánh xạ View
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

        setupWindowInsets()
        setupBottomNav() // Gọi hàm cài đặt Bottom Nav (bao gồm việc đổi màu tab)
        setupFlashcardAnimation()
        setupActionButtons()
        setupBreathingEffect()
        setupAudioButton()

        fetchStudyWords()
    }

    // ====================================================================
    // CÁC HÀM XỬ LÝ ĐỒNG HỒ
    // ====================================================================
    private fun updateTimerUI() {
        val minutes = secondsElapsed / 60
        val seconds = secondsElapsed % 60
        val timeString = String.format("%02d:%02d", minutes, seconds)
        tvTimer.text = timeString
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
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
        }
    }

    private fun setupAudioButton() {
        btnSpeaker.setOnClickListener {
            if (currentIndex < studyList.size) {
                val wordToSpeak = studyList[currentIndex].word
                tts.speak(wordToSpeak, TextToSpeech.QUEUE_FLUSH, null, "")
            }
        }
    }

    private fun fetchStudyWords() {
        val apiService = RetrofitClient.getInstance(this)

        apiService.getAllVocabularies().enqueue(object : Callback<List<Vocabulary>> {
            override fun onResponse(call: Call<List<Vocabulary>>, response: Response<List<Vocabulary>>) {
                if (response.isSuccessful) {
                    val allWords = response.body() ?: emptyList()

                    studyList = allWords
                        .filter { it.proficiency < 80 && it.category == currentCategory }
                        .take(BATCH_SIZE)

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
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_session_complete)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.setCancelable(false)

        val tvMessage = dialog.findViewById<TextView>(R.id.tvDialogMessage)
        tvMessage.text = "Bạn đã hoàn thành $BATCH_SIZE từ vựng của phiên này. Bạn muốn tiếp tục nạp thêm kiến thức hay nghỉ ngơi một chút?"

        val btnHocTiep = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnHocTiep)
        btnHocTiep.setOnClickListener {
            dialog.dismiss()
            fetchStudyWords()
        }

        val btnNghiNgoi = dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnNghiNgoi)
        btnNghiNgoi.setOnClickListener {
            dialog.dismiss()

            val minutesLearned = secondsElapsed / 60
            // Ghi chú: Sau này em sẽ gọi API đẩy biến minutesLearned này lên MongoDB tại đây

            finish()
        }

        dialog.show()
    }

    private fun handleScore(scoreType: String) {
        if (currentIndex >= studyList.size) return

        val currentWord = studyList[currentIndex]
        var newProficiency = currentWord.proficiency

        when (scoreType) {
            "AGAIN" -> newProficiency = 0
            "GOOD" -> newProficiency = (newProficiency + 30).coerceAtMost(90)
            "EASY" -> newProficiency = 100
        }

        val apiService = RetrofitClient.getInstance(this)
        val request = mapOf("proficiency" to newProficiency)

        apiService.updateProficiency(currentWord.id, request).enqueue(object : Callback<Any> {
            override fun onResponse(call: Call<Any>, response: Response<Any>) {
                if (response.isSuccessful) {
                    if (scoreType == "AGAIN") {
                        studyList = studyList + currentWord
                    }
                    currentIndex++
                    loadWordToUI()
                } else {
                    Toast.makeText(this@KhamPhaActivity, "Lỗi server: Code ${response.code()}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<Any>, t: Throwable) {
                Toast.makeText(this@KhamPhaActivity, "Lỗi mạng", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupActionButtons() {
        val btnAgain = findViewById<MaterialCardView>(R.id.btnAgain)
        val btnGood = findViewById<MaterialCardView>(R.id.btnGood)
        val btnEasy = findViewById<MaterialCardView>(R.id.btnEasy)

        btnAgain.setOnClickListener { handleScore("AGAIN") }
        btnGood.setOnClickListener { handleScore("GOOD") }
        btnEasy.setOnClickListener { handleScore("EASY") }
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

    // ====================================================================
    // MỚI: XỬ LÝ CẬP NHẬT MÀU SẮC CHO BOTTOM NAVIGATION
    // ====================================================================
    private fun setupBottomNav() {
        findViewById<LinearLayout?>(R.id.btnTrangChu)?.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            })
            overridePendingTransition(0, 0)
        }

        // Gọi hàm tô màu đúng tab khi Activity được khởi tạo
        updateActiveTab()
    }

    private fun updateActiveTab() {
        val btnTrangChu = findViewById<LinearLayout>(R.id.btnTrangChu)
        val btnKhamPha = findViewById<LinearLayout>(R.id.btnKhamPha)

        val activeColor = Color.parseColor("#0056D2")
        val inactiveColor = Color.parseColor("#777777")

        // 1. Tẩy nền xanh và chuyển nút Trang Chủ thành màu xám
        btnTrangChu?.let {
            it.background = null
            (it.getChildAt(0) as? ImageView)?.setColorFilter(inactiveColor)
            (it.getChildAt(1) as? TextView)?.setTextColor(inactiveColor)
        }

        // 2. Bôi nền xanh và chuyển màu icon/text nút Khám Phá
        btnKhamPha?.let {
            it.setBackgroundResource(R.drawable.bg_tab_active)
            (it.getChildAt(0) as? ImageView)?.setColorFilter(activeColor)
            (it.getChildAt(1) as? TextView)?.setTextColor(activeColor)
        }
    }

    override fun onDestroy() {
        stopTimer()

        val sharedPref = getSharedPreferences("StudyData", MODE_PRIVATE)
        val savedSeconds = sharedPref.getInt("totalSeconds", 0)
        sharedPref.edit().putInt("totalSeconds", savedSeconds + secondsElapsed).apply()

        if (this::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}