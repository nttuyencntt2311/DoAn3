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

        // 1. Gửi tín hiệu mở Vocabulary
        cardGiaiThichTuVung?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("OPEN_FRAGMENT", "VOCABULARY")
            startActivity(intent)
        }

        // 2. Chuyển sang màn hình Tạo Quiz (Activity)
        cardTaoQuiz?.setOnClickListener {
            val intent = Intent(this, TaoQuizActivity::class.java)
            startActivity(intent)
        }

        // 3. Gửi tín hiệu mở Learning
        cardFlashcard?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("OPEN_FRAGMENT", "LEARNING")
            startActivity(intent)
        }

        // 4. Gửi tín hiệu mở Roadmap
        cardLoTrinhHoc?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("OPEN_FRAGMENT", "ROADMAP")
            startActivity(intent)
        }

        // 5. Chat AI
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