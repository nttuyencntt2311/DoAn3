package com.example.doan3.ui.roadmap

import android.content.Context
import android.content.Intent
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.doan3.databinding.FragmentRoadmapBinding
import com.example.doan3.ui.profile.ClaimWeeklyRewardResponse
import com.example.doan3.ui.profile.RetrofitClient
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RoadmapFragment : Fragment() {

    private var _binding: FragmentRoadmapBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RoadmapViewModel by viewModels()
    private lateinit var stepAdapter: RoadmapStepAdapter

    private var loadingDialog: AlertDialog? = null
    private val loadingHandler = Handler(Looper.getMainLooper())
    private var loadingMessageRunnable: Runnable? = null
    private var pulseAnimator: ObjectAnimator? = null
    private var latestProgressPercent: Int = 0
    private var hasTriggeredGenerate = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRoadmapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        stepAdapter = RoadmapStepAdapter()
        stepAdapter.setOnContinueClickListener { step, questionCount ->
            val intent = Intent(requireContext(), IeltsQuizActivity::class.java).apply {
                putExtra("STEP_ID", step.id)
                putExtra("STEP_TITLE", step.title)
                putExtra("STEP_DESC", step.description)
                putExtra("QUESTION_COUNT", questionCount)
            }
            startActivity(intent)
        }

        binding.rvRoadmapSteps.apply {
            adapter = stepAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        setupWeeklyGiftButton()

        viewModel.roadmapData.observe(viewLifecycleOwner) { aiData ->
            latestProgressPercent = aiData.overallProgressPercent
            binding.tvProgressPercent.text = "${aiData.overallProgressPercent}% Hoàn thành"
            binding.progressOverall.progress = aiData.overallProgressPercent
            binding.tvMotivationText.text = aiData.motivationMessage

            val topicName = aiData.topic?.takeIf { it.isNotBlank() } ?: "Lộ trình cá nhân"
            val savedList = aiData.savedRoadmaps ?: emptyList()
            if (savedList.size > 1) {
                binding.tvOverallMotivation.text = "📚 Chủ đề: $topicName • Chạm để đổi lộ trình đã lưu (${savedList.size}) ▾"
                binding.tvOverallMotivation.setOnClickListener {
                    showSwitchRoadmapDialog(savedList)
                }
            } else {
                binding.tvOverallMotivation.text = "📚 Chủ đề đang học: $topicName"
                binding.tvOverallMotivation.setOnClickListener(null)
            }
            stepAdapter.submitList(aiData.steps)
            updateWeeklyGoalUI(aiData)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            showLoadingDialog(isLoading)
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMsg ->
            if (errorMsg != null) {
                Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        // Hỗ trợ nhận tham số tạo lộ trình từ Fragment arguments hoặc Activity intent
        val argGenerate = arguments?.getBoolean("GENERATE_ROADMAP", false) ?: false
        val intentGenerate = requireActivity().intent.getBooleanExtra("GENERATE_ROADMAP", false)

        if (argGenerate || intentGenerate) {
            hasTriggeredGenerate = true
            val goal = arguments?.getString("GOAL")
                ?: requireActivity().intent.getStringExtra("GOAL")
                ?: "Giao tiếp cơ bản"
            val level = arguments?.getString("LEVEL")
                ?: requireActivity().intent.getStringExtra("LEVEL")
                ?: "Mất gốc"
            val time = arguments?.getString("TIME")
                ?: requireActivity().intent.getStringExtra("TIME")
                ?: "30 phút/ngày"
            val expectation = arguments?.getString("EXPECTATION")
                ?: requireActivity().intent.getStringExtra("EXPECTATION")
                ?: ""

            val prefs = requireContext().getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
            val currentUserId = prefs.getString("USER_ID", "") ?: ""

            viewModel.generateAiRoadmap(
                userId = currentUserId,
                target = goal,
                currentLevel = level,
                freeTime = time,
                expectation = expectation
            )
            arguments?.putBoolean("GENERATE_ROADMAP", false)
            requireActivity().intent.putExtra("GENERATE_ROADMAP", false)
        }
    }

    private fun setupWeeklyGiftButton() {
        binding.btnClaimGift.text = "Nhận quà"
        binding.btnClaimGift.isEnabled = true
        binding.btnClaimGift.alpha = 1.0f

        binding.btnClaimGift.setOnClickListener {
            val ctx = context ?: return@setOnClickListener
            binding.btnClaimGift.isEnabled = false
            binding.btnClaimGift.text = "Đang nhận..."

            RetrofitClient.getInstance(ctx).claimWeeklyReward()
                .enqueue(object : Callback<ClaimWeeklyRewardResponse> {
                    override fun onResponse(
                        call: Call<ClaimWeeklyRewardResponse>,
                        response: Response<ClaimWeeklyRewardResponse>
                    ) {
                        if (!isAdded || _binding == null) return
                        if (response.isSuccessful && response.body() != null) {
                            val body = response.body()!!
                            binding.btnClaimGift.text = "Đã nhận"
                            binding.btnClaimGift.isEnabled = false
                            binding.btnClaimGift.alpha = 0.6f
                            AlertDialog.Builder(ctx)
                                .setTitle("Chúc mừng nhận thưởng! 🎁")
                                .setMessage("Bạn đã nhận +${body.rewardXp} XP phần thưởng Mục tiêu tuần (Tổng: ${body.currentXp} XP • Cấp ${body.currentLevel}).")
                                .setPositiveButton("Tuyệt vời", null)
                                .show()
                        } else {
                            val errText = try {
                                JSONObject(response.errorBody()?.string() ?: "").optString("error", "Chưa đủ điều kiện nhận quà tuần này!")
                            } catch (_: Exception) {
                                "Chưa đủ điều kiện nhận quà tuần này!"
                            }
                            val alreadyClaimed = errText.contains("đã nhận", ignoreCase = true)
                            binding.btnClaimGift.text = if (alreadyClaimed) "Đã nhận" else "Nhận quà"
                            binding.btnClaimGift.isEnabled = !alreadyClaimed
                            binding.btnClaimGift.alpha = if (alreadyClaimed) 0.6f else 1.0f
                            Toast.makeText(ctx, errText, Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onFailure(call: Call<ClaimWeeklyRewardResponse>, t: Throwable) {
                        if (!isAdded || _binding == null) return
                        binding.btnClaimGift.text = "Nhận quà"
                        binding.btnClaimGift.isEnabled = true
                        Toast.makeText(ctx, "Lỗi kết nối máy chủ: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    override fun onResume() {
        super.onResume()
        if (!hasTriggeredGenerate) {
            val prefs = requireContext().getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
            val currentUserId = prefs.getString("USER_ID", "") ?: ""
            viewModel.fetchAiRoadmapForUser(currentUserId)
        } else {
            hasTriggeredGenerate = false
        }
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    private fun updateWeeklyGoalUI(aiData: UserRoadmapResponse) {
        if (_binding == null) return
        val completed = aiData.weeklyLessonsCompleted.coerceIn(0, 5)
        val target = aiData.weeklyGoalTarget.coerceAtLeast(1)
        binding.tvWeeklyStreak.text = aiData.streakDays.toString()
        binding.tvWeeklyGoalTitle.text = "Mục tiêu tuần ($completed/$target)"

        val bars = listOf(
            binding.weeklyBar1,
            binding.weeklyBar2,
            binding.weeklyBar3,
            binding.weeklyBar4,
            binding.weeklyBar5
        )
        bars.forEachIndexed { idx, bar ->
            bar.setBackgroundColor(
                if (idx < completed) Color.parseColor("#F59E0B") else Color.parseColor("#E2E8F0")
            )
        }

        if (aiData.isWeeklyRewardClaimed) {
            binding.btnClaimGift.text = "Đã nhận"
            binding.btnClaimGift.isEnabled = false
            binding.btnClaimGift.alpha = 0.6f
        } else {
            binding.btnClaimGift.text = "Nhận quà"
            binding.btnClaimGift.isEnabled = true
            binding.btnClaimGift.alpha = if (completed >= target) 1.0f else 0.78f
        }
    }

    private fun stopLoadingAnimations() {
        loadingMessageRunnable?.let { loadingHandler.removeCallbacks(it) }
        loadingMessageRunnable = null
        pulseAnimator?.cancel()
        pulseAnimator = null
    }

    private fun showLoadingDialog(show: Boolean) {
        if (!isAdded) return
        if (show) {
            if (loadingDialog == null) {
                val ctx = requireContext()
                val container = LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER_HORIZONTAL
                    setPadding(24.dp(), 26.dp(), 24.dp(), 24.dp())
                    background = GradientDrawable().apply {
                        setColor(Color.WHITE)
                        cornerRadius = 24.dp().toFloat()
                    }
                }

                val iconBadge = TextView(ctx).apply {
                    text = "🧠"
                    textSize = 34f
                    gravity = Gravity.CENTER
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.parseColor("#EEF2FF"))
                    }
                    layoutParams = LinearLayout.LayoutParams(68.dp(), 68.dp()).apply {
                        bottomMargin = 14.dp()
                    }
                }
                pulseAnimator = ObjectAnimator.ofFloat(iconBadge, View.SCALE_X, 0.92f, 1.08f).apply {
                    duration = 750L
                    repeatMode = ValueAnimator.REVERSE
                    repeatCount = ValueAnimator.INFINITE
                    start()
                }
                container.addView(iconBadge)

                val titleTv = TextView(ctx).apply {
                    text = "LexiVT AI đang thiết kế lộ trình"
                    textSize = 17f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(Color.parseColor("#0F172A"))
                    gravity = Gravity.CENTER
                }
                container.addView(titleTv)

                val statusMessages = listOf(
                    "🔍 Đang đọc hồ sơ hành vi & điểm mạnh của bạn...",
                    "📚 Đang chọn lọc từ vựng trọng tâm & từ dễ quên...",
                    "🗺️ Đang sắp xếp 4 chặng học cá nhân hóa...",
                    "✨ Sắp hoàn tất, chuẩn bị bắt đầu học ngay nhé!"
                )
                val statusTv = TextView(ctx).apply {
                    text = statusMessages[0]
                    textSize = 13.5f
                    setTextColor(Color.parseColor("#475569"))
                    gravity = Gravity.CENTER
                    setPadding(0, 8.dp(), 0, 16.dp())
                }
                container.addView(statusTv)

                val progressBar = ProgressBar(ctx, null, android.R.attr.progressBarStyleHorizontal).apply {
                    isIndeterminate = true
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        8.dp()
                    )
                }
                container.addView(progressBar)

                var msgIndex = 0
                val runnable = object : Runnable {
                    override fun run() {
                        msgIndex = (msgIndex + 1) % statusMessages.size
                        statusTv.text = statusMessages[msgIndex]
                        loadingHandler.postDelayed(this, 1800L)
                    }
                }
                loadingMessageRunnable = runnable
                loadingHandler.postDelayed(runnable, 1800L)

                loadingDialog = AlertDialog.Builder(ctx)
                    .setView(container)
                    .setCancelable(false)
                    .create().apply {
                        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    }
            }
            loadingDialog?.show()
        } else {
            stopLoadingAnimations()
            loadingDialog?.dismiss()
            loadingDialog = null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopLoadingAnimations()
        loadingDialog?.dismiss()
        loadingDialog = null
        _binding = null
    }

    private fun showSwitchRoadmapDialog(savedList: List<SavedRoadmapSummary>) {
        val ctx = context ?: return
        val labels = savedList.map { item ->
            val mark = if (item.isActive) "✅ " else "📘 "
            "$mark${item.topic} (${item.overallProgressPercent}%)"
        }.toTypedArray()

        AlertDialog.Builder(ctx)
            .setTitle("Chuyển lộ trình đã lưu 📚")
            .setItems(labels) { _, which ->
                val chosen = savedList[which]
                if (chosen.isActive) return@setItems
                RetrofitClient.getInstance(ctx).activateRoadmap(chosen.id)
                    .enqueue(object : Callback<UserRoadmapResponse> {
                        override fun onResponse(call: Call<UserRoadmapResponse>, response: Response<UserRoadmapResponse>) {
                            if (!isAdded) return
                            if (response.isSuccessful) {
                                Toast.makeText(ctx, "Đã chuyển sang lộ trình: ${chosen.topic}", Toast.LENGTH_SHORT).show()
                                val uid = ctx.getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE).getString("USER_ID", "") ?: ""
                                viewModel.fetchAiRoadmapForUser(uid)
                            }
                        }

                        override fun onFailure(call: Call<UserRoadmapResponse>, t: Throwable) {
                            if (!isAdded) return
                            Toast.makeText(ctx, "Lỗi đổi lộ trình: ${t.message}", Toast.LENGTH_SHORT).show()
                        }
                    })
            }
            .setNegativeButton("Đóng", null)
            .show()
    }
}
