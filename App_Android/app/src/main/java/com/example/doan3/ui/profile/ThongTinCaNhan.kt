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

        // Ánh xạ ID
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

        // Lấy dữ liệu từ API về
        loadUserDataFromAPI()

        val saveAction = {
            val hoVaTen = edtName.text.toString().trim()
            val ngaySinh = edtBirthDate.text.toString().trim()

            // TODO: Gọi API PUT hoặc PATCH /api/users/profile để lưu dữ liệu lên MongoDB
            // Gửi lên: hoVaTen, ngaySinh, gioiTinhDuocChon

            Toast.makeText(this, "Đã lưu thông tin (Giả lập)!", Toast.LENGTH_SHORT).show()
            finish()
        }

        btnSaveTop.setOnClickListener { saveAction() }
        btnSaveBottom.setOnClickListener { saveAction() }
    }

    private fun loadUserDataFromAPI() {
        // TODO: Gọi API GET /api/users/profile để lấy dữ liệu từ MongoDB fill vào UI

        // Dữ liệu giả lập tạm thời
        edtName.setText("Người dùng LexiVT")
        edtBirthDate.setText("")
        gioiTinhDuocChon = "Nam"
        updateGioiTinhUI()
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