package com.example.doan3.ui.profile

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.doan3.MainActivity
import com.example.doan3.R
import com.example.doan3.ui.ai.ChatAIActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class dangnhap : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)

        // Tự động đăng nhập nếu đã có JWT_TOKEN hợp lệ trong máy
        val sharedPreferences = getSharedPreferences("DoAn3Prefs", MODE_PRIVATE)
        val savedToken = sharedPreferences.getString("JWT_TOKEN", "") ?: ""
        if (savedToken.isNotEmpty()) {
            RetrofitClient.token = savedToken
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.dangnhap)

        val tvRegister = findViewById<TextView>(R.id.tvRegister)
        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        tvForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }

        val btnGoogleLogin = findViewById<MaterialButton>(R.id.btnGoogleLogin)
        btnGoogleLogin.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Đăng nhập bằng Google")
                .setMessage("Tính năng đăng nhập bằng Google đang được tích hợp. Vui lòng sử dụng Email và Mật khẩu để tiếp tục học cùng LexiVT.")
                .setPositiveButton("Đã hiểu", null)
                .show()
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

            btnLogin.isEnabled = false
            btnLogin.text = "Đang đăng nhập..."

            val request = LoginRequest(email, password)
            RetrofitClient.getInstance(this).loginUser(request).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    btnLogin.isEnabled = true
                    btnLogin.text = "Đăng nhập"

                    if (response.isSuccessful && response.body() != null) {
                        val loginData = response.body()
                        val token = loginData?.token ?: ""
                        val userId = loginData?.user?.id ?: ""
                        val userName = loginData?.user?.name ?: ""
                        val refreshToken = loginData?.refreshToken ?: ""

                        sharedPreferences.edit().apply {
                            putString("JWT_TOKEN", token)
                            putString("USER_ID", userId)
                            putString("REFRESH_TOKEN", refreshToken)
                            putString("USER_NAME", userName)
                            apply()
                        }

                        RetrofitClient.token = token
                        ChatAIActivity.clearActiveSession()

                        Toast.makeText(this@dangnhap, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@dangnhap, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        val msg = when (response.code()) {
                            400, 401 -> "Sai email hoặc mật khẩu!"
                            500 -> "Máy chủ đang gặp sự cố, vui lòng thử lại sau."
                            else -> "Đăng nhập thất bại (Mã lỗi: ${response.code()})"
                        }
                        Toast.makeText(this@dangnhap, msg, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    btnLogin.isEnabled = true
                    btnLogin.text = "Đăng nhập"
                    Toast.makeText(this@dangnhap, "Lỗi kết nối: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
        }
    }

    private fun showForgotPasswordDialog() {
        val inputEmail = EditText(this).apply {
            hint = "Nhập email đã đăng ký"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(this)
            .setTitle("Quên mật khẩu")
            .setMessage("Nhập email tài khoản để gửi yêu cầu hỗ trợ khôi phục mật khẩu:")
            .setView(inputEmail)
            .setNegativeButton("Hủy", null)
            .setPositiveButton("Gửi hỗ trợ") { _, _ ->
                val email = inputEmail.text.toString().trim()
                if (email.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập email của bạn!", Toast.LENGTH_SHORT).show()
                } else {
                    val intent = Intent(this, SendEmailActivity::class.java).apply {
                        putExtra("PREFILL_TOPIC", "Tài khoản & Đăng nhập")
                        putExtra("PREFILL_CONTENT", "Yêu cầu khôi phục mật khẩu cho tài khoản email: $email")
                    }
                    startActivity(intent)
                }
            }
            .show()
    }
}