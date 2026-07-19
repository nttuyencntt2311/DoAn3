package com.example.doan3.ui.quiz // Cập nhật đúng tên package của bạn

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.doan3.R
import com.example.doan3.ui.profile.KhamPhaActivity
import com.example.doan3.ui.profile.RetrofitClient // Import RetrofitClient của bạn
import com.example.doan3.ui.profile.GenerateCustomQuizRequest
import com.example.doan3.ui.profile.GenerateCustomQuizResponse
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TaoQuizActivity : AppCompatActivity() {

    private lateinit var edtContent: EditText
    private lateinit var btnTaoQuiz: MaterialButton
    private lateinit var btnUpload: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tao_quiz)

        // Ánh xạ khớp ID
        edtContent = findViewById(R.id.edtContent)
        btnTaoQuiz = findViewById(R.id.btnTaoQuiz)
        btnUpload = findViewById(R.id.btnUpload)

        // Nút tải tài liệu (Tạm thời mockup chức năng mở file)
        btnUpload.setOnClickListener {
            Toast.makeText(this, "Tính năng đọc PDF/TXT đang được phát triển...", Toast.LENGTH_SHORT).show()
        }

        // Nút Tạo Quiz
        btnTaoQuiz.setOnClickListener {
            val content = edtContent.text.toString().trim()
            if (content.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập nội dung muốn học!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Giao diện loading
            btnTaoQuiz.isEnabled = false
            btnTaoQuiz.text = "AI đang xử lý..."

            // Gọi API
            val request = GenerateCustomQuizRequest(content = content, questionCount = 10)

            RetrofitClient.getInstance(this).generateCustomQuiz(request).enqueue(object : Callback<GenerateCustomQuizResponse> {
                override fun onResponse(
                    call: Call<GenerateCustomQuizResponse>,
                    response: Response<GenerateCustomQuizResponse>
                ) {
                    btnTaoQuiz.isEnabled = true
                    btnTaoQuiz.text = "Tạo Quiz ngay"

                    if (response.isSuccessful && response.body() != null) {
                        val categoryName = response.body()!!.categoryName
                        Toast.makeText(this@TaoQuizActivity, "Tạo thành công!", Toast.LENGTH_SHORT).show()

                        // Chuyển sang màn Khám Phá và truyền tên Category AI vừa tạo
                        val intent = Intent(this@TaoQuizActivity, KhamPhaActivity::class.java)
                        intent.putExtra("CATEGORY_NAME", categoryName)
                        startActivity(intent)
                        finish() // Đóng màn tạo quiz
                    } else {
                        Toast.makeText(this@TaoQuizActivity, "Lỗi từ Server", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<GenerateCustomQuizResponse>, t: Throwable) {
                    btnTaoQuiz.isEnabled = true
                    btnTaoQuiz.text = "Tạo Quiz ngay"
                    Toast.makeText(this@TaoQuizActivity, "Lỗi kết nối mạng!", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}