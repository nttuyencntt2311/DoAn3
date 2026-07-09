package com.example.doan3.ui.home

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.doan3.ui.profile.UserStatsResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.doan3.ui.profile.RetrofitClient

class HomeViewModel : ViewModel() {

    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> = _userName

    private val _userLevel = MutableLiveData<String>()
    val userLevel: LiveData<String> = _userLevel

    private val _streakDays = MutableLiveData<Int>()
    val streakDays: LiveData<Int> = _streakDays

    private val _progressPercent = MutableLiveData<Int>()
    val progressPercent: LiveData<Int> = _progressPercent

    private val _taskTargetText = MutableLiveData<String>()
    val taskTargetText: LiveData<String> = _taskTargetText

    private val _wordsCount = MutableLiveData<Int>()
    val wordsCount: LiveData<Int> = _wordsCount

    private val _timeLearned = MutableLiveData<String>()
    val timeLearned: LiveData<String> = _timeLearned

    private val _nextLessonName = MutableLiveData<String>()
    val nextLessonName: LiveData<String> = _nextLessonName

    private val _nextLessonXp = MutableLiveData<String>()
    val nextLessonXp: LiveData<String> = _nextLessonXp

    init {
        // Gán trạng thái mặc định ban đầu khi vừa mở màn hình
        setDefaultNewUserValues()
    }

    fun fetchUserData(context: Context) {
        RetrofitClient.getInstance(context).getUserStats().enqueue(object : Callback<UserStatsResponse> {
            override fun onResponse(call: Call<UserStatsResponse>, response: Response<UserStatsResponse>) {
                val stats = response.body()?.stats
                if (response.isSuccessful && stats != null) {
                    _userName.value = stats.name
                    _userLevel.value = "Cấp: ${stats.level}"
                    _streakDays.value = stats.streakDays
                    _wordsCount.value = stats.wordsLearned
                    _timeLearned.value = "${stats.timeLearnedMinutes} phút"
                    _nextLessonXp.value = "${stats.xp} XP"

                    // Tính phần trăm tiến trình giả lập dựa trên XP
                    _progressPercent.value = if (stats.xp < 100) stats.xp else (stats.xp % 100)
                } else {
                    // Nếu server trả về lỗi (ví dụ 404, 500), đưa giao diện về mặc định
                    setDefaultNewUserValues()
                }
            }

            override fun onFailure(call: Call<UserStatsResponse>, t: Throwable) {
                // ĐÃ KHÔI PHỤC: Chủ động đưa giao diện về mặc định khi mất kết nối mạng
                setDefaultNewUserValues()
            }
        })
    }

    private fun setDefaultNewUserValues() {
        _userName.value = "Học viên mới"
        _userLevel.value = "Mới nhập môn"
        _streakDays.value = 0
        _progressPercent.value = 0
        _taskTargetText.value = "Mục tiêu hôm nay: 0/10 từ mới"
        _wordsCount.value = 0
        _timeLearned.value = "0 phút"
        _nextLessonName.value = "Bài 1: Bắt đầu hành trình"
        _nextLessonXp.value = "0 XP"
    }
}