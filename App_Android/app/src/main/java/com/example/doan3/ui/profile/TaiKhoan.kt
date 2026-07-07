package com.example.doan3.ui.profile

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.example.doan3.R
import com.example.doan3.ui.profile.dangnhap

class TaiKhoanActivity : AppCompatActivity() {

    private lateinit var tvUserFullName: TextView
    private lateinit var tvUserEmail: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.taikkhoan)

        // Ánh xạ View
        tvUserFullName = findViewById(R.id.tvUserFullName)
        tvUserEmail = findViewById(R.id.tvUserEmail)

        // 1. Nút quay lại
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // 2. Bắt sự kiện mở trang Thông tin cá nhân
        findViewById<RelativeLayout>(R.id.btnPersonalInfo).setOnClickListener {
            val intent = Intent(this, ThongTinCaNhanActivity::class.java)
            startActivity(intent)
        }

        // 3. Nút Đăng xuất
        findViewById<MaterialCardView>(R.id.btnLogout).setOnClickListener {
            // TODO: Xóa SharedPreferences lưu Token/Session ở đây khi có API

            Toast.makeText(this, "Đã đăng xuất tài khoản!", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, dangnhap::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // Mở trang Điều khoản & Chính sách
        findViewById<RelativeLayout>(R.id.btnTerms).setOnClickListener {
            Toast.makeText(this, "Tính năng Điều khoản đang phát triển!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        loadAccountData()
    }

    private fun loadAccountData() {
        // TODO: Gọi API GET /api/users/profile để lấy tên và email từ MongoDB

        // Giả lập dữ liệu hiển thị tạm thời
        tvUserEmail.text = "user@example.com"
        tvUserFullName.text = "Người dùng LexiVT"
    }
}