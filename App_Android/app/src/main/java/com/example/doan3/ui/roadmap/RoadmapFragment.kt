package com.example.doan3.ui.roadmap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.doan3.databinding.FragmentRoadmapBinding

class RoadmapFragment : Fragment() {

    private var _binding: FragmentRoadmapBinding? = null
    private val binding get() = _binding!!

    // Khởi tạo ViewModel
    private val viewModel: RoadmapViewModel by viewModels()
    private lateinit var stepAdapter: RoadmapStepAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRoadmapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Setup RecyclerView
        stepAdapter = RoadmapStepAdapter()
        binding.rvRoadmapSteps.apply {
            adapter = stepAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        // 2. Lắng nghe dữ liệu (Observe Data)
        viewModel.roadmapData.observe(viewLifecycleOwner) { aiData ->
            // Cập nhật phần thông tin chung
            binding.tvProgressPercent.text = "${aiData.overallProgressPercent}% Hoàn thành"
            binding.progressOverall.progress = aiData.overallProgressPercent
            binding.tvMotivationText.text = aiData.motivationMessage

            // Cập nhật danh sách các chặng lộ trình
            stepAdapter.submitList(aiData.steps)
        }

        // 3. Gọi ViewModel lấy dữ liệu (Ví dụ truyền User ID là "user_123")
        viewModel.fetchAiRoadmapForUser("user_123")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}