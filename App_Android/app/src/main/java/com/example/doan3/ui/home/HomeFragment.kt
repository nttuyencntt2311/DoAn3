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

        // (ĐÃ XÓA dòng fetchUserData ở đây để tránh bị gọi 2 lần, chuyển nó xuống hàm onResume bên dưới)

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

        // Lưu ý: wordsCount và timeLearned sẽ được nạp ban đầu từ ViewModel,
        // sau đó sẽ được cập nhật chính xác lại từ hàm syncLearnedWords() bên dưới.
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
    // HÀM NÀY CHẠY MỖI KHI NGƯỜI DÙNG QUAY LẠI TRANG CHỦ
    // ====================================================================
    override fun onResume() {
        super.onResume()
        // 1. Thực hiện quét và nạp dữ liệu từ Backend (MongoDB)
        viewModel.fetchUserData(requireContext())

        // 2. Kích hoạt hàm đồng bộ từ vựng ngay lập tức
        syncLearnedWords()
    }

    // ====================================================================
    // HÀM TỰ ĐỘNG ĐẾM SỐ TỪ VỰNG VÀ THỜI GIAN ĐỂ ĐẨY RA TRANG CHỦ
    // ====================================================================
    private fun syncLearnedWords() {
        val context = activity ?: return
        val apiService = RetrofitClient.getInstance(context)

        apiService.getAllVocabularies().enqueue(object : Callback<List<Vocabulary>> {
            override fun onResponse(call: Call<List<Vocabulary>>, response: Response<List<Vocabulary>>) {
                if (_binding == null || !isAdded) return

                if (response.isSuccessful) {
                    val allWords = response.body() ?: emptyList()
                    val learnedCount = allWords.count { it.proficiency >= 80 }
                    binding.tvWordsCount.text = learnedCount.toString()

                    // ====================================================================
                    // ĐỌC THỜI GIAN THỰC TẾ TỪ BỘ NHỚ MÁY (SharedPreferences)
                    // ====================================================================
                    val sharedPref = requireContext().getSharedPreferences("StudyData", android.content.Context.MODE_PRIVATE)
                    val totalSeconds = sharedPref.getInt("totalSeconds", 0)

                    // Chuyển đổi giây thành phút (Ví dụ 130 giây = 2 phút)
                    val totalMinutes = totalSeconds / 60

                    binding.tvTimeLearned.text = "$totalMinutes phút"
                }
            }

            override fun onFailure(call: Call<List<Vocabulary>>, t: Throwable) {
                // Có thể thêm log lỗi vào đây nếu cần thiết
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}