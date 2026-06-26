package com.example.doan3.ui.vocabulary

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.doan3.databinding.FragmentWordDetailBinding
import com.google.android.material.chip.Chip

class WordDetailFragment : Fragment() {

    private var _binding: FragmentWordDetailBinding? = null
    private val binding get() = _binding!!

    private var vocabulary: Vocabulary? = null

    companion object {
        fun newInstance(vocab: Vocabulary): WordDetailFragment {
            val fragment = WordDetailFragment()
            val args = Bundle()
            args.putSerializable("VOCAB_DATA", vocab)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            vocabulary = it.getSerializable("VOCAB_DATA") as? Vocabulary
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWordDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vocabulary?.let { vocab ->
            // Thông tin cơ bản
            binding.tvWordTitle.text = vocab.word
            binding.tvPronunciation.text = vocab.pronunciation
            binding.tvWordType.text = vocab.type

            // Định nghĩa
            binding.tvDefinitionVi.text = vocab.meaning
            binding.tvDefinitionEn.text = vocab.defEng

            // Ví dụ (Kiểm tra xem có đủ 2 ví dụ không để hiển thị)
            if (vocab.examples.isNotEmpty()) {
                binding.cardExample1.visibility = View.VISIBLE
                binding.tvEx1Eng.text = "\"${vocab.examples[0].eng}\""
                binding.tvEx1Vi.text = vocab.examples[0].vi
            } else {
                binding.cardExample1.visibility = View.GONE
            }

            if (vocab.examples.size > 1) {
                binding.cardExample2.visibility = View.VISIBLE
                binding.tvEx2Eng.text = "\"${vocab.examples[1].eng}\""
                binding.tvEx2Vi.text = vocab.examples[1].vi
            } else {
                binding.cardExample2.visibility = View.GONE
            }

            // Đồng nghĩa (Màu xanh)
            binding.cgSynonyms.removeAllViews()
            for (syn in vocab.synonyms) {
                val chip = Chip(requireContext()).apply {
                    text = syn
                    setTextColor(Color.parseColor("#008A27"))
                    chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#66FFA6"))
                    isCheckable = false
                    isCloseIconVisible = false
                }
                binding.cgSynonyms.addView(chip)
            }

            // Trái nghĩa (Màu đỏ)
            binding.cgAntonyms.removeAllViews()
            for (ant in vocab.antonyms) {
                val chip = Chip(requireContext()).apply {
                    text = ant
                    setTextColor(Color.parseColor("#D32F2F"))
                    chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#FFCDD2"))
                    isCheckable = false
                    isCloseIconVisible = false
                }
                binding.cgAntonyms.addView(chip)
            }

            // AI Tip & Điểm
            binding.tvAiTipContent.text = vocab.aiTip
            binding.popularityProgressBar.progress = vocab.popularityScore
            binding.tvPopularityDesc.text = vocab.popularityText
        }

        // Các nút bấm
        binding.btnBack.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        binding.btnPlayAudio.setOnClickListener {
            Toast.makeText(requireContext(), "Phát âm: ${vocabulary?.word}", Toast.LENGTH_SHORT).show()
        }

        binding.btnSave.setOnClickListener {
            Toast.makeText(requireContext(), "Đã lưu từ vựng!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}