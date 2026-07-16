package com.example.doan3.ui.profile

import android.content.Context
import android.content.Intent
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
        // 1. Tránh vòng lặp vô hạn: Nếu đã thử làm mới token 2-3 lần mà vẫn lỗi 401 thì dừng
        if (responseCount(response) >= 3) {
            return null
        }

        // 2. Lấy Refresh Token từ SharedPreferences
        val sharedPreferences = context.getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
        val refreshToken = sharedPreferences.getString("REFRESH_TOKEN", "")

        if (refreshToken.isNullOrEmpty()) {
            return null // Không có Refresh Token -> Chấp nhận lỗi 401
        }

        // 3. Tự động gọi API Refresh Token ĐỒNG BỘ (Synchronous)
        val newAccessToken = fetchNewToken(refreshToken)

        return if (newAccessToken != null) {
            // 4a. Nếu lấy được Token mới thành công -> Lưu đè vào SharedPreferences
            sharedPreferences.edit().putString("JWT_TOKEN", newAccessToken).apply()

            // 4b. Tạo lại Request cũ nhưng thay bằng Token mới, sau đó gửi đi
            response.request.newBuilder()
                .header("Authorization", "Bearer $newAccessToken")
                .build()
        } else {
            // 5. Nếu Refresh Token cũng đã hết hạn/lỗi -> Bắt buộc đăng xuất
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

            // LƯU Ý QUAN TRỌNG: Hãy sửa biến baseUrl dưới đây bằng đúng BASE_URL
            // mà bạn đang khai báo trong file ApiService.kt
            val baseUrl = "http://10.0.2.2:3000" // <-- SỬA LẠI ĐỊA CHỈ NÀY CHO ĐÚNG MÁY BẠN

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
        sharedPreferences.edit().clear().apply()

        // Mở lại màn hình dangnhap.kt và xóa hết lịch sử các Activity trước đó
        val intent = Intent(context, dangnhap::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
    }
}