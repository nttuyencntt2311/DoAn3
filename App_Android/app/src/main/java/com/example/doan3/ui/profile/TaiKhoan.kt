package com.example.doan3.ui.profile

import android.content.Context
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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TaiKhoanActivity : AppCompatActivity() {

    private lateinit var tvUserFullName: TextView
    private lateinit var tvUserEmail: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.taikkhoan)

        // Ánh xạ UI
        tvUserFullName = findViewById(R.id.tvUserFullName)
        tvUserEmail = findViewById(R.id.tvUserEmail)

        // 1. Nút quay lại
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // 2. Mở trang Thông tin cá nhân
        findViewById<RelativeLayout>(R.id.btnPersonalInfo).setOnClickListener {
            val intent = Intent(this, ThongTinCaNhanActivity::class.java)
            startActivity(intent)
        }

        // 3. Mở trang Đổi mật khẩu (ĐOẠN CODE VỪA ĐƯỢC THÊM)
        findViewById<RelativeLayout>(R.id.btnChangePassword).setOnClickListener {
            val intent = Intent(this, DoiMatKhauActivity::class.java)
            startActivity(intent)
        }

        // 4. Mở trang Điều khoản & Chính sách
        findViewById<RelativeLayout>(R.id.btnTerms).setOnClickListener {
            startActivity(Intent(this, DieuKhoanActivity::class.java))
        }

        findViewById<RelativeLayout>(R.id.btnLogout).setOnClickListener {
            val sharedPreferences = getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
            val refreshToken = sharedPreferences.getString("REFRESH_TOKEN", "") ?: ""

            // GỌI API BÁO BACKEND HỦY TOKEN TRƯỚC KHI XÓA TRÊN MÁY
            if (refreshToken.isNotEmpty()) {
                val request = RefreshTokenRequest(refreshToken)
                RetrofitClient.getInstance(this).logout(request).enqueue(object : Callback<LogoutResponse> {
                    override fun onResponse(call: Call<LogoutResponse>, response: Response<LogoutResponse>) { }
                    override fun onFailure(call: Call<LogoutResponse>, t: Throwable) { }
                })
            }

            // Dọn dẹp máy và chuyển màn hình (Giữ nguyên code cũ của bạn)
            sharedPreferences.edit().clear().apply()
            Toast.makeText(this, "Đã đăng xuất tài khoản!", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, dangnhap::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()

        }
    }

    override fun onResume() {
        super.onResume()
        // Tự động load lại dữ liệu người dùng khi mở màn hình này
        loadAccountData()
    }

    private fun loadAccountData() {
        RetrofitClient.getInstance(this).getProfile().enqueue(object : Callback<GetProfileResponse> {
            override fun onResponse(call: Call<GetProfileResponse>, response: Response<GetProfileResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()?.user
                    if (user != null) {
                        tvUserEmail.text = user.email
                        tvUserFullName.text = user.name
                    }
                } else {
                    tvUserEmail.text = "Lỗi tải email"
                    tvUserFullName.text = "Lỗi tải thông tin"
                }
            }

            override fun onFailure(call: Call<GetProfileResponse>, t: Throwable) {
                tvUserEmail.text = "Mất kết nối"
                tvUserFullName.text = "Mất kết nối"
            }
        })
    }
}