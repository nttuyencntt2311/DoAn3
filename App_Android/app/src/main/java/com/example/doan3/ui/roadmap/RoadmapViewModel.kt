package com.example.doan3.ui.roadmap

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.doan3.ui.profile.RetrofitClient
import kotlinx.coroutines.launch

class RoadmapViewModel(application: Application) : AndroidViewModel(application) {

    private val _roadmapData = MutableLiveData<UserRoadmapResponse>()
    val roadmapData: LiveData<UserRoadmapResponse> = _roadmapData

    // QUẢN LÝ TRẠNG THÁI LOADING & LỖI
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // 🚀 1. LẤY LỘ TRÌNH ĐÃ LƯU CỦA USER TỪ BACKEND
    fun fetchAiRoadmapForUser(userId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val apiService = RetrofitClient.getInstance(getApplication())

                // 🛠 ĐÃ SỬA LỖI Ở ĐÂY: Không truyền userId vào hàm này nữa vì Backend đã tự lấy từ Token
                val response = apiService.getRoadmapForUser()

                if (response.isSuccessful && response.body() != null) {
                    _roadmapData.value = response.body()
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = "Không tìm thấy lộ trình học tập: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Lỗi kết nối máy chủ: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 🚀 2. GỬI TIÊU CHÍ FORM LÊN ĐỂ AI GENERATE LỘ TRÌNH MỚI
    fun generateAiRoadmap(
        userId: String,
        target: String,
        currentLevel: String,
        freeTime: String,
        expectation: String
    ) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val apiService = RetrofitClient.getInstance(getApplication())
                val request = GenerateRoadmapRequest(
                    userId = userId,
                    target = target,
                    currentLevel = currentLevel,
                    freeTime = freeTime,
                    expectation = expectation
                )

                val response = apiService.generateRoadmap(request)

                if (response.isSuccessful && response.body() != null) {
                    _roadmapData.value = response.body()
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = "Lỗi hệ thống khi tạo lộ trình: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Không thể tạo lộ trình từ AI: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}