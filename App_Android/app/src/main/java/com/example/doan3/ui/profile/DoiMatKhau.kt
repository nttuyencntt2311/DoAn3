package com.example.doan3.ui.profile

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import com.example.doan3.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DoiMatKhauActivity : AppCompatActivity() {

    private lateinit var etCurrentPassword: EditText
    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var tilNewPassword: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout

    // Các biến chứa Icon điều kiện
    private lateinit var icReqMin8: ImageView
    private lateinit var icReqUpper: ImageView
    private lateinit var icReqLower: ImageView
    private lateinit var icReqNumber: ImageView

    // Các biến chứa TextView điều kiện
    private lateinit var tvReqMin8: TextView
    private lateinit var tvReqUpper: TextView
    private lateinit var tvReqLower: TextView
    private lateinit var tvReqNumber: TextView

    // Biểu thức chính quy (Regex) để kiểm tra
    private val passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$".toRegex()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.doimatkhau)

        anhXaGiaoDien()
        setupPasswordWatcher()

        // Bắt sự kiện quay lại
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        // Bắt sự kiện Lưu (nếu ID nút lưu của bạn khác btnSavePassword, hãy đổi id chỗ này cho khớp file XML)
        findViewById<View>(R.id.btnUpdatePassword)?.setOnClickListener {
            handleUpdatePassword()
        }
    }

    private fun anhXaGiaoDien() {
        etCurrentPassword = findViewById(R.id.etCurrentPassword)
        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        tilNewPassword = findViewById(R.id.tilNewPassword)
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword)

        icReqMin8 = findViewById(R.id.icReqMin8)
        icReqUpper = findViewById(R.id.icReqUpper)
        icReqLower = findViewById(R.id.icReqLower)
        icReqNumber = findViewById(R.id.icReqNumber)

        tvReqMin8 = findViewById(R.id.tvReqMin8)
        tvReqUpper = findViewById(R.id.tvReqUpper)
        tvReqLower = findViewById(R.id.tvReqLower)
        tvReqNumber = findViewById(R.id.tvReqNumber)
    }

    private fun setupPasswordWatcher() {
        etNewPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val password = s.toString()

                // Logic đổi màu khi đạt điều kiện (Xanh: #10B981, Xám: #64748B)
                updateRequirementStatus(password.length >= 8, icReqMin8, tvReqMin8)
                updateRequirementStatus(password.any { it.isUpperCase() }, icReqUpper, tvReqUpper)
                updateRequirementStatus(password.any { it.isLowerCase() }, icReqLower, tvReqLower)
                updateRequirementStatus(password.any { it.isDigit() }, icReqNumber, tvReqNumber)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun updateRequirementStatus(isValid: Boolean, icon: ImageView, text: TextView) {
        if (isValid) {
            icon.imageTintList = ColorStateList.valueOf(Color.parseColor("#10B981"))
            text.setTextColor(Color.parseColor("#10B981"))
        } else {
            icon.imageTintList = ColorStateList.valueOf(Color.parseColor("#94A3B8"))
            text.setTextColor(Color.parseColor("#64748B"))
        }
    }

    private fun handleUpdatePassword() {
        val currentPass = etCurrentPassword.text.toString().trim()
        val newPass = etNewPassword.text.toString().trim()
        val confirmPass = etConfirmPassword.text.toString().trim()

        // Xóa lỗi cũ
        tilNewPassword.error = null
        tilConfirmPassword.error = null

        // 1. Kiểm tra rỗng
        if (currentPass.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập mật khẩu hiện tại", Toast.LENGTH_SHORT).show()
            return
        }

        // 2. Kiểm tra độ mạnh mật khẩu mới (theo Regex tổng hợp)
        if (!newPass.matches(passwordPattern)) {
            tilNewPassword.error = "Mật khẩu chưa đủ mạnh"
            Toast.makeText(this, "Vui lòng thỏa mãn các điều kiện mật khẩu", Toast.LENGTH_SHORT).show()
            return
        }

        // 3. Kiểm tra mật khẩu mới và xác nhận có khớp không
        if (newPass != confirmPass) {
            tilConfirmPassword.error = "Mật khẩu xác nhận không khớp"
            return
        }

        // 4. GỌI API ĐỔI MẬT KHẨU
        val requestBody = ChangePasswordRequest(currentPass, newPass)
        RetrofitClient.getInstance(this).changePassword(requestBody)
            .enqueue(object : Callback<ChangePasswordResponse> {
                override fun onResponse(call: Call<ChangePasswordResponse>, response: Response<ChangePasswordResponse>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@DoiMatKhauActivity, "Cập nhật mật khẩu thành công!", Toast.LENGTH_LONG).show()
                        finish() // Đóng trang đổi mật khẩu
                    } else {
                        Toast.makeText(this@DoiMatKhauActivity, "Mật khẩu hiện tại không chính xác!", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<ChangePasswordResponse>, t: Throwable) {
                    Toast.makeText(this@DoiMatKhauActivity, "Lỗi kết nối máy chủ!", Toast.LENGTH_SHORT).show()
                }
            })
    }
}