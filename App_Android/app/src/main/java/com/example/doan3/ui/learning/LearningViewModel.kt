package com.example.doan3.ui.learning

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.doan3.ui.vocabulary.Vocabulary // Import cái khuôn Vocabulary dùng chung

class LearningViewModel : ViewModel() {

    // Danh sách từ mới AI trả về
    private val _learningList = MutableLiveData<List<Vocabulary>>()
    val learningList: LiveData<List<Vocabulary>> get() = _learningList

    // Vị trí từ hiện tại đang học (0, 1, 2...)
    private val _currentIndex = MutableLiveData<Int>(0)
    val currentIndex: LiveData<Int> get() = _currentIndex

    init {
        fetchNewWordsFromAI()
    }

    // SAU NÀY BẠN GỌI AI VÀO ĐÂY ĐỂ LẤY TỪ MỚI CHO NGƯỜI DÙNG HỌC
    private fun fetchNewWordsFromAI() {
        Handler(Looper.getMainLooper()).postDelayed({
            val mockNewWords = listOf(
                Vocabulary("Pioneering", "/ˌpaɪəˈnɪərɪŋ/", "Tiên phong, đi đầu", 0),
                Vocabulary("Innovative", "/ˈɪn.ə.və.tɪv/", "Sáng tạo, đổi mới", 0),
                Vocabulary("Breakthrough", "/ˈbreɪk.θruː/", "Bước đột phá", 0)
            )
            _learningList.value = mockNewWords
            _currentIndex.value = 0 // Bắt đầu từ thẻ số 0
        }, 500)
    }

    fun nextWord() {
        val currentList = _learningList.value ?: return
        val currentIdx = _currentIndex.value ?: 0
        if (currentIdx < currentList.size - 1) {
            _currentIndex.value = currentIdx + 1
        }
    }

    fun prevWord() {
        val currentIdx = _currentIndex.value ?: 0
        if (currentIdx > 0) {
            _currentIndex.value = currentIdx - 1
        }
    }
}