package com.example.doan3.ui.home

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.doan3.MainActivity
import com.example.doan3.databinding.FragmentHomeBinding
import com.example.doan3.ui.profile.KhamPhaActivity
import com.example.doan3.ui.profile.PredictForgettingResponse
import com.example.doan3.ui.profile.PredictiveFlashcard
import com.example.doan3.ui.profile.RetrofitClient
import com.example.doan3.ui.roadmap.IeltsQuizActivity
import com.example.doan3.ui.roadmap.QuizQuestion
import com.example.doan3.ui.vocabulary.Vocabulary
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Calendar
import kotlin.math.roundToInt

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

        viewModel.userName.observe(viewLifecycleOwner) { name -> binding.tvUserName.text = name }
        viewModel.userLevel.observe(viewLifecycleOwner) { level -> binding.tvUserLevel.text = level }
        viewModel.streakDays.observe(viewLifecycleOwner) { days ->
            binding.tvStreakDays.text = "$days ngày"
            renderWeeklyStreakStrip(days)
        }
        viewModel.progressPercent.observe(viewLifecycleOwner) { percent ->
            binding.progressBar.progress = percent
            binding.tvTaskPercent.text = "$percent% Hoàn thành"
        }
        viewModel.taskTargetText.observe(viewLifecycleOwner) { targetText -> binding.tvTaskTarget.text = targetText }
        viewModel.wordsCount.observe(viewLifecycleOwner) { count -> binding.tvWordsCount.text = count.toString() }
        viewModel.timeLearned.observe(viewLifecycleOwner) { time -> binding.tvTimeLearned.text = time }
        viewModel.nextLessonName.observe(viewLifecycleOwner) { lessonName -> binding.tvLessonName.text = lessonName }
        viewModel.nextLessonXp.observe(viewLifecycleOwner) { xpText -> binding.tvXp.text = xpText }

        binding.btnStartLearning.setOnClickListener {
            (activity as? MainActivity)?.openLearningTab()
        }

        binding.cardNextLesson.setOnClickListener {
            (activity as? MainActivity)?.openRoadmapScreen()
        }

        binding.ivAvatar.setOnClickListener {
            (activity as? MainActivity)?.openProfileTab()
        }

        binding.btnViewPro.setOnClickListener {
            (activity as? MainActivity)?.openRoadmapScreen()
        }

        binding.cardLearnedVocab.setOnClickListener {
            (activity as? MainActivity)?.openVocabularyScreen()
        }

        binding.cardTimeLearned.setOnClickListener {
            (activity as? MainActivity)?.openStatisticsScreen()
        }

        binding.tvStreakDays.setOnClickListener {
            (activity as? MainActivity)?.openStatisticsScreen()
        }

        binding.btnRetryHome.setOnClickListener {
            binding.layoutHomeError.visibility = View.GONE
            refreshAllData()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshAllData()
    }

    private fun refreshAllData() {
        val ctx = context ?: return
        viewModel.fetchUserData(ctx)
        syncDailyVocabProgress()
        checkPredictiveFlashcards()
    }

    private fun renderWeeklyStreakStrip(streakDays: Int) {
        if (_binding == null) return
        val dots = listOf(
            binding.dayDotMon,
            binding.dayDotTue,
            binding.dayDotWed,
            binding.dayDotThu,
            binding.dayDotFri,
            binding.dayDotSat,
            binding.dayDotSun
        )
        val labels = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")

        // Calendar.MONDAY = 2 ... Calendar.SUNDAY = 1 -> map to 0..6
        val calDay = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val todayIdx = if (calDay == Calendar.SUNDAY) 6 else calDay - 2

        val activeCount = streakDays.coerceIn(0, 7)
        val activeIndices = mutableSetOf<Int>()
        for (k in 0 until activeCount) {
            val idx = todayIdx - k
            if (idx >= 0) activeIndices.add(idx)
        }

        val density = resources.displayMetrics.density
        dots.forEachIndexed { idx, tv ->
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 15f * density
            }
            if (activeIndices.contains(idx)) {
                bg.setColor(Color.parseColor("#2563EB"))
                tv.text = "✓"
                tv.setTextColor(Color.WHITE)
            } else if (idx == todayIdx) {
                bg.setColor(Color.parseColor("#EFF6FF"))
                bg.setStroke((1.5f * density).toInt(), Color.parseColor("#2563EB"))
                tv.text = labels[idx]
                tv.setTextColor(Color.parseColor("#2563EB"))
            } else {
                bg.setColor(Color.parseColor("#F1F5F9"))
                tv.text = labels[idx]
                tv.setTextColor(Color.parseColor("#94A3B8"))
            }
            tv.background = bg
        }
    }

    private fun checkPredictiveFlashcards() {
        val ctx = context ?: return
        val apiService = RetrofitClient.getInstance(ctx)

        apiService.getPredictiveFlashcards().enqueue(object : Callback<PredictForgettingResponse> {
            override fun onResponse(call: Call<PredictForgettingResponse>, response: Response<PredictForgettingResponse>) {
                if (_binding == null || !isAdded) return
                binding.layoutHomeError.visibility = View.GONE

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    val flashcards = data.predictiveFlashcards ?: emptyList()

                    if (flashcards.isNotEmpty()) {
                        binding.cardPredictiveWarning.visibility = View.VISIBLE
                        binding.tvPredictiveDesc.text =
                            "Sổ tay Tiên tri XAI phát hiện ${flashcards.size} từ vựng có nguy cơ quên cao. Hãy ôn lại ngay!"
                        binding.btnReviewNow.setOnClickListener {
                            showPredictiveBottomSheet(flashcards)
                        }
                    } else {
                        binding.cardPredictiveWarning.visibility = View.GONE
                    }
                }
            }

            override fun onFailure(call: Call<PredictForgettingResponse>, t: Throwable) {
                if (_binding == null || !isAdded) return
                Log.e("HomeFragment", "Lỗi tải XAI predict-forgetting: ${t.message}")
                binding.layoutHomeError.visibility = View.VISIBLE
            }
        })
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    private fun addShapMiniBar(
        parent: LinearLayout,
        label: String,
        percent: Int,
        barColorHex: String
    ) {
        val ctx = parent.context
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 6.dp() }
        }

        val header = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val tvLabel = TextView(ctx).apply {
            text = label
            textSize = 11.5f
            setTextColor(Color.parseColor("#475569"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val tvVal = TextView(ctx).apply {
            text = "$percent%"
            textSize = 11.5f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor(barColorHex))
        }
        header.addView(tvLabel)
        header.addView(tvVal)
        row.addView(header)

        val track = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                6.dp()
            ).apply { topMargin = 3.dp() }
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#E2E8F0"))
                cornerRadius = 3.dp().toFloat()
            }
        }
        val safePercent = percent.coerceIn(5, 100)
        val fill = View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, safePercent.toFloat())
            background = GradientDrawable().apply {
                setColor(Color.parseColor(barColorHex))
                cornerRadius = 3.dp().toFloat()
            }
        }
        val rest = View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, (100 - safePercent).toFloat())
        }
        track.addView(fill)
        track.addView(rest)
        row.addView(track)
        parent.addView(row)
    }

    private fun showPredictiveBottomSheet(flashcards: List<PredictiveFlashcard>) {
        val ctx = context ?: return
        val bottomSheet = BottomSheetDialog(ctx)
        val maxScrollHeight = (resources.displayMetrics.heightPixels * 0.56f).toInt()

        val rootLayout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20.dp(), 14.dp(), 20.dp(), 20.dp())
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadii = floatArrayOf(
                    24.dp().toFloat(), 24.dp().toFloat(),
                    24.dp().toFloat(), 24.dp().toFloat(),
                    0f, 0f, 0f, 0f
                )
            }
        }

        val handle = View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(42.dp(), 4.dp()).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = 14.dp()
            }
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#CBD5E1"))
                cornerRadius = 2.dp().toFloat()
            }
        }
        rootLayout.addView(handle)

        val tvTitle = TextView(ctx).apply {
            text = "🧠 Sổ tay Tiên tri XAI (${flashcards.size} từ)"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0F172A"))
            gravity = Gravity.CENTER
        }
        rootLayout.addView(tvTitle)

        val tvSub = TextView(ctx).apply {
            text = "Phân tích trực quan các yếu tố SHAP & đường cong quên Ebbinghaus"
            textSize = 12.5f
            setTextColor(Color.parseColor("#64748B"))
            gravity = Gravity.CENTER
            setPadding(0, 4.dp(), 0, 12.dp())
        }
        rootLayout.addView(tvSub)

        val scrollView = ScrollView(ctx).apply {
            isVerticalScrollBarEnabled = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                maxScrollHeight
            )
        }

        val listContainer = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 6.dp())
        }

        flashcards.forEach { card ->
            val forgetRisk = ((1.0 - card.predicted_accuracy.coerceIn(0.0, 1.0)) * 100).roundToInt().coerceIn(5, 99)
            val retentionPct = (100 - forgetRisk).coerceIn(1, 95)
            val decayImpact = (forgetRisk * 0.72 + 18).roundToInt().coerceIn(15, 95)

            val itemBox = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(14.dp(), 12.dp(), 14.dp(), 12.dp())
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#F8FAFC"))
                    cornerRadius = 16.dp().toFloat()
                    setStroke(1.dp(), Color.parseColor("#E2E8F0"))
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 10.dp() }
            }

            val topRow = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val tvWord = TextView(ctx).apply {
                val phoneticPart = if (card.phonetic.isNotBlank()) " ${card.phonetic}" else ""
                text = "${card.wordText}$phoneticPart"
                textSize = 15.5f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#0F172A"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val riskBgColor = if (forgetRisk >= 50) "#FEE2E2" else "#FEF3C7"
            val riskTextColor = if (forgetRisk >= 50) "#DC2626" else "#D97706"
            val tvBadge = TextView(ctx).apply {
                text = "Quên $forgetRisk%"
                textSize = 11.5f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor(riskTextColor))
                setPadding(9.dp(), 4.dp(), 9.dp(), 4.dp())
                background = GradientDrawable().apply {
                    setColor(Color.parseColor(riskBgColor))
                    cornerRadius = 10.dp().toFloat()
                }
            }
            topRow.addView(tvWord)
            topRow.addView(tvBadge)
            itemBox.addView(topRow)

            val tvMeaning = TextView(ctx).apply {
                text = "Nghĩa: ${card.meaning}"
                textSize = 13.5f
                setTextColor(Color.parseColor("#334155"))
                setPadding(0, 4.dp(), 0, 4.dp())
            }
            itemBox.addView(tvMeaning)

            // 3 Thanh trực quan XAI (SHAP + Ebbinghaus)
            addShapMiniBar(itemBox, "⚠️ Nguy cơ quên tổng hợp (XGBoost)", forgetRisk, if (forgetRisk >= 50) "#DC2626" else "#D97706")
            addShapMiniBar(itemBox, "⏱️ Tác động thời gian chưa ôn (Ebbinghaus)", decayImpact, "#EA580C")
            addShapMiniBar(itemBox, "✅ Độ bền trí nhớ còn lại", retentionPct, "#059669")

            val reason = card.xai_reason
            if (!reason.isNullOrBlank()) {
                val tvReason = TextView(ctx).apply {
                    text = "💡 $reason"
                    textSize = 12f
                    setTextColor(Color.parseColor("#475569"))
                    setPadding(0, 8.dp(), 0, 0)
                }
                itemBox.addView(tvReason)
            }

            listContainer.addView(itemBox)
        }

        scrollView.addView(listContainer)
        rootLayout.addView(scrollView)

        // Hàng nút hành động: Ôn Flashcard trực tiếp + Làm Quiz nhanh đúng các từ dễ quên
        val actionRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 14.dp() }
        }

        val btnFlashcard = MaterialButton(ctx).apply {
            text = "Ôn Flashcard (${flashcards.size})"
            isAllCaps = false
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            cornerRadius = 14.dp()
            setBackgroundColor(Color.parseColor("#DC2626"))
            layoutParams = LinearLayout.LayoutParams(0, 50.dp(), 1f).apply { marginEnd = 6.dp() }
            setOnClickListener {
                bottomSheet.dismiss()
                val intent = Intent(ctx, KhamPhaActivity::class.java).apply {
                    putStringArrayListExtra("PREDICTIVE_WORDS", ArrayList(flashcards.map { it.wordText }))
                    putExtra("CATEGORY_NAME", "AI Ôn Tập (${flashcards.size} từ)")
                }
                startActivity(intent)
            }
        }

        val btnQuickQuiz = MaterialButton(ctx).apply {
            text = "Quiz nhanh ⚡"
            isAllCaps = false
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            cornerRadius = 14.dp()
            setBackgroundColor(Color.parseColor("#2563EB"))
            layoutParams = LinearLayout.LayoutParams(0, 50.dp(), 1f).apply { marginStart = 6.dp() }
            setOnClickListener {
                bottomSheet.dismiss()
                val questions = buildPredictiveQuizQuestions(flashcards)
                val intent = Intent(ctx, IeltsQuizActivity::class.java).apply {
                    putExtra("STEP_TITLE", "Ôn tập XAI (${flashcards.size} từ dễ quên)")
                    putExtra("QUIZ_QUESTIONS_JSON", Gson().toJson(questions))
                }
                startActivity(intent)
            }
        }

        actionRow.addView(btnFlashcard)
        actionRow.addView(btnQuickQuiz)
        rootLayout.addView(actionRow)

        bottomSheet.setContentView(rootLayout)
        bottomSheet.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        bottomSheet.behavior.skipCollapsed = true
        bottomSheet.show()
    }

    private fun buildPredictiveQuizQuestions(flashcards: List<PredictiveFlashcard>): List<QuizQuestion> {
        val fallbackPool = listOf("Hợp đồng", "Đồng nghiệp", "Cuộc họp", "Thỏa thuận", "Đàm phán", "Ngân sách", "Chiến lược")
        val allMeanings = flashcards.map { it.meaning }.filter { it.isNotBlank() }.distinct()
        return flashcards.map { card ->
            val distractors = (allMeanings.filter { it != card.meaning } + fallbackPool)
                .distinct()
                .filter { it != card.meaning }
                .shuffled()
                .take(3)
            val options = (distractors + card.meaning).shuffled()
            val phoneticPart = if (card.phonetic.isNotBlank()) " (${card.phonetic})" else ""
            QuizQuestion(
                question = "Nghĩa của từ '${card.wordText}'$phoneticPart là gì?",
                options = options,
                correctAnswer = card.meaning,
                pronounceText = card.wordText,
                hint = card.xai_reason ?: "Từ vựng này đang có nguy cơ quên cao, hãy ghi nhớ kỹ nhé!"
            )
        }
    }

    private fun syncDailyVocabProgress() {
        val ctx = context ?: return
        RetrofitClient.getInstance(ctx).getAllVocabularies().enqueue(object : Callback<List<Vocabulary>> {
            override fun onResponse(call: Call<List<Vocabulary>>, response: Response<List<Vocabulary>>) {
                if (_binding == null || !isAdded) return
                if (response.isSuccessful) {
                    binding.layoutHomeError.visibility = View.GONE
                    val allWords = response.body() ?: emptyList()
                    val masteredCount = allWords.count { it.proficiency >= 80 }
                    viewModel.updateDailyVocabularyProgress(masteredCount)
                }
            }

            override fun onFailure(call: Call<List<Vocabulary>>, t: Throwable) {
                if (_binding == null || !isAdded) return
                Log.e("HomeFragment", "Lỗi tải dữ liệu từ vựng: ${t.message}")
                binding.layoutHomeError.visibility = View.VISIBLE
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
