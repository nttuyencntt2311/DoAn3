package com.example.doan3.ui.profile

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.example.doan3.R


class DieuKhoanActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.dieukhoan)

        // Nút quay lại
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        // Khai báo các thành phần của Tab
        val tabTerms = findViewById<MaterialCardView>(R.id.tabTerms)
        val tabPrivacy = findViewById<MaterialCardView>(R.id.tabPrivacy)
        val tvTabTerms = findViewById<TextView>(R.id.tvTabTerms)
        val tvTabPrivacy = findViewById<TextView>(R.id.tvTabPrivacy)
        val icTabTerms = findViewById<ImageView>(R.id.icTabTerms)
        val icTabPrivacy = findViewById<ImageView>(R.id.icTabPrivacy)

        // Khai báo 2 khung chứa nội dung
        val layoutTermsContent = findViewById<LinearLayout>(R.id.layoutTermsContent)
        val layoutPrivacyContent = findViewById<LinearLayout>(R.id.layoutPrivacyContent)

        // Sự kiện khi bấm tab "Điều khoản sử dụng"
        tabTerms.setOnClickListener {
            // Đổi màu tab Điều khoản sang Xanh
            tabTerms.setCardBackgroundColor(Color.parseColor("#2563EB"))
            tvTabTerms.setTextColor(Color.WHITE)
            icTabTerms.setColorFilter(Color.WHITE)

            // Đổi màu tab Bảo mật sang Xám
            tabPrivacy.setCardBackgroundColor(Color.parseColor("#F8FAFC"))
            tvTabPrivacy.setTextColor(Color.parseColor("#64748B"))
            icTabPrivacy.setColorFilter(Color.parseColor("#64748B"))

            // Ẩn/Hiện nội dung
            layoutTermsContent.visibility = View.VISIBLE
            layoutPrivacyContent.visibility = View.GONE
        }

        // Sự kiện khi bấm tab "Chính sách bảo mật"
        tabPrivacy.setOnClickListener {
            // Đổi màu tab Bảo mật sang Xanh
            tabPrivacy.setCardBackgroundColor(Color.parseColor("#2563EB"))
            tvTabPrivacy.setTextColor(Color.WHITE)
            icTabPrivacy.setColorFilter(Color.WHITE)

            // Đổi màu tab Điều khoản sang Xám
            tabTerms.setCardBackgroundColor(Color.parseColor("#F8FAFC"))
            tvTabTerms.setTextColor(Color.parseColor("#64748B"))
            icTabTerms.setColorFilter(Color.parseColor("#64748B"))

            // Ẩn/Hiện nội dung
            layoutPrivacyContent.visibility = View.VISIBLE
            layoutTermsContent.visibility = View.GONE
        }
    }
}