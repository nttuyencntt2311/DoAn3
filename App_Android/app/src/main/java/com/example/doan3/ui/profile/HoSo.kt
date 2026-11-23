package com.example.doan3.ui.profile

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doan3.MainActivity
import com.example.doan3.R
import com.example.doan3.ui.vocabulary.Vocabulary
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HoSoFragment : Fragment() {

    private var myUserName: String = ""
    private var latestLeaderboard: List<LeaderboardUser> = emptyList()

    private var tvProfileName: TextView? = null
    private var tvAccountType: TextView? = null
    private var tvHeaderStreak: TextView? = null

    private var rvLeaderboard: RecyclerView? = null
    private var tvStatLevel: TextView? = null
    private var tvStatVocab: TextView? = null
    private var tvStatDays: TextView? = null

    private var btnSettings: View? = null
    private var btnHelpCenter: View? = null
    private var switchPacingAlert: SwitchCompat? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.hoso, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.bottomNav)?.visibility = View.GONE

        val prefs = requireContext().getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
        myUserName = prefs.getString("USER_NAME", "") ?: ""

        rvLeaderboard = view.findViewById(R.id.rvLeaderboard)
        rvLeaderboard?.layoutManager = LinearLayoutManager(requireContext())

        tvProfileName = view.findViewById(R.id.tvProfileName)
        tvAccountType = view.findViewById(R.id.tvAccountType)
        tvHeaderStreak = view.findViewById(R.id.tvHeaderStreak)
        tvStatLevel = view.findViewById(R.id.tvStatLevel)
        tvStatVocab = view.findViewById(R.id.tvStatVocab)
        tvStatDays = view.findViewById(R.id.tvStatDays)

        btnSettings = view.findViewById(R.id.btnSettings)
        btnHelpCenter = view.findViewById(R.id.btnHelpCenter)
        switchPacingAlert = view.findViewById(R.id.switchPacingAlert)

        setupUserActions()
        setupPacingAlertSwitch()
        setupAvatarDisplay(view)
        loadUserStats()
    }

    private fun setupPacingAlertSwitch() {
        val prefs = requireContext().getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
        val isAlertEnabled = prefs.getBoolean("PREF_PACING_ALERT", true)

        switchPacingAlert?.isChecked = isAlertEnabled
        updateRadarSwitchColor(isAlertEnabled)

        switchPacingAlert?.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("PREF_PACING_ALERT", isChecked).apply()
            updateRadarSwitchColor(isChecked)
            val status = if (isChecked) "Đã bật" else "Đã tắt"
            Toast.makeText(requireContext(), "$status nhắc nhở thời gian Radar XAI", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateRadarSwitchColor(isChecked: Boolean) {
        val color = if (isChecked) Color.parseColor("#D97706") else Color.parseColor("#E2E8F0")
        switchPacingAlert?.trackTintList = ColorStateList.valueOf(color)
    }

    private fun setupUserActions() {
        btnSettings?.setOnClickListener {
            (activity as? MainActivity)?.openAccountTab()
        }

        btnHelpCenter?.setOnClickListener {
            startActivity(Intent(requireContext(), HelpCenterActivity::class.java))
        }

        tvHeaderStreak?.setOnClickListener {
            (activity as? MainActivity)?.openStatisticsScreen()
        }
    }

    private fun loadUserStats() {
        val ctx = context ?: return
        val apiService = RetrofitClient.getInstance(ctx)

        apiService.getUserStats().enqueue(object : Callback<UserStatsResponse> {
            override fun onResponse(call: Call<UserStatsResponse>, response: Response<UserStatsResponse>) {
                if (!isAdded || view == null) return
                val stats = response.body()?.stats

                if (stats != null) {
                    myUserName = stats.name
                    tvProfileName?.text = stats.name
                    tvHeaderStreak?.text = "${stats.streakDays} 🔥"
                    tvStatLevel?.text = "${stats.level}"
                    AvatarHelper.saveUserLevel(ctx, stats.level)
                    tvStatDays?.text = stats.streakDays.toString()

                    val danhHieu = when (stats.level) {
                        in 1..3 -> "🌱 Tân binh ngôn ngữ"
                        in 4..9 -> "🔥 Kẻ Phá Vỡ Giới Hạn"
                        in 10..19 -> "💎 Học giả ưu tú"
                        in 20..49 -> "⚡ Bậc thầy giao tiếp"
                        else -> "👑 Thần đồng ngoại ngữ"
                    }

                    tvAccountType?.text = "$danhHieu • ${stats.xp} XP"
                    tvAccountType?.setTextColor(Color.parseColor("#F59E0B"))

                    if (latestLeaderboard.isNotEmpty()) {
                        rvLeaderboard?.adapter = LeaderboardAdapter(latestLeaderboard, myUserName)
                    }
                }
            }

            override fun onFailure(call: Call<UserStatsResponse>, t: Throwable) {
                if (!isAdded || view == null) return
                setLoadingDefaultValues()
            }
        })

        apiService.getAllVocabularies().enqueue(object : Callback<List<Vocabulary>> {
            override fun onResponse(call: Call<List<Vocabulary>>, response: Response<List<Vocabulary>>) {
                if (!isAdded || view == null) return
                if (response.isSuccessful) {
                    val allWords = response.body() ?: emptyList()
                    val masteredCount = allWords.count { it.proficiency >= 80 }
                    tvStatVocab?.text = masteredCount.toString()
                }
            }

            override fun onFailure(call: Call<List<Vocabulary>>, t: Throwable) {
                if (!isAdded || view == null) return
                tvStatVocab?.text = "0"
            }
        })

        apiService.getLeaderboard().enqueue(object : Callback<List<LeaderboardUser>> {
            override fun onResponse(call: Call<List<LeaderboardUser>>, response: Response<List<LeaderboardUser>>) {
                if (!isAdded || view == null) return
                if (response.isSuccessful && response.body() != null) {
                    latestLeaderboard = response.body()!!
                    if (latestLeaderboard.isNotEmpty()) {
                        rvLeaderboard?.adapter = LeaderboardAdapter(latestLeaderboard, myUserName)
                    }
                }
            }

            override fun onFailure(call: Call<List<LeaderboardUser>>, t: Throwable) {
                if (!isAdded || view == null) return
                Toast.makeText(requireContext(), "Lỗi kết nối BXH: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setLoadingDefaultValues() {
        tvProfileName?.text = "Học viên mới"
        tvHeaderStreak?.text = "0 🔥"
        tvStatLevel?.text = "0"
        tvStatVocab?.text = "0"
        tvStatDays?.text = "0"
        tvAccountType?.text = "🌱 Tân binh ngôn ngữ"
    }

    private fun setupAvatarDisplay(view: View) {
        val ctx = context ?: return
        val imgHeader = view.findViewById<ImageView>(R.id.imgHeaderAvatar)
        val imgMain = view.findViewById<ImageView>(R.id.imgProfileAvatar)
        AvatarHelper.applyAvatarToImageView(ctx, imgHeader)
        AvatarHelper.applyAvatarToImageView(ctx, imgMain)

        val openPicker = View.OnClickListener {
            AvatarHelper.showAvatarPickerDialog(ctx) {
                AvatarHelper.applyAvatarToImageView(ctx, imgHeader)
                AvatarHelper.applyAvatarToImageView(ctx, imgMain)
                Toast.makeText(ctx, "Đã đổi Avatar mới ✨", Toast.LENGTH_SHORT).show()
            }
        }
        imgMain?.setOnClickListener(openPicker)
        imgHeader?.setOnClickListener(openPicker)
    }

    override fun onResume() {
        super.onResume()
        val ctx = context ?: return
        view?.let { v ->
            AvatarHelper.applyAvatarToImageView(ctx, v.findViewById(R.id.imgHeaderAvatar))
            AvatarHelper.applyAvatarToImageView(ctx, v.findViewById(R.id.imgProfileAvatar))
        }
    }
}
