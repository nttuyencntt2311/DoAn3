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
import com.example.doan3.MainActivity
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
    private var serverTotalWords: Int = 0
    private var serverAccuracy: Int = 0

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
            val fm = requireActivity().supportFragmentManager
            if (fm.backStackEntryCount > 0) {
                fm.popBackStack()
            } else {
                (activity as? MainActivity)?.openHomeTab()
            }
        }

        setupTabs()

        viewModel.fetchRealStatistics(requireContext())

        viewModel.statsData.observe(viewLifecycleOwner) { stats ->
            binding.tvStreakDays.text = stats.streakDays.toString()
            binding.tvTotalTime.text = "${stats.totalHours} giờ"

            serverTotalWords = stats.totalWords
            serverAccuracy = stats.accuracyPercent

            binding.tvTotalWords.text = stats.totalWords.toString()
            binding.tvAccuracy.text = "${stats.accuracyPercent}%"

            drawChart(stats.chartData ?: listOf(0, 0, 0, 0, 0, 0, 0))
        }

        // Chỉ fallback đếm số từ ĐÃ THUỘC (proficiency >= 80) nếu server trả 0, không bao giờ gán tổng từ hệ thống
        val apiService = RetrofitClient.getInstance(requireContext())
        apiService.getAllVocabularies().enqueue(object : Callback<List<Vocabulary>> {
            override fun onResponse(call: Call<List<Vocabulary>>, response: Response<List<Vocabulary>>) {
                if (_binding == null || !isAdded) return
                if (response.isSuccessful) {
                    val allWords = response.body() ?: emptyList()
                    val masteredCount = allWords.count { it.proficiency >= 80 }
                    val computedAccuracy = if (allWords.isNotEmpty()) {
                        (masteredCount * 100) / allWords.size
                    } else 0

                    if (serverTotalWords == 0) {
                        binding.tvTotalWords.text = masteredCount.toString()
                    }
                    if (serverAccuracy == 0) {
                        binding.tvAccuracy.text = "${computedAccuracy}%"
                    }
                }
            }

            override fun onFailure(call: Call<List<Vocabulary>>, t: Throwable) {
                // Giữ nguyên dữ liệu từ statsData
            }
        })

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

    private fun createMemoryCard(memory: MemoryItem): View {
        val density = resources.displayMetrics.density
        val pad = (16 * density).toInt()
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, (16 * density).toInt())
            }
            setBackgroundResource(R.drawable.bg_bar_rounded)
            setPadding(pad, pad, pad, pad)
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
            setPadding(0, (4 * density).toInt(), 0, (4 * density).toInt())
        }

        val tvDateAndXp = TextView(requireContext()).apply {
            text = "${memory.date} • +${memory.xp} XP"
            textSize = 11f
            setTextColor(Color.parseColor("#2563EB"))
            setTypeface(null, Typeface.BOLD_ITALIC)
        }

        layout.addView(tvTitle)
        layout.addView(tvDesc)
        layout.addView(tvDateAndXp)

        return layout
    }

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

    private fun drawChart(data: List<Int>) {
        binding.chartContainer.removeAllViews()
        val daysOfWeek = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")

        val maxPercent = 100f
        val maxHeightDp = 82

        for (i in 0 until 7) {
            val percent = if (i < data.size) data[i] else 0

            val columnLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            }

            val valueText = TextView(requireContext()).apply {
                text = if (percent > 0) "$percent%" else ""
                textSize = 10f
                setTextColor(Color.parseColor("#0056D2"))
                setTypeface(null, Typeface.BOLD)
                gravity = Gravity.CENTER
            }

            val barView = View(requireContext()).apply {
                setBackgroundColor(if (percent > 0) Color.parseColor("#0056D2") else Color.parseColor("#E2E8F0"))
                val heightInPx = ((percent / maxPercent) * maxHeightDp * resources.displayMetrics.density).toInt()
                val minHeightPx = if (percent > 0) (8 * resources.displayMetrics.density).toInt() else (4 * resources.displayMetrics.density).toInt()

                layoutParams = LinearLayout.LayoutParams(
                    (16 * resources.displayMetrics.density).toInt(),
                    kotlin.math.max(heightInPx, minHeightPx)
                ).apply {
                    topMargin = (2 * resources.displayMetrics.density).toInt()
                    bottomMargin = (4 * resources.displayMetrics.density).toInt()
                }
            }

            val dayText = TextView(requireContext()).apply {
                text = daysOfWeek[i]
                textSize = 11f
                setTextColor(if (percent > 0) Color.parseColor("#1A1A1A") else Color.parseColor("#888888"))
                gravity = Gravity.CENTER
            }

            columnLayout.addView(valueText)
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