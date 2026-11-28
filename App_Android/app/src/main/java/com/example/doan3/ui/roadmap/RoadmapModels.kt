package com.example.doan3.ui.roadmap

enum class StepStatus {
    COMPLETED,
    CURRENT,
    LOCKED
}

data class RoadmapStep(
    val id: String,
    val title: String,
    val description: String,
    val xaiReason: String? = null,
    val status: StepStatus,
    val currentProgress: Int = 0,
    val maxProgress: Int = 0
)

data class SavedRoadmapSummary(
    val id: String,
    val topic: String,
    val overallProgressPercent: Int = 0,
    val isActive: Boolean = false
)

data class UserRoadmapResponse(
    val id: String? = null,
    val userId: String,
    val topic: String? = null,
    val overallProgressPercent: Int,
    val motivationMessage: String,
    val steps: List<RoadmapStep>,
    val savedRoadmaps: List<SavedRoadmapSummary>? = null,
    val weeklyLessonsCompleted: Int = 0,
    val weeklyGoalTarget: Int = 5,
    val isWeeklyRewardClaimed: Boolean = false,
    val streakDays: Int = 0
)

data class GenerateRoadmapRequest(
    val userId: String,
    val target: String,
    val currentLevel: String,
    val freeTime: String,
    val expectation: String
)
