package com.example.doan3.ui.profile

import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

// === MODELS ĐĂNG KÝ / ĐĂNG NHẬP ===
data class RegisterRequest(val name: String, val email: String, val password: String, val gender: String = "Nam")
data class RegisterResponse(val message: String, val error: String?)
data class LoginRequest(val email: String, val password: String)
data class UserData(val id: String, val name: String, val email: String)
// Cập nhật LoginResponse để nhận thêm refreshToken
data class LoginResponse(val message: String, val token: String?, val refreshToken: String?, val user: UserData?, val error: String?)

// === MODELS THÔNG TIN CÁ NHÂN ===
data class ProfileData(val _id: String, val name: String, val email: String, val gender: String, val birthDate: String)
data class GetProfileResponse(val user: ProfileData?, val error: String?)
data class UpdateProfileRequest(val name: String, val gender: String, val birthDate: String)
data class UpdateProfileResponse(val message: String, val user: ProfileData?, val error: String?)
data class ChangePasswordRequest(val currentPass: String, val newPass: String)
data class ChangePasswordResponse(val message: String?, val error: String?)

// === MODELS GAMIFICATION (THỐNG KÊ HỌC TẬP) ===
data class UserStatsData(
    val name: String,
    val xp: Int,
    val level: Int,
    val streakDays: Int,
    val wordsLearned: Int,
    val timeLearnedMinutes: Int
)
data class UserStatsResponse(val stats: UserStatsData?, val error: String?)

// === MODELS REFRESH TOKEN / LOGOUT MỚI BỔ SUNG ===
data class RefreshTokenRequest(val refreshToken: String)
data class RefreshTokenResponse(val token: String)
data class LogoutResponse(val message: String)

data class LearningStatsResponse(
    val streakDays: Int,
    val totalHours: Int,
    val totalWords: Int,
    val accuracyPercent: Int,
    val chartData: List<Int>,
    val memories: List<MemoryItem> // Đón dữ liệu Kỷ niệm từ Backend
)
data class MemoryItem(
    val date: String,
    val title: String,
    val description: String,
    val xp: Int
)

data class SubmitQuizRequest(
    val topic: String,
    val score: Int,
    val totalQuestions: Int,
    val earnedXp: Int,
    val learnedWords: Int,
    val timeSpentMinutes: Int // Đã đồng bộ trường mới với backend
)
data class SubmitQuizResponse(val message: String)

// === API INTERFACE ===
interface ApiService {
    @POST("api/auth/register")
    fun registerUser(@Body request: RegisterRequest): Call<RegisterResponse>

    @POST("api/auth/login")
    fun loginUser(@Body request: LoginRequest): Call<LoginResponse>

    @GET("api/auth/profile")
    fun getProfile(): Call<GetProfileResponse>

    @PUT("api/auth/update-profile")
    fun updateProfile(@Body request: UpdateProfileRequest): Call<UpdateProfileResponse>

    @GET("api/auth/stats") // Endpoint cũ đáp ứng cho màn hình HOME
    fun getUserStats(): Call<UserStatsResponse>

    @GET("api/stats/learning") // Gọi chính xác đến file statsRoutes mới ở Backend
    fun getLearningStats(): Call<LearningStatsResponse>


    @PUT("api/auth/change-password")
    fun changePassword(@Body request: ChangePasswordRequest): Call<ChangePasswordResponse>

    // 2 HÀM MỚI THÊM:
    @POST("api/auth/refresh-token")
    fun refreshToken(@Body request: RefreshTokenRequest): Call<RefreshTokenResponse>

    @POST("/api/quiz/submit")
    fun submitQuiz(@Body request: SubmitQuizRequest): Call<SubmitQuizResponse>

    @POST("api/auth/logout")
    fun logout(@Body request: RefreshTokenRequest): Call<LogoutResponse>
}

// === AUTHENTICATOR (TỰ ĐỘNG GIA HẠN TOKEN KHI BỊ LỖI 401) ===
class TokenAuthenticator(private val context: Context) : okhttp3.Authenticator {
    override fun authenticate(route: okhttp3.Route?, response: okhttp3.Response): okhttp3.Request? {
        // Tránh lặp vô hạn nếu refresh token cũng chết
        if (response.responseCount > 2) {
            return null
        }

        val sharedPreferences = context.getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
        val refreshToken = sharedPreferences.getString("REFRESH_TOKEN", "") ?: ""

        if (refreshToken.isEmpty()) return null

        val json = "{\"refreshToken\":\"$refreshToken\"}"
        val mediaType = okhttp3.MediaType.parse("application/json; charset=utf-8")
        val body = okhttp3.RequestBody.create(mediaType, json)

        val refreshRequest = okhttp3.Request.Builder()
            .url(RetrofitClient.BASE_URL + "api/auth/refresh-token") // Đảm bảo đúng đường dẫn
            .post(body)
            .build()

        val client = okhttp3.OkHttpClient()
        try {
            val refreshResponse = client.newCall(refreshRequest).execute()
            if (refreshResponse.isSuccessful) {
                val responseBody = refreshResponse.body()?.string()
                val jsonObject = org.json.JSONObject(responseBody ?: "")
                val newAccessToken = jsonObject.getString("token")

                // Lưu Token mới vào điện thoại
                sharedPreferences.edit().putString("JWT_TOKEN", newAccessToken).apply()

                // Gắn token mới vào request cũ và chạy lại
                return response.request().newBuilder()
                    .header("Authorization", "Bearer $newAccessToken")
                    .build()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private val okhttp3.Response.responseCount: Int
        get() {
            var result = 1
            var r = priorResponse()
            while (r != null) {
                result++
                r = r.priorResponse()
            }
            return result
        }
}

// === RETROFIT CLIENT ===
object RetrofitClient {
    const val BASE_URL = "http://10.0.2.2:3000/" // Đổi IP nếu dùng máy thật

    var token: String = "" // Hỗ trợ code cũ

    val instance: ApiService by lazy {
        val interceptor = Interceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            if (token.isNotEmpty()) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }
            chain.proceed(requestBuilder.build())
        }
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        Retrofit.Builder().baseUrl(BASE_URL).client(client).addConverterFactory(GsonConverterFactory.create()).build().create(ApiService::class.java)
    }

    fun getInstance(context: Context): ApiService {
        val interceptor = Interceptor { chain ->
            val sharedPreferences = context.getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
            val dynamicToken = sharedPreferences.getString("JWT_TOKEN", "")
            val requestBuilder = chain.request().newBuilder()
            if (!dynamicToken.isNullOrEmpty()) {
                requestBuilder.addHeader("Authorization", "Bearer $dynamicToken")
            }
            chain.proceed(requestBuilder.build())
        }

        // ĐÃ BỔ SUNG: add authenticator vào Client
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .authenticator(TokenAuthenticator(context))
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}