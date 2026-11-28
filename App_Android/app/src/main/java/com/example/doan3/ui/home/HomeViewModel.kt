package com.example.doan3.ui.home

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.doan3.ui.profile.AvatarHelper
import com.example.doan3.ui.profile.RetrofitClient
import com.example.doan3.ui.profile.UserStatsResponse
import com.example.doan3.ui.roadmap.StepStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeViewModel : ViewModel() {

    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> get() = _userName

    private val _userLevel = MutableLiveData<String>()
    val userLevel: LiveData<String> get() = _userLevel

    private val _streakDays = MutableLiveData<Int>()
    val streakDays: LiveData<Int> get() = _streakDays

    private val _progressPercent = MutableLiveData<Int>()
    val progressPercent: LiveData<Int> get() = _progressPercent

    private val _taskTargetText = MutableLiveData<String>()
    val taskTargetText: LiveData<String> get() = _taskTargetText

    private val _wordsCount = MutableLiveData<Int>()
    val wordsCount: LiveData<Int> get() = _wordsCount

    private val _timeLearned = MutableLiveData<String>()
    val timeLearned: LiveData<String> get() = _timeLearned

    private val _nextLessonName = MutableLiveData<String>()
    val nextLessonName: LiveData<String> get() = _nextLessonName

    private val _nextLessonXp = MutableLiveData<String>()
    val nextLessonXp: LiveData<String> get() = _nextLessonXp

    private val _hasRoadmapStep = MutableLiveData<Boolean>(false)
    val hasRoadmapStep: LiveData<Boolean> get() = _hasRoadmapStep

    private val _fallbackCategory = MutableLiveData<String?>(null)
    val fallbackCategory: LiveData<String?> get() = _fallbackCategory

    init {
        setDefaultNewUserValues()
    }

    fun fetchUserData(context: Context) {
        val api = RetrofitClient.getInstance(context)
        api.getUserStats().enqueue(object : Callback<UserStatsResponse> {
            override fun onResponse(call: Call<UserStatsResponse>, response: Response<UserStatsResponse>) {
                val stats = response.body()?.stats
                if (response.isSuccessful && stats != null) {
                    _userName.value = stats.name
                    _userLevel.value = "Cấp ${stats.level} • ${stats.xp} XP"
                    _streakDays.value = stats.streakDays
                    _wordsCount.value = stats.wordsLearned
                    _timeLearned.value = "${stats.timeLearnedMinutes} phút"
                    AvatarHelper.saveUserLevel(context, stats.level)
                }
            }

            override fun onFailure(call: Call<UserStatsResponse>, t: Throwable) {}
        })

        // Tải chặng học tiếp theo thật từ Lộ trình AI của học viên
        viewModelScope.launch {
            try {
                val res = withContext(Dispatchers.IO) { api.getRoadmapForUser() }
                if (res.isSuccessful) {
                    val steps = res.body()?.steps ?: emptyList()
                    val nextStep = steps.firstOrNull { it.status == StepStatus.CURRENT }
                        ?: steps.firstOrNull { it.status != StepStatus.COMPLETED }
                    if (nextStep != null) {
                        _hasRoadmapStep.value = true
                        _nextLessonName.value = nextStep.title
                        val maxP = nextStep.maxProgress.coerceAtLeast(1)
                        val xpReward = 50 + maxP * 10
                        _nextLessonXp.value = "+$xpReward XP • Tiến độ ${nextStep.currentProgress}/$maxP bài"
                    } else if (steps.isNotEmpty()) {
                        _hasRoadmapStep.value = true
                        _nextLessonName.value = "🎉 Đã hoàn thành toàn bộ lộ trình!"
                        _nextLessonXp.value = "Nhấn để tạo lộ trình nâng cao mới"
                    }
                }
            } catch (_: Exception) {
                // Giữ fallback từ chủ đề từ vựng nếu chưa có lộ trình
            }
        }
    }

    fun updateNextLessonFallback(categoryName: String, remainingCount: Int) {
        if (_hasRoadmapStep.value == true) return
        _fallbackCategory.value = categoryName
        _nextLessonName.value = "Chủ đề: $categoryName"
        _nextLessonXp.value = "+50 XP • Còn $remainingCount từ chưa thuộc"
    }

    fun updateDailyVocabularyProgress(masteredCount: Int) {
        if ((_wordsCount.value ?: 0) == 0 && masteredCount > 0) {
            _wordsCount.value = masteredCount
        }
        val dailyTarget = 10
        val wordsToday = masteredCount % dailyTarget
        val percent = if (wordsToday == 0 && masteredCount > 0) 100 else ((wordsToday.toFloat() / dailyTarget) * 100).toInt()
        val displayWords = if (wordsToday == 0 && masteredCount > 0) dailyTarget else wordsToday

        _progressPercent.value = percent
        _taskTargetText.value = "Mục tiêu hôm nay: $displayWords/$dailyTarget từ mới"
    }

    private fun setDefaultNewUserValues() {
        _userName.value = "Học viên mới"
        _userLevel.value = "Mới nhập môn"
        _streakDays.value = 0
        _progressPercent.value = 0
        _taskTargetText.value = "Mục tiêu hôm nay: 0/10 từ mới"
        _wordsCount.value = 0
        _timeLearned.value = "0 phút"
        _nextLessonName.value = "Đang tải bài học gợi ý..."
        _nextLessonXp.value = "+50 XP • Nhấn để học ngay"
    }
}
