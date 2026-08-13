package com.example.doan3.ui.vocabulary

import android.os.Bundle
import android.speech.tts.TextToSpeech // THÊM IMPORT TTS
import android.util.Log
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

    // Thêm biến này ở dưới các biến currentStatusFilter (Code của bạn 1)
    private var currentSearchQuery = ""

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
        setupSearchListener() // THÊM DÒNG NÀY ĐỂ BẮT ĐẦU LẮNG NGHE TÌM KIẾM (Code của bạn 1)
        observeViewModel()

        // Xử lý nút mũi tên quay lại trang chủ
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    // (Code của bạn 2)
    override fun onResume() {
        super.onResume()
        // 🚀 Gọi đúng tên hàm đang có trong VocabularyViewModel

        viewModel.fetchVocabularyList() // Lấy danh sách từ vựng
        viewModel.fetchStats()          // Lấy thông số thống kê
    }

    // Thiết lập ngôn ngữ đọc là tiếng Anh Mỹ (Code của bạn 1 có thêm log chi tiết)
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Cài đặt ngôn ngữ và hứng kết quả trả về
            val result = tts.setLanguage(Locale.US)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Báo lỗi nếu thiết bị không hỗ trợ hoặc chưa tải ngôn ngữ này
                Log.e("TTS_DEBUG", "Ngôn ngữ không được hỗ trợ hoặc thiết bị thiếu dữ liệu!")
            } else {
                // Thành công, TTS đã sẵn sàng hoạt động
                Log.i("TTS_DEBUG", "Khởi tạo thành công! Sẵn sàng phát âm.")
            }
        } else {
            Log.e("TTS_DEBUG", "Khởi tạo TextToSpeech thất bại!")
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

            // 🚀 TỰ ĐỘNG ĐẾM SỐ LƯỢNG NGAY TRÊN APP (Cực kỳ chính xác và đồng bộ 100% với Trang chủ - Code bạn 2)
            val totalWords = list.size
            val masteredCount = list.count { it.proficiency >= 80 } // Số từ Đã thuộc
            val learningCount = list.count { it.proficiency in 1..79 } // Số từ Đang học
            val newCount = list.count { it.proficiency == 0 } // Số từ Mới

            val masteryPercent = if (totalWords > 0) (masteredCount * 100 / totalWords) else 0

            // Cập nhật ngay lập tức lên Giao diện
            binding.tvTotalWords.text = totalWords.toString()
            binding.tvMasteryPercent.text = "$masteryPercent%"

            binding.tabLayout.getTabAt(0)?.text = "Đã thuộc\n($masteredCount)"
            binding.tabLayout.getTabAt(1)?.text = "Đang học\n($learningCount)"
            binding.tabLayout.getTabAt(2)?.text = "Từ mới\n($newCount)"

            // Lọc và đổ danh sách vào RecyclerView
            applyFilters()
        }

        // 2. Lắng nghe lỗi
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrEmpty()) {
                android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        // 3. LẮNG NGHE THỐNG KÊ VÀ ĐỔ VÀO GIAO DIỆN (Code của bạn 1 - Giữ nguyên không xóa)
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

    // HÀM LẮNG NGHE SỰ KIỆN GÕ TỪ KHÓA TÌM KIẾM (Code của bạn 1)
    private fun setupSearchListener() {
        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s.toString().trim()
                applyFilters() // Gọi lại hàm lọc mỗi khi người dùng gõ phím
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

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

        // THÊM MỚI: Lọc theo từ khóa tìm kiếm (Code của bạn 1)
        if (currentSearchQuery.isNotEmpty()) {
            filteredList = filteredList.filter {
                // So sánh không phân biệt hoa thường
                it.word.contains(currentSearchQuery, ignoreCase = true)
            }
        }

        // THÊM MỚI: Xử lý hiển thị thông báo "chờ cập nhật dữ liệu" (Code của bạn 1)
        if (filteredList.isEmpty()) {
            binding.rvVocabulary.visibility = View.GONE
            binding.tvEmptyMessage.visibility = View.VISIBLE // Hiện thông báo chờ cập nhật
        } else {
            binding.rvVocabulary.visibility = View.VISIBLE
            binding.tvEmptyMessage.visibility = View.GONE // Ẩn thông báo đi
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