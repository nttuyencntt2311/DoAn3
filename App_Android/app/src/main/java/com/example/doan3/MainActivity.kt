package com.example.doan3

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.doan3.databinding.ActivityMainBinding
import com.example.doan3.ui.home.HomeFragment
import com.example.doan3.ui.learning.LearningFragment
import com.example.doan3.ui.profile.HoSoFragment
import com.example.doan3.ui.profile.RetrofitClient
import com.example.doan3.ui.profile.TaiKhoanFragment
import com.example.doan3.ui.profile.TroLyAIFragment
import com.example.doan3.ui.roadmap.RoadmapFragment
import com.example.doan3.ui.statistics.StatisticsFragment
import com.example.doan3.ui.vocabulary.VocabularyFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentActiveTab: LinearLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPreferences = getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
        val savedToken = sharedPreferences.getString("JWT_TOKEN", "") ?: ""
        RetrofitClient.token = savedToken

        // Căn kích thước viên thuốc trượt khớp chính xác với chiều rộng 1 tab khi layout xong
        binding.bottomNav.post {
            val tabWidth = binding.bottomNav.width / 5
            if (tabWidth > 0) {
                val insetPx = (4 * resources.displayMetrics.density).toInt()
                val lp = binding.navSlidingPill.layoutParams
                lp.width = (tabWidth - insetPx * 2).coerceAtLeast(1)
                binding.navSlidingPill.layoutParams = lp
                val active = currentActiveTab ?: binding.btnTrangChu
                moveSlidingPill(active, animate = false)
            }
        }

        if (savedInstanceState == null) {
            switchRootTab(HomeFragment(), binding.btnTrangChu, animate = false)
        }

        binding.btnTrangChu.setOnClickListener {
            switchRootTab(HomeFragment(), binding.btnTrangChu)
        }

        binding.btnKhamPha.setOnClickListener {
            switchRootTab(LearningFragment(), binding.btnKhamPha)
        }

        binding.btnTroLyAI.setOnClickListener {
            switchRootTab(TroLyAIFragment(), binding.btnTroLyAI)
        }

        binding.btnHoSo.setOnClickListener {
            switchRootTab(HoSoFragment(), binding.btnHoSo)
        }

        binding.btnTaiKhoan.setOnClickListener {
            switchRootTab(TaiKhoanFragment(), binding.btnTaiKhoan)
        }

        handleIntentData(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntentData(intent)
    }

    private fun handleIntentData(intent: Intent) {
        if (intent.getBooleanExtra("OPEN_STATISTICS", false)) {
            intent.removeExtra("OPEN_STATISTICS")
            openSubFragment(StatisticsFragment(), binding.btnHoSo)
            return
        }

        val openFragment = intent.getStringExtra("OPEN_FRAGMENT")
        if (openFragment != null) {
            when (openFragment) {
                "HOME" -> switchRootTab(HomeFragment(), binding.btnTrangChu)
                "LEARNING" -> switchRootTab(LearningFragment(), binding.btnKhamPha)
                "AI_ASSISTANT" -> switchRootTab(TroLyAIFragment(), binding.btnTroLyAI)
                "PROFILE" -> switchRootTab(HoSoFragment(), binding.btnHoSo)
                "ACCOUNT" -> switchRootTab(TaiKhoanFragment(), binding.btnTaiKhoan)
                "VOCABULARY" -> openSubFragment(VocabularyFragment(), binding.btnKhamPha)
                "ROADMAP" -> openSubFragment(RoadmapFragment(), binding.btnKhamPha)
                "STATISTICS" -> openSubFragment(StatisticsFragment(), binding.btnHoSo)
            }
            intent.removeExtra("OPEN_FRAGMENT")
        }
    }

    fun switchRootTab(fragment: Fragment, activeTab: LinearLayout, animate: Boolean = true) {
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        val tx = supportFragmentManager.beginTransaction()
        if (animate) {
            tx.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        tx.replace(R.id.nav_host_fragment, fragment).commit()
        updateBottomNavUI(activeTab, animate)
    }

    fun openSubFragment(fragment: Fragment, activeTab: LinearLayout? = null) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.nav_host_fragment, fragment)
            .addToBackStack(null)
            .commit()
        if (activeTab != null) {
            updateBottomNavUI(activeTab, animate = true)
        }
    }

    fun openHomeTab() = switchRootTab(HomeFragment(), binding.btnTrangChu)
    fun openLearningTab() = switchRootTab(LearningFragment(), binding.btnKhamPha)
    fun openAiTab() = switchRootTab(TroLyAIFragment(), binding.btnTroLyAI)
    fun openProfileTab() = switchRootTab(HoSoFragment(), binding.btnHoSo)
    fun openAccountTab() = switchRootTab(TaiKhoanFragment(), binding.btnTaiKhoan)

    fun openVocabularyScreen() = openSubFragment(VocabularyFragment(), binding.btnKhamPha)
    fun openRoadmapScreen(args: Bundle? = null) {
        val fragment = RoadmapFragment().apply { arguments = args }
        openSubFragment(fragment, binding.btnKhamPha)
    }
    fun openStatisticsScreen() = openSubFragment(StatisticsFragment(), binding.btnHoSo)

    private fun <T : View> findChildViewByType(root: ViewGroup, clazz: Class<T>): T? {
        for (i in 0 until root.childCount) {
            val child = root.getChildAt(i)
            if (clazz.isInstance(child)) return clazz.cast(child)
            if (child is ViewGroup) {
                val found = findChildViewByType(child, clazz)
                if (found != null) return found
            }
        }
        return null
    }

    private fun moveSlidingPill(activeTab: LinearLayout, animate: Boolean) {
        val tabs = listOf(
            binding.btnTrangChu,
            binding.btnKhamPha,
            binding.btnTroLyAI,
            binding.btnHoSo,
            binding.btnTaiKhoan
        )
        val index = tabs.indexOf(activeTab).coerceAtLeast(0)
        val tabWidth = binding.bottomNav.width / 5f
        if (tabWidth <= 0f) return

        val insetPx = 4f * resources.displayMetrics.density
        val targetX = index * tabWidth + insetPx

        if (animate) {
            binding.navSlidingPill.animate()
                .translationX(targetX)
                .setDuration(260L)
                .setInterpolator(DecelerateInterpolator(1.8f))
                .start()
        } else {
            binding.navSlidingPill.translationX = targetX
        }
    }

    fun updateBottomNavUI(activeTab: LinearLayout, animate: Boolean = true) {
        currentActiveTab = activeTab
        val tabs = listOf(
            binding.btnTrangChu,
            binding.btnKhamPha,
            binding.btnTroLyAI,
            binding.btnHoSo,
            binding.btnTaiKhoan
        )

        val activeColor = Color.parseColor("#2563EB")
        val inactiveColor = Color.parseColor("#94A3B8")

        for (tab in tabs) {
            val icon = findChildViewByType(tab, ImageView::class.java)
            val text = findChildViewByType(tab, TextView::class.java)

            icon?.clearColorFilter()
            icon?.setColorFilter(inactiveColor, android.graphics.PorterDuff.Mode.SRC_IN)
            text?.setTextColor(inactiveColor)
            text?.setTypeface(null, Typeface.NORMAL)
        }

        val activeIcon = findChildViewByType(activeTab, ImageView::class.java)
        val activeText = findChildViewByType(activeTab, TextView::class.java)

        activeIcon?.clearColorFilter()
        activeIcon?.setColorFilter(activeColor, android.graphics.PorterDuff.Mode.SRC_IN)
        activeText?.setTextColor(activeColor)
        activeText?.setTypeface(null, Typeface.BOLD)

        // Hiệu ứng nảy nhẹ icon khi chọn tab
        if (animate && activeIcon != null) {
            activeIcon.scaleX = 0.85f
            activeIcon.scaleY = 0.85f
            activeIcon.animate()
                .scaleX(1.08f)
                .scaleY(1.08f)
                .setDuration(140L)
                .setInterpolator(OvershootInterpolator(2.2f))
                .withEndAction {
                    activeIcon.animate().scaleX(1f).scaleY(1f).setDuration(100L).start()
                }
                .start()
        }

        moveSlidingPill(activeTab, animate)
    }
}
