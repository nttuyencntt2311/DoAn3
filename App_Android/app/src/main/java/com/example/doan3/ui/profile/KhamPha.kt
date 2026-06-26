package com.example.doan3.ui.profile

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.card.MaterialCardView
import com.example.doan3.R
import com.example.doan3.MainActivity



class KhamPhaActivity : AppCompatActivity() {

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.khampha)

        // 1. Xử lý tràn viền (Đảm bảo file khampha.xml có id="main")
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 2. Xử lý chuyển trang Trang Chủ (Tạm thời đang link về Đăng nhập)
        val btnTrangChu = findViewById<LinearLayout>(R.id.btnTrangChu)
        btnTrangChu.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)

        }


        // ======================================================
        // THÊM MỚI: Xử lý chuyển sang trang Hồ Sơ
        // ======================================================
        val btnHoSo = findViewById<LinearLayout>(R.id.btnHoSo)
        btnHoSo.setOnClickListener {
            val intent = Intent(this, hoso::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0) // Tắt hiệu ứng trượt để giống chuyển Tab
        }

        // 3. Xử lý vuốt thẻ từ vựng
        val cardTuVung = findViewById<MaterialCardView>(R.id.cardTuVung)
        var xDown = 0f

        cardTuVung.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    xDown = event.x
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val xUp = event.x
                    if (xUp - xDown > 200) {
                        Toast.makeText(this, "Đã thuộc!", Toast.LENGTH_SHORT).show()
                        loadNextWord()
                    }
                    v.performClick()
                    true
                }
                else -> false
            }
        }

        // 4. TẠO HIỆU ỨNG NHẤP NHÁY CHO NÚT
        val btnHanhDong = findViewById<MaterialCardView>(R.id.btnHanhDongVuot)
        val nhapNhay = android.animation.ObjectAnimator.ofFloat(btnHanhDong, "alpha", 1f, 0.6f, 1f)
        nhapNhay.duration = 1500 // Tốc độ nhịp thở (1.5 giây)
        nhapNhay.repeatCount = android.animation.ObjectAnimator.INFINITE // Lặp lại vô hạn
        nhapNhay.start()

        // 5. Xử lý bấm vào ô Quiz để chuyển sang màn hình làm bài
        val cardQuizIelts = findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardQuizIelts)
        cardQuizIelts.setOnClickListener {
            val intent = Intent(this, IeltsQuizActivity::class.java)
            startActivity(intent)
        }
    } // Đây là dấu đóng ngoặc của hàm onCreate

    private fun loadNextWord() {
        // Logic đổi từ vựng mới sẽ viết tại đây
    }
}