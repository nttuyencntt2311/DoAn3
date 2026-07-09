package com.example.doan3.ui.profile

import android.content.Context
import androidx.core.content.edit
import com.google.gson.annotations.SerializedName
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

// === CÁC MODEL TỪ MODULE KHÁC CỦA VĂN ===
import com.example.doan3.ui.vocabulary.Vocabulary
import com.example.doan3.ui.roadmap.UserRoadmapResponse
import com.example.doan3.ui.vocabulary.VocabStatsResponse

// ==========================================
//                 MODELS
// ==========================================

// === MODELS ĐĂNG KÝ / ĐĂNG NHẬP ===
data class RegisterRequest(val name: String, val email: String, val password: String, val gender: String = "Nam")
data class RegisterResponse(val message: String, val error: String?)
data class LoginRequest(val email: String, val password: String)
data class UserData(val id: String, val name: String, val email: String)
data class LoginResponse(val message: String, val token: String?, val refreshToken: String?, val user: UserData?, val error: String?)

// === MODELS THÔNG TIN CÁ NHÂN ===
data class ProfileData(
    @SerializedName("_id") val id: String, // Giữ chuẩn của Văn
    val name: String,
    val email: String,
    val gender: String,
    val birthDate: String
)
data class GetProfileResponse(val user: ProfileData?, val error: String?)
data class UpdateProfileRequest(val name: String, val gender: String, val birthDate: String)
data class UpdateProfileResponse(val message: String, val user: ProfileData?, val error: String?)
data class ChangePasswordRequest(val currentPass: String, val newPass: String)
data class ChangePasswordResponse(val message: String?, val error: String?)

// === MODELS GAMIFICATION & STATS ===
data class UserStatsData(
    val name: String,
    val xp: Int,
    val level: Int,
    val streakDays: Int,
    val wordsLearned: Int,
    val timeLearnedMinutes: Int,
    val quizzesCount: Int // Của Tuyến
)
data class UserStatsResponse(val stats: UserStatsData?, val error: String?)

data class LearningStatsResponse(
    val streakDays: Int,
    val totalHours: Int,
    val totalWords: Int,
    val accuracyPercent: Int,
    val chartData: List<Int>,
    val memories: List<MemoryItem>
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
    val timeSpentMinutes: Int
)
data class SubmitQuizResponse(val message: String)

// === MODELS REFRESH TOKEN / LOGOUT ===
data class RefreshTokenRequest(val refreshToken: String)
data class RefreshTokenResponse(val token: String)
data class LogoutResponse(val message: String)
data class LogoutRequest(val refreshToken: String) // Của Tuyến


// ==========================================
//             API INTERFACE
// ==========================================
interface ApiService {

    // --- AUTH & PROFILE ---
    @POST("api/auth/register")
    fun registerUser(@Body request: RegisterRequest): Call<RegisterResponse>

    @POST("api/auth/login")
    fun loginUser(@Body request: LoginRequest): Call<LoginResponse>

    @GET("api/auth/profile")
    fun getProfile(): Call<GetProfileResponse>

    @PUT("api/auth/update-profile")
    fun updateProfile(@Body request: UpdateProfileRequest): Call<UpdateProfileResponse>

    @GET("api/auth/stats")
    fun getUserStats(): Call<UserStatsResponse>

    @GET("api/stats/learning")
    fun getLearningStats(): Call<LearningStatsResponse>

    @PUT("api/auth/change-password")
    fun changePassword(@Body request: ChangePasswordRequest): Call<ChangePasswordResponse>

    @POST("api/auth/refresh-token")
    fun refreshToken(@Body request: RefreshTokenRequest): Call<RefreshTokenResponse>

    // Logout endpoints (Giữ lại cả 2 cấu trúc tham số theo đúng thiết kế của mỗi bạn)
    @POST("api/auth/logout")
    fun logout(@Body request: RefreshTokenRequest): Call<LogoutResponse> // Của Văn

    @POST("api/auth/logout")
    fun logoutUser(@Body request: LogoutRequest): Call<Void> // Của Tuyến

    // --- QUIZ (Từ Tuyến) ---
    @POST("api/quiz/submit")
    fun submitQuiz(@Body request: SubmitQuizRequest): Call<SubmitQuizResponse>

    // --- VOCABULARY & ROADMAP (Từ Văn) ---
    @GET("api/vocabulary")
    fun getAllVocabularies(): Call<List<Vocabulary>>

    @GET("api/vocabulary/{word}")
    fun getVocabularyByWord(@Path("word") word: String): Call<Vocabulary>

    @GET("api/roadmap/{userId}")
    fun getRoadmap(@Path("userId") userId: String): Call<UserRoadmapResponse>

    @GET("api/vocabulary/stats")
    fun getVocabStats(): Call<VocabStatsResponse>

    @PUT("api/vocabulary/{id}/proficiency")
    fun updateProficiency(
        @Path("id") id: String,
        @Body body: Map<String, Int>
    ): Call<Any>
}


// ==========================================
//             AUTHENTICATOR
// ==========================================
class TokenAuthenticator(private val context: Context) : okhttp3.Authenticator {
    override fun authenticate(route: okhttp3.Route?, response: okhttp3.Response): okhttp3.Request? {
        val sharedPreferences = context.getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
        val refreshToken = sharedPreferences.getString("REFRESH_TOKEN", "") ?: ""

        if (refreshToken.isEmpty() || response.responseCount() > 2) return null

        val json = """{"refreshToken":"$refreshToken"}"""
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val body = json.toRequestBody(mediaType)

        val refreshRequest = okhttp3.Request.Builder()
            .url(RetrofitClient.BASE_URL + "api/auth/refresh-token")
            .post(body)
            .build()

        val client = OkHttpClient()
        try {
            val refreshResponse = client.newCall(refreshRequest).execute()
            if (refreshResponse.isSuccessful) {
                val responseBody = refreshResponse.body?.string() ?: ""
                val newAccessToken = JSONObject(responseBody).getString("token")

                sharedPreferences.edit { putString("JWT_TOKEN", newAccessToken) }

                return response.request.newBuilder().header("Authorization", "Bearer $newAccessToken").build()
            } else {
                // Đăng xuất nếu Refresh token cũng hết hạn
                sharedPreferences.edit { clear() }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun okhttp3.Response.responseCount(): Int {
        var result = 1
        var r = priorResponse
        while (r != null) {
            result++
            r = r.priorResponse
        }
        return result
    }
}


// ==========================================
//             RETROFIT CLIENT
// ==========================================
object RetrofitClient {
    const val BASE_URL = "http://10.0.2.2:3000/"

    // Biến token toàn cục của Tuyến (dùng cho instance không qua Context)
    var token: String = ""

    // Instance của Tuyến (Dùng khi không truyền Context)
    val instance: ApiService by lazy {
        val interceptor = Interceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            if (token.isNotEmpty()) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }
            chain.proceed(requestBuilder.build())
        }
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    // Instance của Văn và Tuyến kết hợp (Dùng Context, có Authenticator & Logging)
    fun getInstance(context: Context): ApiService {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }

        val authInterceptor = Interceptor { chain ->
            val sharedPreferences = context.getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
            val dynamicToken = sharedPreferences.getString("JWT_TOKEN", "")
            val requestBuilder = chain.request().newBuilder()

            if (!dynamicToken.isNullOrEmpty()) {
                requestBuilder.addHeader("Authorization", "Bearer $dynamicToken")
            }
            chain.proceed(requestBuilder.build())
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor(authInterceptor)
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