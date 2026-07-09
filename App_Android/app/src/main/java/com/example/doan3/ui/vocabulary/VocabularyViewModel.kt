package com.example.doan3.ui.vocabulary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.doan3.ui.profile.RetrofitClient
class VocabularyViewModel(application: Application) : AndroidViewModel(application) {

    // --- CÁC BIẾN LƯU TRỮ DỮ LIỆU ---

    // 1. Biến chứa danh sách từ vựng
    private val _vocabularyList = MutableLiveData<List<Vocabulary>>()
    val vocabularyList: LiveData<List<Vocabulary>> get() = _vocabularyList

    // 2. Biến chứa dữ liệu thống kê (Tổng số từ, thành thạo,...)
    private val _vocabStats = MutableLiveData<VocabStatsResponse>()
    val vocabStats: LiveData<VocabStatsResponse> get() = _vocabStats

    // 3. Biến quản lý trạng thái loading
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    // 4. Biến quản lý thông báo lỗi
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    init {
        // Tự động fetch cả danh sách và thống kê khi ViewModel được khởi tạo
        fetchVocabularyList()
        fetchStats()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // --- CÁC HÀM GỌI API ---

    fun fetchVocabularyList() {
        _isLoading.value = true
        val apiService = RetrofitClient.getInstance(getApplication())

        apiService.getAllVocabularies().enqueue(object : Callback<List<Vocabulary>> {
            override fun onResponse(call: Call<List<Vocabulary>>, response: Response<List<Vocabulary>>) {
                _isLoading.value = false
                if (response.isSuccessful) {
                    _vocabularyList.value = response.body() ?: emptyList()
                } else {
                    _errorMessage.value = "Lỗi server (danh sách): ${response.code()}"
                }
            }

            override fun onFailure(call: Call<List<Vocabulary>>, t: Throwable) {
                _isLoading.value = false
                _errorMessage.value = "Không kết nối được server: ${t.message}"
            }
        })
    }

    fun fetchStats() {
        val apiService = RetrofitClient.getInstance(getApplication())

        apiService.getVocabStats().enqueue(object : Callback<VocabStatsResponse> {
            override fun onResponse(call: Call<VocabStatsResponse>, response: Response<VocabStatsResponse>) {
                if (response.isSuccessful) {
                    _vocabStats.value = response.body()
                } else {
                    // Nếu lỗi chỉ ở phần thống kê, có thể log ra hoặc báo lỗi nhẹ
                    // _errorMessage.value = "Lỗi tải thống kê: ${response.code()}"
                }
            }

            override fun onFailure(call: Call<VocabStatsResponse>, t: Throwable) {
                // Tùy chọn: Báo lỗi nếu rớt mạng khi gọi hàm này
                // _errorMessage.value = "Lỗi mạng khi tải thống kê"
            }
        })
    }
}