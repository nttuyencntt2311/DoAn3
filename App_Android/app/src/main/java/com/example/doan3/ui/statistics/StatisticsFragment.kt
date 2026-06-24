package com.example.doan3.ui.statistics

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.doan3.R // Import R của bạn để lấy icon
import com.example.doan3.databinding.FragmentStatisticsBinding
import com.google.android.material.card.MaterialCardView

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

        // Lắng nghe dữ liệu THỐNG KÊ
        viewModel.statsData.observe(viewLifecycleOwner) { stats ->
            binding.tvStreakDays.text = stats.streakDays.toString()
            binding.tvTotalTime.text = "${stats.totalHours} giờ"
            binding.tvTotalWords.text = String.format("%,d", stats.totalWords) // Thêm dấu phẩy cho đẹp (1,240)
            binding.tvAccuracy.text = "${stats.accuracyPercent}%"
            drawWeeklyChart(stats.chartData)
        }

        // Lắng nghe dữ liệu KỶ NIỆM
        viewModel.memoriesData.observe(viewLifecycleOwner) { memories ->
            drawMemories(memories)
        }
    }

    private fun setupTabs() {
        val density = resources.displayMetrics.density

        binding.tabStatistics.setOnClickListener {
            binding.tabStatistics.setCardBackgroundColor(Color.WHITE)
            binding.tabStatistics.cardElevation = 2f * density
            binding.tvTabStatistics.setTextColor(Color.parseColor("#1A1A1A"))

            binding.tabMemories.setCardBackgroundColor(Color.TRANSPARENT)
            binding.tabMemories.cardElevation = 0f
            binding.tvTabMemories.setTextColor(Color.parseColor("#888888"))

            binding.groupStatistics.visibility = View.VISIBLE
            binding.layoutMemories.visibility = View.GONE
        }

        binding.tabMemories.setOnClickListener {
            binding.tabMemories.setCardBackgroundColor(Color.WHITE)
            binding.tabMemories.cardElevation = 2f * density
            binding.tvTabMemories.setTextColor(Color.parseColor("#1A1A1A"))

            binding.tabStatistics.setCardBackgroundColor(Color.TRANSPARENT)
            binding.tabStatistics.cardElevation = 0f
            binding.tvTabStatistics.setTextColor(Color.parseColor("#888888"))

            binding.groupStatistics.visibility = View.GONE
            binding.layoutMemories.visibility = View.VISIBLE
        }
    }

    // ĐÃ VIẾT LẠI HÀM NÀY ĐỂ GIỐNG ẢNH THIẾT KẾ CỦA BẠN (CÓ ICON VÀ +XP)
    private fun drawMemories(memories: List<MemoryItem>) {
        binding.containerMemories.removeAllViews()
        val density = resources.displayMetrics.density

        for (memory in memories) {
            // Thẻ Card bên ngoài
            val card = MaterialCardView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = (16 * density).toInt() }
                setCardBackgroundColor(Color.WHITE)
                radius = 16f * density
                cardElevation = 2f * density
                strokeWidth = 0
            }

            // Thanh ngang chứa toàn bộ (Icon - Text - XP)
            val rowLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding((16 * density).toInt(), (16 * density).toInt(), (16 * density).toInt(), (16 * density).toInt())
            }

            // 1. Cục Icon bên trái (Tạm dùng icon ngôi sao mặc định)
            val iconView = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams((48 * density).toInt(), (48 * density).toInt()).apply {
                    marginEnd = (16 * density).toInt()
                }
                // Bạn có thể đổi hình vẽ thành icon xịn của bạn ở dòng dưới nhé
                setImageResource(R.drawable.ic_launcher_foreground)
                setPadding((8 * density).toInt(), (8 * density).toInt(), (8 * density).toInt(), (8 * density).toInt())
            }

            // 2. Cục Text ở giữa (Chứa Title, Desc, Date)
            val textLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val tvTitle = TextView(requireContext()).apply {
                text = memory.title
                textSize = 15f
                setTextColor(Color.parseColor("#1A1A1A"))
                setTypeface(null, Typeface.BOLD)
            }

            val tvDesc = TextView(requireContext()).apply {
                text = memory.description
                textSize = 13f
                setTextColor(Color.parseColor("#666666"))
                setPadding(0, (4 * density).toInt(), 0, (4 * density).toInt())
            }

            val tvDate = TextView(requireContext()).apply {
                text = memory.date
                textSize = 12f
                setTextColor(Color.parseColor("#888888"))
            }

            textLayout.addView(tvTitle)
            textLayout.addView(tvDesc)
            textLayout.addView(tvDate)

            // 3. Cục Điểm XP bên phải
            val tvXp = TextView(requireContext()).apply {
                text = "+${memory.xp} xp"
                textSize = 16f
                setTextColor(Color.parseColor("#FF9800")) // Màu cam giống ảnh
                setTypeface(null, Typeface.BOLD)
            }

            // Gắn 3 cục vào thanh ngang
            rowLayout.addView(iconView)
            rowLayout.addView(textLayout)
            rowLayout.addView(tvXp)

            card.addView(rowLayout)
            binding.containerMemories.addView(card)
        }
    }

    private fun drawWeeklyChart(data: List<Int>) {
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
                layoutParams = LinearLayout.LayoutParams((16 * resources.displayMetrics.density).toInt(), heightInPx).apply {
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