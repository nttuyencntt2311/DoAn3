package com.example.doan3.ui.vocabulary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.doan3.databinding.FragmentWordDetailBinding

class WordDetailFragment : Fragment() {

    private var _binding: FragmentWordDetailBinding? = null
    private val binding get() = _binding!!

    // Biến để chứa từ vựng được truyền sang
    private var vocabulary: Vocabulary? = null

    companion object {
        // Hàm đóng gói dữ liệu để truyền
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
        // Mở gói và lấy dữ liệu ra
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

        // 1. GẮN DỮ LIỆU ĐÃ NHẬN LÊN GIAO DIỆN
        vocabulary?.let { vocab ->
            binding.tvWordTitle.text = vocab.word
            binding.tvPronunciation.text = vocab.pronunciation
            binding.tvDefinition.text = vocab.meaning
            binding.tvWordType.text = vocab.type
            binding.tvExampleEng1.text = vocab.exampleEng
            binding.tvExampleVi1.text = vocab.exampleVi
            binding.tvMasteryPercent.text = "${vocab.proficiency}%"
            binding.tvLastReviewTime.text = vocab.lastReviewTime
            binding.tvImageCaption.text = vocab.imageCaption
        }

        // 2. Xử lý nút mũi tên quay về danh sách
        binding.btnBack.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        // 3. Nút bấm chức năng (Chưa có tính năng nên hiện chữ lên cho vui)
        binding.btnPlayAudio.setOnClickListener {
            Toast.makeText(requireContext(), "Phát âm: ${vocabulary?.word}", Toast.LENGTH_SHORT).show()
        }

        binding.btnAddToFlashcard.setOnClickListener {
            Toast.makeText(requireContext(), "Đã thêm vào Flashcard!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}