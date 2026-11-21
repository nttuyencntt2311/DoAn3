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
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView

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
}