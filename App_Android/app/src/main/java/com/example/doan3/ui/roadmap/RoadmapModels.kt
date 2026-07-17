package com.example.doan3.ui.roadmap

// Các trạng thái của 1 chặng học
enum class StepStatus {
    COMPLETED,  // Đã hoàn thành (Xanh lá)
    CURRENT,    // Đang học (Xanh dương)
    LOCKED      // Chưa mở khóa (Xám)
}

// Dữ liệu cho 1 chặng học (1 node trên timeline)
data class RoadmapStep(
    val id: String,
    val title: String,
    val description: String,
    val status: StepStatus,
    val currentProgress: Int = 0,
    val maxProgress: Int = 0
)

// Dữ liệu tổng quan AI trả về cho cả màn hình
data class UserRoadmapResponse(
    val userId: String,
    val overallProgressPercent: Int,
    val motivationMessage: String,
    val steps: List<RoadmapStep>
)

data class GenerateRoadmapRequest(
    val userId: String,
    val target: String,
    val currentLevel: String,
    val freeTime: String,
    val expectation: String
)