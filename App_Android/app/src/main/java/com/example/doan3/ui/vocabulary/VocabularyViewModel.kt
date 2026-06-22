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
                Vocabulary("Momentum", "/məˈmen.təm/", "Động lực, đà (vật lý/tâm lý)", 95),
                Vocabulary("Linguistic", "/lɪŋˈɡwɪs.tɪk/", "Thuộc về ngôn ngữ học", 88),
                Vocabulary("Proficiency", "/prəˈfɪʃ.ən.si/", "Sự thông thạo, năng lực", 92),
                Vocabulary("Vitality", "/vaɪˈtæl.ə.ti/", "Sức sống, sự sinh động", 100),
                Vocabulary("Nuance", "/ˈnjuː.ɑːns/", "Sắc thái, khác biệt nhỏ", 85),
                Vocabulary("Articulation", "/ɑːˌtɪk.jəˈleɪ.ʃən/", "Sự phát âm rõ ràng", 78)
            )

            // Đẩy dữ liệu vào biến, giao diện sẽ TỰ ĐỘNG nhận được
            _vocabularyList.value = mockDataFromAI
            _isLoading.value = false // Tắt loading

        }, 1000) // Delay 1 giây
    }
}