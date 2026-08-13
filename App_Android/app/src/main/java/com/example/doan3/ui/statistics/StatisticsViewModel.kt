package com.example.doan3.ui.statistics

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.doan3.ui.profile.RetrofitClient
import com.example.doan3.ui.profile.LearningStatsResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.doan3.ui.profile.MemoryItem

// 1. KHUÔN DỮ LIỆU THỐNG KÊ (Dùng để hiển thị lên UI)
data class LearningStats(
    val streakDays: Int,
    val totalHours: Int,
    val totalWords: Int,
    val accuracyPercent: Int,
    val chartData: List<Int>?
)

class StatisticsViewModel : ViewModel() {

    private val _statsData = MutableLiveData<LearningStats>()
    val statsData: LiveData<LearningStats> get() = _statsData

    private val _memoriesData = MutableLiveData<List<MemoryItem>>()
    val memoriesData: LiveData<List<MemoryItem>> get() = _memoriesData

    init {
        // Tải danh sách kỷ niệm (Tạm thời dùng Mock Data)
        fetchMemories()
    }

    // =========================================================================
    // HÀM LẤY DỮ LIỆU THỐNG KÊ THẬT TỪ BACKEND (THAY THẾ MOCK DATA CŨ)
    // =========================================================================
    fun fetchRealStatistics(context: Context) {
        RetrofitClient.getInstance(context).getLearningStats()
            .enqueue(object : Callback<LearningStatsResponse> {
                override fun onResponse(call: Call<LearningStatsResponse>, response: Response<LearningStatsResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val serverStats = response.body()!!

                        // 1. Cập nhật thống kê chung
                        // SỬA LỖI: Thêm `?.` và `?: 0` để xử lý an toàn tránh NullPointerException
                        _statsData.value = LearningStats(
                            streakDays = serverStats.stats?.streakDays ?: 0,
                            totalHours = serverStats.stats?.totalHours ?: 0,
                            totalWords = serverStats.stats?.totalWords ?: 0,
                            accuracyPercent = serverStats.stats?.accuracyPercent ?: 0,
                            chartData = serverStats.stats?.chartData ?: listOf(0, 0, 0, 0, 0, 0, 0)
                        )

                        // 2. CẬP NHẬT KỶ NIỆM (Lấy dữ liệu xịn từ Server)
                        // Đảo ngược list để Kỷ niệm mới nhất hiện lên trên cùng (tùy chọn)
                        _memoriesData.value = serverStats.memories?.reversed() ?: emptyList()
                    } else {
                        setEmptyStats()
                    }
                }

                override fun onFailure(call: Call<LearningStatsResponse>, t: Throwable) {
                    setEmptyStats()
                }
            })
    }

    // Hàm set giá trị mặc định khi có lỗi mạng
    private fun setEmptyStats() {
        _statsData.value = LearningStats(0, 0, 0, 0, listOf(0, 0, 0, 0, 0, 0, 0))
    }

    // Giữ nguyên dữ liệu ảo cho phần Kỷ niệm (Memories)
    private fun fetchMemories() {
        val mockMemories = listOf(
            MemoryItem("Hôm nay", "🔥 Chuỗi 15 ngày rực cháy", "Bạn đã duy trì thói quen học tiếng Anh 15 ngày liên tiếp.", 50),
            MemoryItem("10/06/2026", "📚 Vượt mốc 800 từ vựng", "Kho từ vựng của bạn đã được mở rộng đáng kể.", 100)
        )
        _memoriesData.value = mockMemories
    }
}