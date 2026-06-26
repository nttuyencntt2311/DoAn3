package com.example.doan3.ui.learning

import android.os.Handler
import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

// Model dữ liệu cho Bài học mới
data class Lesson(
    val title: String,
    val category: String,
    val lessonNumber: Int,
    val difficultyLevel: String, // "DỄ", "TRUNG BÌNH", "KHÓ"
    val progressPercent: Int,
    val vocabCount: Int,
    val exerciseCount: Int,
    val buttonText: String,
    val isButtonOutlined: Boolean // Trạng thái nút (Xanh đặc hay Viền trắng)
)

class LearningViewModel : ViewModel() {

    private val _learningList = MutableLiveData<List<Lesson>>()
    val learningList: LiveData<List<Lesson>> get() = _learningList

    init {
        fetchLessons()
    }

    private fun fetchLessons() {
        // Dữ liệu giả lập (Mock data) khớp 100% với ảnh của bạn
        Handler(Looper.getMainLooper()).postDelayed({
            val mockLessons = listOf(
                Lesson(
                    title = "Chào hỏi & Giới thiệu",
                    category = "Giao tiếp cơ bản",
                    lessonNumber = 1,
                    difficultyLevel = "DỄ",
                    progressPercent = 45,
                    vocabCount = 15,
                    exerciseCount = 3,
                    buttonText = "Tiếp tục học",
                    isButtonOutlined = false
                ),
                Lesson(
                    title = "Biểu đồ trong Writing Task 1",
                    category = "IELTS Academic",
                    lessonNumber = 4,
                    difficultyLevel = "TRUNG BÌNH",
                    progressPercent = 12,
                    vocabCount = 25,
                    exerciseCount = 5,
                    buttonText = "Bắt đầu lại",
                    isButtonOutlined = false
                ),
                Lesson(
                    title = "Đàm phán hợp đồng",
                    category = "Business English",
                    lessonNumber = 15,
                    difficultyLevel = "KHÓ",
                    progressPercent = 88,
                    vocabCount = 30,
                    exerciseCount = 8,
                    buttonText = "Tiếp tục",
                    isButtonOutlined = true
                )
            )
            _learningList.value = mockLessons
        }, 300)
    }
}