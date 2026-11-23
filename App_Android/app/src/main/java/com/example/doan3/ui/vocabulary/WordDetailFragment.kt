package com.example.doan3.ui.vocabulary

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.app.Activity
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.result.contract.ActivityResultContracts
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.doan3.R
import com.example.doan3.ui.profile.RetrofitClient
import com.example.doan3.ui.profile.SaveWordRequest
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale

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

class WordDetailFragment : Fragment(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private var isTtsReady = false
    private var wordTextToSpeak: String = ""

    private var fetchedExample = "Đang tải dữ liệu..."
    private var fetchedSynonyms = "Đang tải dữ liệu..."
    private var fetchedAntonyms = "Đang tải dữ liệu..."
    private var tvPronunciationResult: TextView? = null

    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (!isAdded) return@registerForActivityResult
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = matches?.firstOrNull()?.trim() ?: ""
            evaluatePronunciation(spoken)
        }
    }

    companion object {
        private const val ARG_ID = "arg_id"
        private const val ARG_WORD = "arg_word"
        private const val ARG_PRONUNCIATION = "arg_pronun"
        private const val ARG_MEANING = "arg_meaning"

        private val dictionaryApi: DictionaryApi by lazy {
            Retrofit.Builder()
                .baseUrl("https://api.dictionaryapi.dev/api/v2/entries/en/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(DictionaryApi::class.java)
        }

        fun newInstance(vocab: Vocabulary): WordDetailFragment {
            val fragment = WordDetailFragment()
            val args = Bundle().apply {
                putString(ARG_ID, vocab.id)
                putString(ARG_WORD, vocab.word)
                putString(ARG_PRONUNCIATION, vocab.pronunciation)
                putString(ARG_MEANING, vocab.meaning)
            }
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

        tvPronunciationResult = view.findViewById(R.id.tvPronunciationResult)
        view.findViewById<MaterialButton>(R.id.btnMicPractice)?.setOnClickListener {
            startPronunciationPractice()
        }
        val btnBack = view.findViewById<ImageView>(R.id.btnBack)
        val tvDetailWord = view.findViewById<TextView>(R.id.tvDetailWord)
        val tvDetailPronunciation = view.findViewById<TextView>(R.id.tvDetailPronunciation)
        val tvDetailMeaning = view.findViewById<TextView>(R.id.tvDetailMeaning)
        val btnDetailSpeaker = view.findViewById<ImageView>(R.id.btnDetailSpeaker)
        val btnSaveWord = view.findViewById<MaterialButton>(R.id.btnSaveWord)
        val btnShare = view.findViewById<ImageView>(R.id.btnShare)

        val tabExample = view.findViewById<TextView>(R.id.tabExample)
        val tabSynonyms = view.findViewById<TextView>(R.id.tabSynonyms)
        val tabAntonyms = view.findViewById<TextView>(R.id.tabAntonyms)
        val tvDynamicContent = view.findViewById<TextView>(R.id.tvDynamicContent)

        arguments?.let {
            wordIdToSave = it.getString(ARG_ID) ?: ""
            wordTextToSpeak = it.getString(ARG_WORD) ?: ""
            tvDetailWord.text = wordTextToSpeak
            tvDetailPronunciation.text = it.getString(ARG_PRONUNCIATION) ?: ""
            tvDetailMeaning.text = it.getString(ARG_MEANING) ?: ""
        }

        dictionaryApi.getWordDetails(wordTextToSpeak).enqueue(object : Callback<List<DictionaryResponse>> {
            override fun onResponse(call: Call<List<DictionaryResponse>>, response: Response<List<DictionaryResponse>>) {
                if (!isAdded || getView() == null) return
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    val meaning = response.body()!![0].meanings.firstOrNull()
                    val definition = meaning?.definitions?.firstOrNull()

                    fetchedExample = definition?.example ?: "Không tìm thấy ví dụ cụ thể."
                    fetchedSynonyms = if (!meaning?.synonyms.isNullOrEmpty()) {
                        meaning!!.synonyms.take(5).joinToString(", ")
                    } else "Không có từ đồng nghĩa."
                    fetchedAntonyms = if (!meaning?.antonyms.isNullOrEmpty()) {
                        meaning!!.antonyms.take(5).joinToString(", ")
                    } else "Không có từ trái nghĩa."

                    tabExample.performClick()
                } else {
                    tvDynamicContent.text = "Không tìm thấy dữ liệu mở rộng cho từ này."
                }
            }

            override fun onFailure(call: Call<List<DictionaryResponse>>, t: Throwable) {
                if (!isAdded || getView() == null) return
                tvDynamicContent.text = "Lỗi kết nối từ điển quốc tế."
            }
        })

        tabExample.setOnClickListener {
            tvDynamicContent.text = fetchedExample
            tabExample.setTextColor(Color.parseColor("#0056D2"))
            tabSynonyms.setTextColor(Color.parseColor("#888888"))
            tabAntonyms.setTextColor(Color.parseColor("#888888"))
        }

        tabSynonyms.setOnClickListener {
            tvDynamicContent.text = fetchedSynonyms
            tabSynonyms.setTextColor(Color.parseColor("#4CAF50"))
            tabExample.setTextColor(Color.parseColor("#888888"))
            tabAntonyms.setTextColor(Color.parseColor("#888888"))
        }

        tabAntonyms.setOnClickListener {
            tvDynamicContent.text = fetchedAntonyms
            tabAntonyms.setTextColor(Color.parseColor("#F44336"))
            tabExample.setTextColor(Color.parseColor("#888888"))
            tabSynonyms.setTextColor(Color.parseColor("#888888"))
        }

        btnBack.setOnClickListener { requireActivity().supportFragmentManager.popBackStack() }
        btnDetailSpeaker.setOnClickListener {
            if (isTtsReady && wordTextToSpeak.isNotEmpty()) {
                tts.speak(wordTextToSpeak, TextToSpeech.QUEUE_FLUSH, null, "")
            }
        }

        btnShare.setOnClickListener {
            val shareText = "Học từ vựng cùng LexiVT!\nTừ: $wordTextToSpeak\nPhiên âm: ${tvDetailPronunciation.text}\nNghĩa: ${tvDetailMeaning.text}"
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }
            startActivity(Intent.createChooser(shareIntent, "Chia sẻ từ vựng qua..."))
        }

        btnSaveWord.setOnClickListener {
            val ctx = context ?: return@setOnClickListener
            val sharedPref = ctx.getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
            val currentUserId = sharedPref.getString("USER_ID", "") ?: ""
            if (currentUserId.isEmpty()) {
                Toast.makeText(ctx, "Vui lòng đăng nhập lại để lưu từ!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSaveWord.text = "ĐANG LƯU..."
            btnSaveWord.isEnabled = false
            val request = SaveWordRequest(userId = currentUserId, wordId = wordIdToSave)

            RetrofitClient.getInstance(ctx).saveWord(request).enqueue(object : Callback<Any> {
                override fun onResponse(call: Call<Any>, response: Response<Any>) {
                    if (!isAdded || getView() == null) return
                    if (response.isSuccessful) {
                        btnSaveWord.text = "ĐÃ LƯU"
                        btnSaveWord.setBackgroundColor(Color.parseColor("#4CAF50"))
                        Toast.makeText(requireContext(), "Đã thêm '$wordTextToSpeak' vào sổ tay!", Toast.LENGTH_SHORT).show()
                    } else {
                        btnSaveWord.text = "LƯU TỪ"
                        btnSaveWord.isEnabled = true
                        Toast.makeText(requireContext(), "Lỗi server, chưa lưu được!", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<Any>, t: Throwable) {
                    if (!isAdded || getView() == null) return
                    btnSaveWord.text = "LƯU TỪ"
                    btnSaveWord.isEnabled = true
                    Toast.makeText(requireContext(), "Lỗi kết nối mạng", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
            isTtsReady = true
        }
    }

    override fun onDestroyView() {
        if (this::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroyView()
    }

    private fun startPronunciationPractice() {
        if (wordTextToSpeak.isBlank()) return
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Hãy đọc to từ: $wordTextToSpeak")
            }
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Thiết bị chưa hỗ trợ nhận diện giọng nói Google", Toast.LENGTH_SHORT).show()
        }
    }

    private fun evaluatePronunciation(spoken: String) {
        val target = wordTextToSpeak.lowercase(Locale.US).replace(Regex("[^a-z ]"), "").trim()
        val actual = spoken.lowercase(Locale.US).replace(Regex("[^a-z ]"), "").trim()
        if (actual.isEmpty() || target.isEmpty()) return

        val dist = levenshtein(target, actual)
        val maxLen = maxOf(target.length, actual.length).coerceAtLeast(1)
        val score = (((1.0 - dist.toDouble() / maxLen) * 100.0).toInt()).coerceIn(0, 100)

        tvPronunciationResult?.visibility = View.VISIBLE
        when {
            score >= 80 -> {
                tvPronunciationResult?.setTextColor(Color.parseColor("#047857"))
                tvPronunciationResult?.text = "🎯 Chuẩn $score%! Bạn đọc: '$spoken' — Phát âm rất rõ!"
            }
            score >= 50 -> {
                tvPronunciationResult?.setTextColor(Color.parseColor("#B45309"))
                tvPronunciationResult?.text = "👍 Đạt $score% — Bạn đọc: '$spoken'. Nghe lại loa và thử thêm lần nữa nhé!"
            }
            else -> {
                tvPronunciationResult?.setTextColor(Color.parseColor("#DC2626"))
                tvPronunciationResult?.text = "🔄 Chưa khớp ($score%) — Máy nghe được: '$spoken'. Hãy thử đọc chậm lại từ '$wordTextToSpeak'."
            }
        }
    }

    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
            }
        }
        return dp[a.length][b.length]
    }
}
