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
import com.example.doan3.ui.vocabulary.Vocabulary
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
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
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchUserData(requireContext())
        syncDailyVocabProgress()
        checkPredictiveFlashcards()
    }

    private fun checkPredictiveFlashcards() {
        val ctx = context ?: return
        val apiService = RetrofitClient.getInstance(ctx)

        apiService.getPredictiveFlashcards().enqueue(object : Callback<PredictForgettingResponse> {
            override fun onResponse(call: Call<PredictForgettingResponse>, response: Response<PredictForgettingResponse>) {
                if (_binding == null || !isAdded) return

                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!
                    val flashcards = data.predictiveFlashcards ?: emptyList()

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
                    Log.e("HomeFragment", "Lỗi Server API Sổ Tay: ${response.code()}")
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

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    private fun showPredictiveBottomSheet(flashcards: List<PredictiveFlashcard>) {
        val ctx = requireContext()
        val bottomSheet = BottomSheetDialog(ctx)

        // Root layout: Header cố định + ScrollView danh sách thẻ + Footer nút ghim dưới đáy
        val rootLayout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20.dp(), 16.dp(), 20.dp(), 24.dp())
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadii = floatArrayOf(
                    24.dp().toFloat(), 24.dp().toFloat(),
                    24.dp().toFloat(), 24.dp().toFloat(),
                    0f, 0f, 0f, 0f
                )
            }
        }

        // Thanh kéo nhỏ trên đỉnh BottomSheet
        val dragHandle = View(ctx).apply {
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#CBD5E1"))
                cornerRadius = 3.dp().toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(40.dp(), 4.dp()).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = 14.dp()
            }
        }
        rootLayout.addView(dragHandle)

        val titleText = TextView(ctx).apply {
            text = "⚠️ TỪ VỰNG DỄ QUÊN"
            textSize = 19f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#DC2626"))
            gravity = Gravity.CENTER
        }
        rootLayout.addView(titleText)

        val subtitleText = TextView(ctx).apply {
            text = "Sổ tay Tiên tri XAI phát hiện ${flashcards.size} từ cần ôn tập lại"
            textSize = 13f
            setTextColor(Color.parseColor("#64748B"))
            gravity = Gravity.CENTER
            setPadding(0, 4.dp(), 0, 14.dp())
        }
        rootLayout.addView(subtitleText)

        // Danh sách thẻ nằm trong ScrollView có giới hạn chiều cao tối đa ~52% màn hình
        val maxListHeight = (resources.displayMetrics.heightPixels * 0.52).toInt()
        val scrollView = ScrollView(ctx).apply {
            isVerticalScrollBarEnabled = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                maxListHeight
            )
        }

        val listContainer = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 2.dp(), 0, 4.dp())
        }

        flashcards.forEach { card ->
            val acc = if (card.predicted_accuracy > 1.0) card.predicted_accuracy else card.predicted_accuracy * 100
            val dangerLevel = (100 - acc).toInt().coerceIn(0, 100)

            val cardBox = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(14.dp(), 12.dp(), 14.dp(), 12.dp())
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#F8FAFC"))
                    cornerRadius = 14.dp().toFloat()
                    setStroke(1.dp(), Color.parseColor("#E2E8F0"))
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 10.dp()
                }
            }

            // Hàng 1: Từ vựng + phiên âm bên trái, badge nguy cơ bên phải
            val topRow = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val tvWordTitle = TextView(ctx).apply {
                val phoneticPart = if (card.phonetic.isNotBlank()) " ${card.phonetic}" else ""
                text = "${card.wordText}$phoneticPart"
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#0F172A"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val tvBadge = TextView(ctx).apply {
                text = "Nguy cơ $dangerLevel%"
                textSize = 11f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#DC2626"))
                setPadding(8.dp(), 3.dp(), 8.dp(), 3.dp())
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#FEE2E2"))
                    cornerRadius = 10.dp().toFloat()
                }
            }

            topRow.addView(tvWordTitle)
            topRow.addView(tvBadge)
            cardBox.addView(topRow)

            // Hàng 2: Nghĩa tiếng Việt
            val tvMeaning = TextView(ctx).apply {
                text = "Nghĩa: ${card.meaning}"
                textSize = 14f
                setTextColor(Color.parseColor("#334155"))
                setPadding(0, 4.dp(), 0, 0)
            }
            cardBox.addView(tvMeaning)

            // Hàng 3: Lý do XAI (loại bỏ đoạn lặp "Nguy cơ quên XX%" nếu đã có ở badge)
            if (!card.xai_reason.isNullOrBlank()) {
                val cleanedReason = card.xai_reason
                    .replace(Regex("""Nguy cơ quên\s*\d+%?\s*"""), "")
                    .trim()
                    .removePrefix("(")
                    .removeSuffix(").")
                    .removeSuffix(")")
                    .trim()

                if (cleanedReason.isNotEmpty()) {
                    val tvReason = TextView(ctx).apply {
                        text = "🧠 $cleanedReason"
                        textSize = 12f
                        setTextColor(Color.parseColor("#64748B"))
                        setPadding(0, 4.dp(), 0, 0)
                    }
                    cardBox.addView(tvReason)
                }
            }

            listContainer.addView(cardBox)
        }

        scrollView.addView(listContainer)
        rootLayout.addView(scrollView)

        // Footer: Nút hành động ghim cố định ở đáy popup, không bao giờ bị tràn khuất
        val btnReview = MaterialButton(ctx).apply {
            text = "ĐẾN SỔ TAY ÔN TẬP (${flashcards.size} TỪ)"
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            cornerRadius = 14.dp()
            setBackgroundColor(Color.parseColor("#DC2626"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                52.dp()
            ).apply {
                topMargin = 14.dp()
            }
            setOnClickListener {
                bottomSheet.dismiss()
                val intent = Intent(ctx, KhamPhaActivity::class.java).apply {
                    putStringArrayListExtra("PREDICTIVE_WORDS", ArrayList(flashcards.map { it.wordText }))
                    putExtra("CATEGORY_NAME", "AI Ôn Tập")
                }
                startActivity(intent)
            }
        }
        rootLayout.addView(btnReview)

        bottomSheet.setContentView(rootLayout)
        bottomSheet.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        bottomSheet.behavior.skipCollapsed = true
        bottomSheet.show()
    }

    private fun syncDailyVocabProgress() {
        val ctx = context ?: return
        RetrofitClient.getInstance(ctx).getAllVocabularies().enqueue(object : Callback<List<Vocabulary>> {
            override fun onResponse(call: Call<List<Vocabulary>>, response: Response<List<Vocabulary>>) {
                if (_binding == null || !isAdded) return
                if (response.isSuccessful) {
                    val allWords = response.body() ?: emptyList()
                    val masteredCount = allWords.count { it.proficiency >= 80 }
                    viewModel.updateDailyVocabularyProgress(masteredCount)
                }
            }

            override fun onFailure(call: Call<List<Vocabulary>>, t: Throwable) {
                Log.e("HomeFragment", "Lỗi tải dữ liệu từ vựng: ${t.message}")
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
