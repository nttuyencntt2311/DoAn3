package com.example.doan3.ui.statistics

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.doan3.R
import com.example.doan3.databinding.FragmentStatisticsBinding
import com.example.doan3.ui.profile.MemoryItem
import com.example.doan3.ui.profile.RetrofitClient
import com.example.doan3.ui.vocabulary.Vocabulary
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatisticsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        setupTabs()

        // Ra lệnh cho ViewModel gọi API lấy thống kê (Chuỗi ngày, tổng giờ, biểu đồ)
        viewModel.fetchRealStatistics(requireContext())

        // =========================================================
        // 1. KÉO DỮ LIỆU TỪ VỰNG & % THÀNH THẠO (GIỐNG HỆT LEARNING)
        // =========================================================
        val apiService = RetrofitClient.getInstance(requireContext())
        apiService.getAllVocabularies().enqueue(object : Callback<List<Vocabulary>> {
            override fun onResponse(call: Call<List<Vocabulary>>, response: Response<List<Vocabulary>>) {
                if (_binding == null || !isAdded) return

                if (response.isSuccessful) {
                    val allWords = response.body() ?: emptyList()

                    // Tính Tổng số từ
                    val totalWordsCount = allWords.size

                    // Tính % Thành thạo (Từ có proficiency >= 80)
                    val masteredCount = allWords.count { it.proficiency >= 80 }
                    val accuracy = if (totalWordsCount > 0) {
                        (masteredCount * 100) / totalWordsCount
                    } else 0

                    // Đổ dữ liệu thẳng vào ID của giao diện XML
                    binding.tvTotalWords.text = totalWordsCount.toString()
                    binding.tvAccuracy.text = "${accuracy}%"
                }
            }

            override fun onFailure(call: Call<List<Vocabulary>>, t: Throwable) {
                // Xử lý lỗi nếu cần
            }
        })

        // =========================================================
        // 2. LẮNG NGHE VÀ ĐỔ DỮ LIỆU CÒN LẠI VÀO GIAO DIỆN
        // =========================================================
        viewModel.statsData.observe(viewLifecycleOwner) { stats ->
            // Kéo dữ liệu cho Chuỗi ngày học
            binding.tvStreakDays.text = stats.streakDays.toString()

            // Kéo dữ liệu Tổng giờ học
            binding.tvTotalTime.text = "${stats.totalHours} giờ"

            // Kéo mảng dữ liệu (List<Int>) truyền vào hàm vẽ biểu đồ
            drawChart(stats.chartData ?: listOf(0, 0, 0, 0, 0, 0, 0))
        }

        // =========================================================
        // 3. LẮNG NGHE DỮ LIỆU KỶ NIỆM TỪ VIEWMODEL
        // =========================================================
        viewModel.memoriesData.observe(viewLifecycleOwner) { memories ->
            binding.containerMemories.removeAllViews()

            if (memories.isEmpty()) {
                val emptyText = TextView(requireContext()).apply {
                    text = "Bạn chưa có kỷ niệm nào. Hãy học chăm chỉ hơn nhé!"
                    textSize = 14f
                    setTextColor(Color.parseColor("#888888"))
                    setPadding(0, 50, 0, 0)
                    gravity = Gravity.CENTER
                }
                binding.containerMemories.addView(emptyText)
            } else {
                for (memory in memories) {
                    val cardView = createMemoryCard(memory)
                    binding.containerMemories.addView(cardView)
                }
            }
        }
    }

    // Hàm hỗ trợ vẽ thẻ CardView cho mỗi Kỷ niệm
    private fun createMemoryCard(memory: MemoryItem): View {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, (16 * resources.displayMetrics.density).toInt())
            }
            setBackgroundResource(R.drawable.bg_bar_rounded)
            setPadding(40, 40, 40, 40)
            elevation = 4f
        }

        val tvTitle = TextView(requireContext()).apply {
            text = memory.title
            textSize = 16f
            setTextColor(Color.parseColor("#1A1A1A"))
            setTypeface(null, Typeface.BOLD)
        }

        val tvDesc = TextView(requireContext()).apply {
            text = memory.description
            textSize = 14f
            setTextColor(Color.parseColor("#64748B"))
            setPadding(0, 10, 0, 10)
        }

        val tvDateAndXp = TextView(requireContext()).apply {
            text = "${memory.date} • +${memory.xp} XP"
            textSize = 12f
            setTextColor(Color.parseColor("#2563EB"))
            setTypeface(null, Typeface.BOLD_ITALIC)
        }

        layout.addView(tvTitle)
        layout.addView(tvDesc)
        layout.addView(tvDateAndXp)

        return layout
    }

    // Hàm thiết lập Tabs
    private fun setupTabs() {
        binding.tabStatistics.setOnClickListener {
            binding.tabStatistics.setCardBackgroundColor(Color.WHITE)
            binding.tvTabStatistics.setTextColor(Color.parseColor("#1A1A1A"))
            binding.tabStatistics.cardElevation = 8f

            binding.tabMemories.setCardBackgroundColor(Color.TRANSPARENT)
            binding.tvTabMemories.setTextColor(Color.parseColor("#888888"))
            binding.tabMemories.cardElevation = 0f

            binding.groupStatistics.visibility = View.VISIBLE
            binding.layoutMemories.visibility = View.GONE
        }

        binding.tabMemories.setOnClickListener {
            binding.tabMemories.setCardBackgroundColor(Color.WHITE)
            binding.tvTabMemories.setTextColor(Color.parseColor("#1A1A1A"))
            binding.tabMemories.cardElevation = 8f

            binding.tabStatistics.setCardBackgroundColor(Color.TRANSPARENT)
            binding.tvTabStatistics.setTextColor(Color.parseColor("#888888"))
            binding.tabStatistics.cardElevation = 0f

            binding.layoutMemories.visibility = View.VISIBLE
            binding.groupStatistics.visibility = View.GONE
        }
    }

    // Hàm vẽ biểu đồ cột 7 ngày
    private fun drawChart(data: List<Int>) {
        binding.chartContainer.removeAllViews()
        val daysOfWeek = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")

        for (i in 0 until 7) {
            val percent = if (i < data.size) data[i] else 0
            val columnLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            }

            val barView = View(requireContext()).apply {
                setBackgroundColor(Color.parseColor("#0056D2"))

                val heightInPx = ((percent / 100f) * 100 * resources.displayMetrics.density).toInt()

                layoutParams = LinearLayout.LayoutParams(
                    (16 * resources.displayMetrics.density).toInt(),
                    heightInPx
                ).apply {
                    bottomMargin = (8 * resources.displayMetrics.density).toInt()
                }
            }

            val dayText = TextView(requireContext()).apply {
                text = daysOfWeek[i]
                textSize = 12f
                setTextColor(Color.parseColor("#888888"))
                gravity = Gravity.CENTER
            }

            columnLayout.addView(barView)
            columnLayout.addView(dayText)
            binding.chartContainer.addView(columnLayout)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}