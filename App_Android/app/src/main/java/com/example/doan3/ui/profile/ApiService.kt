package com.example.doan3.ui.profile

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

// === MODELS ĐĂNG KÝ / ĐĂNG NHẬP ===
data class RegisterRequest(val name: String, val email: String, val password: String, val gender: String = "Nam")
data class RegisterResponse(val message: String, val error: String?)
data class LoginRequest(val email: String, val password: String)
data class UserData(val id: String, val name: String, val email: String)
data class LoginResponse(val message: String, val token: String?, val user: UserData?, val error: String?)

// === MODELS THÔNG TIN CÁ NHÂN (MỚI) ===
data class ProfileData(val _id: String, val name: String, val email: String, val gender: String, val birthDate: String)
data class GetProfileResponse(val user: ProfileData?, val error: String?)
data class UpdateProfileRequest(val name: String, val gender: String, val birthDate: String)
data class UpdateProfileResponse(val message: String, val user: ProfileData?, val error: String?)

// === KHAI BÁO ĐƯỜNG DẪN API ===
interface ApiService {
    @POST("api/auth/register")
    fun registerUser(@Body request: RegisterRequest): Call<RegisterResponse>

    @POST("api/auth/login")
    fun loginUser(@Body request: LoginRequest): Call<LoginResponse>

    // Thêm 2 API lấy và sửa Profile
    @GET("api/auth/profile/{id}")
    fun getProfile(@Path("id") userId: String): Call<GetProfileResponse>

    @PUT("api/auth/profile/{id}")
    fun updateProfile(@Path("id") userId: String, @Body request: UpdateProfileRequest): Call<UpdateProfileResponse>
}

// === CẤU HÌNH RETROFIT ===
object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:3000/"

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(ApiService::class.java)
    }
}