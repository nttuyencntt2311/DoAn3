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

        // Thực hiện quét và nạp dữ liệu từ Backend (MongoDB)
        viewModel.fetchUserData(requireContext())

        // ====================================================================
        // ĐỔ DỮ LIỆU VÀO GIAO DIỆN (Đã sửa ID chuẩn theo đúng XML của bạn)
        // ====================================================================
        viewModel.userName.observe(viewLifecycleOwner) { name ->
            binding.tvUserName.text = name
        }
        viewModel.userLevel.observe(viewLifecycleOwner) { level ->
            binding.tvUserLevel.text = level
        }

        // Chip StreakDays trong XML của bạn
        viewModel.streakDays.observe(viewLifecycleOwner) { days ->
            binding.tvStreakDays.text = "$days ngày"
        }

        // ProgressBar và Text % (Sử dụng đúng ID progressBar và tvTaskPercent)
        viewModel.progressPercent.observe(viewLifecycleOwner) { percent ->
            binding.progressBar.progress = percent
            binding.tvTaskPercent.text = "$percent% Hoàn\nthành"
        }

        viewModel.taskTargetText.observe(viewLifecycleOwner) { targetText ->
            binding.tvTaskTarget.text = targetText
        }
        viewModel.wordsCount.observe(viewLifecycleOwner) { count ->
            binding.tvWordsCount.text = count.toString()
        }
        viewModel.timeLearned.observe(viewLifecycleOwner) { time ->
            binding.tvTimeLearned.text = time
        }
        viewModel.nextLessonName.observe(viewLifecycleOwner) { lessonName ->
            binding.tvLessonName.text = lessonName
        }
        viewModel.nextLessonXp.observe(viewLifecycleOwner) { xpText ->
            binding.tvXp.text = xpText
        }

        // ====================================================================
        // QUẢN LÝ CÁC NÚT CLICK CHUYỂN MÀN HÌNH
        // ====================================================================
        binding.btnStartLearning.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, com.example.doan3.ui.learning.LearningFragment())
                .addToBackStack(null).commit()
        }

        binding.btnViewPro.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, RoadmapFragment())
                .addToBackStack(null).commit()
        }

        binding.cardLearnedVocab.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, VocabularyFragment())
                .addToBackStack(null).commit()
        }

        binding.cardTimeLearned.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, com.example.doan3.ui.statistics.StatisticsFragment())
                .addToBackStack(null).commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}