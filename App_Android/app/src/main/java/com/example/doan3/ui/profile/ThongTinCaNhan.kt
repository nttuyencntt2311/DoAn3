package com.example.doan3.ui.profile

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.example.doan3.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ThongTinCaNhanActivity : AppCompatActivity() {

    private lateinit var edtName: TextInputEditText
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
        edtName = findViewById(R.id.edtName)
        edtBirthDate = findViewById(R.id.edtBirthDate)
        btnMale = findViewById(R.id.btnMale)
        btnFemale = findViewById(R.id.btnFemale)
        tvMale = findViewById(R.id.tvMale)
        tvFemale = findViewById(R.id.tvFemale)

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

    private fun loadUserDataFromAPI() {
        RetrofitClient.getInstance(this).getProfile().enqueue(object : Callback<GetProfileResponse> {
            override fun onResponse(call: Call<GetProfileResponse>, response: Response<GetProfileResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()?.user
                    if (user != null) {
                        edtName.setText(user.name)
                        edtBirthDate.setText(user.birthDate)
                        gioiTinhDuocChon = if (user.gender.isNotEmpty()) user.gender else "Nam"
                        updateGioiTinhUI()
                    }
                }
            }

            override fun onFailure(call: Call<GetProfileResponse>, t: Throwable) {
                Toast.makeText(this@ThongTinCaNhanActivity, "Lỗi kết nối khi tải dữ liệu!", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveAction() {
        val name = edtName.text.toString().trim()
        val birthDate = edtBirthDate.text.toString().trim()

        if (name.isEmpty()) {
            Toast.makeText(this, "Tên không được để trống!", Toast.LENGTH_SHORT).show()
            return
        }

        val request = UpdateProfileRequest(name, gioiTinhDuocChon, birthDate)

        RetrofitClient.getInstance(this).updateProfile(request).enqueue(object : Callback<UpdateProfileResponse> {
            override fun onResponse(call: Call<UpdateProfileResponse>, response: Response<UpdateProfileResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@ThongTinCaNhanActivity, "Đã lưu thông tin thành công!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@ThongTinCaNhanActivity, "Lưu thất bại, thử lại sau!", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<UpdateProfileResponse>, t: Throwable) {
                Toast.makeText(this@ThongTinCaNhanActivity, "Mất kết nối máy chủ!", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateGioiTinhUI() {
        if (gioiTinhDuocChon == "Nam") {
            btnMale.strokeColor = android.graphics.Color.parseColor("#2563EB")
            btnMale.strokeWidth = 4
            tvMale.setTextColor(android.graphics.Color.parseColor("#2563EB"))

            btnFemale.strokeColor = android.graphics.Color.parseColor("#CBD5E1")
            btnFemale.strokeWidth = 2
            tvFemale.setTextColor(android.graphics.Color.parseColor("#64748B"))
        } else {
            btnFemale.strokeColor = android.graphics.Color.parseColor("#2563EB")
            btnFemale.strokeWidth = 4
            tvFemale.setTextColor(android.graphics.Color.parseColor("#2563EB"))

            btnMale.strokeColor = android.graphics.Color.parseColor("#CBD5E1")
            btnMale.strokeWidth = 2
            tvMale.setTextColor(android.graphics.Color.parseColor("#64748B"))
        }
    }
}