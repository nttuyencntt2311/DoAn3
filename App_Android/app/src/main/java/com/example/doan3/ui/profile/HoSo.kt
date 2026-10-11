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
import com.example.doan3.ui.vocabulary.Vocabulary
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.appcompat.widget.SwitchCompat // Cần import thêm class này cho Switch
import android.content.res.ColorStateList
import android.graphics.Color

class hoso : AppCompatActivity() {

    // Biến lưu tên người dùng hiện tại để truyền sang Adapter Bảng xếp hạng
    private var myUserName: String = ""

    // Khai báo đúng kiểu dữ liệu TextView cho các ô hiển thị chữ
    private var tvProfileName: TextView? = null
    private var tvAccountType: TextView? = null
    private var tvHeaderStreak: TextView? = null

    private var rvLeaderboard: RecyclerView? = null
    private var tvStatLevel: TextView? = null
    private var tvStatVocab: TextView? = null
    private var tvStatDays: TextView? = null
    private var tvStatMinutes: TextView? = null

    // Khai báo các nút chức năng
    private var btnLogout: View? = null
    private var btnChangePassword: View? = null
    private var btnHelpCenter: View? = null

    // 🚀 BỔ SUNG KHAI BÁO SWITCH CHO RADAR XAI
    private var switchPacingAlert: SwitchCompat? = null

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

            // Khởi tạo Bảng xếp hạng
            rvLeaderboard = findViewById(R.id.rvLeaderboard)
            rvLeaderboard?.layoutManager = LinearLayoutManager(this)

            // Ánh xạ View chuẩn kiểu TextView
            tvProfileName = findViewById(R.id.tvProfileName)
            tvAccountType = findViewById(R.id.tvAccountType)
            tvHeaderStreak = findViewById(R.id.tvHeaderStreak)
            tvStatLevel = findViewById(R.id.tvStatLevel)
            tvStatVocab = findViewById(R.id.tvStatVocab)
            tvStatDays = findViewById(R.id.tvStatDays)

            btnLogout = findViewById(R.id.btnLogout)
            btnChangePassword = findViewById(R.id.btnChangePassword)
            btnHelpCenter = findViewById(R.id.btnHelpCenter)

            // 🚀 ÁNH XẠ SWITCH RADAR XAI TỪ GIAO DIỆN
            switchPacingAlert = findViewById(R.id.switchPacingAlert)

            navTrangChu = findViewById(R.id.btnTrangChu)
            navKhamPha = findViewById(R.id.btnKhamPha)
            navTroLyAI = findViewById(R.id.btnTroLyAI)
            navHoSo = findViewById(R.id.btnHoSo)
            navTaiKhoan = findViewById(R.id.btnTaiKhoan)

            setupBottomNavigation()
            setupUserActions()
            loadUserStats()

            // 🚀 GỌI HÀM CÀI ĐẶT RADAR XAI
            setupPacingAlertSwitch()

        } catch (e: Exception) {
            Toast.makeText(this, "Lỗi khởi tạo: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }


    // =========================================================================
    // 🚀 XỬ LÝ NÚT GẠT CÀI ĐẶT RADAR XAI (ĐÃ SỬA LỖI MÀU SẮC)
    // =========================================================================
    private fun setupPacingAlertSwitch() {
        val prefs = getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
        val isAlertEnabled = prefs.getBoolean("PREF_PACING_ALERT", true)

        switchPacingAlert?.isChecked = isAlertEnabled

        // Cập nhật màu đúng với trạng thái lúc vừa mở màn hình
        updateRadarSwitchColor(isAlertEnabled)

        switchPacingAlert?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("PREF_PACING_ALERT", isChecked).apply()

            // Tự động đổi màu (Cam/Xám) mỗi khi người dùng chạm vào
            updateRadarSwitchColor(isChecked)

            val status = if (isChecked) "Đã bật" else "Đã tắt"
            Toast.makeText(this, "$status nhắc nhở thời gian Radar XAI", Toast.LENGTH_SHORT).show()
        }
    }

    // Hàm phụ trợ giúp đổi màu nền của Switch
    private fun updateRadarSwitchColor(isChecked: Boolean) {
        if (isChecked) {
            // Khi BẬT: Nền màu cam
            switchPacingAlert?.trackTintList = ColorStateList.valueOf(Color.parseColor("#D97706"))
        } else {
            // Khi TẮT: Nền màu xám nhạt
            switchPacingAlert?.trackTintList = ColorStateList.valueOf(Color.parseColor("#E2E8F0"))
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
    // HÀM LẤY DỮ LIỆU ĐỔ VÀO HỒ SƠ VÀ BẢNG XẾP HẠNG
    // =========================================================================
    private fun loadUserStats() {
        val apiService = RetrofitClient.getInstance(this)

        // 1. LẤY CHỈ SỐ USER
        apiService.getUserStats().enqueue(object : Callback<UserStatsResponse> {
            override fun onResponse(call: Call<UserStatsResponse>, response: Response<UserStatsResponse>) {
                val stats = response.body()?.stats // ĐÃ SỬA: Lấy object stats từ response

                if (stats != null) {
                    // LƯU LẠI TÊN ĐỂ LÁT NỮA NÉM VÀO BẢNG XẾP HẠNG
                    myUserName = stats.name

                    tvProfileName?.text = stats.name
                    tvHeaderStreak?.text = stats.streakDays.toString()
                    tvStatLevel?.text = "${stats.level}"
                    tvStatDays?.text = stats.streakDays.toString()

                    // Khoe luôn XP ra màn hình
                    tvStatMinutes?.text = "🔥 Điểm kinh nghiệm: ${stats.xp} XP"

                    // CẬP NHẬT DANH HIỆU DỰA VÀO CẤP ĐỘ
                    val danhHieu = when (stats.level) {
                        in 1..3 -> "🌱 Tân binh ngôn ngữ"
                        in 4..9 -> "🔥 Kẻ Phá Vỡ Giới Hạn"
                        in 10..19 -> "💎 Học giả ưu tú"
                        in 20..49 -> "⚡ Bậc thầy giao tiếp"
                        else -> "👑 Thần đồng ngoại ngữ"
                    }

                    tvAccountType?.text = danhHieu
                    tvAccountType?.setTextColor(android.graphics.Color.parseColor("#F59E0B"))
                }
            }

            override fun onFailure(call: Call<UserStatsResponse>, t: Throwable) {
                setLoadingDefaultValues()
            }
        })

        // 2. LẤY DỮ LIỆU TỪ VỰNG
        apiService.getAllVocabularies().enqueue(object : Callback<List<Vocabulary>> {
            override fun onResponse(call: Call<List<Vocabulary>>, response: Response<List<Vocabulary>>) {
                if (response.isSuccessful) {
                    val allWords = response.body() ?: emptyList()
                    val masteredCount = allWords.count { it.proficiency >= 80 }
                    tvStatVocab?.text = masteredCount.toString()
                }
            }

            override fun onFailure(call: Call<List<Vocabulary>>, t: Throwable) {
                tvStatVocab?.text = "0"
            }
        })

        // 3. LẤY BẢNG XẾP HẠNG (LEADERBOARD)
        apiService.getLeaderboard().enqueue(object : Callback<List<LeaderboardUser>> {
            override fun onResponse(call: Call<List<LeaderboardUser>>, response: Response<List<LeaderboardUser>>) {
                if (response.isSuccessful && response.body() != null) {
                    val leaderboardList = response.body()!!

                    if (leaderboardList.isEmpty()) {
                        Toast.makeText(this@hoso, "Bảng xếp hạng đang trống!", Toast.LENGTH_SHORT).show()
                    } else {
                        val adapter = LeaderboardAdapter(leaderboardList, myUserName)
                        rvLeaderboard?.adapter = adapter
                    }
                } else {
                    // Hiện lỗi nếu API Backend sai (VD: 404, 500)
                    Toast.makeText(this@hoso, "Lỗi API Bảng xếp hạng: ${response.code()}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<List<LeaderboardUser>>, t: Throwable) {
                // Hiện thông báo nếu mất mạng hoặc sập Server Node.js
                Toast.makeText(this@hoso, "Lỗi kết nối BXH: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun setLoadingDefaultValues() {
        tvProfileName?.text = "Học viên mới"
        tvHeaderStreak?.text = "0"
        tvStatLevel?.text = "0"
        tvStatVocab?.text = "0"
        tvStatDays?.text = "0"
        tvStatMinutes?.text = "🔥 Điểm kinh nghiệm: 0 XP"
        tvAccountType?.text = "🌱 Tân binh ngôn ngữ"
    }

    private fun setupBottomNavigation() {
        findViewById<View>(R.id.btnTrangChu)?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("OPEN_FRAGMENT", "HOME")
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            overridePendingTransition(0, 0)
            finish()
        }

        findViewById<View>(R.id.btnKhamPha)?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("OPEN_FRAGMENT", "LEARNING")
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            overridePendingTransition(0, 0)
            finish()
        }

        findViewById<View>(R.id.btnTroLyAI)?.setOnClickListener {
            if (this !is TroLyAIActivity) {
                val intent = Intent(this, TroLyAIActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                overridePendingTransition(0, 0)
                finish()
            }
        }

        findViewById<View>(R.id.btnHoSo)?.setOnClickListener {
            if (this !is hoso) {
                val intent = Intent(this, hoso::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
                overridePendingTransition(0, 0)
                finish()
            }
        }

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