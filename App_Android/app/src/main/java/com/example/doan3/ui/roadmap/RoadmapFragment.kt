package com.example.doan3.ui.roadmap

import android.content.Context
import android.content.Intent // 🚀 ĐÃ THÊM IMPORT NÀY ĐỂ HẾT ĐỎ INTENT
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.doan3.databinding.FragmentRoadmapBinding

class RoadmapFragment : Fragment() {

    private var _binding: FragmentRoadmapBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RoadmapViewModel by viewModels()
    private lateinit var stepAdapter: RoadmapStepAdapter

    private var loadingDialog: androidx.appcompat.app.AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRoadmapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Khởi tạo RecyclerView & Nhận sự kiện Click
        stepAdapter = RoadmapStepAdapter()

        // 🚀 Nhận thêm biến questionCount từ Adapter truyền sang
        stepAdapter.setOnContinueClickListener { step, questionCount ->
            val intent = Intent(requireContext(), IeltsQuizActivity::class.java).apply {
                putExtra("STEP_ID", step.id)
                putExtra("STEP_TITLE", step.title)
                putExtra("STEP_DESC", step.description)
                putExtra("QUESTION_COUNT", questionCount) // 🚀 Đóng gói số câu hỏi vào Intent
            }
            startActivity(intent)
        }

        binding.rvRoadmapSteps.apply {
            adapter = stepAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        // 2. Lắng nghe dữ liệu trả về từ AI (Observe Data)
        viewModel.roadmapData.observe(viewLifecycleOwner) { aiData ->
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

        // 3. XỬ LÝ GENERATE MỚI (chỉ chạy khi điền form)
        val intent = requireActivity().intent
        val isGenerateNew = intent.getBooleanExtra("GENERATE_ROADMAP", false)

        if (isGenerateNew) {
            val goal = intent.getStringExtra("GOAL") ?: "Giao tiếp cơ bản"
            val level = intent.getStringExtra("LEVEL") ?: "Mất gốc"
            val time = intent.getStringExtra("TIME") ?: "30 phút/ngày"
            val expectation = intent.getStringExtra("EXPECTATION") ?: ""

            val prefs = requireContext().getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
            val currentUserId = prefs.getString("USER_ID", "") ?: ""

            viewModel.generateAiRoadmap(
                userId = currentUserId,
                target = goal,
                currentLevel = level,
                freeTime = time,
                expectation = expectation
            )
            intent.putExtra("GENERATE_ROADMAP", false)
        }
    }

    // 🚀 CHỖ SỬA QUAN TRỌNG: Tải lại lộ trình học bất cứ khi nào người dùng quay lại màn hình này
    override fun onResume() {
        super.onResume()
        // Kiểm tra xem có đang trong tiến trình tạo mới không, nếu không thì tự cập nhật lại dữ liệu
        val isGenerateNew = requireActivity().intent.getBooleanExtra("GENERATE_ROADMAP", false)
        if (!isGenerateNew) {
            val prefs = requireContext().getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
            val currentUserId = prefs.getString("USER_ID", "") ?: ""
            viewModel.fetchAiRoadmapForUser(currentUserId)
        }
    }

    // Hiệu ứng Loading
    private fun showLoadingDialog(show: Boolean) {
        if (show) {
            if (loadingDialog == null) {
                val progressBar = android.widget.ProgressBar(requireContext()).apply {
                    setPadding(40, 40, 40, 40)
                }
                loadingDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setView(progressBar)
                    .setMessage("LexiVT AI đang thiết kế bài học dành riêng cho bạn...")
                    .setCancelable(false) // Không cho bấm ra ngoài khi đang loading
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
        _binding = null
    }
}