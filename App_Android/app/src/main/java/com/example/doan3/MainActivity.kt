package com.example.doan3

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.doan3.databinding.ActivityMainBinding
import com.example.doan3.ui.home.HomeFragment
import com.example.doan3.ui.profile.hoso
import com.example.doan3.ui.profile.RetrofitClient
import com.example.doan3.ui.profile.TaiKhoanActivity
import com.example.doan3.ui.profile.TroLyAIActivity
import com.example.doan3.ui.learning.LearningFragment
import com.example.doan3.ui.statistics.StatisticsFragment
import com.example.doan3.ui.vocabulary.VocabularyFragment
import com.example.doan3.ui.roadmap.RoadmapFragment
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPreferences = getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
        val savedToken = sharedPreferences.getString("JWT_TOKEN", "") ?: ""
        RetrofitClient.token = savedToken

        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
            updateBottomNavUI(binding.btnTrangChu)
        }

        binding.btnTrangChu.setOnClickListener {
            replaceFragment(HomeFragment())
            updateBottomNavUI(binding.btnTrangChu)
        }

        binding.btnKhamPha.setOnClickListener {
            replaceFragment(LearningFragment())
            updateBottomNavUI(binding.btnKhamPha)
        }

        binding.btnTroLyAI.setOnClickListener {
            val intent = Intent(this, TroLyAIActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        binding.btnHoSo.setOnClickListener {
            val intent = Intent(this, hoso::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

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
                "HOME" -> {
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

    // ====================================================================
    // 🚀 BỘ QUÉT XUYÊN THẤU XML ĐỂ TÌM VIEW BỊ ẨN SÂU BÊN TRONG
    // ====================================================================
    private fun <T : View> findChildViewByType(root: ViewGroup, clazz: Class<T>): T? {
        for (i in 0 until root.childCount) {
            val child = root.getChildAt(i)
            if (clazz.isInstance(child)) {
                return clazz.cast(child)
            }
            if (child is ViewGroup) {
                val found = findChildViewByType(child, clazz)
                if (found != null) return found
            }
        }
        return null
    }

    // ====================================================================
    // HÀM UPDATE UI MỚI NHẤT (Chống 100% kẹt màu dù XML thiết kế thế nào)
    // ====================================================================
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

        // BƯỚC 1: TẨY TRẮNG VÀ TẮT HẾT TẤT CẢ CÁC NÚT
        for (tab in tabs) {
            val pLeft = tab.paddingLeft
            val pTop = tab.paddingTop
            val pRight = tab.paddingRight
            val pBottom = tab.paddingBottom

            // 1. Gỡ background của thẻ LinearLayout ngoài cùng
            tab.setBackgroundResource(0)
            tab.background = null
            tab.setPadding(pLeft, pTop, pRight, pBottom)

            // 2. Quét sâu để lấy các phần tử cốt lõi
            val cardView = findChildViewByType(tab, MaterialCardView::class.java)
            val icon = findChildViewByType(tab, ImageView::class.java)
            val text = findChildViewByType(tab, TextView::class.java)

            // 3. Xóa màu nền xanh kẹt cứng bên trong CardView (nếu có)
            cardView?.setCardBackgroundColor(Color.TRANSPARENT)
            cardView?.cardElevation = 0f

            // 4. Ép màu xám tuyệt đối
            icon?.clearColorFilter()
            icon?.setColorFilter(inactiveColor, android.graphics.PorterDuff.Mode.SRC_IN)
            text?.setTextColor(inactiveColor)
        }

        // BƯỚC 2: CHỈ BẬT DUY NHẤT NÚT ĐANG ĐƯỢC CHỌN (TÔ XANH)
        val activePLeft = activeTab.paddingLeft
        val activePTop = activeTab.paddingTop
        val activePRight = activeTab.paddingRight
        val activePBottom = activeTab.paddingBottom

        val activeCard = findChildViewByType(activeTab, MaterialCardView::class.java)
        if (activeCard != null) {
            // Nếu UI dùng CardView bọc -> bôi màu nền cho CardView
            activeCard.setCardBackgroundColor(Color.parseColor("#EFF6FF"))
            activeCard.cardElevation = 0f
        } else {
            // Nếu không dùng CardView -> bôi màu cho Layout chính
            activeTab.setBackgroundResource(R.drawable.bg_tab_active)
        }
        activeTab.setPadding(activePLeft, activePTop, activePRight, activePBottom)

        val activeIcon = findChildViewByType(activeTab, ImageView::class.java)
        val activeText = findChildViewByType(activeTab, TextView::class.java)

        activeIcon?.clearColorFilter()
        activeIcon?.setColorFilter(activeColor, android.graphics.PorterDuff.Mode.SRC_IN)
        activeText?.setTextColor(activeColor)
    }
}