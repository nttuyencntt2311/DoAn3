package com.example.doan3.ui.statistics

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

// 1. KHUÔN DỮ LIỆU THỐNG KÊ
data class LearningStats(
    val streakDays: Int,
    val totalHours: Int,
    val totalWords: Int,
    val accuracyPercent: Int,
    val chartData: List<Int>
)

// 2. KHUÔN DỮ LIỆU KỶ NIỆM (Đã thêm XP)
data class MemoryItem(
    val date: String,
    val title: String,
    val description: String,
    val xp: Int // <-- Thêm trường điểm XP
)

class StatisticsViewModel : ViewModel() {

    private val _statsData = MutableLiveData<LearningStats>()
    val statsData: LiveData<LearningStats> get() = _statsData

    private val _memoriesData = MutableLiveData<List<MemoryItem>>()
    val memoriesData: LiveData<List<MemoryItem>> get() = _memoriesData

    init {
        fetchStatistics()
        fetchMemories()
    }

    private fun fetchStatistics() {
        // ĐÃ ĐỒNG BỘ SỐ LIỆU VỚI HOME (45 giờ, 1240 từ)
        val mockData = LearningStats(
            streakDays = 15,
            totalHours = 45,    // Khớp với Home
            totalWords = 1240,  // Khớp với Home
            accuracyPercent = 92,
            chartData = listOf(40, 60, 80, 30, 100, 70, 90)
        )
        _statsData.value = mockData
    }

    private fun fetchMemories() {
        // ĐÃ THÊM ĐIỂM XP VÀO CUỐI
        val mockMemories = listOf(
            MemoryItem("Hôm nay", "🔥 Chuỗi 15 ngày rực cháy", "Bạn đã duy trì thói quen học tiếng Anh 15 ngày liên tiếp.", 50),
            MemoryItem("10/06/2026", "📚 Vượt mốc 800 từ vựng", "Kho từ vựng của bạn đang ngày một phong phú.", 20),
            MemoryItem("05/06/2026", "🎯 Cú đêm chăm chỉ", "Bạn đã hoàn thành bài học lúc 23:45. Tinh thần thật đáng nể!", 10),
            MemoryItem("01/06/2026", "🌟 Khởi đầu hoàn hảo", "Chào mừng bạn gia nhập thế giới từ vựng LexiVT.", 100)
        )
        _memoriesData.value = mockMemories
    }
}