package com.example.doan3.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    init {
        // Gán trạng thái mặc định an toàn cho người mới trong lúc chờ tải dữ liệu mạng
        setDefaultNewUserValues()
        fetchUserData()
    }

    fun fetchUserData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("Users").document(currentUser.uid)
                .addSnapshotListener { document, error ->
                    if (error != null) {
                        setDefaultNewUserValues()
                        return@addSnapshotListener
                    }

                    if (document != null && document.exists()) {
                        // 1. Đồng bộ thông tin cá nhân khách hàng
                        val name = document.getString("hoVaTen")
                        _userName.value = if (name != null && name.isNotEmpty()) name else "Học viên"

                        // ĐỒNG BỘ: Chuyển sang lấy getLong("level") để khớp 100% với file HoSo.kt
                        val level = document.getLong("level")
                        _userLevel.value = if (level != null) "Cấp $level" else "Mới nhập môn"

                        // Chuyển Long từ Firebase thành Int bằng .toInt() và chống null bằng ?: 0
                        _streakDays.value = document.getLong("streak")?.toInt() ?: 0

                        // 2. Đồng bộ tiến độ & Nhiệm vụ hàng ngày
                        val progress = document.getLong("progress")?.toInt() ?: 0
                        _progressPercent.value = progress

                        val currentTaskProgress = document.getLong("taskProgress")?.toInt() ?: 0
                        val maxTask = document.getLong("taskMax")?.toInt() ?: 10
                        _taskTargetText.value = "Mục tiêu hôm nay: $currentTaskProgress/$maxTask từ mới"

                        // 3. Thống kê tổng số từ và thời gian học thực tế
                        _wordsCount.value = document.getLong("wordsLearned")?.toInt() ?: 0
                        val time = document.getLong("timeLearned")?.toInt() ?: 0
                        _timeLearned.value = "$time giờ"

                        // 4. BÀI HỌC TIẾP THEO (Code bất tử chống mọi loại lỗi đỏ)
                        val savedNextLesson = document.getString("nextLesson") ?: ""

                        if (savedNextLesson.isNotEmpty()) {
                            _nextLessonName.value = savedNextLesson
                            _nextLessonXp.value = "+50 XP Tiềm năng"
                        } else {
                            _nextLessonName.value = "Bài 1: Khởi động & Từ vựng cơ bản"
                            _nextLessonXp.value = "Nhấn để bắt đầu bài học đầu tiên!"
                        }
                    } else {
                        setDefaultNewUserValues()
                    }
                }
        } else {
            _userName.value = "Chưa đăng nhập"
            _nextLessonName.value = "Vui lòng đăng nhập"
            _nextLessonXp.value = ""
        }
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