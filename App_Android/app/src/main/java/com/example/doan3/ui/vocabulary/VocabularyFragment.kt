package com.example.doan3.ui.vocabulary

import android.os.Bundle
import android.speech.tts.TextToSpeech // THÊM IMPORT TTS
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.doan3.R
import com.example.doan3.databinding.FragmentVocabularyBinding
import com.google.android.material.tabs.TabLayout
import java.util.Locale // THÊM IMPORT LOCALE

// ĐÃ SỬA: Kế thừa thêm TextToSpeech.OnInitListener
class VocabularyFragment : Fragment(), TextToSpeech.OnInitListener {

    private var _binding: FragmentVocabularyBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VocabularyViewModel by viewModels()
    private lateinit var adapter: VocabularyAdapter

    // Biến quản lý bộ đọc phát âm
    private lateinit var tts: TextToSpeech

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

        // Khởi tạo TextToSpeech
        tts = TextToSpeech(requireContext(), this)

        setupRecyclerView()
        setupFilterListeners()
        observeViewModel()

        // Xử lý nút mũi tên quay lại trang chủ
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    // Thiết lập ngôn ngữ đọc là tiếng Anh Mỹ
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
        }
    }

    private fun setupRecyclerView() {
        // ĐÃ SỬA: Truyền 2 sự kiện vào Adapter (1 cho việc mở chi tiết, 1 cho việc bấm loa)
        adapter = VocabularyAdapter(
            onItemClick = { clickedVocab ->
                openWordDetailFragment(clickedVocab)
            },
            onSpeakClick = { wordText ->
                // Lệnh phát âm từ vựng
                tts.speak(wordText, TextToSpeech.QUEUE_FLUSH, null, "")
            }
        )
        binding.rvVocabulary.layoutManager = LinearLayoutManager(requireContext())
        binding.rvVocabulary.adapter = adapter
    }

    // HÀM MỞ TRANG CHI TIẾT VÀ TRUYỀN DỮ LIỆU SANG
    private fun openWordDetailFragment(vocab: Vocabulary) {
        val detailFragment = WordDetailFragment.newInstance(vocab)

        requireActivity().supportFragmentManager.beginTransaction()
            .replace((requireView().parent as ViewGroup).id, detailFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun observeViewModel() {
        // 1. Lắng nghe danh sách từ vựng
        viewModel.vocabularyList.observe(viewLifecycleOwner) { list ->
            fullVocabularyList = list
            applyFilters()
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
                binding.tvTotalWords.text = it.totalWords.toString()
                binding.tvMasteryPercent.text = "${it.masteryPercentage}%"

                // (Phần tvNewWordsThisWeek nếu em đã xóa bên XML thì nhớ comment hoặc xóa dòng dưới này đi nhé)
                // binding.tvNewWordsThisWeek.text = it.newWordsThisWeek.toString()

                binding.tabLayout.getTabAt(0)?.text = "Đã thuộc\n(${it.masteredWords})"
                binding.tabLayout.getTabAt(1)?.text = "Đang học\n(${it.learningWords})"
                binding.tabLayout.getTabAt(2)?.text = "Từ mới\n(${it.newWords})"
            }
        }
    }

    // --- CÁC HÀM XỬ LÝ LOGIC LỌC DỮ LIỆU ---

    private fun setupFilterListeners() {
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

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentStatusFilter = tab?.position ?: 0
                applyFilters()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun applyFilters() {
        var filteredList = fullVocabularyList

        filteredList = when (currentStatusFilter) {
            0 -> filteredList.filter { it.proficiency >= 80 }
            1 -> filteredList.filter { it.proficiency in 1..79 }
            2 -> filteredList.filter { it.proficiency == 0 }
            else -> filteredList
        }

        if (currentCategoryFilter != "Tất cả") {
            filteredList = filteredList.filter { it.category == currentCategoryFilter }
        }

        adapter.updateData(filteredList)
    }

    private fun updateChipUI(selectedTextView: android.widget.TextView) {
        binding.tvFilterAll.setBackgroundResource(R.drawable.bg_chip_inactive)
        binding.tvFilterWork.setBackgroundResource(R.drawable.bg_chip_inactive)
        binding.tvFilterTravel.setBackgroundResource(R.drawable.bg_chip_inactive)

        selectedTextView.setBackgroundResource(R.drawable.bg_chip_active)
    }

    override fun onDestroyView() {
        // Tắt bộ đọc để giải phóng RAM khi rời khỏi Fragment này
        if (this::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroyView()
        _binding = null
    }

}