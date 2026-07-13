package com.example.doan3.ui.vocabulary

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.doan3.R
import com.example.doan3.ui.profile.RetrofitClient
import com.example.doan3.ui.profile.SaveWordRequest
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

// ==========================================
// CÁC MODEL CHO DICTIONARY API QUỐC TẾ
// ==========================================
data class DictionaryResponse(val meanings: List<MeaningData>)
data class MeaningData(
    val partOfSpeech: String,
    val definitions: List<DefinitionData>,
    val synonyms: List<String>,
    val antonyms: List<String>
)
data class DefinitionData(
    val definition: String,
    val example: String?
)

interface DictionaryApi {
    @retrofit2.http.GET("{word}")
    fun getWordDetails(@retrofit2.http.Path("word") word: String): Call<List<DictionaryResponse>>
}

// ==========================================
// FRAGMENT CHÍNH
// ==========================================
class WordDetailFragment : Fragment(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private var wordTextToSpeak: String = ""

    // Các biến lưu trữ dữ liệu từ điển
    private var fetchedExample = "Đang tải dữ liệu..."
    private var fetchedSynonyms = "Đang tải dữ liệu..."
    private var fetchedAntonyms = "Đang tải dữ liệu..."

    companion object {
        private const val ARG_ID = "arg_id"
        private const val ARG_WORD = "arg_word"
        private const val ARG_PRONUNCIATION = "arg_pronun"
        private const val ARG_MEANING = "arg_meaning"

        fun newInstance(vocab: Vocabulary): WordDetailFragment {
            val fragment = WordDetailFragment()
            val args = Bundle()
            args.putString(ARG_ID, vocab.id)
            args.putString(ARG_WORD, vocab.word)
            args.putString(ARG_PRONUNCIATION, vocab.pronunciation)
            args.putString(ARG_MEANING, vocab.meaning)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_word_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var wordIdToSave = ""
        tts = TextToSpeech(requireContext(), this)

        // 1. Ánh xạ View cơ bản
        val btnBack = view.findViewById<ImageView>(R.id.btnBack)
        val tvDetailWord = view.findViewById<TextView>(R.id.tvDetailWord)
        val tvDetailPronunciation = view.findViewById<TextView>(R.id.tvDetailPronunciation)
        val tvDetailMeaning = view.findViewById<TextView>(R.id.tvDetailMeaning)
        val btnDetailSpeaker = view.findViewById<ImageView>(R.id.btnDetailSpeaker)
        val btnSaveWord = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSaveWord)
        val btnShare = view.findViewById<ImageView>(R.id.btnShare)

        // 2. Ánh xạ 3 Tabs và Nội dung động (Đảm bảo bên file XML em đã đặt ID cho 4 thành phần này)
        val tabExample = view.findViewById<TextView>(R.id.tabExample)
        val tabSynonyms = view.findViewById<TextView>(R.id.tabSynonyms)
        val tabAntonyms = view.findViewById<TextView>(R.id.tabAntonyms)
        val tvDynamicContent = view.findViewById<TextView>(R.id.tvDynamicContent)

        // Đổ dữ liệu tĩnh vào Giao diện
        arguments?.let {
            wordIdToSave = it.getString(ARG_ID) ?: ""
            wordTextToSpeak = it.getString(ARG_WORD) ?: ""
            tvDetailWord.text = wordTextToSpeak
            tvDetailPronunciation.text = it.getString(ARG_PRONUNCIATION) ?: ""
            tvDetailMeaning.text = it.getString(ARG_MEANING) ?: ""
        }

        // ==========================================
        // 3. GỌI API DICTIONARY QUỐC TẾ
        // ==========================================
        val retrofitDict = retrofit2.Retrofit.Builder()
            .baseUrl("https://api.dictionaryapi.dev/api/v2/entries/en/")
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()

        val dictApi = retrofitDict.create(DictionaryApi::class.java)

        dictApi.getWordDetails(wordTextToSpeak).enqueue(object : Callback<List<DictionaryResponse>> {
            override fun onResponse(call: Call<List<DictionaryResponse>>, response: Response<List<DictionaryResponse>>) {
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    val meaning = response.body()!![0].meanings.firstOrNull()
                    val definition = meaning?.definitions?.firstOrNull()

                    // Bóc tách logic dữ liệu an toàn
                    fetchedExample = definition?.example ?: "Không tìm thấy ví dụ cụ thể."

                    fetchedSynonyms = if (!meaning?.synonyms.isNullOrEmpty()) {
                        meaning!!.synonyms.take(5).joinToString(", ")
                    } else "Không có từ đồng nghĩa."

                    fetchedAntonyms = if (!meaning?.antonyms.isNullOrEmpty()) {
                        meaning!!.antonyms.take(5).joinToString(", ")
                    } else "Không có từ trái nghĩa."

                    // Tự động load Tab Ví dụ đầu tiên khi có dữ liệu
                    tabExample.performClick()
                } else {
                    tvDynamicContent.text = "Không tìm thấy dữ liệu mở rộng cho từ này."
                }
            }

            override fun onFailure(call: Call<List<DictionaryResponse>>, t: Throwable) {
                tvDynamicContent.text = "Lỗi kết nối từ điển quốc tế."
            }
        })

        // ==========================================
        // 4. SỰ KIỆN CLICK CHUYỂN TAB
        // ==========================================
        tabExample.setOnClickListener {
            tvDynamicContent.text = fetchedExample
            tabExample.setTextColor(android.graphics.Color.parseColor("#0056D2"))
            tabSynonyms.setTextColor(android.graphics.Color.parseColor("#888888"))
            tabAntonyms.setTextColor(android.graphics.Color.parseColor("#888888"))
        }

        tabSynonyms.setOnClickListener {
            tvDynamicContent.text = fetchedSynonyms
            tabSynonyms.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
            tabExample.setTextColor(android.graphics.Color.parseColor("#888888"))
            tabAntonyms.setTextColor(android.graphics.Color.parseColor("#888888"))
        }

        tabAntonyms.setOnClickListener {
            tvDynamicContent.text = fetchedAntonyms
            tabAntonyms.setTextColor(android.graphics.Color.parseColor("#F44336"))
            tabExample.setTextColor(android.graphics.Color.parseColor("#888888"))
            tabSynonyms.setTextColor(android.graphics.Color.parseColor("#888888"))
        }

        // --- CÁC NÚT CŨ GIỮ NGUYÊN HOẠT ĐỘNG ---
        btnBack.setOnClickListener { requireActivity().supportFragmentManager.popBackStack() }
        btnDetailSpeaker.setOnClickListener { if (wordTextToSpeak.isNotEmpty()) tts.speak(wordTextToSpeak, TextToSpeech.QUEUE_FLUSH, null, "") }

        btnShare.setOnClickListener {
            val shareText = "Học từ vựng cùng LexiVT!\nTừ: $wordTextToSpeak\nPhiên âm: ${tvDetailPronunciation.text}\nNghĩa: ${tvDetailMeaning.text}"
            val shareIntent = android.content.Intent().apply {
                action = android.content.Intent.ACTION_SEND
                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }
            startActivity(android.content.Intent.createChooser(shareIntent, "Chia sẻ từ vựng qua..."))
        }

        btnSaveWord.setOnClickListener {
            btnSaveWord.text = "ĐANG LƯU..."
            btnSaveWord.isEnabled = false
            val apiService = RetrofitClient.getInstance(requireContext())
            val sharedPref = requireContext().getSharedPreferences("DoAn3Prefs", android.content.Context.MODE_PRIVATE)
            val currentUserId = sharedPref.getString("USER_ID", "6a4d1424ee8dceac3f4c668c") ?: "6a4d1424ee8dceac3f4c668c"
            val request = SaveWordRequest(userId = currentUserId, wordId = wordIdToSave)

            apiService.saveWord(request).enqueue(object : Callback<Any> {
                override fun onResponse(call: Call<Any>, response: Response<Any>) {
                    if (response.isSuccessful) {
                        btnSaveWord.text = "ĐÃ LƯU"
                        btnSaveWord.setBackgroundColor(android.graphics.Color.parseColor("#4CAF50"))
                        android.widget.Toast.makeText(requireContext(), "Đã thêm '$wordTextToSpeak' vào sổ tay!", android.widget.Toast.LENGTH_SHORT).show()
                    } else {
                        btnSaveWord.text = "LƯU TỪ"
                        btnSaveWord.isEnabled = true
                        android.widget.Toast.makeText(requireContext(), "Lỗi server, chưa lưu được!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<Any>, t: Throwable) {
                    btnSaveWord.text = "LƯU TỪ"
                    btnSaveWord.isEnabled = true
                    android.widget.Toast.makeText(requireContext(), "Lỗi kết nối mạng", android.widget.Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
        }
    }

    override fun onDestroyView() {
        if (this::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroyView()
    }
}