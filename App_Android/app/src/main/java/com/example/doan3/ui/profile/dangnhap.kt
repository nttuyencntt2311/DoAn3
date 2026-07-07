package com.example.doan3.ui.profile

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.example.doan3.R
import com.example.doan3.MainActivity

class dangnhap : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.dangnhap)

        // Nút chuyển qua trang Đăng Ký
        val tvRegister = findViewById<TextView>(R.id.tvRegister)
        tvRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // Ánh xạ UI
        val edtEmailLogin = findViewById<TextInputEditText>(R.id.edtEmailLogin)
        val edtPasswordLogin = findViewById<TextInputEditText>(R.id.edtPasswordLogin)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)

        // Xử lý nút Đăng nhập
        btnLogin.setOnClickListener {
            val email = edtEmailLogin.text.toString().trim()
            val password = edtPasswordLogin.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập Email và Mật khẩu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // TODO: Gọi API POST /api/login đến Node.js ở đây
            // Giả lập đăng nhập thành công tạm thời để test UI
            Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()

            // Chuyển vào trang chủ (MainActivity)
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}