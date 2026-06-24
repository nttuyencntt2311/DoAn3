package com.example.doan3.ui.profile

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.doan3.R

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Gọi file giao diện dangky.xml của bạn
        setContentView(R.layout.dangky)

        // Đoạn này xử lý tràn viền (nhớ thêm android:id="@+id/main" vào thẻ ngoài cùng của file dangky.xml giống lúc nãy nhé)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Xử lý nút "Đăng nhập" để quay lại trang trước
        val tvLogin = findViewById<TextView>(R.id.tvLogin)
        tvLogin?.setOnClickListener {
            finish() // Đóng trang đăng ký này lại, màn hình sẽ tự động lùi về MainActivity
        }
    }
}