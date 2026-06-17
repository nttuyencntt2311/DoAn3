package com.example.doan3.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {

    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> = _userName

    private val _progressBarPercent = MutableLiveData<Int>()
    val progressBarPercent: LiveData<Int> = _progressBarPercent

    private val _wordsLearned = MutableLiveData<Int>()
    val wordsLearned: LiveData<Int> = _wordsLearned

    private val _timeLearned = MutableLiveData<Int>()
    val timeLearned: LiveData<Int> = _timeLearned

    init {
        // Khởi tạo dữ liệu giả lập (Mock data) để hiển thị lên giao diện
        _userName.value = "Minh Nguyễn"
        _progressBarPercent.value = 80
        _wordsLearned.value = 1240
        _timeLearned.value = 45
    }
}