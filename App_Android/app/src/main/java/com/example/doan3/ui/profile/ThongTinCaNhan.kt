package com.example.doan3.ui.profile

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.doan3.R
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar
import java.util.Locale

class ThongTinCaNhanActivity : AppCompatActivity() {

    private lateinit var tvUserId: TextView
    private lateinit var edtName: TextInputEditText
    private lateinit var edtEmail: TextInputEditText
    private lateinit var edtPhone: TextInputEditText
    private lateinit var edtBirthDate: TextInputEditText
    private lateinit var btnMale: MaterialCardView
    private lateinit var btnFemale: MaterialCardView
    private lateinit var tvMale: TextView
    private lateinit var tvFemale: TextView

    private var gioiTinhDuocChon = "Nam"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.thongtincanhan)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
        tvUserId = findViewById(R.id.tvUserId)
        edtName = findViewById(R.id.edtName)
        edtEmail = findViewById(R.id.edtEmail)
        edtPhone = findViewById(R.id.edtPhone)
        edtBirthDate = findViewById(R.id.edtBirthDate)
        btnMale = findViewById(R.id.btnMale)
        btnFemale = findViewById(R.id.btnFemale)
        tvMale = findViewById(R.id.tvMale)
        tvFemale = findViewById(R.id.tvFemale)

        // Email là định danh tài khoản nên chỉ hiển thị đọc
        edtEmail.isEnabled = false

        // Gắn DatePickerDialog cho ô ngày sinh
        edtBirthDate.setOnClickListener { showBirthDatePicker() }

        val btnSaveTop = findViewById<TextView>(R.id.btnSaveTop)
        val btnSaveBottom = findViewById<MaterialCardView>(R.id.btnSaveBottom)

        btnMale.setOnClickListener {
            gioiTinhDuocChon = "Nam"
            updateGioiTinhUI()
        }

        btnFemale.setOnClickListener {
            gioiTinhDuocChon = "Nữ"
            updateGioiTinhUI()
        }

        btnSaveTop.setOnClickListener { saveAction() }
        btnSaveBottom.setOnClickListener { saveAction() }

        loadUserDataFromAPI()
    }

    private fun showBirthDatePicker() {
        val calendar = Calendar.getInstance()
        val currentText = edtBirthDate.text?.toString()?.trim() ?: ""
        val parts = currentText.split("/")
        if (parts.size == 3) {
            val d = parts[0].toIntOrNull()
            val m = parts[1].toIntOrNull()
            val y = parts[2].toIntOrNull()
            if (d != null && m != null && y != null && y in 1920..2025) {
                calendar.set(y, m - 1, d)
            }
        }

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val formatted = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year)
                edtBirthDate.setText(formatted)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun loadUserDataFromAPI() {
        val prefs = getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
        val savedUserId = prefs.getString("USER_ID", "") ?: ""
        if (savedUserId.isNotEmpty()) {
            tvUserId.text = "ID: ${savedUserId.takeLast(8).uppercase()}"
            edtPhone.setText(prefs.getString("USER_PHONE_$savedUserId", "") ?: "")
        }

        RetrofitClient.getInstance(this).getProfile().enqueue(object : Callback<GetProfileResponse> {
            override fun onResponse(call: Call<GetProfileResponse>, response: Response<GetProfileResponse>) {
                if (isFinishing || isDestroyed) return
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()?.user
                    if (user != null) {
                        tvUserId.text = "ID: ${user.id.takeLast(8).uppercase()}"
                        edtName.setText(user.name)
                        edtEmail.setText(user.email)
                        edtBirthDate.setText(user.birthDate)
                        edtPhone.setText(prefs.getString("USER_PHONE_${user.id}", "") ?: "")
                        gioiTinhDuocChon = if (user.gender.isNotEmpty()) user.gender else "Nam"
                        updateGioiTinhUI()
                    }
                }
            }

            override fun onFailure(call: Call<GetProfileResponse>, t: Throwable) {
                if (isFinishing || isDestroyed) return
                Toast.makeText(this@ThongTinCaNhanActivity, "Lỗi kết nối khi tải dữ liệu!", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveAction() {
        val name = edtName.text.toString().trim()
        val birthDate = edtBirthDate.text.toString().trim()
        val phone = edtPhone.text.toString().trim()

        if (name.isEmpty()) {
            Toast.makeText(this, "Tên không được để trống!", Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
        val userId = prefs.getString("USER_ID", "") ?: ""
        if (userId.isNotEmpty()) {
            prefs.edit()
                .putString("USER_PHONE_$userId", phone)
                .putString("USER_NAME", name)
                .apply()
        }

        val request = UpdateProfileRequest(name, gioiTinhDuocChon, birthDate)
        RetrofitClient.getInstance(this).updateProfile(request).enqueue(object : Callback<UpdateProfileResponse> {
            override fun onResponse(call: Call<UpdateProfileResponse>, response: Response<UpdateProfileResponse>) {
                if (isFinishing || isDestroyed) return
                if (response.isSuccessful) {
                    Toast.makeText(this@ThongTinCaNhanActivity, "Đã lưu thông tin thành công!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@ThongTinCaNhanActivity, "Lưu thất bại, thử lại sau!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UpdateProfileResponse>, t: Throwable) {
                if (isFinishing || isDestroyed) return
                Toast.makeText(this@ThongTinCaNhanActivity, "Mất kết nối máy chủ!", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateGioiTinhUI() {
        if (gioiTinhDuocChon == "Nam") {
            btnMale.strokeColor = Color.parseColor("#2563EB")
            btnMale.strokeWidth = 4
            tvMale.setTextColor(Color.parseColor("#2563EB"))

            btnFemale.strokeColor = Color.parseColor("#CBD5E1")
            btnFemale.strokeWidth = 2
            tvFemale.setTextColor(Color.parseColor("#64748B"))
        } else {
            btnFemale.strokeColor = Color.parseColor("#2563EB")
            btnFemale.strokeWidth = 4
            tvFemale.setTextColor(Color.parseColor("#2563EB"))

            btnMale.strokeColor = Color.parseColor("#CBD5E1")
            btnMale.strokeWidth = 2
            tvMale.setTextColor(Color.parseColor("#64748B"))
        }
    }
}