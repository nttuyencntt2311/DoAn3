package com.example.doan3.ui.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.doan3.MainActivity
import com.example.doan3.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.view.View

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

        // 1. Nút quay lại (Đã xóa đoạn trùng lặp, chỉ giữ lại 1 cái chuẩn nhất)
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // 2. Mở trang Thông tin cá nhân
        findViewById<View>(R.id.btnPersonalInfo).setOnClickListener {
            val intent = Intent(this, ThongTinCaNhanActivity::class.java)
            startActivity(intent)
        }

        // 3. Mở trang Đổi mật khẩu
        findViewById<View>(R.id.btnChangePassword).setOnClickListener {
            val intent = Intent(this, DoiMatKhauActivity::class.java)
            startActivity(intent)
        }

        // 4. Mở trang Điều khoản & Chính sách
        findViewById<View>(R.id.btnTerms).setOnClickListener {
            startActivity(Intent(this, DieuKhoanActivity::class.java))
        }

        // 5. Nút đăng xuất
        findViewById<View>(R.id.btnLogout).setOnClickListener {
            val sharedPreferences = getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
            val refreshToken = sharedPreferences.getString("REFRESH_TOKEN", "") ?: ""

            if (refreshToken.isNotEmpty()) {
                val request = LogoutRequest(refreshToken)
                RetrofitClient.getInstance(this).logoutUser(request)
                    .enqueue(object : Callback<Void> {
                        override fun onResponse(call: Call<Void>, response: Response<Void>) {
                            thucHienDangXuatCucBo()
                        }

                        override fun onFailure(call: Call<Void>, t: Throwable) {
                            thucHienDangXuatCucBo()
                        }
                    })
            } else {
                thucHienDangXuatCucBo()
            }
        }

        // Cấu hình thanh điều hướng Bottom Navigation
        setupBottomNavigation()
    }

    /**
     * Hàm phụ trợ xử lý xóa bộ nhớ đệm SharedPreferences và chuyển màn hình.
     */
    private fun thucHienDangXuatCucBo() {
        // 1. Xóa thông tin đăng nhập và Token
        val sharedPreferences = getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()

        // 2. 🚀 ĐÃ BỔ SUNG: Xóa sạch thời gian học (StudyData) để tránh lỗi 8 phút
        val studyPref = getSharedPreferences("StudyData", Context.MODE_PRIVATE)
        studyPref.edit().clear().apply()

        Toast.makeText(this, "Đã đăng xuất tài khoản an toàn!", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, dangnhap::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
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

    // ====================================================================
    // HÀM MỚI BỔ SUNG: Xử lý Bottom Navigation cho màn hình Tài khoản
    // ====================================================================
    private fun setupBottomNavigation() {
        findViewById<View>(R.id.btnTrangChu)?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("OPEN_FRAGMENT", "HOME")
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            overridePendingTransition(0, 0)
            finish()
        }

        findViewById<View>(R.id.btnKhamPha)?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("OPEN_FRAGMENT", "LEARNING")
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            overridePendingTransition(0, 0)
            finish()
        }

        findViewById<View>(R.id.btnTroLyAI)?.setOnClickListener {
            val intent = Intent(this, TroLyAIActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            overridePendingTransition(0, 0)
            finish()
        }

        findViewById<View>(R.id.btnHoSo)?.setOnClickListener {
            val intent = Intent(this, hoso::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            overridePendingTransition(0, 0)
            finish()
        }

        findViewById<View>(R.id.btnTaiKhoan)?.setOnClickListener {
            // Do nothing
        }
    }
}