package com.example.doan3.ui.vocabulary

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.doan3.ui.profile.RetrofitClient
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class VocabularyViewModel(application: Application) : AndroidViewModel(application) {

    private val _vocabularyList = MutableLiveData<List<Vocabulary>>()
    val vocabularyList: LiveData<List<Vocabulary>> get() = _vocabularyList

    private val _vocabStats = MutableLiveData<VocabStatsResponse>()
    val vocabStats: LiveData<VocabStatsResponse> get() = _vocabStats

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    private val gson = Gson()
    private val cachePrefs by lazy {
        getApplication<Application>().getSharedPreferences("VocabCachePrefs", Context.MODE_PRIVATE)
    }

    init {
        fetchVocabularyList()
        fetchStats()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    private fun saveOfflineCache(list: List<Vocabulary>) {
        try {
            cachePrefs.edit().putString("CACHED_VOCAB_LIST", gson.toJson(list)).apply()
        } catch (_: Exception) {}
    }

    private fun loadOfflineCache(): List<Vocabulary> {
        return try {
            val json = cachePrefs.getString("CACHED_VOCAB_LIST", null) ?: return emptyList()
            val type = object : TypeToken<List<Vocabulary>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun fetchVocabularyList() {
        _isLoading.value = true
        val apiService = RetrofitClient.getInstance(getApplication())

        apiService.getAllVocabularies().enqueue(object : Callback<List<Vocabulary>> {
            override fun onResponse(call: Call<List<Vocabulary>>, response: Response<List<Vocabulary>>) {
                _isLoading.value = false
                if (response.isSuccessful) {
                    val list = response.body() ?: emptyList()
                    _vocabularyList.value = list
                    if (list.isNotEmpty()) saveOfflineCache(list)
                } else {
                    val cached = loadOfflineCache()
                    if (cached.isNotEmpty()) {
                        _vocabularyList.value = cached
                        _errorMessage.value = "Đang hiển thị kho từ lưu ngoại tuyến (Offline)"
                    } else {
                        _errorMessage.value = "Lỗi server (danh sách): ${response.code()}"
                    }
                }
            }

            override fun onFailure(call: Call<List<Vocabulary>>, t: Throwable) {
                _isLoading.value = false
                val cached = loadOfflineCache()
                if (cached.isNotEmpty()) {
                    _vocabularyList.value = cached
                    _errorMessage.value = "Không có mạng — đang hiển thị ${cached.size} từ đã lưu offline ✈️"
                } else {
                    _errorMessage.value = "Không kết nối được server: ${t.message}"
                }
            }
        })
    }

    fun fetchStats() {
        val apiService = RetrofitClient.getInstance(getApplication())

        apiService.getVocabStats().enqueue(object : Callback<VocabStatsResponse> {
            override fun onResponse(call: Call<VocabStatsResponse>, response: Response<VocabStatsResponse>) {
                if (response.isSuccessful) {
                    _vocabStats.value = response.body()
                }
            }

            override fun onFailure(call: Call<VocabStatsResponse>, t: Throwable) {}
        })
    }
}
