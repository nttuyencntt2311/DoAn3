package com.example.doan3.ui.learning

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.doan3.R
import com.example.doan3.databinding.FragmentLearningBinding
import com.google.android.material.button.MaterialButton

class LearningFragment : Fragment() {

    private var _binding: FragmentLearningBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LearningViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLearningBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup RecyclerView
        val adapter = LessonAdapter()
        binding.rvLessons.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLessons.adapter = adapter

        // Theo dõi dữ liệu từ ViewModel và cập nhật danh sách
        viewModel.learningList.observe(viewLifecycleOwner) { lessons ->
            adapter.submitList(lessons)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Adapter nội bộ để hiển thị danh sách (Thực tế nên tách ra file riêng)
    inner class LessonAdapter : RecyclerView.Adapter<LessonAdapter.LessonViewHolder>() {

        private var lessonList = listOf<Lesson>()

        fun submitList(list: List<Lesson>) {
            lessonList = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LessonViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_lesson, parent, false)
            return LessonViewHolder(view)
        }

        override fun onBindViewHolder(holder: LessonViewHolder, position: Int) {
            val lesson = lessonList[position]
            holder.bind(lesson)
        }

        override fun getItemCount() = lessonList.size

        inner class LessonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvDifficulty: TextView = itemView.findViewById(R.id.tvDifficulty)
            private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
            private val tvSubtitle: TextView = itemView.findViewById(R.id.tvSubtitle)
            private val tvProgressPercent: TextView = itemView.findViewById(R.id.tvProgressPercent)
            private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
            private val tvVocabCount: TextView = itemView.findViewById(R.id.tvVocabCount)
            private val tvExerciseCount: TextView = itemView.findViewById(R.id.tvExerciseCount)
            private val btnAction: MaterialButton = itemView.findViewById(R.id.btnAction)

            fun bind(lesson: Lesson) {
                tvTitle.text = lesson.title
                tvSubtitle.text = "${lesson.category} • Bài ${lesson.lessonNumber}"
                tvProgressPercent.text = "${lesson.progressPercent}%"
                progressBar.progress = lesson.progressPercent
                tvVocabCount.text = "${lesson.vocabCount} từ vựng"
                tvExerciseCount.text = "${lesson.exerciseCount} bài tập"

                // Cấu hình Badge Độ khó (Màu sắc)
                tvDifficulty.text = lesson.difficultyLevel
                when (lesson.difficultyLevel) {
                    "DỄ" -> {
                        tvDifficulty.setTextColor(Color.parseColor("#117A65"))
                        tvDifficulty.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#D1F2EB"))
                    }
                    "TRUNG BÌNH" -> {
                        tvDifficulty.setTextColor(Color.parseColor("#B9770E"))
                        tvDifficulty.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FDEBD0"))
                    }
                    "KHÓ" -> {
                        tvDifficulty.setTextColor(Color.parseColor("#943126"))
                        tvDifficulty.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FADBD8"))
                    }
                }

                // Cấu hình Nút bấm (Style)
                btnAction.text = lesson.buttonText
                if (lesson.isButtonOutlined) {
                    // Style Outline (Nền trắng, viền xanh)
                    btnAction.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
                    btnAction.strokeWidth = 3
                    btnAction.strokeColor = android.content.res.ColorStateList.valueOf(Color.parseColor("#0056D2"))
                    btnAction.setTextColor(Color.parseColor("#0056D2"))
                } else {
                    // Style Filled (Nền xanh đậm, chữ trắng)
                    btnAction.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#0056D2"))
                    btnAction.strokeWidth = 0
                    btnAction.setTextColor(Color.WHITE)
                }
            }
        }
    }
}