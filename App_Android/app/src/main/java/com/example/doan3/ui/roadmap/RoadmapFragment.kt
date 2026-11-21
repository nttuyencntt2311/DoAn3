package com.example.doan3.ui.roadmap

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.doan3.databinding.FragmentRoadmapBinding

class RoadmapFragment : Fragment() {

    private var _binding: FragmentRoadmapBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RoadmapViewModel by viewModels()
    private lateinit var stepAdapter: RoadmapStepAdapter

    private var loadingDialog: AlertDialog? = null
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
            binding.tvOverallMotivation.text = "Mục tiêu của bạn đang được thực hiện rất tốt. Tiếp tục nhé!"
            stepAdapter.submitList(aiData.steps)
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
        val prefs = requireContext().getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
        val userId = prefs.getString("USER_ID", "guest") ?: "guest"
        val giftKey = "WEEKLY_GIFT_CLAIMED_$userId"

        fun refreshGiftButtonUI() {
            val claimed = prefs.getBoolean(giftKey, false)
            if (claimed) {
                binding.btnClaimGift.text = "Đã nhận"
                binding.btnClaimGift.isEnabled = false
                binding.btnClaimGift.alpha = 0.6f
            } else {
                binding.btnClaimGift.text = "Nhận quà"
                binding.btnClaimGift.isEnabled = true
                binding.btnClaimGift.alpha = 1.0f
            }
        }

        refreshGiftButtonUI()

        binding.btnClaimGift.setOnClickListener {
            if (prefs.getBoolean(giftKey, false)) return@setOnClickListener

            if (latestProgressPercent <= 0) {
                Toast.makeText(
                    requireContext(),
                    "Hãy hoàn thành ít nhất 1 chặng trong lộ trình để mở khóa quà tuần này nhé!",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            prefs.edit().putBoolean(giftKey, true).apply()
            refreshGiftButtonUI()
            AlertDialog.Builder(requireContext())
                .setTitle("Chúc mừng nhận thưởng! 🎁")
                .setMessage("Bạn đã nhận phần thưởng Mục tiêu tuần (+100 XP khích lệ). Hãy tiếp tục giữ vững chuỗi học tập nhé!")
                .setPositiveButton("Tuyệt vời", null)
                .show()
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

    private fun showLoadingDialog(show: Boolean) {
        if (!isAdded) return
        if (show) {
            if (loadingDialog == null) {
                val progressBar = android.widget.ProgressBar(requireContext()).apply {
                    setPadding(40, 40, 40, 40)
                }
                loadingDialog = AlertDialog.Builder(requireContext())
                    .setView(progressBar)
                    .setMessage("LexiVT AI đang thiết kế bài học dành riêng cho bạn...")
                    .setCancelable(false)
                    .create()
            }
            loadingDialog?.show()
        } else {
            loadingDialog?.dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        loadingDialog?.dismiss()
        loadingDialog = null
        _binding = null
    }
}