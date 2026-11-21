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
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPreferences = getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
        val savedToken = sharedPreferences.getString("JWT_TOKEN", "") ?: ""
        RetrofitClient.token = savedToken

        if (savedInstanceState == null) {
            switchRootTab(HomeFragment(), binding.btnTrangChu)
        }

        // Cả 5 tab Bottom Nav đều dùng Fragment trong cùng MainActivity
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

    /** Chuyển sang 1 trong 5 tab gốc và xóa sạch BackStack con để tránh kẹt khi bấm Back */
    fun switchRootTab(fragment: Fragment, activeTab: LinearLayout) {
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
        updateBottomNavUI(activeTab)
    }

    /** Mở Fragment con (Vocabulary, Roadmap, Statistics...) có hỗ trợ BackStack */
    fun openSubFragment(fragment: Fragment, activeTab: LinearLayout? = null) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .addToBackStack(null)
            .commit()
        if (activeTab != null) {
            updateBottomNavUI(activeTab)
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

        for (tab in tabs) {
            val pLeft = tab.paddingLeft
            val pTop = tab.paddingTop
            val pRight = tab.paddingRight
            val pBottom = tab.paddingBottom

            tab.setBackgroundResource(0)
            tab.background = null
            tab.setPadding(pLeft, pTop, pRight, pBottom)

            val cardView = findChildViewByType(tab, MaterialCardView::class.java)
            val icon = findChildViewByType(tab, ImageView::class.java)
            val text = findChildViewByType(tab, TextView::class.java)

            cardView?.setCardBackgroundColor(Color.TRANSPARENT)
            cardView?.cardElevation = 0f

            icon?.clearColorFilter()
            icon?.setColorFilter(inactiveColor, android.graphics.PorterDuff.Mode.SRC_IN)
            text?.setTextColor(inactiveColor)
        }

        val activePLeft = activeTab.paddingLeft
        val activePTop = activeTab.paddingTop
        val activePRight = activeTab.paddingRight
        val activePBottom = activeTab.paddingBottom

        val activeCard = findChildViewByType(activeTab, MaterialCardView::class.java)
        if (activeCard != null) {
            activeCard.setCardBackgroundColor(Color.parseColor("#EFF6FF"))
            activeCard.cardElevation = 0f
        } else {
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