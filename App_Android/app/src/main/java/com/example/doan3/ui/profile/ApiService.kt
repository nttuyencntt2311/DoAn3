package com.example.doan3.ui.profile

import android.content.Context
import androidx.core.content.edit
import com.example.doan3.ui.roadmap.QuizQuestion
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
import java.util.concurrent.TimeUnit
import retrofit2.Response
import retrofit2.http.Query

// === CÁC MODEL TỪ MODULE KHÁC ===
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
    @SerializedName("_id") val id: String,
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

data class GoogleLoginRequest(val email: String, val name: String)
data class ForgotPasswordRequest(val email: String)
data class ForgotPasswordResponse(val message: String?, val demoOtp: String?, val error: String?)
data class ResetPasswordRequest(val email: String, val otp: String, val newPassword: String)
data class ResetPasswordResponse(val message: String?, val error: String?)
data class PersonalVocabRequest(val word: String, val pronunciation: String, val meaning: String, val category: String)
data class PersonalVocabResponse(val message: String?, val vocabulary: Vocabulary?, val error: String?)


// === MODELS GAMIFICATION & STATS ===
data class UserStatsData(
    val name: String,
    val xp: Int,
    val level: Int,
    val streakDays: Int,
    val wordsLearned: Int,
    val timeLearnedMinutes: Int,
    val quizzesCount: Int
)
data class UserStatsResponse(val stats: UserStatsData?, val error: String?)

// ĐÃ KẾT HỢP: Bao gồm cả cấu trúc lồng nhau và cấu trúc phẳng
data class LearningStatsResponse(
    val stats: StatsData? = null,
    val streakDays: Int? = null,
    val totalHours: Int? = null,
    val totalWords: Int? = null,
    val accuracyPercent: Int? = null,
    val chartData: List<Int>? = null,
    val memories: List<MemoryItem>? = null
)

data class StatsData(
    val streakDays: Int,
    val totalHours: Int,
    val totalWords: Int,
    val accuracyPercent: Int,
    val chartData: List<Int>?
)

data class MemoryItem(
    val date: String,
    val title: String,
    val description: String,
    val xp: Int
)



// === MODEL QUIZ VÀ BỆNH ÁN XAI MỚI ===
data class QuestionLog(
    val question: String,
    val topic: String,
    val isCorrect: Boolean,
    val elapsedTimeMs: Long
)

data class SubmitQuizRequest(
    val topic: String,
    val score: Int,
    val totalQuestions: Int,
    val earnedXp: Int,
    val learnedWords: Int,
    val timeSpentMinutes: Int,
    val difficulty: Int = 3,
    val wrongWords: List<String> = emptyList(),
    val avgElapsedTimeMs: Long = 0L,
    val questionLogs: List<QuestionLog> = emptyList()
)

// Chức năng " Sổ tay tiên tri "
data class PredictiveFlashcard(
    val wordId: String,
    val wordText: String,
    val meaning: String,
    val phonetic: String,
    val predicted_accuracy: Double,
    val is_struggling: Boolean,
    val xai_reason: String? = null
)

data class PredictForgettingResponse(
    val message: String,
    val warning: String,
    val predictiveFlashcards: List<PredictiveFlashcard>
)

// Cấu trúc chi tiết của 1 yếu tố SHAP để sau này dùng vẽ biểu đồ
data class ShapDetail(
    @SerializedName("feature") val feature: String,
    @SerializedName("vi_description") val viDescription: String,
    @SerializedName("value") val value: Double,
    @SerializedName("shap_score") val shapScore: Double
)

// Cấu trúc Giải thích XAI (Điểm mạnh / Điểm yếu) ĐÃ ĐƯỢC CẬP NHẬT
data class XaiExplanation(
    @SerializedName("base_value") val baseValue: Double? = null,
    @SerializedName("negative_factors") val negativeFactors: List<String>,
    @SerializedName("positive_factors") val positiveFactors: List<String>,
    @SerializedName("detailed_shap") val detailedShap: List<ShapDetail>? = null // Dữ liệu vẽ biểu đồ SHAP
)

data class CounterfactualSuggestion(
    @SerializedName("current_accuracy") val currentAccuracy: Double = 0.0,
    @SerializedName("target_accuracy") val targetAccuracy: Double = 0.0,
    @SerializedName("delta_percent") val deltaPercent: Double = 0.0,
    @SerializedName("summary_vi") val summaryVi: String = ""
)

// Cấu trúc Bệnh án AI tổng thể (SHAP + Counterfactual + Grounded NLG)
data class AiReport(
    @SerializedName("predicted_accuracy") val predictedAccuracy: Double,
    @SerializedName("is_struggling") val isStruggling: Boolean,
    @SerializedName("xai_explanation") val xaiExplanation: XaiExplanation,
    @SerializedName("counterfactual") val counterfactual: CounterfactualSuggestion? = null,
    @SerializedName("pedagogical_feedback") val pedagogicalFeedback: String? = null
)

data class SubmitQuizResponse(
    val message: String,
    val isLevelUp: Boolean,
    @SerializedName("ai_report") val aiReport: AiReport? = null // Bổ sung trường này để hứng bệnh án
)

// === MODEL ĐỂ LƯU TỪ VỰNG YÊU THÍCH ===
data class SaveWordRequest(
    val userId: String,
    val wordId: String
)

// === MODELS REFRESH TOKEN / LOGOUT ===
data class RefreshTokenRequest(val refreshToken: String)
data class RefreshTokenResponse(val token: String)
data class LogoutResponse(val message: String)
data class LogoutRequest(val refreshToken: String)


// === MODELS TÍNH NĂNG AI / LỊCH SỬ CHAT ===
data class ChatResponse(val reply: String)
data class ChatRequest(val message: String, val sessionId: String? = null)

data class CreateSessionRequest(
    val userId: String,
    val chatType: String,
    val firstMessage: String
)
data class CreateSessionResponse(val sessionId: String, val title: String)

data class ChatHistoryResponse(val messages: List<HistoryMessage>)
data class HistoryMessage(val role: String, val text: String)

data class ChatSessionItem(val _id: String, val title: String, val chatType: String)
data class UserSessionsResponse(val sessions: List<ChatSessionItem>)

// === MODEL CHO TẠO QUIZ BẰNG AI ===
data class GenerateCustomQuizRequest(val content: String, val questionCount: Int)
data class GenerateCustomQuizResponse(val message: String, val categoryName: String)
data class CustomCategoriesResponse(val success: Boolean, val categories: List<String>)

// === MODEL CẬP NHẬT ĐIỂM ===
data class ProficiencyRequest(val proficiency: Int, val userId: String)
data class ProficiencyResponse(val message: String, val wordsLearned: Int, val xp: Int)

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

    @PUT("api/auth/profile")
    fun updateProfile(@Body request: UpdateProfileRequest): Call<UpdateProfileResponse>

    @GET("api/auth/stats")
    fun getUserStats(): Call<UserStatsResponse>

    @GET("api/stats/learning")
    fun getLearningStats(): Call<LearningStatsResponse>

    @PUT("api/auth/change-password")
    fun changePassword(@Body request: ChangePasswordRequest): Call<ChangePasswordResponse>

    @POST("api/auth/refresh-token")
    fun refreshToken(@Body request: RefreshTokenRequest): Call<RefreshTokenResponse>

    // Logout endpoints
    @POST("api/auth/logout")
    fun logout(@Body request: RefreshTokenRequest): Call<LogoutResponse>


    @POST("api/auth/logout")
    fun logoutUser(@Body request: LogoutRequest): Call<Void>

    @POST("api/auth/save-word")
    fun saveWord(@Body request: SaveWordRequest): Call<Any>

    // --- QUIZ ---
    @POST("api/quiz/submit")
    fun submitQuiz(@Body request: SubmitQuizRequest): Call<SubmitQuizResponse>

    // --- VOCABULARY & ROADMAP ---
    @GET("api/vocabularies")
    fun getAllVocabularies(): Call<List<Vocabulary>>

    @GET("api/vocabularies/{word}")
    fun getVocabularyByWord(@Path("word") word: String): Call<Vocabulary>

    @GET("api/vocabularies/stats")
    fun getVocabStats(): Call<VocabStatsResponse>

    @PUT("api/vocabularies/{id}/proficiency")
    fun updateProficiency(
        @Path("id") id: String,
        @Body request: ProficiencyRequest
    ): Call<ProficiencyResponse>

    // --- AI / CHAT ---
    @POST("api/ai/chat")
    fun sendChatMessage(@Body request: ChatRequest): Call<ChatResponse>

    @POST("api/ai/session")
    fun createChatSession(@Body request: CreateSessionRequest): Call<CreateSessionResponse>

    @GET("api/ai/session/{sessionId}/messages")
    fun getChatHistory(@Path("sessionId") sessionId: String): Call<ChatHistoryResponse>

    @GET("api/ai/sessions/user/{userId}")
    fun getUserSessions(@Path("userId") userId: String): Call<UserSessionsResponse>

    // --- ROADMAP (SUSPEND) ---
    @GET("api/ai/my-roadmap")
    suspend fun getRoadmapForUser(): Response<UserRoadmapResponse>

    @POST("api/ai/generate-roadmap")
    suspend fun generateRoadmap(
        @Body request: com.example.doan3.ui.roadmap.GenerateRoadmapRequest
    ): Response<UserRoadmapResponse>

    @GET("api/ai/generate-quiz")
    suspend fun getStepQuiz(
        @Query("stepTitle") title: String,
        @Query("stepDesc") desc: String,
        @Query("questionCount") questionCount: Int
    ): retrofit2.Response<List<QuizQuestion>>

    @POST("api/ai/roadmap/complete-step")
    suspend fun completeRoadmapStep(@Body body: Map<String, String>): retrofit2.Response<Any>

    // --- CUSTOM QUIZ ---
    @POST("api/vocabularies/custom-quiz")
    fun generateCustomQuiz(@Body request: GenerateCustomQuizRequest): Call<GenerateCustomQuizResponse>

    @GET("api/vocabularies/custom-categories")
    fun getCustomQuizCategories(): Call<CustomCategoriesResponse>

    // --- PREDICTIVE FLASHCARDS (SỔ TAY TIÊN TRI) ---
    @GET("api/vocabularies/predict-forgetting")
    fun getPredictiveFlashcards(): Call<PredictForgettingResponse>

    // --- QUÊN MẬT KHẨU OTP & TỪ VỰNG CÁ NHÂN ---
    @POST("api/auth/google-login")
    fun googleLogin(@Body request: GoogleLoginRequest): Call<LoginResponse>

    @POST("api/auth/forgot-password")
    fun forgotPassword(@Body request: ForgotPasswordRequest): Call<ForgotPasswordResponse>

    @POST("api/auth/reset-password")
    fun resetPassword(@Body request: ResetPasswordRequest): Call<ResetPasswordResponse>

    @POST("api/vocabularies/personal")
    fun addPersonalVocab(@Body request: PersonalVocabRequest): Call<PersonalVocabResponse>

    // --- LEADERBOARD ---
    @GET("api/users/leaderboard")
    fun getLeaderboard(): Call<List<LeaderboardUser>>
}

// ==========================================
//             RETROFIT CLIENT
// ==========================================
object RetrofitClient {
    // 🚀 Sử dụng cổng 3000 hoặc 5000 tùy vào cấu hình Node.js
    const val BASE_URL = "http://10.0.2.2:8080/"

    var token: String = ""

    val instance: ApiService by lazy {
        val interceptor = Interceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            if (token.isNotEmpty()) {
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }
            chain.proceed(requestBuilder.build())
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    // 🚀 DÙNG SINGLETON ĐỂ KHÔNG BỊ RÒ RỈ BỘ NHỚ VÀ MẤT TOKEN
    @Volatile
    private var contextInstance: ApiService? = null

    fun getInstance(context: Context): ApiService {
        return contextInstance ?: synchronized(this) {
            contextInstance ?: buildContextRetrofit(context).also { contextInstance = it }
        }
    }

    private fun buildContextRetrofit(context: Context): ApiService {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }

        val authInterceptor = Interceptor { chain ->
            // Luôn lấy token mới nhất từ bộ nhớ máy
            val sharedPreferences = context.applicationContext.getSharedPreferences("DoAn3Prefs", Context.MODE_PRIVATE)
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
            .authenticator(TokenAuthenticator(context.applicationContext))
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}