package com.example.doan3.ui.home
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
        // Mình áp dụng cho 4 thẻ quan trọng: stats, pro, next lesson, card daily task
        binding.layoutStats.startAnimation(fadeInSlideAnim)
        binding.cardPro.startAnimation(fadeInSlideAnim)
        binding.cardNextLesson.startAnimation(fadeInSlideAnim)
        binding.cardDailyTask.startAnimation(fadeInSlideAnim)

        // 1. DỮ LIỆU ĐỘNG CHO THANH PROGRESS (Thanh chạy)
        // Ta dùng Handler để tạo hiệu ứng thanh progress chạy lên
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

        // 2. LẮNG NGHE DỮ LIỆU KHÁC (Giữ nguyên)
        viewModel.userName.observe(viewLifecycleOwner) { name ->
            binding.tvUserName.text = name
        }

        viewModel.wordsLearned.observe(viewLifecycleOwner) { words ->
            binding.tvWordsCount.text = String.format("%,d", words)
        }

        viewModel.timeLearned.observe(viewLifecycleOwner) { hours ->
            binding.tvTimeLearned.text = "$hours giờ"
        }

        // 3. XỬ LÝ SỰ KIỆN CLICK (Thêm hiệu ứng Rung/Shake nếu muốn)
        binding.btnStartLearning.setOnClickListener {
            // Hiệu ứng click/ripple đã có sẵn
            // Bạn có thể thêm hiệu ứng shake nếu muốn báo lỗi (ví dụ: cần PRO banner)
            // binding.btnStartLearning.startAnimation(shakeAnim)

            // Logic mở màn hình học tập
        }

        binding.btnViewPro.setOnClickListener {
            // Logic mở lộ trình PRO
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}