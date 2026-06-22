package com.example.doan3.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.example.doan3.R


class IeltsQuizActivity : AppCompatActivity() {

    // 1. Khai báo các biến đại diện cho 4 đáp án ở cấp Class
    private lateinit var btnOptionA: MaterialCardView
    private lateinit var btnOptionB: MaterialCardView
    private lateinit var btnOptionC: MaterialCardView
    private lateinit var btnOptionD: MaterialCardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Lưu ý: Đảm bảo tên file layout khớp với tên file XML của bạn
        setContentView(R.layout.ielts_quiz)

        // Nút quay lại
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack?.setOnClickListener {
            finish()
        }

        // 2. Ánh xạ các thẻ đáp án từ XML vào code
        btnOptionA = findViewById(R.id.btnOptionA)
        btnOptionB = findViewById(R.id.btnOptionB)
        btnOptionC = findViewById(R.id.btnOptionC)
        btnOptionD = findViewById(R.id.btnOptionD)

        // 3. Gắn sự kiện click cho từng đáp án
        btnOptionA.setOnClickListener { xuLyChonDapAn(btnOptionA, "A") }
        btnOptionB.setOnClickListener { xuLyChonDapAn(btnOptionB, "B") }
        btnOptionC.setOnClickListener { xuLyChonDapAn(btnOptionC, "C") }
        btnOptionD.setOnClickListener { xuLyChonDapAn(btnOptionD, "D") }
    }

    // --- HÀM LOGIC XỬ LÝ KHI NGƯỜI DÙNG CHỌN ĐÁP ÁN ---
    private fun xuLyChonDapAn(dapAnDuocChon: MaterialCardView, tenDapAn: String) {

        // Bước A: Reset toàn bộ form về trạng thái chưa chọn
        resetTatCaDapAn()

        // Bước B: Bật highlight xanh cho cái khung thẻ bên ngoài
        dapAnDuocChon.setCardBackgroundColor(ContextCompat.getColor(this, R.color.bg_light_blue))
        dapAnDuocChon.strokeColor = ContextCompat.getColor(this, R.color.primary_blue)
        dapAnDuocChon.strokeWidth = 5

        // Bước C: Lấy đúng các phần tử con dựa theo tên đáp án (A, B, C, hoặc D) để nhuộm màu
        val (tvLetter, tvAnswer, imgTick) = getChildViewsByOption(tenDapAn)

        tvLetter?.setBackgroundResource(R.drawable.bg_circle_blue) // Đổi vòng tròn thành xanh
        tvLetter?.setTextColor(ContextCompat.getColor(this, R.color.white)) // Đổi chữ cái thành trắng
        tvAnswer?.setTextColor(ContextCompat.getColor(this, R.color.primary_blue)) // Đổi chữ đáp án thành xanh
        imgTick?.visibility = View.VISIBLE // Hiện dấu tick lên

        // Hiển thị Toast kiểm tra
        Toast.makeText(this, "Đang chọn đáp án: $tenDapAn", Toast.LENGTH_SHORT).show()
    }

    // --- HÀM PHỤ TRỢ: TẨY TRẮNG FORM ---
    private fun resetTatCaDapAn() {
        val mauNenTrang = ContextCompat.getColor(this, R.color.white)
        val mauVienXam = ContextCompat.getColor(this, R.color.outline_gray)
        val mauChuDen = ContextCompat.getColor(this, R.color.text_dark)

        // 1. Reset các khung hình chữ nhật lớn bên ngoài trước
        val danhSachDapAn = listOf(btnOptionA, btnOptionB, btnOptionC, btnOptionD)
        for (card in danhSachDapAn) {
            card.setCardBackgroundColor(mauNenTrang)
            card.strokeColor = mauVienXam
            card.strokeWidth = 2
        }

        // 2. Chạy vòng lặp để reset các ID độc lập A, B, C, D bên trong
        val danhSachTen = listOf("A", "B", "C", "D")
        for (ten in danhSachTen) {
            val (tvLetter, tvAnswer, imgTick) = getChildViewsByOption(ten)

            tvLetter?.setBackgroundResource(R.drawable.bg_circle_gray)
            tvLetter?.setTextColor(mauChuDen)
            tvAnswer?.setTextColor(mauChuDen)
            imgTick?.visibility = View.INVISIBLE // Ẩn dấu tick đi
        }
    }

    // --- HÀM ĐẶC BIỆT: Tự động lấy đúng ID theo chữ cái tương ứng để tránh lỗi xung đột ID ---
    private fun getChildViewsByOption(tenDapAn: String): Triple<TextView?, TextView?, ImageView?> {
        return when (tenDapAn) {
            "A" -> Triple(findViewById(R.id.tvLetterA), findViewById(R.id.tvAnswerA), findViewById(R.id.imgTickA))
            "B" -> Triple(findViewById(R.id.tvLetterB), findViewById(R.id.tvAnswerB), findViewById(R.id.imgTickB))
            "C" -> Triple(findViewById(R.id.tvLetterC), findViewById(R.id.tvAnswerC), findViewById(R.id.imgTickC))
            "D" -> Triple(findViewById(R.id.tvLetterD), findViewById(R.id.tvAnswerD), findViewById(R.id.imgTickD))
            else -> Triple(null, null, null)
        }
    }
}