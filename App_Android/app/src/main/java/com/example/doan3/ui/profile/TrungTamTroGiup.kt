package com.example.doan3.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.doan3.R


class HelpCenterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.trungtamtrogiup)

        // 1. Nút quay lại
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        // 2. Mở trang Gửi Email
        findViewById<View>(R.id.btnEmailSupport).setOnClickListener {
            startActivity(Intent(this, SendEmailActivity::class.java))
        }

        // 3. Mở Link Facebook
        findViewById<View>(R.id.btnCommunity).setOnClickListener {
            val facebookGroupUrl = "https://www.facebook.com/groups/tuvungtienganh"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(facebookGroupUrl)
            startActivity(intent)
        }

        // 4. MỞ TRANG TÀI KHOẢN (Vừa thêm)
        findViewById<View>(R.id.btnAccountSupport).setOnClickListener {
            startActivity(Intent(this, TaiKhoanActivity::class.java))
        }
    }
}