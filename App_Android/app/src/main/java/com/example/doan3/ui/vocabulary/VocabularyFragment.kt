package com.example.doan3.ui.vocabulary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.doan3.databinding.FragmentVocabularyBinding

class VocabularyFragment : Fragment() {

    private var _binding: FragmentVocabularyBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VocabularyViewModel by viewModels()
    private lateinit var adapter: VocabularyAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVocabularyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()

        // Xử lý nút mũi tên quay lại trang chủ
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        // Cài đặt adapter và định nghĩa việc mở trang chi tiết khi bấm vào dòng
        adapter = VocabularyAdapter { clickedVocab ->
            openWordDetailFragment(clickedVocab)
        }
        binding.rvVocabulary.layoutManager = LinearLayoutManager(requireContext())
        binding.rvVocabulary.adapter = adapter
    }

    // HÀM MỞ TRANG CHI TIẾT VÀ TRUYỀN DỮ LIỆU SANG
    private fun openWordDetailFragment(vocab: Vocabulary) {
        val detailFragment = WordDetailFragment.newInstance(vocab)

        requireActivity().supportFragmentManager.beginTransaction()
            .replace((requireView().parent as ViewGroup).id, detailFragment)
            .addToBackStack(null) // Để có thể bấm Back quay lại được
            .commit()
    }

    private fun observeViewModel() {
        viewModel.vocabularyList.observe(viewLifecycleOwner) { list ->
            adapter.updateData(list)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Bật tắt loading (nếu có)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}