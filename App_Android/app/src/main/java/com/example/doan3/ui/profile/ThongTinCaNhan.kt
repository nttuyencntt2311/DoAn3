package com.example.doan3.ui.profile

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.example.doan3.R
// Thêm 2 thư viện này để dùng Firebase Auth và Firestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.material.textfield.TextInputEditText

class ThongTinCaNhanActivity : AppCompatActivity() {

    // Khai báo biến
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Biến lưu trữ giới tính mặc định là Nam
    private var gioiTinhDuocChon = "Nam"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.thongtincanhan)

        // Khởi tạo Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // 1. Nút quay lại
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        // ==============================================================
        // PHẦN 1: ÁNH XẠ CÁC NÚT VÀ Ô NHẬP LIỆU
        // ==============================================================
        // (BẠN CẦN MỞ FILE thongtincanhan.xml LÊN, TÌM ID CỦA Ô NHẬP TÊN, NGÀY SINH... ĐỂ THAY VÀO DƯỚI ĐÂY)
        // VÍ DỤ TÔI TẠM ĐẶT LÀ edtName, edtNgaySinh. Bạn hãy bỏ dấu // ở đầu dòng nếu muốn dùng:

        // val edtName = findViewById<TextInputEditText>(R.id.edtName)
        // val edtNgaySinh = findViewById<TextInputEditText>(R.id.edtNgaySinh)

        // Các nút Giới tính (đã đúng với code cũ của bạn)
        val btnMale = findViewById<MaterialCardView>(R.id.btnMale)
        val btnFemale = findViewById<MaterialCardView>(R.id.btnFemale)
        val tvMale = findViewById<TextView>(R.id.tvMale)
        val tvFemale = findViewById<TextView>(R.id.tvFemale)


        // ==============================================================
        // PHẦN 2: TỰ ĐỘNG TẢI DỮ LIỆU TỪ MẠNG XUỐNG KHI VỪA MỞ TRANG
        // ==============================================================
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Dùng UID để chui vào bảng Users, tìm đúng tờ giấy của user này
            db.collection("Users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    // Nếu tìm thấy dữ liệu cũ trên mạng
                    if (document != null && document.exists()) {

                        // Lấy chữ và điền vào ô (Bỏ dấu // nếu muốn dùng)
                        // edtName.setText(document.getString("hoVaTen"))
                        // edtNgaySinh.setText(document.getString("ngaySinh"))

                        // Lấy giới tính và tự động kích hoạt nút màu xanh
                        val gioiTinhCu = document.getString("gioiTinh")
                        if (gioiTinhCu == "Nữ") {
                            btnFemale.performClick()
                        } else {
                            btnMale.performClick()
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show()
                }
        }


        // ==============================================================
        // PHẦN 3: XỬ LÝ ĐỔI MÀU NÚT GIỚI TÍNH
        // ==============================================================
        btnMale.setOnClickListener {
            gioiTinhDuocChon = "Nam" // Lưu lại sự lựa chọn

            btnMale.strokeColor = android.graphics.Color.parseColor("#2563EB")
            btnMale.strokeWidth = 4
            tvMale.setTextColor(android.graphics.Color.parseColor("#2563EB"))

            btnFemale.strokeColor = android.graphics.Color.parseColor("#CBD5E1")
            btnFemale.strokeWidth = 2
            tvFemale.setTextColor(android.graphics.Color.parseColor("#64748B"))
        }

        btnFemale.setOnClickListener {
            gioiTinhDuocChon = "Nữ" // Lưu lại sự lựa chọn

            btnFemale.strokeColor = android.graphics.Color.parseColor("#2563EB")
            btnFemale.strokeWidth = 4
            tvFemale.setTextColor(android.graphics.Color.parseColor("#2563EB"))

            btnMale.strokeColor = android.graphics.Color.parseColor("#CBD5E1")
            btnMale.strokeWidth = 2
            tvMale.setTextColor(android.graphics.Color.parseColor("#64748B"))
        }


        // ==============================================================
        // PHẦN 4: NÚT "LƯU THAY ĐỔI" - ĐẨY DỮ LIỆU LÊN MẠNG
        // ==============================================================
        val btnSaveTop = findViewById<TextView>(R.id.btnSaveTop)
        val btnSaveBottom = findViewById<MaterialCardView>(R.id.btnSaveBottom)

        val saveAction = {
            if (currentUser != null) {
                // 1. Gom tất cả dữ liệu thành một gói (HashMap)
                val userMap = hashMapOf(
                    // "hoVaTen" to edtName.text.toString().trim(),
                    // "ngaySinh" to edtNgaySinh.text.toString().trim(),
                    "gioiTinh" to gioiTinhDuocChon
                )

                // 2. Gửi gói dữ liệu lên Cloud Firestore
                db.collection("Users").document(currentUser.uid)
                    .set(userMap)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Đã đồng bộ lên Cloud thành công!", Toast.LENGTH_SHORT).show()
                        finish() // Lưu xong thì tắt trang
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Lỗi khi lưu: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Vui lòng đăng nhập trước!", Toast.LENGTH_SHORT).show()
            }
        }

        btnSaveTop.setOnClickListener { saveAction() }
        btnSaveBottom.setOnClickListener { saveAction() }
    }
}