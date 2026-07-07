package com.example.doan3.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.doan3.MainActivity
import com.example.doan3.R

class hoso : AppCompatActivity() {

    private var tvProfileName: View? = null
    private var tvAccountType: View? = null
    private var tvHeaderStreak: View? = null
    private var tvStatLevel: View? = null
    private var tvStatVocab: View? = null
    private var tvStatDays: View? = null
    private var tvStatMinutes: View? = null

    private var btnLogout: View? = null
    private var btnSettings: View? = null

    private var navTrangChu: View? = null
    private var navKhamPha: View? = null
    private var navTroLyAI: View? = null
    private var navHoSo: View? = null
    private var navTaiKhoan: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            enableEdgeToEdge()
            setContentView(R.layout.hoso)

            // Ánh xạ an toàn dưới dạng View chung
            tvProfileName = findViewById(R.id.tvProfileName)
            tvAccountType = findViewById(R.id.tvAccountType)
            tvHeaderStreak = findViewById(R.id.tvHeaderStreak)
            tvStatLevel = findViewById(R.id.tvStatLevel)
            tvStatVocab = findViewById(R.id.tvStatVocab)
            tvStatDays = findViewById(R.id.tvStatDays)
            tvStatMinutes = findViewById(R.id.tvStatMinutes)

            btnLogout = findViewById(R.id.btnLogout)
            btnSettings = findViewById(R.id.btnSettings)

            navTrangChu = findViewById(R.id.btnTrangChu)
            navKhamPha = findViewById(R.id.btnKhamPha)
            navTroLyAI = findViewById(R.id.btnTroLyAI)
            navHoSo = findViewById(R.id.btnHoSo)
            navTaiKhoan = findViewById(R.id.btnTaiKhoan)

            // Cài đặt sự kiện chuyển màn hình cho thanh Menu Bottom
            setupBottomNavigation()

            // Nút cài đặt chuyển sang trang Tài Khoản
            btnSettings?.setOnClickListener {
                startActivity(Intent(this, TaiKhoanActivity::class.java))
            }

            btnLogout?.setOnClickListener {
                Toast.makeText(this, "Đã đăng xuất tài khoản!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, dangnhap::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }

            // Gọi hàm lấy dữ liệu từ hệ thống API mới
            loadUserStatsFromAPI()

        } catch (e: Exception) {
            Toast.makeText(this, "Lỗi khởi tạo màn hình: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserStatsFromAPI()
    }

    private fun loadUserStatsFromAPI() {
        // Hiện tại kích hoạt UI hiển thị dữ liệu mặc định an toàn để không văng App
        setDefaultStatsUI()
    }

    private fun setDefaultStatsUI() {
        setSafeText(tvProfileName, "Người dùng LexiVT")
        setSafeText(tvAccountType, "Học viên")
        setSafeText(tvHeaderStreak, "0 ngày")
        setSafeText(tvStatLevel, "Cấp 1")
        setSafeText(tvStatVocab, "0 từ")
        setSafeText(tvStatDays, "0 ngày")
        setSafeText(tvStatMinutes, "0 phút")
    }

    private fun setSafeText(view: View?, text: String) {
        (view as? TextView)?.text = text
    }

    private fun setupBottomNavigation() {
        // 1. Nút Trang chủ -> Về MainActivity
        navTrangChu?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        // 2. Nút Khám phá -> KHÔI PHỤC: Mở KhamPhaActivity (Code của Tuyền)
        navKhamPha?.setOnClickListener {
            val intent = Intent(this, KhamPhaActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        // 3. Nút Trợ lý AI -> KHÔI PHỤC: Mở TroLyAIActivity (Code của Vân)
        navTroLyAI?.setOnClickListener {
            val intent = Intent(this, TroLyAIActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        // 4. Nút Tài khoản -> Mở TaiKhoanActivity
        navTaiKhoan?.setOnClickListener {
            val intent = Intent(this, TaiKhoanActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
    }
}