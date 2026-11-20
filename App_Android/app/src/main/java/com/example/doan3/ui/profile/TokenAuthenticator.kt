package com.example.doan3.ui.profile

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import org.json.JSONObject

class TokenAuthenticator(private val context: Context) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        android.util.Log.e("DEBUG_AUTH", "BỊ LỖI 401, TOKEN AUTHENTICATOR ĐANG CHẠY!")

        if (responseCount(response) >= 3) {
            return null
        }

        val sharedPreferences = context.getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
        val refreshToken = sharedPreferences.getString("REFRESH_TOKEN", "")

        if (refreshToken.isNullOrEmpty()) {
            android.util.Log.e("DEBUG_AUTH", "KHÔNG CÓ REFRESH TOKEN -> ĐÁ VỀ LOGIN!")
            logoutUser(sharedPreferences)
            return null
        }

        val newAccessToken = fetchNewToken(refreshToken)

        return if (newAccessToken != null) {
            android.util.Log.d("DEBUG_AUTH", "LẤY TOKEN MỚI THÀNH CÔNG!")
            sharedPreferences.edit().putString("JWT_TOKEN", newAccessToken).apply()
            RetrofitClient.token = newAccessToken

            response.request.newBuilder()
                .header("Authorization", "Bearer $newAccessToken")
                .build()
        } else {
            android.util.Log.e("DEBUG_AUTH", "LẤY TOKEN MỚI THẤT BẠI -> ĐÁ VỀ LOGIN!")
            logoutUser(sharedPreferences)
            null
        }
    }
    // Hàm gọi API trực tiếp bằng OkHttp (để tránh vòng lặp khởi tạo Retrofit)
    private fun fetchNewToken(refreshToken: String): String? {
        try {
            val client = OkHttpClient()

            // Tạo Body JSON gửi đi
            val jsonBody = JSONObject().apply {
                put("refreshToken", refreshToken)
            }.toString()
            val body = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

            // LƯU Ý QUAN TRỌNG: Code của bạn dùng 3000, code bạn bạn dùng 5000.
            // Ở đây giữ số 5000 (đã sửa để gọi đúng tới Backend) từ code bạn bạn.
            val baseUrl = RetrofitClient.BASE_URL.trimEnd('/')

            val request = Request.Builder()
                .url("$baseUrl/api/auth/refresh-token")
                .post(body)
                .build()

            // Gọi đồng bộ (execute) thay vì bất đồng bộ (enqueue)
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    val jsonObject = JSONObject(responseBody)
                    // Bóc tách "token" từ JSON phản hồi
                    return jsonObject.optString("token", null)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    // Hàm đếm số lần gọi thất bại để tránh lặp vô hạn
    private fun responseCount(response: Response?): Int {
        var result = 1
        var rs = response
        while (rs?.priorResponse != null) {
            result++
            rs = rs.priorResponse
        }
        return result
    }

    // Hàm xử lý khi Refresh Token hết hạn: Xóa dữ liệu và đá về màn hình Đăng nhập
    private fun logoutUser(sharedPreferences: android.content.SharedPreferences) {
        // 1. Xóa toàn bộ dữ liệu phiên cũ
        sharedPreferences.edit().clear().apply()

        // Reset biến global (Từ code bạn bạn)
        RetrofitClient.token = ""

        // 2. 🚀 ĐÃ SỬA: Bắt buộc gọi startActivity trên Main Thread để chống văng app (Từ code bạn bạn)
        // Mở lại màn hình dangnhap.kt và xóa hết lịch sử các Activity trước đó
        Handler(Looper.getMainLooper()).post {
            val intent = Intent(context, dangnhap::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
        }
    }
}