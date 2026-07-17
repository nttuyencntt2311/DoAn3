package com.example.doan3.ui.profile

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.doan3.R
import com.example.doan3.MainActivity
import com.example.doan3.ui.ai.ChatAIActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.example.doan3.databinding.DialogCreateRoadmapBinding

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
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("OPEN_FRAGMENT", "VOCABULARY")
            }
            startActivity(intent)
        }

        // 2. Chuyển sang màn hình Tạo Quiz (Activity)
        cardTaoQuiz?.setOnClickListener {
            val intent = Intent(this, TaoQuizActivity::class.java)
            startActivity(intent)
        }

        // 3. Gửi tín hiệu mở Learning
        cardFlashcard?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("OPEN_FRAGMENT", "LEARNING")
            }
            startActivity(intent)
        }

        // 4. Mở Form tạo lộ trình (ĐÃ SỬA: Không chuyển trang trực tiếp mà hiện BottomSheet trước)
        cardLoTrinhHoc?.setOnClickListener {
            showRoadmapFormDialog()
        }

        // 5. Chat AI
        cardTroChuyenTrucTiep?.setOnClickListener {
            val intent = Intent(this, ChatAIActivity::class.java)
            startActivity(intent)
        }
    }

    // Hàm hiển thị Form đẹp mắt và xử lý sự kiện đóng gói dữ liệu
    private fun showRoadmapFormDialog() {
        val dialog = BottomSheetDialog(this)
        val dialogBinding = DialogCreateRoadmapBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        // Khởi tạo data mẫu cho các Spinner
        dialogBinding.spinnerGoal.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf("Giao tiếp công việc", "Thi IELTS/TOEIC", "Du lịch", "Lấy lại căn bản")
        )
        dialogBinding.spinnerLevel.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf("Mất gốc", "Cơ bản", "Trung cấp")
        )
        dialogBinding.spinnerTime.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf("15 phút/ngày", "30 phút/ngày", "1 tiếng/ngày")
        )

        dialogBinding.btnGenerate.setOnClickListener {
            val goal = dialogBinding.spinnerGoal.selectedItem.toString()
            val level = dialogBinding.spinnerLevel.selectedItem.toString()
            val time = dialogBinding.spinnerTime.selectedItem.toString()
            val expectation = dialogBinding.edtExpectation.text.toString().trim() // Lấy mong muốn của user

            dialog.dismiss()

            // 🚀 CHUYỂN SANG MAINACTIVITY (Chứa RoadmapFragment) VÀ ĐÓNG GÓI DỮ LIỆU
            val intent = Intent(this@TroLyAIActivity, MainActivity::class.java).apply {
                putExtra("OPEN_FRAGMENT", "ROADMAP")
                putExtra("GENERATE_ROADMAP", true)
                putExtra("GOAL", goal)
                putExtra("LEVEL", level)
                putExtra("TIME", time)
                putExtra("EXPECTATION", expectation)
                // Giúp quay lại MainActivity mượt mà không bị tạo trùng màn hình
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            startActivity(intent)
        }

        dialog.show()
    }

    private fun setupBottomNavigation() {
        val navKhamPha = findViewById<LinearLayout>(R.id.btnKhamPha)
        val navTroLyAI = findViewById<LinearLayout>(R.id.btnTroLyAI)
        val navHoSo = findViewById<LinearLayout>(R.id.btnHoSo)
        val navTaiKhoan = findViewById<LinearLayout>(R.id.btnTaiKhoan)

        val btnTrangChu = findViewById<LinearLayout>(R.id.btnTrangChu)
        btnTrangChu.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
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