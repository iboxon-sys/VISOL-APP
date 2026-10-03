package com.example.network

import com.example.model.UserProfile
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * REST API DTO for candidate profiles returned by the backend
 */
data class CandidateDto(
    val id: String = "",
    val fullName: String = "",
    val gender: String = "Ayol",
    val age: Int = 20,
    val viloyat: String = "",
    val tuman: String = "",
    val mahalla: String = "",
    val photoPath: String = "",
    val bio: String = "",
    val telegram: String = "",
    val instagram: String = "",
    val tiktok: String = "",
    val phone: String = "",
    val isVerifiedCamera: Boolean = true,
    val birthDate: String = "",
    val isMatched: Boolean? = false
) {
    fun toUserProfile(): UserProfile {
        return UserProfile(
            id = id,
            fullName = fullName,
            gender = gender,
            age = age,
            viloyat = viloyat,
            tuman = tuman,
            mahalla = mahalla,
            photoPath = photoPath,
            bio = bio,
            telegram = telegram,
            instagram = instagram,
            tiktok = tiktok,
            phone = phone,
            isVerifiedCamera = isVerifiedCamera,
            isCurrentUser = false,
            birthDate = birthDate,
            isMatched = isMatched ?: false
        )
    }
}

interface CandidateApiService {
    @GET("api/candidates")
    suspend fun getCandidates(
        @Query("gender") gender: String? = null,
        @Query("minAge") minAge: Int? = null,
        @Query("maxAge") maxAge: Int? = null
    ): Response<List<CandidateDto>>

    @GET("api/v1/users/followers")
    suspend fun getFollowers(
        @Query("userId") userId: String
    ): Response<List<CandidateDto>>

    @GET("api/v1/users/following")
    suspend fun getFollowing(
        @Query("userId") userId: String
    ): Response<List<CandidateDto>>

    @POST("api/v1/users/follow")
    suspend fun toggleFollow(
        @Query("targetUserId") targetUserId: String
    ): Response<Map<String, Boolean>>

    @POST("api/v1/users/match-status")
    suspend fun updateMatchStatus(
        @Query("isMatched") isMatched: Boolean
    ): Response<Map<String, Any>>

    companion object {
        var BASE_URL = "https://api.visol.app/"

        fun create(customBaseUrl: String? = null): CandidateApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .writeTimeout(8, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .addInterceptor(logging)
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            val url = if (!customBaseUrl.isNullOrBlank()) {
                if (customBaseUrl.endsWith("/")) customBaseUrl else "$customBaseUrl/"
            } else BASE_URL

            return Retrofit.Builder()
                .baseUrl(url)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
                .build()
                .create(CandidateApiService::class.java)
        }
    }
}
