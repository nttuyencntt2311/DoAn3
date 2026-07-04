package com.example.doan3.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.doan3.R
import com.example.doan3.databinding.FragmentHomeBinding
import com.example.doan3.ui.vocabulary.VocabularyFragment
import com.example.doan3.ui.roadmap.RoadmapFragment

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Tự động làm mới dữ liệu từ Firestore mỗi lần user mở tab Trang chủ
        viewModel.fetchUserData()

        // =============================================================
        // ĐỔNG BỘ TOÀN BỘ DỮ LIỆU ĐỘNG VÀO CÁC ID CÓ SẴN TRONG XML CỦA BẠN
        // =============================================================

        // 1. Khung thông tin cá nhân (Avatar giữ nguyên mặc định của bạn)
        viewModel.userName.observe(viewLifecycleOwner) { name ->
            binding.tvUserName.text = name
        }

        viewModel.userLevel.observe(viewLifecycleOwner) { level ->
            binding.tvUserLevel.text = level
        }

        viewModel.streakDays.observe(viewLifecycleOwner) { streak ->
            binding.chipStreak.text = "$streak ngày"
        }

        // 2. Khung Nhiệm vụ hàng ngày (cardDailyTask)
        viewModel.progressPercent.observe(viewLifecycleOwner) { percent ->
            binding.tvTaskPercent.text = "$percent% Hoàn\nthành"
            binding.progressBar.progress = percent
        }

        viewModel.taskTargetText.observe(viewLifecycleOwner) { targetText ->
            binding.tvTaskTarget.text = targetText
        }

        // 3. Khung lưới Thống kê (layoutStats)
        viewModel.wordsCount.observe(viewLifecycleOwner) { count ->
            binding.tvWordsCount.text = String.format("%,d", count)
        }

        viewModel.timeLearned.observe(viewLifecycleOwner) { time ->
            binding.tvTimeLearned.text = time
        }

        // 4. Khung Bài học tiếp theo (cardNextLesson) - ĐÃ ĐỒNG BỘ THÔNG MINH
        viewModel.nextLessonName.observe(viewLifecycleOwner) { lessonName ->
            binding.tvLessonName.text = lessonName
        }

        viewModel.nextLessonXp.observe(viewLifecycleOwner) { xpText ->
            binding.tvXp.text = xpText
        }

        // =============================================================
        // CÁC SỰ KIỆN CLICK CHUYỂN MÀN HÌNH (GIỮ NGUYÊN HOÀN TOÀN HÀNH VI CỦA BẠN)
        // =============================================================
        binding.btnStartLearning.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, com.example.doan3.ui.learning.LearningFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnViewPro.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, RoadmapFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.cardLearnedVocab.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, VocabularyFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.cardTimeLearned.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, com.example.doan3.ui.statistics.StatisticsFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}