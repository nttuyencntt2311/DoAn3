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
import com.example.doan3.ui.profile.TaiKhoanActivity
import com.example.doan3.ui.profile.TroLyAIActivity
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
            replaceFragment(LearningFragment())
            updateBottomNavUI(binding.btnKhamPha)
        }

        // 3. Nút Trợ lý AI (KHÔNG GỌI updateBottomNavUI ở đây nữa để tránh kẹt màu)
        binding.btnTroLyAI.setOnClickListener {
            val intent = Intent(this, TroLyAIActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        // 4. Nút Hồ sơ (KHÔNG GỌI updateBottomNavUI ở đây nữa)
        binding.btnHoSo.setOnClickListener {
            val intent = Intent(this, hoso::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        // 5. Nút Tài khoản (KHÔNG GỌI updateBottomNavUI ở đây nữa)
        binding.btnTaiKhoan.setOnClickListener {
            val intent = Intent(this, TaiKhoanActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        val openStatistics = intent.getBooleanExtra("OPEN_STATISTICS", false)
        if (openStatistics) {
            replaceFragment(StatisticsFragment())
        }

        handleIntentData(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntentData(intent)
    }

    private fun handleIntentData(intent: Intent) {
        val openFragment = intent.getStringExtra("OPEN_FRAGMENT")
        if (openFragment != null) {
            when (openFragment) {
                "HOME" -> { // BỔ SUNG LỆNH NÀY ĐỂ MỞ ĐÚNG TRANG CHỦ
                    replaceFragment(HomeFragment())
                    updateBottomNavUI(binding.btnTrangChu)
                }
                "LEARNING" -> {
                    replaceFragment(LearningFragment())
                    updateBottomNavUI(binding.btnKhamPha)
                }
                "VOCABULARY" -> {
                    replaceFragment(VocabularyFragment())
                    updateBottomNavUI(binding.btnKhamPha)
                }
                "ROADMAP" -> {
                    replaceFragment(RoadmapFragment())
                    updateBottomNavUI(binding.btnKhamPha)
                }
            }
            intent.removeExtra("OPEN_FRAGMENT")
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }

    // HÀM UPDATE UI ĐÃ ĐƯỢC CHỐNG LỖI KẸT MÀU
    // HÀM UPDATE UI MỚI NHẤT - CHỐNG DÍNH MÀU TUYỆT ĐỐI
    fun updateBottomNavUI(activeTab: LinearLayout) {
        val tabs = listOf(
            binding.btnTrangChu,
            binding.btnKhamPha,
            binding.btnTroLyAI,
            binding.btnHoSo,
            binding.btnTaiKhoan
        )

        val activeColor = Color.parseColor("#0056D2")
        val inactiveColor = Color.parseColor("#94A3B8")

        // BƯỚC 1: TẨY TRẮNG VÀ TẮT HẾT TẤT CẢ CÁC NÚT (ĐƯA VỀ XÁM)
        for (tab in tabs) {
            tab.setBackgroundResource(0)
            tab.background = null // Bắt buộc Android xóa sạch nền

            val icon = tab.getChildAt(0) as? ImageView
            val text = tab.getChildAt(1) as? TextView

            icon?.clearColorFilter() // Xóa sạch bộ lọc màu cũ bị kẹt
            icon?.setColorFilter(inactiveColor)
            text?.setTextColor(inactiveColor)
        }

        // BƯỚC 2: CHỈ BẬT DUY NHẤT NÚT ĐANG ĐƯỢC CHỌN (TÔ XANH)
        activeTab.setBackgroundResource(R.drawable.bg_tab_active)

        val activeIcon = activeTab.getChildAt(0) as? ImageView
        val activeText = activeTab.getChildAt(1) as? TextView

        activeIcon?.clearColorFilter()
        activeIcon?.setColorFilter(activeColor)
        activeText?.setTextColor(activeColor)
    }
}