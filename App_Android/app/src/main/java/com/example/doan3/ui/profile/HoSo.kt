package com.example.doan3.ui.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.doan3.MainActivity
import com.example.doan3.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class hoso : AppCompatActivity() {

    // CODE CHUẨN ĐI LÀM: Khai báo đúng kiểu dữ liệu TextView cho các ô hiển thị chữ
    private var tvProfileName: TextView? = null
    private var tvAccountType: TextView? = null
    private var tvHeaderStreak: TextView? = null
    private var tvStatLevel: TextView? = null
    private var tvStatVocab: TextView? = null
    private var tvStatDays: TextView? = null
    private var tvStatMinutes: TextView? = null

    // Khai báo các nút chức năng (Giữ nguyên View)
    private var btnLogout: View? = null
    private var btnChangePassword: View? = null
    private var btnHelpCenter: View? = null

    // Khai báo các nút điều hướng Bottom Navigation
    private var navTrangChu: View? = null
    private var navKhamPha: View? = null
    private var navTroLyAI: View? = null
    private var navHoSo: View? = null
    private var navTaiKhoan: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            enableEdgeToEdge()
            setContentView(R.layout.hoso)

            // Ánh xạ View chuẩn kiểu TextView
            tvProfileName = findViewById(R.id.tvProfileName)
            tvAccountType = findViewById(R.id.tvAccountType)
            tvHeaderStreak = findViewById(R.id.tvHeaderStreak)
            tvStatLevel = findViewById(R.id.tvStatLevel)
            tvStatVocab = findViewById(R.id.tvStatVocab)
            tvStatDays = findViewById(R.id.tvStatDays)
            tvStatMinutes = findViewById(R.id.tvStatMinutes)

            btnLogout = findViewById(R.id.btnLogout)
            btnChangePassword = findViewById(R.id.btnChangePassword)
            btnHelpCenter = findViewById(R.id.btnHelpCenter)

            navTrangChu = findViewById(R.id.btnTrangChu)
            navKhamPha = findViewById(R.id.btnKhamPha)
            navTroLyAI = findViewById(R.id.btnTroLyAI)
            navHoSo = findViewById(R.id.btnHoSo)
            navTaiKhoan = findViewById(R.id.btnTaiKhoan)

            setupBottomNavigation()
            setupUserActions()
            loadUserStats()

        } catch (e: Exception) {
            Toast.makeText(this, "Lỗi khởi tạo: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupUserActions() {
        btnChangePassword?.setOnClickListener {
            val intent = Intent(this, DoiMatKhauActivity::class.java)
            startActivity(intent)
        }

        btnHelpCenter?.setOnClickListener {
            val intent = Intent(this, HelpCenterActivity::class.java)
            startActivity(intent)
        }

        btnLogout?.setOnClickListener {
            val sharedPreferences = getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
            val refreshToken = sharedPreferences.getString("REFRESH_TOKEN", "") ?: ""

            if (refreshToken.isNotEmpty()) {
                val request = RefreshTokenRequest(refreshToken)
                // ĐÃ SỬA LỖI: Chuyển đổi Callback từ Void thành LogoutResponse cho đúng khớp với ApiService của Văn
                RetrofitClient.getInstance(this).logout(request)
                    .enqueue(object : Callback<LogoutResponse> {
                        override fun onResponse(call: Call<LogoutResponse>, response: Response<LogoutResponse>) {
                            thucHienDangXuatCucBo()
                        }
                        override fun onFailure(call: Call<LogoutResponse>, t: Throwable) {
                            thucHienDangXuatCucBo()
                        }
                    })
            } else {
                thucHienDangXuatCucBo()
            }
        }

        tvHeaderStreak?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("OPEN_STATISTICS", true)
            startActivity(intent)
        }
    }

    private fun thucHienDangXuatCucBo() {
        val sharedPreferences = getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()

        Toast.makeText(this, "Đã đăng xuất tài khoản!", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, dangnhap::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun loadUserStats() {
        RetrofitClient.getInstance(this).getUserStats().enqueue(object : Callback<UserStatsResponse> {
            override fun onResponse(call: Call<UserStatsResponse>, response: Response<UserStatsResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val stats = response.body()?.stats
                    if (stats != null) {
                        // CODE CHUẨN: Gán dữ liệu trực tiếp bằng thuộc tính .text ngắn gọn, tối ưu hiệu năng
                        tvProfileName?.text = stats.name
                        tvHeaderStreak?.text = stats.streakDays.toString()
                        tvStatLevel?.text = "${stats.level}"
                        tvStatVocab?.text = stats.wordsLearned.toString()
                        tvStatDays?.text = stats.streakDays.toString()
                        tvStatMinutes?.text = "Số phút học trong tuần: ${stats.timeLearnedMinutes.toString()}"
                        tvAccountType?.text = "Học viên chính thức"
                    }
                } else {
                    setLoadingDefaultValues()
                }
            }

            override fun onFailure(call: Call<UserStatsResponse>, t: Throwable) {
                setLoadingDefaultValues()
            }
        })
    }

    private fun setLoadingDefaultValues() {
        tvProfileName?.text = "Học viên mới"
        tvHeaderStreak?.text = "0"
        tvStatLevel?.text = "0"
        tvStatVocab?.text = "0"
        tvStatDays?.text = "0"
        tvStatMinutes?.text = "Số phút học trong tuần: 0"
        tvAccountType?.text = "Tài khoản miễn phí"
    }

    private fun setupBottomNavigation() {
        navTrangChu?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        navKhamPha?.setOnClickListener {
            Toast.makeText(this, "Chưa thiết lập màn hình Khám phá!", Toast.LENGTH_SHORT).show()
        }

        navTroLyAI?.setOnClickListener {
            Toast.makeText(this, "Chưa thiết lập màn hình Trợ lý AI!", Toast.LENGTH_SHORT).show()
        }

        navTaiKhoan?.setOnClickListener {
            val intent = Intent(this, TaiKhoanActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
    }
}