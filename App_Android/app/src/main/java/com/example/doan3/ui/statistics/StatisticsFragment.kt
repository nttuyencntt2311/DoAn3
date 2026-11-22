package com.example.doan3.ui.statistics

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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

            override fun onFailure(call: Call<List<Vocabulary>>, t: Throwable) {}
        })

        viewModel.memoriesData.observe(viewLifecycleOwner) { memories ->
            binding.containerMemories.removeAllViews()

            if (memories.isEmpty()) {
                val emptyText = TextView(requireContext()).apply {
                    text = "Bạn chưa có kỷ niệm nào. Hãy học chăm chỉ hơn nhé!"
                    textSize = 14f
                    setTextColor(Color.parseColor("#64748B"))
                    setPadding(0, (32 * resources.displayMetrics.density).toInt(), 0, 0)
                    gravity = Gravity.CENTER
                }
                binding.containerMemories.addView(emptyText)
            } else {
                for (memory in memories) {
                    binding.containerMemories.addView(createMemoryCard(memory))
                }
            }
        }
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    private fun createMemoryCard(memory: MemoryItem): View {
        val ctx = requireContext()
        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 12.dp()
            }
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 18.dp().toFloat()
                setStroke(1.dp(), Color.parseColor("#E2E8F0"))
            }
            setPadding(18.dp(), 16.dp(), 18.dp(), 16.dp())
            elevation = 2f
        }

        val tvTitle = TextView(ctx).apply {
            text = memory.title
            textSize = 16f
            setTextColor(Color.parseColor("#0F172A"))
            setTypeface(null, Typeface.BOLD)
        }

        val tvDesc = TextView(ctx).apply {
            text = memory.description
            textSize = 13.5f
            setTextColor(Color.parseColor("#475569"))
            setPadding(0, 6.dp(), 0, 12.dp())
        }

        val footerRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val tvDate = TextView(ctx).apply {
            text = memory.date
            textSize = 12f
            setTextColor(Color.parseColor("#64748B"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val tvXpBadge = TextView(ctx).apply {
            text = "+${memory.xp} XP"
            textSize = 12f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#2563EB"))
            setPadding(10.dp(), 4.dp(), 10.dp(), 4.dp())
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#EFF6FF"))
                cornerRadius = 10.dp().toFloat()
            }
        }

        footerRow.addView(tvDate)
        footerRow.addView(tvXpBadge)

        layout.addView(tvTitle)
        layout.addView(tvDesc)
        layout.addView(footerRow)

        return layout
    }

    private fun setupTabs() {
        binding.tabStatistics.setOnClickListener {
            binding.tabStatistics.setCardBackgroundColor(Color.WHITE)
            binding.tvTabStatistics.setTextColor(Color.parseColor("#0F172A"))
            binding.tabStatistics.cardElevation = 2f

            binding.tabMemories.setCardBackgroundColor(Color.TRANSPARENT)
            binding.tvTabMemories.setTextColor(Color.parseColor("#64748B"))
            binding.tabMemories.cardElevation = 0f

            binding.groupStatistics.visibility = View.VISIBLE
            binding.layoutMemories.visibility = View.GONE
        }

        binding.tabMemories.setOnClickListener {
            binding.tabMemories.setCardBackgroundColor(Color.WHITE)
            binding.tvTabMemories.setTextColor(Color.parseColor("#0F172A"))
            binding.tabMemories.cardElevation = 2f

            binding.tabStatistics.setCardBackgroundColor(Color.TRANSPARENT)
            binding.tvTabStatistics.setTextColor(Color.parseColor("#64748B"))
            binding.tabStatistics.cardElevation = 0f

            binding.layoutMemories.visibility = View.VISIBLE
            binding.groupStatistics.visibility = View.GONE
        }
    }

    private fun drawChart(data: List<Int>) {
        binding.chartContainer.removeAllViews()
        val daysOfWeek = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")

        val maxPercent = 100f
        val maxHeightDp = 92
        val radiusPx = 7.dp().toFloat()

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
                setTextColor(Color.parseColor("#2563EB"))
                setTypeface(null, Typeface.BOLD)
                gravity = Gravity.CENTER
            }

            val barView = View(requireContext()).apply {
                background = GradientDrawable().apply {
                    setColor(if (percent > 0) Color.parseColor("#2563EB") else Color.parseColor("#E2E8F0"))
                    cornerRadii = floatArrayOf(radiusPx, radiusPx, radiusPx, radiusPx, 2f, 2f, 2f, 2f)
                }
                val heightInPx = ((percent / maxPercent) * maxHeightDp * resources.displayMetrics.density).toInt()
                val minHeightPx = if (percent > 0) 8.dp() else 4.dp()

                layoutParams = LinearLayout.LayoutParams(
                    18.dp(),
                    kotlin.math.max(heightInPx, minHeightPx)
                ).apply {
                    topMargin = 4.dp()
                    bottomMargin = 6.dp()
                }
            }

            val dayText = TextView(requireContext()).apply {
                text = daysOfWeek[i]
                textSize = 11.5f
                setTextColor(if (percent > 0) Color.parseColor("#0F172A") else Color.parseColor("#94A3B8"))
                setTypeface(null, if (percent > 0) Typeface.BOLD else Typeface.NORMAL)
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
