package com.example.doan3.ui.roadmap

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class RoadmapViewModel : ViewModel() {

    private val _roadmapData = MutableLiveData<UserRoadmapResponse>()
    val roadmapData: LiveData<UserRoadmapResponse> = _roadmapData

    fun fetchAiRoadmapForUser(userId: String) {
        // Dùng Coroutine giả lập thời gian tải mạng từ AI (0.5 giây)
        viewModelScope.launch {
            delay(500)

            // DỮ LIỆU MẪU (Mock Data) - Sau này thay bằng hàm gọi API AI
            val mockAiData = UserRoadmapResponse(
                userId = userId,
                overallProgressPercent = 35,
                motivationMessage = "Bạn chỉ còn 25 từ vựng nữa để đạt cột mốc 'Giao tiếp cơ bản'.",
                steps = listOf(
                    RoadmapStep("1", "Nền tảng", "50/50 từ vựng cốt lõi", StepStatus.COMPLETED, 50, 50),
                    RoadmapStep("2", "GIAO TIẾP CƠ BẢN", "Tiến trình bài học", StepStatus.CURRENT, 15, 40),
                    RoadmapStep("3", "Thành thạo công việc", "Hoàn thành chặng 2 để mở khoá nội dung này.", StepStatus.LOCKED),
                    RoadmapStep("4", "Chuyên sâu học thuật", "Hoàn thành chặng 3 để mở khoá nội dung này.", StepStatus.LOCKED)
                )
            )

            _roadmapData.value = mockAiData
        }
    }
}