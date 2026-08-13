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
import com.example.doan3.ui.profile.RetrofitClient
import com.example.doan3.ui.vocabulary.Vocabulary
import com.example.doan3.ui.profile.UserStatsResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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

        // ====================================================================
        // ĐỔ DỮ LIỆU VÀO GIAO DIỆN
        // ====================================================================
        viewModel.userName.observe(viewLifecycleOwner) { name ->
            binding.tvUserName.text = name
        }

        viewModel.userLevel.observe(viewLifecycleOwner) { level ->
            binding.tvUserLevel.text = level
        }

        viewModel.streakDays.observe(viewLifecycleOwner) { days ->
            binding.tvStreakDays.text = "$days ngày"
        }

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

        binding.tvStreakDays.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, com.example.doan3.ui.statistics.StatisticsFragment())
                .addToBackStack(null).commit()
        }
    }

    // ====================================================================
    // HÀM CHẠY MỖI KHI NGƯỜI DÙNG QUAY LẠI TRANG CHỦ
    // ====================================================================
    override fun onResume() {
        super.onResume()

        viewModel.fetchUserData(requireContext())
        syncLearnedWords()
    }

    // ====================================================================
    // HÀM TỰ ĐỘNG ĐẾM SỐ TỪ VỰNG, CẬP NHẬT TIẾN ĐỘ & THỜI GIAN
    // ====================================================================
    private fun syncLearnedWords() {
        val context = activity ?: return
        val apiService = RetrofitClient.getInstance(context)

        // --------------------------------------------------------------------
        // LOGIC 1: LẤY THỐNG KÊ TỪ BACKEND (getUserStats)
        // --------------------------------------------------------------------
        android.util.Log.d("HomeFragment", "Đang gọi API getUserStats...")

        apiService.getUserStats().enqueue(object : Callback<UserStatsResponse> {
            override fun onResponse(call: Call<UserStatsResponse>, response: Response<UserStatsResponse>) {
                if (_binding == null || !isAdded) return

                if (response.isSuccessful && response.body() != null) {
                    val stats = response.body()?.stats
                    val learnedCount = stats?.wordsLearned ?: 0
                    android.util.Log.d("HomeFragment", "API Thành công! Số từ đã học: $learnedCount")

                    binding.tvWordsCount.text = learnedCount.toString()

                    val sharedPref = requireContext().getSharedPreferences("StudyData", android.content.Context.MODE_PRIVATE)
                    val totalSeconds = sharedPref.getInt("totalSeconds", 0)
                    val totalMinutes = totalSeconds / 60

                    binding.tvTimeLearned.text = "$totalMinutes phút"
                } else {
                    android.util.Log.e("HomeFragment", "API Lỗi: Code ${response.code()}")
                }
            }

            override fun onFailure(call: Call<UserStatsResponse>, t: Throwable) {
                android.util.Log.e("HomeFragment", "API Thất bại: ${t.message}")
            }
        })

        // --------------------------------------------------------------------
        // LOGIC 2: ĐẾM SỐ TỪ VỰNG, CẬP NHẬT TIẾN ĐỘ & THỜI GIAN (getAllVocabularies)
        // --------------------------------------------------------------------
        apiService.getAllVocabularies().enqueue(object : Callback<List<Vocabulary>> {
            override fun onResponse(call: Call<List<Vocabulary>>, response: Response<List<Vocabulary>>) {
                if (_binding == null || !isAdded) return

                if (response.isSuccessful) {
                    val allWords = response.body() ?: emptyList()

                    // 1. Đếm tổng số từ đã thuộc (Thành thạo >= 80)
                    val learnedCount = allWords.count { it.proficiency >= 80 }
                    binding.tvWordsCount.text = learnedCount.toString()

                    // 2. CẬP NHẬT NHIỆM VỤ HÀNG NGÀY TRÊN GIAO DIỆN
                    val dailyTarget = 10
                    val wordsToday = learnedCount % dailyTarget

                    val percent = if (wordsToday == 0 && learnedCount > 0) 100 else ((wordsToday.toFloat() / dailyTarget) * 100).toInt()
                    val displayWords = if (wordsToday == 0 && learnedCount > 0) dailyTarget else wordsToday

                    binding.progressBar.progress = percent
                    binding.tvTaskPercent.text = "$percent% Hoàn\nthành"
                    binding.tvTaskTarget.text = "Mục tiêu hôm nay: $displayWords/$dailyTarget từ mới"

                    // 3. ĐỌC THỜI GIAN THỰC TẾ TỪ BỘ NHỚ MÁY (SharedPreferences)
                    val sharedPref = requireContext().getSharedPreferences("StudyData", android.content.Context.MODE_PRIVATE)
                    val totalSeconds = sharedPref.getInt("totalSeconds", 0)

                    val totalMinutes = totalSeconds / 60
                    binding.tvTimeLearned.text = "$totalMinutes phút"
                }
            }

            override fun onFailure(call: Call<List<Vocabulary>>, t: Throwable) {
                android.util.Log.e("HomeFragment", "Lỗi tải dữ liệu: ${t.message}")
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}