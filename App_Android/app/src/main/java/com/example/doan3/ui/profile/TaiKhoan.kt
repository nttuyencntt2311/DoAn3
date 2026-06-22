package com.example.doan3.ui.profile

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.RelativeLayout // Đừng quên dòng import này nhé
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.example.doan3.R


class TaiKhoanActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.taikkhoan)

        // 1. Nút quay lại
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        // ====================================================
        // 2. BẮT SỰ KIỆN MỞ TRANG THÔNG TIN CÁ NHÂN (VỪA THÊM)
        // ====================================================
        val btnPersonalInfo = findViewById<RelativeLayout>(R.id.btnPersonalInfo)
        btnPersonalInfo.setOnClickListener {
            val intent = Intent(this, ThongTinCaNhanActivity::class.java)
            startActivity(intent)
        }

        // 3. Nút Đăng xuất
        val btnLogout = findViewById<MaterialCardView>(R.id.btnLogout)
        btnLogout.setOnClickListener {
            Toast.makeText(this, "Đã đăng xuất tài khoản!", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, dangnhap::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
        // Mở trang Điều khoản & Chính sách
        val btnTerms = findViewById<RelativeLayout>(R.id.btnTerms)
        btnTerms.setOnClickListener {
            startActivity(Intent(this, DieuKhoanActivity::class.java))
        }
    }
}