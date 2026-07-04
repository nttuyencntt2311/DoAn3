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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TaiKhoanActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var tvUserFullName: TextView
    private lateinit var tvUserEmail: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.taikkhoan)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

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
            auth.signOut()
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

    // Luôn luôn tải lại dữ liệu mới nhất khi màn hình hiển thị hoặc được quay lại
    override fun onResume() {
        super.onResume()
        loadAccountData()
    }

    private fun loadAccountData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            tvUserEmail.text = currentUser.email ?: ""

            db.collection("Users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val tenHienThi = document.getString("hoVaTen")
                        if (!tenHienThi.isNullOrEmpty()) {
                            tvUserFullName.text = tenHienThi
                        } else {
                            tvUserFullName.text = "Người dùng LexiVT"
                        }
                    }
                }
        }
    }
}