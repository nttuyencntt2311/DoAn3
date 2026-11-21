package com.example.doan3.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.doan3.R
import com.google.android.material.card.MaterialCardView

class SendEmailActivity : AppCompatActivity() {

    private var selectedTopic: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.send_email)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnSelectTopic = findViewById<MaterialCardView>(R.id.btnSelectTopic)
        val tvSelectedTopic = findViewById<TextView>(R.id.tvSelectedTopic)
        val edtSupportContent = findViewById<EditText>(R.id.edtSupportContent)
        val btnSubmitRequest = findViewById<MaterialCardView>(R.id.btnSubmitRequest)

        btnBack.setOnClickListener { finish() }

        // Điền sẵn chủ đề / nội dung nếu được chuyển từ màn Quên mật khẩu
        intent.getStringExtra("PREFILL_TOPIC")?.let {
            selectedTopic = it
            tvSelectedTopic.text = it
        }
        intent.getStringExtra("PREFILL_CONTENT")?.let {
            edtSupportContent.setText(it)
        }

        val topics = arrayOf(
            "Tài khoản & Đăng nhập",
            "Lỗi kỹ thuật",
            "Lộ trình & Bài học",
            "Góp ý tính năng",
            "Vấn đề khác"
        )

        btnSelectTopic.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Chọn vấn đề cần hỗ trợ")
                .setItems(topics) { _, which ->
                    selectedTopic = topics[which]
                    tvSelectedTopic.text = selectedTopic
                }
                .show()
        }

        btnSubmitRequest.setOnClickListener {
            val content = edtSupportContent.text.toString().trim()
            if (selectedTopic.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn chủ đề cần hỗ trợ!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (content.length < 10) {
                Toast.makeText(this, "Vui lòng mô tả chi tiết vấn đề (tối thiểu 10 ký tự)!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:support@lexivt.edu.vn")
                putExtra(Intent.EXTRA_SUBJECT, "[LexiVT Hỗ trợ] $selectedTopic")
                putExtra(Intent.EXTRA_TEXT, content)
            }

            if (emailIntent.resolveActivity(packageManager) != null) {
                startActivity(Intent.createChooser(emailIntent, "Chọn ứng dụng gửi email"))
                finish()
            } else {
                Toast.makeText(this, "Đã ghi nhận yêu cầu hỗ trợ: $selectedTopic", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
}