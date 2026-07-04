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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class ThongTinCaNhanActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Khai báo đúng các biến ID có trong file XML của bạn
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

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        // Ánh xạ đúng 100% ID từ file thongtincanhan.xml
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

        // Kéo dữ liệu từ Firebase về
        loadUserDataFromFirebase()

        val currentUser = auth.currentUser
        val saveAction = {
            if (currentUser != null) {
                val userMap = hashMapOf(
                    "hoVaTen" to edtName.text.toString().trim(),
                    "ngaySinh" to edtBirthDate.text.toString().trim(),
                    "gioiTinh" to gioiTinhDuocChon
                )

                // Lưu dữ liệu lên Firestore
                db.collection("Users").document(currentUser.uid)
                    .set(userMap, SetOptions.merge()) // Thêm merge để không làm mất email
                    .addOnSuccessListener {
                        Toast.makeText(this, "Đã lưu thông tin thành công!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Lỗi khi lưu dữ liệu: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Lỗi: Bạn chưa đăng nhập!", Toast.LENGTH_SHORT).show()
            }
        }

        btnSaveTop.setOnClickListener { saveAction() }
        btnSaveBottom.setOnClickListener { saveAction() }
    }

    private fun loadUserDataFromFirebase() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("Users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val hoVaTen = document.getString("hoVaTen") ?: ""
                        val ngaySinh = document.getString("ngaySinh") ?: ""
                        val gioiTinh = document.getString("gioiTinh") ?: "Nam"

                        edtName.setText(hoVaTen)
                        edtBirthDate.setText(ngaySinh)

                        gioiTinhDuocChon = gioiTinh
                        updateGioiTinhUI()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Không thể tải dữ liệu: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
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