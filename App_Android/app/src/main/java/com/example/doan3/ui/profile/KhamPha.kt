package com.example.doan3.ui.profile

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.View
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

// Thêm TextToSpeech.OnInitListener vào đây
class KhamPhaActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var isFront = true

    // --- CÁC BIẾN LOGIC DỮ LIỆU ---
    private var studyList: List<Vocabulary> = emptyList()
    private var currentIndex = 0

    // Khai báo bộ đọc phát âm
    private lateinit var tts: TextToSpeech

    // Khai báo views
    private lateinit var tvWord: TextView
    private lateinit var tvPronunciation: TextView
    private lateinit var tvMeaning: TextView
    private lateinit var cardTuVung: MaterialCardView
    private lateinit var btnSpeaker: ImageView

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.khampha)

        // Khởi tạo bộ đọc phát âm
        tts = TextToSpeech(this, this)

        // Ánh xạ View
        tvWord = findViewById(R.id.tvWord)
        tvPronunciation = findViewById(R.id.tvPronunciation)
        tvMeaning = findViewById(R.id.tvMeaning)
        cardTuVung = findViewById(R.id.cardTuVung)
        btnSpeaker = findViewById(R.id.btnSpeaker)

        setupWindowInsets()
        setupBottomNav()
        setupFlashcardAnimation()
        setupActionButtons()
        setupBreathingEffect()
        setupAudioButton()

        // BẮT ĐẦU: Gọi API lấy dữ liệu học
        fetchStudyWords()
    }

    // Thiết lập ngôn ngữ đọc là Tiếng Anh
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
        }
    }

    private fun setupAudioButton() {
        btnSpeaker.setOnClickListener {
            if (currentIndex < studyList.size) {
                val wordToSpeak = studyList[currentIndex].word
                // Đọc phát âm từ vựng hiện tại
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
                    studyList = allWords.filter { it.proficiency < 80 }

                    if (studyList.isNotEmpty()) {
                        currentIndex = 0
                        loadWordToUI()
                    } else {
                        tvWord.text = "Hoàn thành!"
                        tvPronunciation.text = ""
                        tvMeaning.text = "Bạn đã thuộc hết từ vựng."
                        Toast.makeText(this@KhamPhaActivity, "Không còn từ mới để học hôm nay!", Toast.LENGTH_LONG).show()
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
            tvWord.text = "Xong!"
            tvPronunciation.text = ""
            tvMeaning.text = "Bạn đã học xong bài hôm nay."
            Toast.makeText(this, "Tuyệt vời! Bạn đã hoàn thành phiên học.", Toast.LENGTH_LONG).show()
        }
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
                    // LOGIC MỚI CHO NÚT AGAIN NẰM Ở ĐÂY:
                    if (scoreType == "AGAIN") {
                        // Nhét từ hiện tại vào cuối danh sách để nó lặp lại sau cùng
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

    private fun setupBottomNav() {
        findViewById<LinearLayout?>(R.id.btnTrangChu)?.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            })
            overridePendingTransition(0, 0)
        }
    }

    // Tắt bộ đọc khi thoát khỏi màn hình để giải phóng bộ nhớ
    override fun onDestroy() {
        if (this::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}