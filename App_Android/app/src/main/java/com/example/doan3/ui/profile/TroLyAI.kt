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
import com.example.doan3.ui.ai.ChatMessage


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
        val cardFlashcard = findViewById<MaterialCardView>(R.id.cardFlashcard)
        val cardLoTrinhHoc = findViewById<MaterialCardView>(R.id.cardLoTrinhHoc)
        val cardTroChuyenTrucTiep = findViewById<MaterialCardView>(R.id.cardTroChuyenTrucTiep)

        // 1. Chuyển sang màn hình Chat AI - Chế độ Giải thích từ vựng
        cardGiaiThichTuVung?.setOnClickListener {
            val intent = Intent(this, ChatAIActivity::class.java)
            intent.putExtra("CHAT_TYPE", "GIAI_THICH")
            startActivity(intent)
        }

        // 2. Chuyển sang màn hình Chat AI - Chế độ Tạo bài Quiz học tập
        cardTaoQuiz?.setOnClickListener {
            val intent = Intent(this, ChatAIActivity::class.java)
            intent.putExtra("CHAT_TYPE", "TAO_QUIZ")
            startActivity(intent)
        }

        // 3. Mở tính năng Flashcard (hoặc màn hình Khám phá có sẵn của bạn)
        cardFlashcard?.setOnClickListener {
            val intent = Intent(this, KhamPhaActivity::class.java)
            startActivity(intent)
        }

        // 4. Chuyển sang màn hình Chat AI - Chế độ Thiết lập lộ trình học
        cardLoTrinhHoc?.setOnClickListener {
            val intent = Intent(this, ChatAIActivity::class.java)
            intent.putExtra("CHAT_TYPE", "LO_TRINH")
            startActivity(intent)
        }
        cardTroChuyenTrucTiep?.setOnClickListener {
            val intent = Intent(this, ChatAIActivity::class.java)
            // Bạn có thể truyền một loại chat mới, ví dụ: "TRO_CHUYEN"
            intent.putExtra("CHAT_TYPE", "TRO_CHUYEN")
            startActivity(intent)
        }
    }

    // ... các khai báo biến RecyclerView, Adapter, EditText, Button ...


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