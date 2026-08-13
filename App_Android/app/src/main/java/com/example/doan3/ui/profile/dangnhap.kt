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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class dangnhap : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.dangnhap)

        val tvRegister = findViewById<TextView>(R.id.tvRegister)
        tvRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        val edtEmailLogin = findViewById<TextInputEditText>(R.id.edtEmailLogin)
        val edtPasswordLogin = findViewById<TextInputEditText>(R.id.edtPasswordLogin)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val email = edtEmailLogin.text.toString().trim()
            val password = edtPasswordLogin.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = LoginRequest(email, password)

            RetrofitClient.getInstance(this).loginUser(request).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {

                    // 1. In mã trạng thái Server trả về ra Logcat
                    android.util.Log.d("DEBUG_LOGIN", "Mã trạng thái Server: ${response.code()}")

                    if (response.isSuccessful && response.body() != null) {
                        val loginData = response.body()

                        // 2. Kiểm tra xem Token có thực sự lấy được không hay bị null
                        android.util.Log.d("DEBUG_LOGIN", "Token lấy được: ${loginData?.token}")

                        val token = loginData?.token
                        val userId = loginData?.user?.id
                        val userName = loginData?.user?.name
                        val refreshToken = loginData?.refreshToken

                        val sharedPreferences = getSharedPreferences("DoAn3Prefs", MODE_PRIVATE)
                        sharedPreferences.edit().apply {
                            putString("JWT_TOKEN", token)
                            putString("USER_ID", userId)
                            putString("REFRESH_TOKEN", refreshToken)
                            putString("USER_NAME", userName)
                            apply()
                        }

                        RetrofitClient.token = token ?: ""

                        Toast.makeText(this@dangnhap, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@dangnhap, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        // 3. Nếu sai email/pass hoặc lỗi 500, in chi tiết lỗi ra
                        val errorMsg = response.errorBody()?.string()
                        android.util.Log.e("DEBUG_LOGIN", "Lỗi Server từ chối: $errorMsg")
                        Toast.makeText(this@dangnhap, "Sai email hoặc mật khẩu!", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    // 4. BẮT ĐƯỢC LỖI MẠNG HOẶC LỖI DỮ LIỆU Ở ĐÂY
                    android.util.Log.e("DEBUG_LOGIN", "BỊ SẬP NGẦM VÌ: ${t.message}")
                    t.printStackTrace() // In toàn bộ dòng lỗi màu đỏ ra Logcat

                    Toast.makeText(this@dangnhap, "Lỗi kết nối: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
        }
    }
}