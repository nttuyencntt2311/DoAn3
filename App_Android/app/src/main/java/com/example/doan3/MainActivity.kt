package com.example.doan3

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.doan3.databinding.ActivityMainBinding
import com.example.doan3.ui.home.HomeFragment
import com.example.doan3.ui.profile.hoso
import com.example.doan3.ui.profile.KhamPhaActivity
import com.example.doan3.ui.profile.TroLyAIActivity
import com.example.doan3.ui.profile.TaoQuizActivity
import com.example.doan3.ui.profile.TaiKhoanActivity
import com.example.doan3.ui.learning.LearningFragment
import com.example.doan3.ui.statistics.StatisticsFragment
import com.example.doan3.ui.vocabulary.VocabularyFragment
import com.example.doan3.ui.roadmap.RoadmapFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Mặc định ban đầu hiển thị trang HomeFragment và bôi xanh nút Trang chủ
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
            updateBottomNavUI(binding.btnTrangChu)
        }

        // 1. Nút Trang chủ
        binding.btnTrangChu.setOnClickListener {
            replaceFragment(HomeFragment())
            updateBottomNavUI(binding.btnTrangChu)
        }

        // 2. Nút Khám phá
        binding.btnKhamPha.setOnClickListener {
            updateBottomNavUI(binding.btnKhamPha)
            replaceFragment(LearningFragment())
        }

        // 3. Nút Trợ lý AI
        binding.btnTroLyAI.setOnClickListener {
            updateBottomNavUI(binding.btnTroLyAI)
            val intent = Intent(this, TroLyAIActivity::class.java)
            startActivity(intent)
        }

        // 4. Nút Hồ sơ
        binding.btnHoSo.setOnClickListener {
            updateBottomNavUI(binding.btnHoSo)
            val intent = Intent(this, hoso::class.java)
            startActivity(intent)
        }

        // 5. Nút Tài khoản
        binding.btnTaiKhoan.setOnClickListener {
            updateBottomNavUI(binding.btnTaiKhoan)
            val intent = Intent(this, TaiKhoanActivity::class.java)
            startActivity(intent)
        }

        // ĐÃ XÓA ĐOẠN CODE LỖI `cardLoTrinhHoc` Ở ĐÂY VÌ NÓ THUỘC VỀ TroLyAIActivity

        // Nhận tín hiệu từ màn hình Hồ sơ gửi về
        val openStatistics = intent.getBooleanExtra("OPEN_STATISTICS", false)
        if (openStatistics) {
            // Tự động chuyển đổi giao diện sang màn hình Thống kê
            supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, StatisticsFragment())
                .commit()
            // Tạm thời nếu mở Thống kê thì bôi xanh nút Hồ sơ
            updateBottomNavUI(binding.btnHoSo)
        }

        // ====================================================================
        // ĐOẠN MỚI THÊM: Đón tín hiệu ngay khi MainActivity vừa khởi tạo
        // ====================================================================
        handleIntentData(intent)
    }

    // ====================================================================
    // HÀM MỚI: Xử lý khi MainActivity đang chạy ngầm và được gọi lên lại
    // ====================================================================
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntentData(intent)
    }

    // ====================================================================
    // HÀM MỚI: Đọc tín hiệu OPEN_FRAGMENT và mở đúng Fragment
    // ====================================================================
    private fun handleIntentData(intent: Intent) {
        val openFragment = intent.getStringExtra("OPEN_FRAGMENT")
        if (openFragment != null) {
            when (openFragment) {
                "VOCABULARY" -> {
                    replaceFragment(VocabularyFragment())
                    updateBottomNavUI(binding.btnKhamPha)
                }
                "LEARNING" -> {
                    replaceFragment(LearningFragment())
                    updateBottomNavUI(binding.btnKhamPha)
                }
                "ROADMAP" -> {
                    replaceFragment(RoadmapFragment())
                    updateBottomNavUI(binding.btnKhamPha)
                }
            }
            // Xóa dữ liệu sau khi mở xong để tránh lỗi khi màn hình xoay
            intent.removeExtra("OPEN_FRAGMENT")
        }
    }

    // Hàm hỗ trợ đổi Fragment mượt mà trong Activity
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }

    // ====================================================================
    // ĐÂY LÀ HÀM MỚI ĐỂ THAY ĐỔI GIAO DIỆN NÚT (TÔ XANH / TẨY XÁM)
    // ====================================================================
    private fun updateBottomNavUI(activeTab: LinearLayout) {
        // Gom 5 nút vào một danh sách
        val tabs = listOf(
            binding.btnTrangChu,
            binding.btnKhamPha,
            binding.btnTroLyAI,
            binding.btnHoSo,
            binding.btnTaiKhoan
        )

        // Định nghĩa mã màu (xanh dương đậm và xám)
        val activeColor = Color.parseColor("#0056D2")
        val inactiveColor = Color.parseColor("#777777")

        for (tab in tabs) {
            // Lấy view icon (vị trí 0) và view chữ (vị trí 1)
            val icon = tab.getChildAt(0) as? ImageView
            val text = tab.getChildAt(1) as? TextView

            if (tab == activeTab) {
                // NẾU LÀ NÚT VỪA ĐƯỢC BẤM:
                tab.setBackgroundResource(R.drawable.bg_tab_active)
                icon?.setColorFilter(activeColor)
                text?.setTextColor(activeColor)
            } else {
                // CÁC NÚT CÒN LẠI:
                tab.background = null
                icon?.setColorFilter(inactiveColor)
                text?.setTextColor(inactiveColor)
            }
        }
    }
}