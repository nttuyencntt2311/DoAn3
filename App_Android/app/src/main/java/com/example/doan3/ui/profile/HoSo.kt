package com.example.doan3.ui.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.doan3.MainActivity
import com.example.doan3.R
import com.example.doan3.ui.vocabulary.Vocabulary // [ĐÃ THÊM]: Import model Từ vựng
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class hoso : AppCompatActivity() {

    // CODE CHUẨN ĐI LÀM: Khai báo đúng kiểu dữ liệu TextView cho các ô hiển thị chữ
    private var tvProfileName: TextView? = null
    private var tvAccountType: TextView? = null
    private var tvHeaderStreak: TextView? = null
    private var tvStatLevel: TextView? = null
    private var tvStatVocab: TextView? = null
    private var tvStatDays: TextView? = null
    private var tvStatMinutes: TextView? = null

    // Khai báo các nút chức năng (Giữ nguyên View)
    private var btnLogout: View? = null
    private var btnChangePassword: View? = null
    private var btnHelpCenter: View? = null

    // Khai báo các nút điều hướng Bottom Navigation
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

            // Ánh xạ View chuẩn kiểu TextView
            tvProfileName = findViewById(R.id.tvProfileName)
            tvAccountType = findViewById(R.id.tvAccountType)
            tvHeaderStreak = findViewById(R.id.tvHeaderStreak)
            tvStatLevel = findViewById(R.id.tvStatLevel)
            tvStatVocab = findViewById(R.id.tvStatVocab)
            tvStatDays = findViewById(R.id.tvStatDays)
            tvStatMinutes = findViewById(R.id.tvStatMinutes)

            btnLogout = findViewById(R.id.btnLogout)
            btnChangePassword = findViewById(R.id.btnChangePassword)
            btnHelpCenter = findViewById(R.id.btnHelpCenter)

            navTrangChu = findViewById(R.id.btnTrangChu)
            navKhamPha = findViewById(R.id.btnKhamPha)
            navTroLyAI = findViewById(R.id.btnTroLyAI)
            navHoSo = findViewById(R.id.btnHoSo)
            navTaiKhoan = findViewById(R.id.btnTaiKhoan)

            setupBottomNavigation()
            setupUserActions()
            loadUserStats()

        } catch (e: Exception) {
            Toast.makeText(this, "Lỗi khởi tạo: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupUserActions() {
        btnChangePassword?.setOnClickListener {
            val intent = Intent(this, DoiMatKhauActivity::class.java)
            startActivity(intent)
        }

        btnHelpCenter?.setOnClickListener {
            val intent = Intent(this, HelpCenterActivity::class.java)
            startActivity(intent)
        }

        btnLogout?.setOnClickListener {
            val sharedPreferences = getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
            val refreshToken = sharedPreferences.getString("REFRESH_TOKEN", "") ?: ""

            if (refreshToken.isNotEmpty()) {
                val request = RefreshTokenRequest(refreshToken)
                // ĐÃ SỬA LỖI: Chuyển đổi Callback từ Void thành LogoutResponse cho đúng khớp với ApiService của Văn
                RetrofitClient.getInstance(this).logout(request)
                    .enqueue(object : Callback<LogoutResponse> {
                        override fun onResponse(call: Call<LogoutResponse>, response: Response<LogoutResponse>) {
                            thucHienDangXuatCucBo()
                        }
                        override fun onFailure(call: Call<LogoutResponse>, t: Throwable) {
                            thucHienDangXuatCucBo()
                        }
                    })
            } else {
                thucHienDangXuatCucBo()
            }
        }

        tvHeaderStreak?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("OPEN_STATISTICS", true)
            startActivity(intent)
        }
    }

    private fun thucHienDangXuatCucBo() {
        val sharedPreferences = getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()

        Toast.makeText(this, "Đã đăng xuất tài khoản!", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, dangnhap::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // =========================================================================
    // [ĐÃ SỬA]: HÀM LẤY DỮ LIỆU ĐỔ VÀO HỒ SƠ (Cập nhật đồng bộ Từ vựng & Chuỗi ngày)
    // =========================================================================
    private fun loadUserStats() {
        val apiService = RetrofitClient.getInstance(this)

        // 1. LẤY CHỈ SỐ USER (Để đồng bộ tvStatDays - Chuỗi ngày học)
        apiService.getUserStats().enqueue(object : Callback<UserStatsResponse> {
            override fun onResponse(call: Call<UserStatsResponse>, response: Response<UserStatsResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val stats = response.body()?.stats
                    if (stats != null) {
                        tvProfileName?.text = stats.name
                        tvHeaderStreak?.text = stats.streakDays.toString()
                        tvStatLevel?.text = "${stats.level}"
                        // tvStatVocab sẽ được cập nhật ở khối lệnh gọi API số 2 bên dưới
                        tvStatDays?.text = stats.streakDays.toString()
                        tvStatMinutes?.text = "Số phút học trong tuần: ${stats.timeLearnedMinutes}"
                        tvAccountType?.text = "Học viên chính thức"
                    }
                } else {
                    setLoadingDefaultValues()
                }
            }

            override fun onFailure(call: Call<UserStatsResponse>, t: Throwable) {
                setLoadingDefaultValues()
            }
        })

        // 2. LẤY DỮ LIỆU TỪ VỰNG (Để đồng bộ tvStatVocab giống hệt màn hình Learning & Statistics)
        apiService.getAllVocabularies().enqueue(object : Callback<List<Vocabulary>> {
            override fun onResponse(call: Call<List<Vocabulary>>, response: Response<List<Vocabulary>>) {
                if (response.isSuccessful) {
                    val allWords = response.body() ?: emptyList()

                    // Đếm số từ đã thành thạo (Từ có proficiency >= 80)
                    val masteredCount = allWords.count { it.proficiency >= 80 }

                    // ĐỒNG BỘ SỐ TỪ VỰNG VÀO tvStatVocab
                    tvStatVocab?.text = masteredCount.toString()
                }
            }

            override fun onFailure(call: Call<List<Vocabulary>>, t: Throwable) {
                // Nếu lỗi mạng, gán tạm bằng 0 hoặc để nguyên
                tvStatVocab?.text = "0"
            }
        })
    }

    private fun setLoadingDefaultValues() {
        tvProfileName?.text = "Học viên mới"
        tvHeaderStreak?.text = "0"
        tvStatLevel?.text = "0"
        tvStatVocab?.text = "0"
        tvStatDays?.text = "0"
        tvStatMinutes?.text = "Số phút học trong tuần: 0"
        tvAccountType?.text = "Tài khoản miễn phí"
    }

    private fun setupBottomNavigation() {
// 1. Nút Trang chủ -> Về MainActivity kèm cờ mở HomeFragment
        findViewById<View>(R.id.btnTrangChu)?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("OPEN_FRAGMENT", "HOME") // <--- CHÌA KHÓA ĐỂ SỬA LỖI HÌNH 1
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            overridePendingTransition(0, 0)
            finish()
        }

        // 2. Nút Khám phá -> Về MainActivity (Kèm lệnh mở LearningFragment)
        findViewById<View>(R.id.btnKhamPha)?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("OPEN_FRAGMENT", "LEARNING") // Tín hiệu bắt buộc mở Learning
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            overridePendingTransition(0, 0)
            finish()
        }

        // 3. Nút Trợ lý AI -> Mở TroLyAIActivity
        findViewById<View>(R.id.btnTroLyAI)?.setOnClickListener {
            // Tránh tự mở lại chính nó nếu đang ở màn hình Trợ Lý AI
            if (this !is TroLyAIActivity) {
                val intent = Intent(this, TroLyAIActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                overridePendingTransition(0, 0)
                finish()
            }
        }

        // 4. Nút Hồ sơ -> Mở hoso
        findViewById<View>(R.id.btnHoSo)?.setOnClickListener {
            if (this !is hoso) {
                val intent = Intent(this, hoso::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                overridePendingTransition(0, 0)
                finish()
            }
        }

        // 5. Nút Tài khoản -> Mở TaiKhoanActivity
        findViewById<View>(R.id.btnTaiKhoan)?.setOnClickListener {
            if (this !is TaiKhoanActivity) {
                val intent = Intent(this, TaiKhoanActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                overridePendingTransition(0, 0)
                finish()
            }
        }
    }
}