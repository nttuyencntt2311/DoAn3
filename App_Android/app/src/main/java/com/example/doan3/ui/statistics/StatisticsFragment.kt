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
    private var serverStreakDays: Int = 0
    private var serverTotalHours: Int = 0
    private var serverTotalWords: Int = 0
    private var serverAccuracy: Int = 0
    private var latestMemories: List<MemoryItem> = emptyList()

    private data class BadgeMilestone(
        val emoji: String,
        val title: String,
        val subtitle: String,
        val current: Int,
        val target: Int,
        val unit: String,
        val accentHex: String,
        val softBgHex: String
    ) {
        val unlocked: Boolean get() = current >= target
    }

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
            serverStreakDays = stats.streakDays
            serverTotalHours = stats.totalHours
            serverTotalWords = stats.totalWords
            serverAccuracy = stats.accuracyPercent

            binding.tvStreakDays.text = stats.streakDays.toString()
            binding.tvTotalTime.text = "${stats.totalHours} giờ"
            binding.tvTotalWords.text = stats.totalWords.toString()
            binding.tvAccuracy.text = "${stats.accuracyPercent}%"

            drawChart(stats.chartData ?: listOf(0, 0, 0, 0, 0, 0, 0))
            renderMemoriesSection()
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
                        serverTotalWords = masteredCount
                        binding.tvTotalWords.text = masteredCount.toString()
                    }
                    if (serverAccuracy == 0) {
                        serverAccuracy = computedAccuracy
                        binding.tvAccuracy.text = "${computedAccuracy}%"
                    }
                    renderMemoriesSection()
                }
            }

            override fun onFailure(call: Call<List<Vocabulary>>, t: Throwable) {}
        })

        viewModel.memoriesData.observe(viewLifecycleOwner) { memories ->
            latestMemories = memories
            renderMemoriesSection()
        }
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    private fun renderMemoriesSection() {
        if (_binding == null || !isAdded) return
        val ctx = requireContext()
        binding.containerMemories.removeAllViews()

        // 1. Tính danh sách Huy hiệu Thành tựu dựa trên chỉ số thực tế của học viên
        val badges = listOf(
            BadgeMilestone("🔥", "Tia lửa bền bỉ", "Học 3 ngày liên tiếp", serverStreakDays, 3, "ngày", "#EA580C", "#FFF7ED"),
            BadgeMilestone("📚", "Kho từ khởi đầu", "Thuộc 20 từ vựng", serverTotalWords, 20, "từ", "#2563EB", "#EFF6FF"),
            BadgeMilestone("⏳", "Chiến binh chăm chỉ", "Tích lũy 2 giờ học", serverTotalHours, 2, "giờ", "#7C3AED", "#F5F3FF"),
            BadgeMilestone("🎯", "Bậc thầy chính xác", "Đạt độ chính xác 60%", serverAccuracy, 60, "%", "#059669", "#ECFDF5"),
            BadgeMilestone("⚡", "Chuỗi 7 ngày vàng", "Duy trì 7 ngày liên tiếp", serverStreakDays, 7, "ngày", "#D97706", "#FFFBEB"),
            BadgeMilestone("👑", "Học giả 100 từ", "Chinh phục 100 từ vựng", serverTotalWords, 100, "từ", "#4F46E5", "#EEF2FF")
        )
        val unlockedCount = badges.count { it.unlocked }

        // 2. Hero Banner "Hành trình chinh phục LexiVT"
        val heroCard = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20.dp(), 18.dp(), 20.dp(), 18.dp())
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(
                    Color.parseColor("#1E3A8A"),
                    Color.parseColor("#2563EB"),
                    Color.parseColor("#4F46E5")
                )
            ).apply {
                cornerRadius = 20.dp().toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 18.dp() }
        }

        val heroTop = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val tvHeroEmoji = TextView(ctx).apply {
            text = "🏆"
            textSize = 30f
            setPadding(0, 0, 12.dp(), 0)
        }
        val heroTextCol = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val tvHeroTitle = TextView(ctx).apply {
            text = "Hành trình LexiVT của bạn"
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
        }
        val tvHeroSub = TextView(ctx).apply {
            text = "Đã mở khóa $unlockedCount / ${badges.size} huy hiệu thành tựu"
            textSize = 12.5f
            setTextColor(Color.parseColor("#E0E7FF"))
            setPadding(0, 2.dp(), 0, 0)
        }
        heroTextCol.addView(tvHeroTitle)
        heroTextCol.addView(tvHeroSub)
        heroTop.addView(tvHeroEmoji)
        heroTop.addView(heroTextCol)
        heroCard.addView(heroTop)

        // Thanh tiến độ huy hiệu trong Hero
        val progressPct = ((unlockedCount.toFloat() / badges.size) * 100f).toInt().coerceIn(8, 100)
        val heroTrack = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                7.dp()
            ).apply { topMargin = 14.dp() }
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#33FFFFFF"))
                cornerRadius = 4.dp().toFloat()
            }
        }
        val heroFill = View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, progressPct.toFloat())
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FDE68A"))
                cornerRadius = 4.dp().toFloat()
            }
        }
        val heroRest = View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, (100 - progressPct).toFloat())
        }
        heroTrack.addView(heroFill)
        heroTrack.addView(heroRest)
        heroCard.addView(heroTrack)
        binding.containerMemories.addView(heroCard)

        // 3. Tiêu đề Bộ sưu tập Huy hiệu
        binding.containerMemories.addView(createSectionHeader("Bộ sưu tập Huy hiệu", "$unlockedCount/${badges.size}"))

        // Lưới 2 cột các thẻ Huy hiệu
        for (i in badges.indices step 2) {
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 10.dp() }
            }
            val leftCard = createBadgeCard(badges[i]).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                    marginEnd = 5.dp()
                }
            }
            row.addView(leftCard)
            if (i + 1 < badges.size) {
                val rightCard = createBadgeCard(badges[i + 1]).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                        marginStart = 5.dp()
                    }
                }
                row.addView(rightCard)
            }
            binding.containerMemories.addView(row)
        }

        // 4. Dòng thời gian Kỷ niệm (Server memories + các cột mốc thực tế đã đạt)
        binding.containerMemories.addView(createSectionHeader("Dòng thời gian đáng nhớ", "Mới nhất").apply {
            setPadding(0, 12.dp(), 0, 10.dp())
        })

        val timelineItems = mutableListOf<MemoryItem>()
        timelineItems.addAll(latestMemories)

        if (serverTotalWords > 0 && timelineItems.none { it.title.contains("từ vựng", ignoreCase = true) }) {
            timelineItems.add(
                MemoryItem(
                    date = "Cột mốc từ vựng",
                    title = "📚 Chinh phục $serverTotalWords từ vựng cốt lõi",
                    description = "Bạn đã nắm vững $serverTotalWords từ trong kho từ vựng cá nhân với độ thành thạo cao.",
                    xp = serverTotalWords * 15
                )
            )
        }
        if (serverTotalHours > 0 && timelineItems.none { it.title.contains("giờ", ignoreCase = true) }) {
            timelineItems.add(
                MemoryItem(
                    date = "Thời gian tích lũy",
                    title = "⏱️ Hoàn thành $serverTotalHours giờ luyện tập tập trung",
                    description = "Sự kiên trì đều đặn mỗi ngày giúp trí nhớ dài hạn Ebbinghaus ngày càng bền vững.",
                    xp = serverTotalHours * 50
                )
            )
        }
        if (serverAccuracy >= 50 && timelineItems.none { it.title.contains("chính xác", ignoreCase = true) }) {
            timelineItems.add(
                MemoryItem(
                    date = "Phong độ Quiz",
                    title = "🎯 Đạt độ chính xác $serverAccuracy% trong các bài luyện tập",
                    description = "Hệ thống XAI ghi nhận tốc độ phản xạ và khả năng ghi nhớ của bạn đang tiến bộ rõ rệt.",
                    xp = serverAccuracy * 2
                )
            )
        }

        if (timelineItems.isEmpty()) {
            val emptyCard = TextView(ctx).apply {
                text = "Hãy hoàn thành bài học đầu tiên để mở khóa kỷ niệm đáng nhớ của bạn nhé! 🚀"
                textSize = 13.5f
                setTextColor(Color.parseColor("#64748B"))
                gravity = Gravity.CENTER
                setPadding(16.dp(), 22.dp(), 16.dp(), 22.dp())
                background = GradientDrawable().apply {
                    setColor(Color.WHITE)
                    cornerRadius = 16.dp().toFloat()
                    setStroke(1.dp(), Color.parseColor("#E2E8F0"))
                }
            }
            binding.containerMemories.addView(emptyCard)
        } else {
            timelineItems.forEachIndexed { idx, memory ->
                val isLast = idx == timelineItems.lastIndex
                binding.containerMemories.addView(createTimelineMemoryCard(memory, isLast))
            }
        }
    }

    private fun createSectionHeader(title: String, badgeText: String): View {
        val ctx = requireContext()
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 2.dp(), 0, 10.dp())
        }
        val tvTitle = TextView(ctx).apply {
            text = title
            textSize = 15.5f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val tvBadge = TextView(ctx).apply {
            text = badgeText
            textSize = 11.5f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#2563EB"))
            setPadding(10.dp(), 3.dp(), 10.dp(), 3.dp())
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#EFF6FF"))
                cornerRadius = 10.dp().toFloat()
            }
        }
        row.addView(tvTitle)
        row.addView(tvBadge)
        return row
    }

    private fun createBadgeCard(badge: BadgeMilestone): View {
        val ctx = requireContext()
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14.dp(), 14.dp(), 14.dp(), 14.dp())
            background = GradientDrawable().apply {
                setColor(if (badge.unlocked) Color.WHITE else Color.parseColor("#F8FAFC"))
                cornerRadius = 18.dp().toFloat()
                setStroke(
                    1.dp(),
                    if (badge.unlocked) Color.parseColor(badge.accentHex) else Color.parseColor("#E2E8F0")
                )
            }
            alpha = if (badge.unlocked) 1f else 0.78f
        }

        val topRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val emojiBox = TextView(ctx).apply {
            text = badge.emoji
            textSize = 20f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(38.dp(), 38.dp())
            background = GradientDrawable().apply {
                setColor(if (badge.unlocked) Color.parseColor(badge.softBgHex) else Color.parseColor("#E2E8F0"))
                cornerRadius = 12.dp().toFloat()
            }
        }
        val statusPill = TextView(ctx).apply {
            text = if (badge.unlocked) "✓ Đã đạt" else "${badge.current.coerceAtMost(badge.target)}/${badge.target}"
            textSize = 10.5f
            setTypeface(null, Typeface.BOLD)
            setTextColor(if (badge.unlocked) Color.parseColor("#047857") else Color.parseColor("#64748B"))
            setPadding(8.dp(), 3.dp(), 8.dp(), 3.dp())
            background = GradientDrawable().apply {
                setColor(if (badge.unlocked) Color.parseColor("#ECFDF5") else Color.parseColor("#E2E8F0"))
                cornerRadius = 9.dp().toFloat()
            }
        }
        val spacer = View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
        }
        topRow.addView(emojiBox)
        topRow.addView(spacer)
        topRow.addView(statusPill)
        card.addView(topRow)

        val tvTitle = TextView(ctx).apply {
            text = badge.title
            textSize = 13.5f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            setPadding(0, 10.dp(), 0, 2.dp())
        }
        val tvSub = TextView(ctx).apply {
            text = badge.subtitle
            textSize = 11.5f
            setTextColor(Color.parseColor("#64748B"))
        }
        card.addView(tvTitle)
        card.addView(tvSub)

        val pct = ((badge.current.toFloat() / badge.target.coerceAtLeast(1)) * 100f).toInt().coerceIn(5, 100)
        val miniTrack = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                5.dp()
            ).apply { topMargin = 10.dp() }
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#E2E8F0"))
                cornerRadius = 3.dp().toFloat()
            }
        }
        val miniFill = View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, pct.toFloat())
            background = GradientDrawable().apply {
                setColor(if (badge.unlocked) Color.parseColor(badge.accentHex) else Color.parseColor("#94A3B8"))
                cornerRadius = 3.dp().toFloat()
            }
        }
        val miniRest = View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, (100 - pct).toFloat())
        }
        miniTrack.addView(miniFill)
        miniTrack.addView(miniRest)
        card.addView(miniTrack)

        return card
    }

    private fun createTimelineMemoryCard(memory: MemoryItem, isLast: Boolean): View {
        val ctx = requireContext()
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Cột mốc timeline bên trái
        val rail = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(22.dp(), LinearLayout.LayoutParams.MATCH_PARENT).apply {
                marginEnd = 10.dp()
            }
        }
        val dot = View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(12.dp(), 12.dp()).apply {
                topMargin = 18.dp()
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#2563EB"))
                setStroke(2.dp(), Color.parseColor("#BFDBFE"))
            }
        }
        rail.addView(dot)
        if (!isLast) {
            val line = View(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(2.dp(), 0, 1f).apply {
                    topMargin = 4.dp()
                }
                setBackgroundColor(Color.parseColor("#DBEAFE"))
            }
            rail.addView(line)
        }
        row.addView(rail)

        // Thẻ nội dung kỷ niệm bên phải
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                bottomMargin = 12.dp()
            }
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 18.dp().toFloat()
                setStroke(1.dp(), Color.parseColor("#E2E8F0"))
            }
            setPadding(16.dp(), 14.dp(), 16.dp(), 14.dp())
            elevation = 1.5f
        }

        val tvTitle = TextView(ctx).apply {
            text = memory.title
            textSize = 15f
            setTextColor(Color.parseColor("#0F172A"))
            setTypeface(null, Typeface.BOLD)
        }

        val tvDesc = TextView(ctx).apply {
            text = memory.description
            textSize = 13f
            setTextColor(Color.parseColor("#475569"))
            setPadding(0, 5.dp(), 0, 10.dp())
        }

        val footerRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val tvDate = TextView(ctx).apply {
            text = memory.date
            textSize = 11.5f
            setTextColor(Color.parseColor("#64748B"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val tvXpBadge = TextView(ctx).apply {
            text = "+${memory.xp} XP"
            textSize = 11.5f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#2563EB"))
            setPadding(9.dp(), 3.dp(), 9.dp(), 3.dp())
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#EFF6FF"))
                cornerRadius = 10.dp().toFloat()
            }
        }

        footerRow.addView(tvDate)
        footerRow.addView(tvXpBadge)

        card.addView(tvTitle)
        card.addView(tvDesc)
        card.addView(footerRow)

        row.addView(card)
        return row
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
