package com.example.doan3.ui.home

import android.content.Intent // Đã thêm import này
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.doan3.R
import com.example.doan3.ui.vocabulary.VocabularyFragment
import com.example.doan3.databinding.FragmentHomeBinding

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

        // Lấy các animation
        val fadeInSlideAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in_slide)
        val shakeAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.shake)

        // Áp dụng animation xuất hiện ban đầu (Fading & Sliding)
        binding.layoutStats.startAnimation(fadeInSlideAnim)
        binding.cardPro.startAnimation(fadeInSlideAnim)
        binding.cardNextLesson.startAnimation(fadeInSlideAnim)
        binding.cardDailyTask.startAnimation(fadeInSlideAnim)

        // 1. DỮ LIỆU ĐỘNG CHO THANH PROGRESS (Thanh chạy)
        viewModel.progressBarPercent.observe(viewLifecycleOwner) { percent ->
            var currentPercent = 0
            val handler = Handler(Looper.getMainLooper())
            handler.postDelayed(object : Runnable {
                override fun run() {
                    if (currentPercent < percent) {
                        currentPercent += 5 // Chạy lên 5% mỗi lần
                        binding.progressBar.progress = currentPercent
                        binding.tvTaskPercent.text = "$currentPercent% Hoàn\nthành"
                        handler.postDelayed(this, 30) // Chạy lại sau 30ms
                    } else {
                        binding.progressBar.progress = percent
                        binding.tvTaskPercent.text = "$percent% Hoàn\nthành"
                    }
                }
            }, 500) // Delay 500ms trước khi chạy
        }

        // 2. LẮNG NGHE DỮ LIỆU KHÁC
        viewModel.userName.observe(viewLifecycleOwner) { name ->
            binding.tvUserName.text = name
        }

        viewModel.wordsLearned.observe(viewLifecycleOwner) { words ->
            binding.tvWordsCount.text = String.format("%,d", words)
        }

        viewModel.timeLearned.observe(viewLifecycleOwner) { hours ->
            binding.tvTimeLearned.text = "$hours giờ"
        }

        // 3. XỬ LÝ SỰ KIỆN CLICK MỞ TRANG HỌC TẬP TỪ MỚI VÀ ĐĂNG KÝ
        binding.btnStartLearning.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, com.example.doan3.ui.learning.LearningFragment())
                .addToBackStack(null)
                .commit()
        }

        // Đã sửa thành Intent để mở RegisterActivity thành công
        binding.btnViewPro.setOnClickListener {
            val intent = Intent(requireContext(), com.example.doan3.ui.profile.RegisterActivity::class.java)
            startActivity(intent)
        }

        // 4. SỰ KIỆN CLICK VÀO THẺ "TỪ VỰNG ĐÃ THUỘC" ĐỂ CHUYỂN TRANG
        binding.cardLearnedVocab.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, VocabularyFragment())
                .addToBackStack(null)
                .commit()
        }

        // 5. SỰ KIỆN CLICK VÀO THẺ "THỜI GIAN HỌC" ĐỂ CHUYỂN SANG TRANG THỐNG KÊ
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