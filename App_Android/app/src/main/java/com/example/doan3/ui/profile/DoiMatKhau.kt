package com.example.doan3.ui.profile

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputLayout
import com.example.doan3.R


class DoiMatKhauActivity : AppCompatActivity() {

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

        // 1. Xử lý nút quay lại
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        // 2. Lắng nghe thay đổi trên ô Mật khẩu mới để cập nhật danh sách điều kiện
        setupPasswordRequirementsListener()

        // 3. Xử lý nút Cập nhật mật khẩu
        findViewById<MaterialCardView>(R.id.btnUpdatePassword).setOnClickListener {
            handleUpdatePassword()
        }
    }

    private fun anhXaGiaoDien() {
        tilNewPassword = findViewById(R.id.tilNewPassword)
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword)
        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)

        icReqMin8 = findViewById(R.id.icReqMin8)
        icReqUpper = findViewById(R.id.icReqUpper)
        icReqLower = findViewById(R.id.icReqLower)
        icReqNumber = findViewById(R.id.icReqNumber)

        tvReqMin8 = findViewById(R.id.tvReqMin8)
        tvReqUpper = findViewById(R.id.tvReqUpper)
        tvReqLower = findViewById(R.id.tvReqLower)
        tvReqNumber = findViewById(R.id.tvReqNumber)
    }

    private fun setupPasswordRequirementsListener() {
        etNewPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()
                validatePasswordRequirements(password)
            }
        })
    }

    // Hàm kiểm tra và cập nhật màu sắc danh sách điều kiện
    private fun validatePasswordRequirements(password: String) {
        val colorActive = Color.parseColor("#007BFF") // Màu xanh khi thỏa mãn
        val colorInactive = Color.parseColor("#94A3B8") // Màu xám khi chưa thỏa mãn
        val colorTextActive = Color.parseColor("#1E293B") // Màu chữ đậm
        val colorTextInactive = Color.parseColor("#64748B") // Màu chữ nhạt

        // 1. Tối thiểu 8 ký tự
        updateRequirementStatus(password.length >= 8, icReqMin8, tvReqMin8, colorActive, colorInactive, colorTextActive, colorTextInactive)

        // 2. Ít nhất một chữ cái viết hoa
        updateRequirementStatus(password.contains("[A-Z]".toRegex()), icReqUpper, tvReqUpper, colorActive, colorInactive, colorTextActive, colorTextInactive)

        // 3. Ít nhất một chữ cái viết thường
        updateRequirementStatus(password.contains("[a-z]".toRegex()), icReqLower, tvReqLower, colorActive, colorInactive, colorTextActive, colorTextInactive)

        // 4. Ít nhất một chữ số
        updateRequirementStatus(password.contains("[0-9]".toRegex()), icReqNumber, tvReqNumber, colorActive, colorInactive, colorTextActive, colorTextInactive)
    }

    // Hàm phụ để cập nhật UI cho từng điều kiện
    private fun updateRequirementStatus(
        isMet: Boolean,
        imageView: ImageView,
        textView: TextView,
        colorActive: Int,
        colorInactive: Int,
        colorTextActive: Int,
        colorTextInactive: Int
    ) {
        if (isMet) {
            imageView.imageTintList = ColorStateList.valueOf(colorActive)
            textView.setTextColor(colorTextActive)
        } else {
            imageView.imageTintList = ColorStateList.valueOf(colorInactive)
            textView.setTextColor(colorTextInactive)
        }
    }

    private fun handleUpdatePassword() {
        val currentPass = findViewById<EditText>(R.id.etCurrentPassword).text.toString()
        val newPass = etNewPassword.text.toString()
        val confirmPass = etConfirmPassword.text.toString()

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

        // 4. Gọi API cập nhật mật khẩu (Giả lập)
        // Tại đây bạn sẽ gọi hàm xử lý PHP/Database
        if (currentPass == "123456Aa") { // Giả lập mật khẩu cũ đúng
            Toast.makeText(this, "Cập nhật mật khẩu thành công!", Toast.LENGTH_LONG).show()
            finish() // Đóng trang
        } else {
            Toast.makeText(this, "Mật khẩu hiện tại không chính xác", Toast.LENGTH_SHORT).show()
        }
    }
}