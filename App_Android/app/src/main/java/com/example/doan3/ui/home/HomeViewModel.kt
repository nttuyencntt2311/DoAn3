package com.example.doan3.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

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

    // Tên bài học động kết nối trực tiếp với tvLessonName trong XML của bạn
    private val _nextLessonName = MutableLiveData<String>()
    val nextLessonName: LiveData<String> = _nextLessonName

    // Nội dung text XP kết nối trực tiếp với tvXp trong XML của bạn
    private val _nextLessonXp = MutableLiveData<String>()
    val nextLessonXp: LiveData<String> = _nextLessonXp

    init {
        // Gán trạng thái mặc định an toàn cho người mới trong lúc chờ tải dữ liệu mạng
        setDefaultNewUserValues()
        fetchUserData()
    }

    fun fetchUserData() {
        // TODO: Sau này bạn dùng Retrofit gọi API GET /api/user/home từ backend Node.js (MongoDB) ở đây.

        // Hiện tại: Gán dữ liệu giả lập tĩnh để test giao diện Trang chủ mà không bị lỗi
        _userName.value = "Người dùng LexiVT"
        _userLevel.value = "Cấp 1"
        _streakDays.value = 5
        _progressPercent.value = 50
        _taskTargetText.value = "Mục tiêu hôm nay: 5/10 từ mới"
        _wordsCount.value = 120
        _timeLearned.value = "2 giờ"

        _nextLessonName.value = "Bài 2: Giao tiếp cơ bản"
        _nextLessonXp.value = "+50 XP Tiềm năng"
    }

    private fun setDefaultNewUserValues() {
        _userName.value = "Học viên mới"
        _userLevel.value = "Mới nhập môn"
        _streakDays.value = 0
        _progressPercent.value = 0
        _taskTargetText.value = "Mục tiêu hôm nay: 0/10 từ mới"
        _wordsCount.value = 0
        _timeLearned.value = "0 giờ"

        // Giá trị mấu chốt xử lý logic cho người mới bắt đầu học bài đầu tiên
        _nextLessonName.value = "Bài 1: Khởi động & Từ vựng cơ bản"
        _nextLessonXp.value = "Nhấn để bắt đầu bài học đầu tiên!"
    }
}