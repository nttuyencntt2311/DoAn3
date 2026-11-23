package com.example.doan3.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.doan3.MainActivity
import com.example.doan3.R
import com.example.doan3.databinding.DialogCreateRoadmapBinding
import com.example.doan3.ui.ai.ChatAIActivity
import com.example.doan3.ui.roadmap.DictationActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.math.roundToInt

class TroLyAIFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.troly_ai, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ẩn bottomNav thừa trong troly_ai.xml vì MainActivity đã có thanh Bottom Nav thống nhất
        view.findViewById<View>(R.id.bottomNav)?.visibility = View.GONE

        setupQuickAsk(view)
        setupXaiSmartChips(view)
        setupAICardsNavigation(view)
    }

    private fun setupQuickAsk(view: View) {
        val edtQuickAsk = view.findViewById<EditText>(R.id.edtQuickAsk)
        val btnQuickSend = view.findViewById<View>(R.id.btnQuickSend)

        btnQuickSend?.setOnClickListener {
            val question = edtQuickAsk?.text?.toString()?.trim() ?: ""
            if (question.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập câu hỏi cho LexiVT AI!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            edtQuickAsk?.setText("")
            val intent = Intent(requireContext(), ChatAIActivity::class.java).apply {
                putExtra("INITIAL_MESSAGE", question)
            }
            startActivity(intent)
        }
    }

    private fun setupAICardsNavigation(view: View) {
        val cardGiaiThichTuVung = view.findViewById<MaterialCardView>(R.id.cardGiaiThichTuVung)
        val cardTaoQuiz = view.findViewById<MaterialCardView>(R.id.cardTaoQuiz)
        val cardFlashcard = view.findViewById<MaterialCardView>(R.id.cardFlashcard)
        val cardLoTrinhHoc = view.findViewById<MaterialCardView>(R.id.cardLoTrinhHoc)
        val cardTroChuyenTrucTiep = view.findViewById<MaterialCardView>(R.id.cardTroChuyenTrucTiep)

        cardGiaiThichTuVung?.setOnClickListener {
            (activity as? MainActivity)?.openVocabularyScreen()
        }

        cardTaoQuiz?.setOnClickListener {
            startActivity(Intent(requireContext(), TaoQuizActivity::class.java))
        }

        cardFlashcard?.setOnClickListener {
            (activity as? MainActivity)?.openLearningTab()
        }

        cardLoTrinhHoc?.setOnClickListener {
            showRoadmapFormDialog()
        }

        view.findViewById<MaterialCardView>(R.id.cardDictation)?.setOnClickListener {
            startActivity(Intent(requireContext(), DictationActivity::class.java))
        }

        cardTroChuyenTrucTiep?.setOnClickListener {
            startActivity(Intent(requireContext(), ChatAIActivity::class.java))
        }
    }

    private fun showRoadmapFormDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val dialogBinding = DialogCreateRoadmapBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)

        dialogBinding.spinnerGoal.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf("Giao tiếp công việc", "Thi IELTS/TOEIC", "Du lịch", "Lấy lại căn bản")
        )
        dialogBinding.spinnerLevel.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf("Mất gốc", "Cơ bản", "Trung cấp")
        )
        dialogBinding.spinnerTime.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf("15 phút/ngày", "30 phút/ngày", "1 tiếng/ngày")
        )

        dialogBinding.btnGenerate.setOnClickListener {
            val goal = dialogBinding.spinnerGoal.selectedItem.toString()
            val level = dialogBinding.spinnerLevel.selectedItem.toString()
            val time = dialogBinding.spinnerTime.selectedItem.toString()
            val expectation = dialogBinding.edtExpectation.text.toString().trim()

            dialog.dismiss()

            val args = Bundle().apply {
                putBoolean("GENERATE_ROADMAP", true)
                putString("GOAL", goal)
                putString("LEVEL", level)
                putString("TIME", time)
                putString("EXPECTATION", expectation)
            }
            (activity as? MainActivity)?.openRoadmapScreen(args)
        }

        dialog.show()
    }

    private fun setupXaiSmartChips(view: View) {
        view.findViewById<View>(R.id.chipXaiAnalyzeWeakness)?.setOnClickListener {
            launchXaiAwareChat(mode = "ANALYZE")
        }
        view.findViewById<View>(R.id.chipXaiPracticeForgotten)?.setOnClickListener {
            launchXaiAwareChat(mode = "PRACTICE")
        }
    }

    private fun launchXaiAwareChat(mode: String) {
        val ctx = context ?: return
        Toast.makeText(ctx, "🧠 Đang tổng hợp dữ liệu bệnh án XAI của bạn...", Toast.LENGTH_SHORT).show()

        RetrofitClient.getInstance(ctx).getPredictiveFlashcards().enqueue(object : Callback<PredictForgettingResponse> {
            override fun onResponse(call: Call<PredictForgettingResponse>, response: Response<PredictForgettingResponse>) {
                if (!isAdded) return
                val cards = response.body()?.predictiveFlashcards ?: emptyList()
                val topWordsSummary = if (cards.isNotEmpty()) {
                    cards.take(5).joinToString(", ") { c ->
                        val risk = ((1.0 - c.predicted_accuracy.coerceIn(0.0, 1.0)) * 100).roundToInt()
                        "${c.wordText} (${c.meaning} - nguy cơ quên $risk%)"
                    }
                } else {
                    "chưa có từ nào ở mức nguy cơ quên cao"
                }

                val prompt = if (mode == "ANALYZE") {
                    "Dựa trên dữ liệu XAI của tôi hiện tại, các từ tôi có nguy cơ quên cao nhất là: $topWordsSummary. Hãy phân tích điểm yếu từ vựng của tôi và đề xuất mẹo ghi nhớ nhanh cho từng từ này nhé!"
                } else {
                    "Dựa trên Sổ tay Tiên tri XAI, tôi đang sắp quên các từ: $topWordsSummary. Hãy tạo giúp tôi 5 câu bài tập điền từ vào ngữ cảnh thực tế để tôi ôn lại ngay nhé!"
                }

                val intent = Intent(requireContext(), ChatAIActivity::class.java).apply {
                    putExtra("INITIAL_MESSAGE", prompt)
                }
                startActivity(intent)
            }

            override fun onFailure(call: Call<PredictForgettingResponse>, t: Throwable) {
                if (!isAdded) return
                val fallbackPrompt = if (mode == "ANALYZE") {
                    "Hãy phân tích điểm yếu tiếng Anh và gợi ý lộ trình ôn tập từ vựng hiệu quả cho tôi."
                } else {
                    "Hãy tạo giúp tôi 5 câu bài tập trắc nghiệm từ vựng IELTS để luyện phản xạ nhanh."
                }
                val intent = Intent(requireContext(), ChatAIActivity::class.java).apply {
                    putExtra("INITIAL_MESSAGE", fallbackPrompt)
                }
                startActivity(intent)
            }
        })
    }
}
