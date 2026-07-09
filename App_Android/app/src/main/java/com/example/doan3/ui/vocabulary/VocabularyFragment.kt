package com.example.doan3.ui.vocabulary

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.doan3.R
import com.example.doan3.databinding.FragmentVocabularyBinding
import com.google.android.material.tabs.TabLayout

class VocabularyFragment : Fragment() {

    private var _binding: FragmentVocabularyBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VocabularyViewModel by viewModels()
    private lateinit var adapter: VocabularyAdapter

    // Lưu trữ toàn bộ danh sách gốc từ API
    private var fullVocabularyList: List<Vocabulary> = emptyList()

    // Biến lưu trạng thái bộ lọc hiện tại
    private var currentCategoryFilter = "Tất cả"
    private var currentStatusFilter = 0 // 0: Đã thuộc, 1: Đang học, 2: Từ mới

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
        setupFilterListeners() // Bổ sung: Kích hoạt lắng nghe sự kiện bấm Tab và Chip
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
        // 1. Lắng nghe danh sách từ vựng
        viewModel.vocabularyList.observe(viewLifecycleOwner) { list ->
            fullVocabularyList = list // Lưu lại toàn bộ danh sách gốc
            applyFilters()            // Gọi hàm lọc để hiển thị thay vì đẩy thẳng vào adapter
        }

        // 2. Lắng nghe lỗi
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrEmpty()) {
                android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        // 3. LẮNG NGHE THỐNG KÊ VÀ ĐỔ VÀO GIAO DIỆN
        viewModel.vocabStats.observe(viewLifecycleOwner) { stats ->
            stats?.let {
                // Cập nhật 3 ô số liệu phía trên
                binding.tvTotalWords.text = it.totalWords.toString()
                binding.tvMasteryPercent.text = "${it.masteryPercentage}%"
                binding.tvNewWordsThisWeek.text = it.newWordsThisWeek.toString()

                // Cập nhật số lượng hiển thị trên 3 cái Tab
                binding.tabLayout.getTabAt(0)?.text = "Đã thuộc\n(${it.masteredWords})"
                binding.tabLayout.getTabAt(1)?.text = "Đang học\n(${it.learningWords})"
                binding.tabLayout.getTabAt(2)?.text = "Từ mới\n(${it.newWords})"
            }
        }
    }

    // --- CÁC HÀM XỬ LÝ LOGIC LỌC DỮ LIỆU ---

    private fun setupFilterListeners() {
        // 1. Xử lý click cho các Chip ngang (Chủ đề)
        binding.tvFilterAll.setOnClickListener {
            currentCategoryFilter = "Tất cả"
            updateChipUI(binding.tvFilterAll)
            applyFilters()
        }
        binding.tvFilterWork.setOnClickListener {
            currentCategoryFilter = "Công việc"
            updateChipUI(binding.tvFilterWork)
            applyFilters()
        }
        binding.tvFilterTravel.setOnClickListener {
            currentCategoryFilter = "Du lịch"
            updateChipUI(binding.tvFilterTravel)
            applyFilters()
        }

        // 2. Xử lý click cho TabLayout (Trạng thái)
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentStatusFilter = tab?.position ?: 0
                applyFilters() // Lọc lại danh sách khi đổi Tab
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun applyFilters() {
        // Bắt đầu với toàn bộ danh sách gốc
        var filteredList = fullVocabularyList

        // BƯỚC A: Lọc theo trạng thái (TabLayout) dựa trên độ thông thạo (proficiency)
        filteredList = when (currentStatusFilter) {
            0 -> filteredList.filter { it.proficiency >= 80 }      // Đã thuộc (>= 80%)
            1 -> filteredList.filter { it.proficiency in 1..79 }   // Đang học (1% - 79%)
            2 -> filteredList.filter { it.proficiency == 0 }       // Từ mới (0%)
            else -> filteredList
        }

        // BƯỚC B: Lọc theo chủ đề (Chip ngang)
        if (currentCategoryFilter != "Tất cả") {
            // Ghi chú: Hiện tại model Vocabulary.kt chưa có trường 'category'.
            // Khi nào em thiết kế DB có thêm phân loại chủ đề thì em mở comment dòng dưới ra nhé:
             filteredList = filteredList.filter { it.category == currentCategoryFilter }
        }

        // Cuối cùng: Đẩy danh sách đã lọc sạch sẽ vào Adapter để hiển thị
        adapter.updateData(filteredList)
    }

    private fun updateChipUI(selectedTextView: android.widget.TextView) {
        // Đưa tất cả về màu xám (không được chọn)
        binding.tvFilterAll.setBackgroundResource(R.drawable.bg_chip_inactive)
        binding.tvFilterWork.setBackgroundResource(R.drawable.bg_chip_inactive)
        binding.tvFilterTravel.setBackgroundResource(R.drawable.bg_chip_inactive)

        // Tô màu xanh cho cái được chọn
        selectedTextView.setBackgroundResource(R.drawable.bg_chip_active)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}