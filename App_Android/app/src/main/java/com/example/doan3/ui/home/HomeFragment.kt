package com.example.doan3.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.doan3.R
import com.example.doan3.databinding.FragmentHomeBinding
import com.example.doan3.ui.vocabulary.VocabularyFragment
import com.example.doan3.ui.roadmap.RoadmapFragment
import com.example.doan3.ui.profile.RetrofitClient
import com.example.doan3.ui.vocabulary.Vocabulary
import com.example.doan3.ui.profile.UserStatsResponse
import com.example.doan3.ui.profile.PredictForgettingResponse
import com.example.doan3.ui.profile.PredictiveFlashcard
import com.google.android.material.bottomsheet.BottomSheetDialog
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
        viewModel.userName.observe(viewLifecycleOwner) { name -> binding.tvUserName.text = name }
        viewModel.userLevel.observe(viewLifecycleOwner) { level -> binding.tvUserLevel.text = level }
        viewModel.streakDays.observe(viewLifecycleOwner) { days -> binding.tvStreakDays.text = "$days ngày" }
        viewModel.progressPercent.observe(viewLifecycleOwner) { percent ->
            binding.progressBar.progress = percent
            binding.tvTaskPercent.text = "$percent% Hoàn\nthành"
        }
        viewModel.taskTargetText.observe(viewLifecycleOwner) { targetText -> binding.tvTaskTarget.text = targetText }
        viewModel.wordsCount.observe(viewLifecycleOwner) { count -> binding.tvWordsCount.text = count.toString() }
        viewModel.timeLearned.observe(viewLifecycleOwner) { time -> binding.tvTimeLearned.text = time }
        viewModel.nextLessonName.observe(viewLifecycleOwner) { lessonName -> binding.tvLessonName.text = lessonName }
        viewModel.nextLessonXp.observe(viewLifecycleOwner) { xpText -> binding.tvXp.text = xpText }

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
        checkPredictiveFlashcards() // Tích hợp logic kiểm tra sổ tay tiên tri
    }

    // ====================================================================
    // 🚀 LOGIC KIỂM TRA TỪ VỰNG DỄ QUÊN ĐÃ ĐƯỢC BỌC THÉP CHỐNG LỖI
    // ====================================================================
    private fun checkPredictiveFlashcards() {
        val ctx = context ?: return // 🚀 Tránh crash nếu Fragment bị đóng khi đang gọi API
        val apiService = RetrofitClient.getInstance(ctx)

        apiService.getPredictiveFlashcards().enqueue(object : Callback<PredictForgettingResponse> {
            override fun onResponse(call: Call<PredictForgettingResponse>, response: Response<PredictForgettingResponse>) {
                if (_binding == null || !isAdded) return

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    val flashcards = data.predictiveFlashcards ?: emptyList() // Đề phòng lỗi Parse Null

                    if (flashcards.isNotEmpty()) {
                        binding.cardPredictiveWarning.visibility = View.VISIBLE
                        binding.tvPredictiveDesc.text = data.warning

                        binding.btnReviewNow.setOnClickListener {
                            showPredictiveBottomSheet(flashcards)
                        }
                    } else {
                        binding.cardPredictiveWarning.visibility = View.GONE
                    }
                } else {
                    binding.cardPredictiveWarning.visibility = View.GONE
                    Log.e("HomeFragment", "Lỗi Server API Sổ Tay: ${response.code()} - ${response.errorBody()?.string()}")
                    Toast.makeText(ctx, "Lỗi tải dữ liệu Sổ tay tiên tri (Code: ${response.code()})", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<PredictForgettingResponse>, t: Throwable) {
                if (_binding != null && isAdded) {
                    binding.cardPredictiveWarning.visibility = View.GONE
                    Log.e("HomeFragment", "Lỗi mạng Sổ tay: ${t.message}")
                }
            }
        })
    }

    private fun showPredictiveBottomSheet(flashcards: List<PredictiveFlashcard>) {
        val bottomSheet = BottomSheetDialog(requireContext())
        val layout = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(60, 60, 60, 60)
            setBackgroundColor(android.graphics.Color.WHITE)
        }

        val titleText = android.widget.TextView(requireContext()).apply {
            text = "⚠️ TỪ VỰNG DỄ QUÊN"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(android.graphics.Color.parseColor("#DC2626"))
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 40)
        }
        layout.addView(titleText)

        flashcards.forEach { card ->
            val wordView = android.widget.TextView(requireContext()).apply {
                val acc = if (card.predicted_accuracy > 1.0) card.predicted_accuracy else card.predicted_accuracy * 100
                val dangerLevel = (100 - acc).toInt().coerceIn(0, 100)

                val reasonLine = if (!card.xai_reason.isNullOrBlank()) "\n  🧠 ${card.xai_reason}" else ""
                text = "• ${card.wordText} (${card.phonetic})\n  Nghĩa: ${card.meaning}\n  Nguy cơ quên: $dangerLevel%$reasonLine"
                textSize = 16f
                setTextColor(android.graphics.Color.parseColor("#1E293B"))
                setPadding(0, 10, 0, 30)
            }
            layout.addView(wordView)
        }

        val btnClose = android.widget.Button(requireContext()).apply {
            text = "ĐẾN SỔ TAY ÔN TẬP"
            setBackgroundColor(android.graphics.Color.parseColor("#DC2626"))
            setTextColor(android.graphics.Color.WHITE)
            setPadding(0, 30, 0, 30)
            setOnClickListener {
                bottomSheet.dismiss()

                val intent = android.content.Intent(requireContext(), com.example.doan3.ui.profile.KhamPhaActivity::class.java)
                val wordList = ArrayList(flashcards.map { it.wordText })
                intent.putStringArrayListExtra("PREDICTIVE_WORDS", wordList)
                intent.putExtra("CATEGORY_NAME", "AI Ôn Tập")
                startActivity(intent)
            }
        }

        val btnParams = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 30, 0, 0) }

        layout.addView(btnClose, btnParams)
        bottomSheet.setContentView(layout)
        bottomSheet.show()
    }

    // ====================================================================
    // HÀM TỰ ĐỘNG ĐẾM SỐ TỪ VỰNG, CẬP NHẬT TIẾN ĐỘ & THỜI GIAN
    // ====================================================================
    private fun syncLearnedWords() {
        val ctx = context ?: return // Cập nhật biến ctx an toàn
        val apiService = RetrofitClient.getInstance(ctx)

        // --------------------------------------------------------------------
        // LOGIC 1: LẤY THỐNG KÊ TỪ BACKEND (getUserStats)
        // --------------------------------------------------------------------
        Log.d("HomeFragment", "Đang gọi API getUserStats...")

        apiService.getUserStats().enqueue(object : Callback<UserStatsResponse> {
            override fun onResponse(call: Call<UserStatsResponse>, response: Response<UserStatsResponse>) {
                if (_binding == null || !isAdded) return

                if (response.isSuccessful && response.body() != null) {
                    val stats = response.body()?.stats
                    val learnedCount = stats?.wordsLearned ?: 0
                    Log.d("HomeFragment", "API Thành công! Số từ đã học: $learnedCount")

                    binding.tvWordsCount.text = learnedCount.toString()

                    val sharedPref = ctx.getSharedPreferences("StudyData", android.content.Context.MODE_PRIVATE)
                    val totalSeconds = sharedPref.getInt("totalSeconds", 0)
                    val totalMinutes = totalSeconds / 60

                    binding.tvTimeLearned.text = "$totalMinutes phút"
                } else {
                    Log.e("HomeFragment", "API Lỗi: Code ${response.code()}")
                }
            }

            override fun onFailure(call: Call<UserStatsResponse>, t: Throwable) {
                Log.e("HomeFragment", "API Thất bại: ${t.message}")
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
                    val sharedPref = ctx.getSharedPreferences("StudyData", android.content.Context.MODE_PRIVATE)
                    val totalSeconds = sharedPref.getInt("totalSeconds", 0)

                    val totalMinutes = totalSeconds / 60
                    binding.tvTimeLearned.text = "$totalMinutes phút"
                }
            }

            override fun onFailure(call: Call<List<Vocabulary>>, t: Throwable) {
                Log.e("HomeFragment", "Lỗi tải dữ liệu: ${t.message}")
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}