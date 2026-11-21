package com.example.doan3.ui.profile

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.example.doan3.ui.ai.ChatAIActivity
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import org.json.JSONObject

class TokenAuthenticator(private val context: Context) : Authenticator {

    companion object {
        private val refreshLock = Any()
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 3) {
            return null
        }

        val sharedPreferences = context.getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
        val failedAuthHeader = response.request.header("Authorization")

        synchronized(refreshLock) {
            // Kiểm tra xem luồng khác đã vừa refresh token thành công chưa
            val latestSavedToken = sharedPreferences.getString("JWT_TOKEN", "") ?: ""
            if (latestSavedToken.isNotEmpty() && failedAuthHeader != "Bearer $latestSavedToken") {
                RetrofitClient.token = latestSavedToken
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $latestSavedToken")
                    .build()
            }

            val refreshToken = sharedPreferences.getString("REFRESH_TOKEN", "")
            if (refreshToken.isNullOrEmpty()) {
                logoutUser(sharedPreferences)
                return null
            }

            val newAccessToken = fetchNewToken(refreshToken)
            return if (!newAccessToken.isNullOrBlank()) {
                sharedPreferences.edit().putString("JWT_TOKEN", newAccessToken).apply()
                RetrofitClient.token = newAccessToken

                response.request.newBuilder()
                    .header("Authorization", "Bearer $newAccessToken")
                    .build()
            } else {
                logoutUser(sharedPreferences)
                null
            }
        }
    }

    private fun fetchNewToken(refreshToken: String): String? {
        try {
            val client = OkHttpClient()
            val jsonBody = JSONObject().apply {
                put("refreshToken", refreshToken)
            }.toString()
            val body = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val baseUrl = RetrofitClient.BASE_URL.trimEnd('/')

            val request = Request.Builder()
                .url("$baseUrl/api/auth/refresh-token")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    if (!responseBody.isNullOrBlank()) {
                        val jsonObject = JSONObject(responseBody)
                        val token = jsonObject.optString("token", "").trim()
                        if (token.isNotEmpty()) {
                            return token
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun responseCount(response: Response?): Int {
        var result = 1
        var rs = response
        while (rs?.priorResponse != null) {
            result++
            rs = rs.priorResponse
        }
        return result
    }

    private fun logoutUser(sharedPreferences: android.content.SharedPreferences) {
        sharedPreferences.edit().clear().apply()
        context.getSharedPreferences("StudyData", Context.MODE_PRIVATE).edit().clear().apply()
        RetrofitClient.token = ""
        ChatAIActivity.clearActiveSession()

        Handler(Looper.getMainLooper()).post {
            val intent = Intent(context, dangnhap::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
        }
    }
}