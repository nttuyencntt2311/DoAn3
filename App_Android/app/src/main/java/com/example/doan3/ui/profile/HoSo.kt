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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class hoso : AppCompatActivity() {

    // Khai báo các thành phần giao diện hiển thị thông tin & chỉ số
    private var tvProfileName: View? = null
    private var tvAccountType: View? = null
    private var tvHeaderStreak: View? = null
    private var tvStatLevel: View? = null
    private var tvStatVocab: View? = null
    private var tvStatDays: View? = null
    private var tvStatMinutes: View? = null

    // Khai báo các nút chức năng
    private var btnLogout: View? = null
    private var btnChangePassword: View? = null
    private var btnHelpCenter: View? = null // Đã thêm khai báo nút Trung tâm trợ giúp

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

            // ====================================================================
            // 1. ÁNH XẠ CÁC VIEW TỪ FILE XML
            // ====================================================================
            tvProfileName = findViewById(R.id.tvProfileName)
            tvAccountType = findViewById(R.id.tvAccountType)
            tvHeaderStreak = findViewById(R.id.tvHeaderStreak)
            tvStatLevel = findViewById(R.id.tvStatLevel)
            tvStatVocab = findViewById(R.id.tvStatVocab)
            tvStatDays = findViewById(R.id.tvStatDays)
            tvStatMinutes = findViewById(R.id.tvStatMinutes)

            btnLogout = findViewById(R.id.btnLogout)
            btnChangePassword = findViewById(R.id.btnChangePassword)
            btnHelpCenter = findViewById(R.id.btnHelpCenter) // Ánh xạ ID btnHelpCenter

            navTrangChu = findViewById(R.id.btnTrangChu)
            navKhamPha = findViewById(R.id.btnKhamPha)
            navTroLyAI = findViewById(R.id.btnTroLyAI)
            navHoSo = findViewById(R.id.btnHoSo)
            navTaiKhoan = findViewById(R.id.btnTaiKhoan)

            // ====================================================================
            // 2. CÀI ĐẶT CÁC LOGIC SỰ KIỆN VÀ TẢI DỮ LIỆU
            // ====================================================================
            setupBottomNavigation()
            setupUserActions()
            loadUserStats()

        } catch (e: Exception) {
            Toast.makeText(this, "Lỗi khởi tạo giao diện hồ sơ: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Xử lý các tương tác của người dùng (Đăng xuất, Đổi mật khẩu, Trợ giúp)
     */
    private fun setupUserActions() {
        // Sự kiện chuyển trang Đổi mật khẩu
        btnChangePassword?.setOnClickListener {
            val intent = Intent(this, DoiMatKhauActivity::class.java)
            startActivity(intent)
        }

        // Sự kiện chuyển trang Trung tâm trợ giúp (VỪA ĐƯỢC THÊM)
        btnHelpCenter?.setOnClickListener {
            val intent = Intent(this, HelpCenterActivity::class.java)
            startActivity(intent)
        }

        // Xử lý Đăng xuất tài khoản
        btnLogout?.setOnClickListener {
            val sharedPreferences = getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
            sharedPreferences.edit().clear().apply()

            Toast.makeText(this, "Đã đăng xuất tài khoản!", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, dangnhap::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
        tvHeaderStreak?.setOnClickListener {
            // Vì StatisticsFragment nằm trong MainActivity, ta dùng Intent truyền tín hiệu về MainActivity
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("OPEN_STATISTICS", true) // Gửi kèm tín hiệu
            startActivity(intent)
        }
    }

    /**
     * Gọi API Backend lấy chỉ số học tập thực tế và cập nhật lên UI
     */
    private fun loadUserStats() {
        RetrofitClient.getInstance(this).getUserStats().enqueue(object : Callback<UserStatsResponse> {
            override fun onResponse(call: Call<UserStatsResponse>, response: Response<UserStatsResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val stats = response.body()?.stats
                    if (stats != null) {
                        setSafeText(tvProfileName, stats.name)
                        setSafeText(tvHeaderStreak, stats.streakDays.toString())
                        setSafeText(tvStatLevel, "${stats.level}")
                        setSafeText(tvStatVocab, stats.wordsLearned.toString())
                        setSafeText(tvStatDays, stats.streakDays.toString())
                        setSafeText(tvStatMinutes, stats.timeLearnedMinutes.toString())
                    }
                } else {
                    Toast.makeText(this@hoso, "Không thể tải chỉ số hồ sơ!", Toast.LENGTH_SHORT).show()
                    setLoadingDefaultValues()
                }
            }

            override fun onFailure(call: Call<UserStatsResponse>, t: Throwable) {
                Toast.makeText(this@hoso, "Lỗi kết nối máy chủ!", Toast.LENGTH_SHORT).show()
                setLoadingDefaultValues()
            }
        })

    }

    /**
     * Hàm gán giá trị giả lập ban đầu khi API lỗi hoặc mất kết nối mạng
     */
    private fun setLoadingDefaultValues() {
        setSafeText(tvProfileName, "Học viên mới")
        setSafeText(tvHeaderStreak, "0")
        setSafeText(tvStatLevel, "0")
        setSafeText(tvStatVocab, "0")
        setSafeText(tvStatDays, "0")
        setSafeText(tvStatMinutes, "0")
    }

    /**
     * Kiểm tra an toàn trước khi gán dữ liệu chuỗi vào TextView
     */
    private fun setSafeText(view: View?, text: String) {
        (view as? TextView)?.text = text
    }

    /**
     * Thiết lập các sự kiện click chuyển màn hình cho thanh Bottom Navigation
     */
    private fun setupBottomNavigation() {
        navTrangChu?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        navKhamPha?.setOnClickListener {
            try {
                val intent = Intent(this, KhamPhaActivity::class.java)
                startActivity(intent)
                overridePendingTransition(0, 0)
            } catch (e: Exception) {
                Toast.makeText(this, "Chưa thiết lập màn hình Khám phá!", Toast.LENGTH_SHORT).show()
            }
        }

        navTroLyAI?.setOnClickListener {
            try {
                val intent = Intent(this, TroLyAIActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
                overridePendingTransition(0, 0)
            } catch (e: Exception) {
                Toast.makeText(this, "Chưa thiết lập màn hình Trợ lý AI!", Toast.LENGTH_SHORT).show()
            }
        }

        navTaiKhoan?.setOnClickListener {
            val intent = Intent(this, TaiKhoanActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

    }
}