package com.example.doan3.ui.vocabulary

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.doan3.R
import com.example.doan3.databinding.FragmentVocabularyBinding
import com.google.android.material.tabs.TabLayout
import java.util.Locale

class VocabularyFragment : Fragment(), TextToSpeech.OnInitListener {

    private var _binding: FragmentVocabularyBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VocabularyViewModel by viewModels()
    private lateinit var adapter: VocabularyAdapter

    private lateinit var tts: TextToSpeech
    private var isTtsReady = false

    private var fullVocabularyList: List<Vocabulary> = emptyList()
    private var currentCategoryFilter = "Tất cả"
    private var currentStatusFilter = 0
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

        tts = TextToSpeech(requireContext(), this)

        setupRecyclerView()
        setupFilterListeners()
        setupSearchListener()
        observeViewModel()

        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnNotification.setOnClickListener {
            Toast.makeText(requireContext(), "Không có thông báo mới", Toast.LENGTH_SHORT).show()
        }

        // Khởi tạo màu chip ban đầu đồng bộ
        updateChipUI(binding.tvFilterAll)
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchVocabularyList()
        viewModel.fetchStats()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.US)
            isTtsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        } else {
            Log.e("TTS_DEBUG", "Khởi tạo TextToSpeech thất bại!")
        }
    }

    private fun setupRecyclerView() {
        adapter = VocabularyAdapter(
            onItemClick = { clickedVocab ->
                openWordDetailFragment(clickedVocab)
            },
            onSpeakClick = { wordText ->
                if (isTtsReady && wordText.isNotEmpty()) {
                    tts.speak(wordText, TextToSpeech.QUEUE_FLUSH, null, "")
                }
            }
        )
        binding.rvVocabulary.layoutManager = LinearLayoutManager(requireContext())
        binding.rvVocabulary.adapter = adapter
    }

    private fun openWordDetailFragment(vocab: Vocabulary) {
        val detailFragment = WordDetailFragment.newInstance(vocab)
        requireActivity().supportFragmentManager.beginTransaction()
            .replace((requireView().parent as ViewGroup).id, detailFragment)
            .addToBackStack(null)
            .commit()
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressLoading.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.vocabularyList.observe(viewLifecycleOwner) { list ->
            fullVocabularyList = list

            val totalWords = list.size
            val masteredCount = list.count { it.proficiency >= 80 }
            val learningCount = list.count { it.proficiency in 1..79 }
            val newCount = list.count { it.proficiency == 0 }
            val masteryPercent = if (totalWords > 0) (masteredCount * 100 / totalWords) else 0

            binding.tvTotalWords.text = totalWords.toString()
            binding.tvMasteryPercent.text = "$masteryPercent%"

            binding.tabLayout.getTabAt(0)?.text = "Đã thuộc\n($masteredCount)"
            binding.tabLayout.getTabAt(1)?.text = "Đang học\n($learningCount)"
            binding.tabLayout.getTabAt(2)?.text = "Từ mới\n($newCount)"

            applyFilters()
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrEmpty()) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        viewModel.vocabStats.observe(viewLifecycleOwner) { stats ->
            stats?.let {
                if (fullVocabularyList.isEmpty()) {
                    binding.tvTotalWords.text = it.totalWords.toString()
                    binding.tvMasteryPercent.text = "${it.masteryPercentage}%"
                    binding.tabLayout.getTabAt(0)?.text = "Đã thuộc\n(${it.masteredWords})"
                    binding.tabLayout.getTabAt(1)?.text = "Đang học\n(${it.learningWords})"
                    binding.tabLayout.getTabAt(2)?.text = "Từ mới\n(${it.newWords})"
                }
            }
        }
    }

    private fun setupSearchListener() {
        binding.etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s.toString().trim()
                applyFilters()
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
            filteredList = filteredList.filter { it.category.contains(currentCategoryFilter, ignoreCase = true) }
        }

        if (currentSearchQuery.isNotEmpty()) {
            filteredList = filteredList.filter {
                it.word.contains(currentSearchQuery, ignoreCase = true) ||
                        it.meaning.contains(currentSearchQuery, ignoreCase = true)
            }
        }

        if (filteredList.isEmpty()) {
            binding.rvVocabulary.visibility = View.GONE
            binding.tvEmptyMessage.visibility = View.VISIBLE
        } else {
            binding.rvVocabulary.visibility = View.VISIBLE
            binding.tvEmptyMessage.visibility = View.GONE
        }

        adapter.updateData(filteredList)
    }

    /** Cập nhật cả màu nền lẫn màu chữ & độ đậm khi chuyển chip lọc */
    private fun updateChipUI(selectedTextView: TextView) {
        val inactiveTextColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
        val chips = listOf(binding.tvFilterAll, binding.tvFilterWork, binding.tvFilterTravel)

        chips.forEach { chip ->
            chip.setBackgroundResource(R.drawable.bg_chip_inactive)
            chip.setTextColor(inactiveTextColor)
            chip.setTypeface(null, Typeface.NORMAL)
        }

        selectedTextView.setBackgroundResource(R.drawable.bg_chip_active)
        selectedTextView.setTextColor(Color.WHITE)
        selectedTextView.setTypeface(null, Typeface.BOLD)
    }

    override fun onDestroyView() {
        if (this::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroyView()
        _binding = null
    }
}