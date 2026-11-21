package com.example.doan3.ui.statistics

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.doan3.ui.profile.LearningStatsResponse
import com.example.doan3.ui.profile.MemoryItem
import com.example.doan3.ui.profile.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

data class LearningStats(
    val streakDays: Int,
    val totalHours: Int,
    val totalWords: Int,
    val accuracyPercent: Int,
    val chartData: List<Int>?
)

class StatisticsViewModel : ViewModel() {

    private val _statsData = MutableLiveData<LearningStats>()
    val statsData: LiveData<LearningStats> get() = _statsData

    private val _memoriesData = MutableLiveData<List<MemoryItem>>(emptyList())
    val memoriesData: LiveData<List<MemoryItem>> get() = _memoriesData

    fun fetchRealStatistics(context: Context) {
        RetrofitClient.getInstance(context).getLearningStats()
            .enqueue(object : Callback<LearningStatsResponse> {
                override fun onResponse(call: Call<LearningStatsResponse>, response: Response<LearningStatsResponse>) {
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        val nested = body.stats

                        _statsData.value = LearningStats(
                            streakDays = nested?.streakDays ?: body.streakDays ?: 0,
                            totalHours = nested?.totalHours ?: body.totalHours ?: 0,
                            totalWords = nested?.totalWords ?: body.totalWords ?: 0,
                            accuracyPercent = nested?.accuracyPercent ?: body.accuracyPercent ?: 0,
                            chartData = nested?.chartData ?: body.chartData ?: listOf(0, 0, 0, 0, 0, 0, 0)
                        )
                        _memoriesData.value = body.memories?.reversed() ?: emptyList()
                    } else {
                        setEmptyStats()
                    }
                }

                override fun onFailure(call: Call<LearningStatsResponse>, t: Throwable) {
                    setEmptyStats()
                }
            })
    }

    private fun setEmptyStats() {
        _statsData.value = LearningStats(0, 0, 0, 0, listOf(0, 0, 0, 0, 0, 0, 0))
        _memoriesData.value = emptyList()
    }
}