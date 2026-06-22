package com.example.doan3.ui.profile

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.example.doan3.R


class dangnhap : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.dangnhap)

        // 1. Chuyển sang trang Đăng Ký khi bấm vào chữ "Đăng ký ngay"
        val tvRegister = findViewById<TextView>(R.id.tvRegister)
        tvRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // 2. Xử lý nút Đăng nhập (Tạm thời cho hiển thị thông báo và vào trang Khám Phá)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        btnLogin.setOnClickListener {
            Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()

            // Chuyển thẳng vào không gian học tập (Khám Phá)
            val intent = Intent(this, KhamPhaActivity::class.java)
            startActivity(intent)

            // Đóng trang đăng nhập này lại để ấn nút Back không bị quay lùi về đây
            finish()
        }
    }
}