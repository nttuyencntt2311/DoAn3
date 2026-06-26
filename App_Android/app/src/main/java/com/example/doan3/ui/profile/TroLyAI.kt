package com.example.doan3.ui.profile

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.doan3.R
import com.google.android.material.card.MaterialCardView
import com.example.doan3.ui.roadmap.RoadmapFragment
import com.example.doan3.ui.vocabulary.WordDetailFragment
import com.example.doan3.MainActivity

class TroLyAIActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.troly_ai)

        setupBottomNavigation()
        setupAICardsNavigation() // Kích hoạt các nút bấm trợ lý AI
    }

    private fun setupAICardsNavigation() {
        // Ánh xạ các CardView từ XML
        val cardGiaiThichTuVung = findViewById<MaterialCardView>(R.id.cardGiaiThichTuVung)
        val cardTaoQuiz = findViewById<MaterialCardView>(R.id.cardTaoQuiz)
        val cardFlashcard = findViewById<MaterialCardView>(R.id.cardFlashcard) // Thêm mới cardFlashcard
        val cardLoTrinhHoc = findViewById<MaterialCardView>(R.id.cardLoTrinhHoc)

// 1. Chuyển sang VocabularyFragment trực tiếp
        cardGiaiThichTuVung?.setOnClickListener {
            supportFragmentManager.beginTransaction()
                // Dùng android.R.id.content để phủ Fragment lên toàn bộ màn hình hiện tại
                .replace(android.R.id.content, WordDetailFragment())
                .addToBackStack(null) // Lưu lại lịch sử để bấm nút Back điện thoại quay về được
                .commit()
        }
        // 2. Tạo Quiz -> Chuyển sang TaoQuizActivity
        cardTaoQuiz?.setOnClickListener {
            startActivity(Intent(this, TaoQuizActivity::class.java))
        }

        // 3. Flashcard -> Chuyển sang KhamPhaActivity (Nơi có logic vuốt thẻ từ vựng)
        cardFlashcard?.setOnClickListener {
            startActivity(Intent(this, KhamPhaActivity::class.java))
        }

// 3. Chuyển sang RoadmapFragment trực tiếp
        cardLoTrinhHoc?.setOnClickListener {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, RoadmapFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun setupBottomNavigation() {
        val navTrangChu = findViewById<LinearLayout>(R.id.btnTrangChu)
        val navKhamPha = findViewById<LinearLayout>(R.id.btnKhamPha)
        val navTroLyAI = findViewById<LinearLayout>(R.id.btnTroLyAI)
        val navHoSo = findViewById<LinearLayout>(R.id.btnHoSo)
        val navTaiKhoan = findViewById<LinearLayout>(R.id.btnTaiKhoan)

// 2. Các nút điều hướng Bottom Nav
        val btnTrangChu = findViewById<LinearLayout>(R.id.btnTrangChu)
        btnTrangChu.setOnClickListener {
            // ⚠️ HÃY THAY TÊN 'MainActivity' THÀNH TÊN ACTIVITY TRANG CHỦ THẬT CỦA BẠN
            // Ví dụ: TrangChuActivity, HomeActivity, hoặc DashboardActivity...
            val intent = Intent(this, MainActivity::class.java)

            // Cờ này giúp kéo màn hình Trang Chủ đã có sẵn dưới Stack lên trước, không tạo mới trùng lặp
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT

            startActivity(intent)

            // Tắt hiệu ứng trượt màn hình để giống chuyển tab hơn
            overridePendingTransition(0, 0)

            // 🚫 KHÔNG DÙNG finish() ở đây để giữ lại trạng thái của tab Khám phá khi quay lại
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