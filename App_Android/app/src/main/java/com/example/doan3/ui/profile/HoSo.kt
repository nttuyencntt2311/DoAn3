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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class hoso : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Đổi toàn bộ sang kiểu View? gốc để KHÔNG BAO GIỜ bị sập do lệch kiểu dữ liệu (ClassCastException)
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

        // Bọc toàn bộ onCreate vào try - catch để hứng lỗi, quyết không cho app bị văng
        try {
            enableEdgeToEdge()
            setContentView(R.layout.hoso)

            auth = FirebaseAuth.getInstance()
            db = FirebaseFirestore.getInstance()

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

            // Cài đặt sự kiện click
            setupBottomNavigation()

            btnSettings?.setOnClickListener {
                startActivity(Intent(this, TaiKhoanActivity::class.java))
            }

            btnLogout?.setOnClickListener {
                auth.signOut()
                Toast.makeText(this, "Đã đăng xuất tài khoản!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, dangnhap::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }

            // Gọi hàm lấy dữ liệu Firebase
            loadUserStatsRealtime()

        } catch (e: Exception) {
            // NẾU CÓ LỖI XẢY RA (Ví dụ lỗi giao diện XML), APP SẼ HIỆN TOAST THÔNG BÁO CHỨ KHÔNG BỊ THOÁT
            Toast.makeText(this, "Lỗi khởi tạo màn hình: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun loadUserStatsRealtime() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            setDefaultStatsUI()
            return
        }

        db.collection("Users").document(currentUser.uid)
            .addSnapshotListener { document, error ->
                if (error != null) {
                    setDefaultStatsUI()
                    return@addSnapshotListener
                }

                if (document == null || !document.exists()) {
                    setDefaultStatsUI()
                } else {
                    val hoVaTen = document.getString("hoVaTen")
                    setSafeText(tvProfileName, if (!hoVaTen.isNullOrEmpty()) hoVaTen else "Người dùng LexiVT")

                    val accountType = document.getString("accountType")
                    setSafeText(tvAccountType, if (!accountType.isNullOrEmpty()) accountType else "Học viên")

                    val streak = document.getLong("streak") ?: 0L
                    setSafeText(tvHeaderStreak, "$streak ngày")

                    val level = document.getLong("level") ?: 1L
                    setSafeText(tvStatLevel, "Cấp $level")

                    val vocabCount = document.getLong("vocabCount") ?: 0L
                    setSafeText(tvStatVocab, "$vocabCount từ")

                    val learningDays = document.getLong("learningDays") ?: 0L
                    setSafeText(tvStatDays, "$learningDays ngày")

                    val weeklyMinutes = document.getLong("weeklyMinutes") ?: 0L
                    setSafeText(tvStatMinutes, "$weeklyMinutes phút")
                }
            }
    }

    private fun setDefaultStatsUI() {
        setSafeText(tvProfileName, "Người dùng")
        setSafeText(tvAccountType, "Học viên")
        setSafeText(tvHeaderStreak, "0 ngày")
        setSafeText(tvStatLevel, "1")
        setSafeText(tvStatVocab, "0")
        setSafeText(tvStatDays, "0")
        setSafeText(tvStatMinutes, "0")
    }

    // Hàm bổ trợ: Chỉ ép kiểu sang TextView khi thực hiện gán text để tránh ClassCastException
    private fun setSafeText(view: View?, text: String) {
        (view as? TextView)?.text = text
    }

    private fun setupBottomNavigation() {
        navTrangChu?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
        navKhamPha?.setOnClickListener {
            startActivity(Intent(this, KhamPhaActivity::class.java))
            overridePendingTransition(0, 0)
        }
        navTroLyAI?.setOnClickListener {
            val intent = Intent(this, TroLyAIActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
            overridePendingTransition(0, 0)
        }
        navTaiKhoan?.setOnClickListener {
            startActivity(Intent(this, TaiKhoanActivity::class.java))
            overridePendingTransition(0, 0)
        }
    }
}