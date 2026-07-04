package com.example.doan3.ui.profile

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.example.doan3.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.dangky)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Nút lùi về trang đăng nhập
        val tvLogin = findViewById<TextView>(R.id.tvLogin)
        tvLogin.setOnClickListener { finish() }

        // Ánh xạ các UI từ dangky.xml
        val edtName = findViewById<TextInputEditText>(R.id.edtName)
        val edtEmail = findViewById<TextInputEditText>(R.id.edtEmail)
        val edtPassword = findViewById<TextInputEditText>(R.id.edtPassword)
        val edtConfirmPassword = findViewById<TextInputEditText>(R.id.edtConfirmPassword)
        val btnRegister = findViewById<MaterialButton>(R.id.btnRegister)

        // Xử lý nút Đăng ký
        btnRegister.setOnClickListener {
            val name = edtName.text.toString().trim()
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString().trim()
            val confirmPassword = edtConfirmPassword.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Mật khẩu xác nhận không khớp!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Mật khẩu phải từ 6 ký tự trở lên!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Gọi Firebase tạo tài khoản
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val uid = auth.currentUser?.uid
                        val userMap = hashMapOf(
                            "hoVaTen" to name,
                            "email" to email,
                            "gioiTinh" to "Nam",
                            "ngaySinh" to ""
                        )

                        if (uid != null) {
                            db.collection("Users").document(uid).set(userMap)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show()
                                    finish() // Quay về trang đăng nhập
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Lỗi tạo dữ liệu: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        Toast.makeText(this, "Đăng ký thất bại: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }
}