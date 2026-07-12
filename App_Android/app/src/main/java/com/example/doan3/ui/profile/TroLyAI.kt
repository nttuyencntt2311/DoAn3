package com.example.doan3.ui.profile

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.doan3.R
import com.google.android.material.card.MaterialCardView
import com.example.doan3.MainActivity
import com.example.doan3.ui.ai.ChatAIActivity

class TroLyAIActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.troly_ai)

        setupBottomNavigation()
        setupAICardsNavigation()
    }

    private fun setupAICardsNavigation() {
        val cardGiaiThichTuVung = findViewById<MaterialCardView>(R.id.cardGiaiThichTuVung)
        val cardTaoQuiz = findViewById<MaterialCardView>(R.id.cardTaoQuiz)
        val cardFlashcard = findViewById<MaterialCardView>(R.id.cardFlashcard)
        val cardLoTrinhHoc = findViewById<MaterialCardView>(R.id.cardLoTrinhHoc)
        val cardTroChuyenTrucTiep = findViewById<MaterialCardView>(R.id.cardTroChuyenTrucTiep)

        // 1. Chuyển sang màn hình Giải thích từ vựng (Màn hình riêng)
        cardGiaiThichTuVung?.setOnClickListener {
            // Khi nào có Activity riêng, bạn bỏ comment 2 dòng dưới và xóa Toast đi nhé
            // val intent = Intent(this, GiaiThichTuVungActivity::class.java)
            // startActivity(intent)
            Toast.makeText(this, "Đang mở màn hình Giải thích từ vựng...", Toast.LENGTH_SHORT).show()
        }

        // 2. Chuyển sang màn hình Tạo Quiz (Màn hình riêng)
        cardTaoQuiz?.setOnClickListener {
            // val intent = Intent(this, TaoQuizActivity::class.java)
            // startActivity(intent)
            Toast.makeText(this, "Đang mở màn hình Tạo Quiz...", Toast.LENGTH_SHORT).show()
        }

        // 3. Mở tính năng Flashcard
        cardFlashcard?.setOnClickListener {
            val intent = Intent(this, KhamPhaActivity::class.java)
            startActivity(intent)
        }

        // 4. Chuyển sang màn hình Lộ trình học (Màn hình riêng)
        cardLoTrinhHoc?.setOnClickListener {
            // val intent = Intent(this, LoTrinhHocActivity::class.java)
            // startActivity(intent)
            Toast.makeText(this, "Đang mở màn hình Lộ trình học...", Toast.LENGTH_SHORT).show()
        }

        // 5. DUY NHẤT nút này sẽ mở khung Chat AI của bạn
        cardTroChuyenTrucTiep?.setOnClickListener {
            val intent = Intent(this, ChatAIActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupBottomNavigation() {
        val navTrangChu = findViewById<LinearLayout>(R.id.btnTrangChu)
        val navKhamPha = findViewById<LinearLayout>(R.id.btnKhamPha)
        val navTroLyAI = findViewById<LinearLayout>(R.id.btnTroLyAI)
        val navHoSo = findViewById<LinearLayout>(R.id.btnHoSo)
        val navTaiKhoan = findViewById<LinearLayout>(R.id.btnTaiKhoan)

        val btnTrangChu = findViewById<LinearLayout>(R.id.btnTrangChu)
        btnTrangChu.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        navKhamPha.setOnClickListener {
            startActivity(Intent(this, KhamPhaActivity::class.java))
            overridePendingTransition(0, 0)
        }

        navTroLyAI.setOnClickListener {
            // Đang ở Trợ lý AI
        }

        navHoSo.setOnClickListener {
            startActivity(Intent(this, hoso::class.java))
            overridePendingTransition(0, 0)
        }

        navTaiKhoan.setOnClickListener {
            startActivity(Intent(this, TaiKhoanActivity::class.java))
            overridePendingTransition(0, 0)
        }
    }
}