package com.example.doan3.ui.profile

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.example.doan3.R


class ThongTinCaNhanActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.thongtincanhan)

        // 1. Nút quay lại
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // 2. Nút Lưu (Cả chữ Lưu ở trên và nút xanh ở dưới)
        val btnSaveTop = findViewById<TextView>(R.id.btnSaveTop)
        val btnSaveBottom = findViewById<MaterialCardView>(R.id.btnSaveBottom)

        val saveAction = {
            Toast.makeText(this, "Đã lưu thông tin cá nhân!", Toast.LENGTH_SHORT).show()
            finish() // Tự động đóng trang sau khi lưu
        }
        btnSaveTop.setOnClickListener { saveAction() }
        btnSaveBottom.setOnClickListener { saveAction() }

        // 3. Xử lý đổi màu khi chọn Giới Tính Nam/Nữ
        val btnMale = findViewById<MaterialCardView>(R.id.btnMale)
        val btnFemale = findViewById<MaterialCardView>(R.id.btnFemale)
        val tvMale = findViewById<TextView>(R.id.tvMale)
        val tvFemale = findViewById<TextView>(R.id.tvFemale)

        // Khi bấm vào chữ Nam
        btnMale.setOnClickListener {
            btnMale.strokeColor = android.graphics.Color.parseColor("#2563EB")
            btnMale.strokeWidth = 4 // Viền đậm lên
            tvMale.setTextColor(android.graphics.Color.parseColor("#2563EB"))

            btnFemale.strokeColor = android.graphics.Color.parseColor("#CBD5E1")
            btnFemale.strokeWidth = 2
            tvFemale.setTextColor(android.graphics.Color.parseColor("#64748B"))
        }

        // Khi bấm vào chữ Nữ
        btnFemale.setOnClickListener {
            btnFemale.strokeColor = android.graphics.Color.parseColor("#2563EB")
            btnFemale.strokeWidth = 4
            tvFemale.setTextColor(android.graphics.Color.parseColor("#2563EB"))

            btnMale.strokeColor = android.graphics.Color.parseColor("#CBD5E1")
            btnMale.strokeWidth = 2
            tvMale.setTextColor(android.graphics.Color.parseColor("#64748B"))
        }
    }
}