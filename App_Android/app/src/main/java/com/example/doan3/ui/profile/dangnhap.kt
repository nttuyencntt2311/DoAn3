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

            // GỌI API ĐĂNG NHẬP XUỐNG NODE.JS
            val request = LoginRequest(email, password)

            RetrofitClient.instance.loginUser(request).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val loginData = response.body()
                        val token = loginData?.token
                        val userId = loginData?.user?.id
                        val userName = loginData?.user?.name

                        // LƯU TOKEN VÀ THÔNG TIN USER VÀO BỘ NHỚ ĐIỆN THOẠI (SharedPreferences)
                        val sharedPreferences = getSharedPreferences("DoAn3Prefs", MODE_PRIVATE)
                        sharedPreferences.edit().apply {
                            putString("JWT_TOKEN", token)
                            putString("USER_ID", userId)
                            putString("USER_NAME", userName)
                            apply()
                        }

                        Toast.makeText(this@dangnhap, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()

                        // Chuyển hướng thẳng vào trang chủ (MainActivity)
                        val intent = Intent(this@dangnhap, MainActivity::class.java)
                        startActivity(intent)
                        finish() // Đóng luôn màn hình đăng nhập để khi bấm nút Back không bị quay lại đây
                    } else {
                        Toast.makeText(this@dangnhap, "Sai email hoặc mật khẩu!", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Toast.makeText(this@dangnhap, "Không thể kết nối đến máy chủ Backend!", Toast.LENGTH_LONG).show()
                }
            })
        }
    }
}