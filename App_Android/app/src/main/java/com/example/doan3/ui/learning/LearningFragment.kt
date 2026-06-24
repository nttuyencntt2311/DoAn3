package com.example.doan3.ui.learning

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.doan3.databinding.FragmentLearningBinding

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

        observeViewModel()

        // 1. Nút quay lại trang chủ
        binding.btnBack.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        // 2. Nút "Xem nghĩa" (Chuyển đổi ẩn/hiện nghĩa)
        binding.btnShowMeaning.setOnClickListener {
            if (binding.tvMeaning.visibility == View.GONE) {
                binding.tvMeaning.visibility = View.VISIBLE
                binding.ivWordImage.visibility = View.GONE // Ẩn ảnh để nhường chỗ cho chữ
                binding.btnShowMeaning.text = "Ẩn nghĩa"
            } else {
                binding.tvMeaning.visibility = View.GONE
                binding.ivWordImage.visibility = View.VISIBLE
                binding.btnShowMeaning.text = "Xem nghĩa"
            }
        }

        // 3. Nút Phát âm
        binding.btnPlayAudio.setOnClickListener {
            val word = binding.tvWord.text.toString()
            Toast.makeText(requireContext(), "Phát âm: $word", Toast.LENGTH_SHORT).show()
        }

        // 4. Mũi tên điều hướng
        binding.btnNext.setOnClickListener {
            viewModel.nextWord()
            resetCardState() // Đổi từ thì phải giấu cái nghĩa đi
        }

        binding.btnPrev.setOnClickListener {
            viewModel.prevWord()
            resetCardState()
        }
    }

    private fun observeViewModel() {
        viewModel.currentIndex.observe(viewLifecycleOwner) { index ->
            val list = viewModel.learningList.value
            if (!list.isNullOrEmpty()) {
                val currentVocab = list[index]

                // Cập nhật UI Flashcard
                binding.tvWord.text = currentVocab.word
                binding.tvPronunciation.text = currentVocab.pronunciation
                binding.tvMeaning.text = currentVocab.meaning

                // Cập nhật thanh tiến độ (VD: 1/3)
                binding.tvProgress.text = "${index + 1}/${list.size}"
            }
        }
    }

    private fun resetCardState() {
        binding.tvMeaning.visibility = View.GONE
        binding.ivWordImage.visibility = View.VISIBLE
        binding.btnShowMeaning.text = "Xem nghĩa"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}