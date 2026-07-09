package com.example.doan3.ui.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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

        // Ánh xạ UI (Giữ nguyên toàn bộ ID gốc)
        tvUserFullName = findViewById(R.id.tvUserFullName)
        tvUserEmail = findViewById(R.id.tvUserEmail)

        // 1. Nút quay lại (Dạng ImageView)
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // 1. Nút quay lại (Dạng View chung)
        findViewById<View>(R.id.btnBack).setOnClickListener {
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
            // Lưu ý: Đảm bảo bạn đã tạo file DieuKhoanActivity.kt trong project
            startActivity(Intent(this, DieuKhoanActivity::class.java))
        }

        // 5. Nút đăng xuất (Đã sửa đổi để gọi API đồng bộ xóa Refresh Token trên Server MongoDB)
        findViewById<View>(R.id.btnLogout).setOnClickListener {
            val sharedPreferences = getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
            val refreshToken = sharedPreferences.getString("REFRESH_TOKEN", "") ?: ""

            // GỌI API BÁO BACKEND HỦY TOKEN TRƯỚC KHI TIẾN HÀNH DỌN DẸP MÁY
            if (refreshToken.isNotEmpty()) {
                val request = LogoutRequest(refreshToken)
                RetrofitClient.getInstance(this).logoutUser(request)
                    .enqueue(object : Callback<Void> {
                        override fun onResponse(call: Call<Void>, response: Response<Void>) {
                            // Sau khi server xử lý xong (hoặc kể cả phiên hết hạn trước đó), dọn dẹp local và thoát
                            thucHienDangXuatCucBo()
                        }

                        override fun onFailure(call: Call<Void>, t: Throwable) {
                            // Gặp sự cố mất mạng vẫn cho phép dọn dẹp cục bộ để user thoát ra ngoài, tránh treo app
                            thucHienDangXuatCucBo()
                        }
                    })
            } else {
                // Nếu không tìm thấy token trên thiết bị, thực hiện thoát cục bộ ngay lập tức
                thucHienDangXuatCucBo()
            }
        }
    }

    /**
     * Hàm phụ trợ xử lý xóa bộ nhớ đệm SharedPreferences và chuyển màn hình.
     * Bảo toàn 100% logic chuyển hướng và các Flags xóa lịch sử trang cũ của bạn.
     */
    private fun thucHienDangXuatCucBo() {
        val sharedPreferences = getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()

        Toast.makeText(this, "Đã đăng xuất tài khoản an toàn!", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, dangnhap::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        // Tự động nạp lại thông tin người dùng mới nhất khi quay lại màn hình này
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