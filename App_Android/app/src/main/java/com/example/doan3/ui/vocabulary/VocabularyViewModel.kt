package com.example.doan3.ui.vocabulary

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class VocabularyViewModel : ViewModel() {

    // Biến chứa danh sách từ vựng. Giao diện sẽ "lắng nghe" (observe) biến này
    private val _vocabularyList = MutableLiveData<List<Vocabulary>>()
    val vocabularyList: LiveData<List<Vocabulary>> get() = _vocabularyList

    // Biến để bật/tắt vòng xoay loading khi đang chờ AI trả lời
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    init {
        // Khi vừa mở ViewModel lên thì kéo dữ liệu luôn
        fetchVocabularyFromAI()
    }

    // SAU NÀY BẠN VIẾT CODE GỌI API/AI VÀO HÀM NÀY
    fun fetchVocabularyFromAI() {
        _isLoading.value = true // Bật loading

        // TÔI ĐANG GIẢ LẬP VIỆC CHỜ AI XỬ LÝ (Mất 1 giây)
        Handler(Looper.getMainLooper()).postDelayed({

            // Đây là lúc AI trả về dữ liệu thành công
            val mockDataFromAI = listOf(
                Vocabulary(
                    word = "Momentum",
                    pronunciation = "/moʊ'men.təm/",
                    meaning = "Động lực, đà phát triển",
                    defEng = "The quality that keeps an event or process developing after it has started.",
                    proficiency = 95,
                    type = "Noun",
                    examples = listOf(
                        ExampleSentence("The campaign is gathering momentum as it reaches its final week.", "Chiến dịch đang dần có thêm đà phát triển khi bước vào tuần cuối cùng."),
                        ExampleSentence("Once you start your morning routine, you'll gain the momentum to finish the day's tasks.", "Một khi bạn bắt đầu thói quen buổi sáng, bạn sẽ có đà để hoàn thành các công việc trong ngày.")
                    ),
                    synonyms = listOf("Impetus", "Energy"),
                    antonyms = listOf("Inertia", "Stagnation"),
                    aiTip = "\"Momentum\" thường được dùng trong cả vật lý và kinh doanh. Trong công việc, hãy dùng nó để mô tả khi một dự án bắt đầu có tiến triển nhanh hơn và không thể bị dừng lại dễ dàng.",
                    popularityScore = 85,
                    popularityText = "Độ phổ biến: Cao (Top 5%)"
                )
            )

            // Đẩy dữ liệu vào biến, giao diện sẽ TỰ ĐỘNG nhận được
            _vocabularyList.value = mockDataFromAI
            _isLoading.value = false // Tắt loading

        }, 1000) // Delay 1 giây
    }
}