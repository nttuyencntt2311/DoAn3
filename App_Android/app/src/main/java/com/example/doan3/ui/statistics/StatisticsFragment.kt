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

        setupTabs() // Giữ nguyên hàm điều hướng tab của bạn (nếu có)

        // Ra lệnh cho ViewModel gọi API lấy thống kê
        viewModel.fetchRealStatistics(requireContext())

        // =========================================================
        // LẮNG NGHE VÀ KÉO DỮ LIỆU ĐỔ VÀO GIAO DIỆN
        // =========================================================
        viewModel.statsData.observe(viewLifecycleOwner) { stats ->

            // 1. Kéo dữ liệu cho Chuỗi ngày học
            binding.tvStreakDays.text = stats.streakDays.toString()

            // 2. Kéo dữ liệu vào bảng MaterialCardView (Tổng giờ, Từ vựng, Chính xác)
            // Lưu ý: Nối thêm chữ " giờ" và "%" để giống với thiết kế XML của bạn
            binding.tvTotalTime.text = "${stats.totalHours} giờ"
            binding.tvTotalWords.text = stats.totalWords.toString()
            binding.tvAccuracy.text = "${stats.accuracyPercent}%"

            // 3. Kéo mảng dữ liệu (List<Int>) truyền vào hàm vẽ biểu đồ
            drawChart(stats.chartData ?: listOf(0, 0, 0, 0, 0, 0, 0))
        }
        // LẮNG NGHE DỮ LIỆU KỶ NIỆM TỪ VIEWMODEL
        viewModel.memoriesData.observe(viewLifecycleOwner) { memories ->
            binding.containerMemories.removeAllViews() // Xóa view cũ tránh trùng lặp

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
                // Vẽ từng tấm thẻ Kỷ niệm
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
                setMargins(0, 0, 0, (16 * resources.displayMetrics.density).toInt()) // Margin Bottom 16dp
            }
            setBackgroundResource(R.drawable.bg_bar_rounded) // Tạo sẵn một background bo góc màu trắng trong res/drawable
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
            setTextColor(Color.parseColor("#2563EB")) // Màu xanh nổi bật
            setTypeface(null, Typeface.BOLD_ITALIC)
        }

        layout.addView(tvTitle)
        layout.addView(tvDesc)
        layout.addView(tvDateAndXp)

        return layout
    }

    // Hàm thiết lập Tabs (Giữ nguyên phần khung của bạn)
    private fun setupTabs() {
        // 1. KHI NGƯỜI DÙNG BẤM VÀO TAB "THỐNG KÊ"
        binding.tabStatistics.setOnClickListener {
            // --- TAB THỐNG KÊ (ACTIVE) ---
            binding.tabStatistics.setCardBackgroundColor(Color.WHITE)
            binding.tvTabStatistics.setTextColor(Color.parseColor("#1A1A1A"))
            binding.tabStatistics.cardElevation = 8f // Bật lại bóng đổ cho tab đang chọn

            // --- TAB KỶ NIỆM (INACTIVE) ---
            binding.tabMemories.setCardBackgroundColor(Color.TRANSPARENT)
            binding.tvTabMemories.setTextColor(Color.parseColor("#888888"))
            binding.tabMemories.cardElevation = 0f // Tắt bóng đổ để xóa viền đen mờ mờ

            // Hiển thị phần Thống Kê (thông qua Group), Ẩn phần Kỷ niệm
            binding.groupStatistics.visibility = View.VISIBLE
            binding.layoutMemories.visibility = View.GONE
        }

        // 2. KHI NGƯỜI DÙNG BẤM VÀO TAB "KỶ NIỆM"
        binding.tabMemories.setOnClickListener {
            // --- TAB KỶ NIỆM (ACTIVE) ---
            binding.tabMemories.setCardBackgroundColor(Color.WHITE)
            binding.tvTabMemories.setTextColor(Color.parseColor("#1A1A1A"))
            binding.tabMemories.cardElevation = 8f // Bật lại bóng đổ cho tab đang chọn

            // --- TAB THỐNG KÊ (INACTIVE) ---
            binding.tabStatistics.setCardBackgroundColor(Color.TRANSPARENT)
            binding.tvTabStatistics.setTextColor(Color.parseColor("#888888"))
            binding.tabStatistics.cardElevation = 0f // Tắt bóng đổ để xóa viền đen mờ mờ

            // Hiển thị phần Kỷ niệm, Ẩn phần Thống Kê (thông qua Group)
            binding.layoutMemories.visibility = View.VISIBLE
            binding.groupStatistics.visibility = View.GONE
        }
    }
    // Hàm vẽ biểu đồ cột 7 ngày (Giữ nguyên logic code tuyệt vời của bạn)
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
                setBackgroundColor(Color.parseColor("#0056D2")) // Màu xanh chủ đạo

                // Quy đổi chiều cao phần trăm ra pixel hiển thị
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