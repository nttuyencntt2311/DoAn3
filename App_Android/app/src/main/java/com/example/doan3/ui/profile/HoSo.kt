package com.example.doan3.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.example.doan3.MainActivity
import com.example.doan3.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class hoso : AppCompatActivity() {

    private lateinit var tvProfileName: TextView
    private lateinit var tvAccountType: TextView
    private lateinit var tvHeaderStreak: TextView
    private lateinit var tvStatLevel: TextView
    private lateinit var tvStatVocab: TextView
    private lateinit var tvStatDays: TextView

    private lateinit var btnUpgradePro: RelativeLayout
    private lateinit var btnChangePassword: RelativeLayout
    private lateinit var switchNotifications: SwitchCompat
    private lateinit var btnHelpCenter: RelativeLayout
    private lateinit var btnLogout: RelativeLayout
    private lateinit var btnSettings: View

    private lateinit var navTrangChu: LinearLayout
    private lateinit var navKhamPha: LinearLayout
    private lateinit var navTroLyAI: LinearLayout
    private lateinit var navHoSo: LinearLayout
    private lateinit var navTaiKhoan: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.hoso)

        anhXaGiaoDien()
        loadDataFromApi()
        setupListeners()
        setupBottomNavigation()

        // =========================================================
        // CODE ĐỒNG BỘ: TỰ ĐỘNG TẢI DỮ LIỆU TỪ FIRESTORE VỀ APP
        // =========================================================
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val currentUser = auth.currentUser

        if (currentUser != null) {
            // Nếu đã đăng nhập, lấy UID của người này để vào đúng "Ngăn kéo" của họ
            db.collection("Users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // Kéo thông tin từ Firestore xuống
                        val tenTrenMang = document.getString("hoVaTen") ?: "Người dùng mới"
                        // val gioiTinh = document.getString("gioiTinh")

                        // Cập nhật lên giao diện (Ní nhớ check lại ID của TextView tên người dùng nhé, tui đang lấy ví dụ là tvProfileName)
                        tvProfileName.text = tenTrenMang

                    } else {
                        // Nếu tài khoản mới tạo, chưa có dữ liệu trên Firestore
                        tvProfileName.text = "Chưa cập nhật tên"
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Lỗi khi đồng bộ dữ liệu!", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Nếu chưa đăng nhập mà lọt vào đây thì đá về trang Đăng nhập
            val intent = Intent(this, dangnhap::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun anhXaGiaoDien() {
        tvProfileName = findViewById(R.id.tvProfileName)
        tvAccountType = findViewById(R.id.tvAccountType)
        tvHeaderStreak = findViewById(R.id.tvHeaderStreak)
        tvStatLevel = findViewById(R.id.tvStatLevel)
        tvStatVocab = findViewById(R.id.tvStatVocab)
        tvStatDays = findViewById(R.id.tvStatDays)

//        btnUpgradePro = findViewById(R.id.btnUpgradePro)
        btnChangePassword = findViewById(R.id.btnChangePassword)
        switchNotifications = findViewById(R.id.switchNotifications)
        btnHelpCenter = findViewById(R.id.btnHelpCenter)
        btnLogout = findViewById(R.id.btnLogout)
        btnSettings = findViewById(R.id.btnSettings)

        navTrangChu = findViewById(R.id.btnTrangChu)
        navKhamPha = findViewById(R.id.btnKhamPha)
        navTroLyAI = findViewById(R.id.btnTroLyAI)
        navHoSo = findViewById(R.id.btnHoSo)
        navTaiKhoan = findViewById(R.id.btnTaiKhoan)
    }

    private fun loadDataFromApi() {
        val mockName = "Phan Le Tu Van"
        val mockRole = "Học viên"
        val mockStreak = 5
        val mockLevel = 12
        val mockVocab = "2.4k"
        val mockDays = 45

        tvProfileName.text = mockName
        tvAccountType.text = mockRole
        tvHeaderStreak.text = "$mockStreak 🔥"
        tvStatLevel.text = mockLevel.toString()
        tvStatVocab.text = mockVocab
        tvStatDays.text = mockDays.toString()
    }

    private fun setupListeners() {
//        btnUpgradePro.setOnClickListener {
//            Toast.makeText(this, "Chuyển sang trang thanh toán PRO", Toast.LENGTH_SHORT).show()
//        }

        // ==========================================
        // ĐÃ SỬA CHỖ NÀY: MỞ TRANG ĐỔI MẬT KHẨU
        // ==========================================
        btnChangePassword.setOnClickListener {
            val intent = Intent(this, DoiMatKhauActivity::class.java)
            startActivity(intent)
        }

        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            val msg = if (isChecked) "Đã bật nhắc nhở học tập" else "Đã tắt nhắc nhở"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
        btnHelpCenter.setOnClickListener {
            startActivity(Intent(this, HelpCenterActivity::class.java))
        }
        btnSettings.setOnClickListener {
            startActivity(Intent(this, TaiKhoanActivity::class.java))
        }
        btnLogout.setOnClickListener {
            Toast.makeText(this, "Đã đăng xuất tài khoản!", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, dangnhap::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun setupBottomNavigation() {
        navTrangChu.setOnClickListener {
            // Khởi tạo Intent để gọi về MainActivity (Trang chủ)
            val intent = Intent(this, MainActivity::class.java)

            // Dùng cờ này để nếu MainActivity đang chạy ở dưới thì gọi nó lên luôn,
            // tránh việc tạo ra nhiều màn hình MainActivity xếp chồng lên nhau gây nặng máy
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

            startActivity(intent)

            // Tắt hiệu ứng trượt màn hình mặc định để cảm giác chuyển Tab được mượt mà hơn
            overridePendingTransition(0, 0)
        }
        navKhamPha.setOnClickListener {
            startActivity(Intent(this, KhamPhaActivity::class.java))
            overridePendingTransition(0, 0)
        }
        navTroLyAI.setOnClickListener {
            // Chuyển hướng sang TroLyAIActivity
            val intent = Intent(this, TroLyAIActivity::class.java)

            // Sử dụng cờ REORDER_TO_FRONT để chuyển tab mượt mà, tránh mở đè nhiều màn hình giống nhau
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT

            startActivity(intent)

            // Tắt hiệu ứng trượt để trải nghiệm giống thanh điều hướng Bottom Navigation thực tế
            overridePendingTransition(0, 0)
        }

        navHoSo.setOnClickListener {
            // Đang ở Hồ sơ nên không làm gì
        }
        navTaiKhoan.setOnClickListener {
            startActivity(Intent(this, TaiKhoanActivity::class.java))
            overridePendingTransition(0, 0)
        }
    }
}