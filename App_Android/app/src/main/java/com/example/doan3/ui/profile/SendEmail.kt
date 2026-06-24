package com.example.doan3.ui.profile

import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.example.doan3.R


class SendEmailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.send_email)

        // Xử lý nút Quay lại
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        // Xử lý nút Gửi yêu cầu
        val btnSubmitRequest = findViewById<MaterialCardView>(R.id.btnSubmitRequest)
        btnSubmitRequest.setOnClickListener {
            Toast.makeText(this, "Đã gửi yêu cầu hỗ trợ thành công!", Toast.LENGTH_SHORT).show()
            finish() // Tự động quay về trang trước sau khi gửi
        }
    }
}