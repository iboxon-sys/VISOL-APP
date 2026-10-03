package com.example.network

import android.content.Context
import android.util.Log
import com.example.model.ChatMessage
import com.example.model.UserPost
import com.example.model.UserProfile
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

/**
 * PRODUCTION-GRADE VISOL REST API CLIENT
 * - Barcha server endpointlari (Auth, Candidates, Chat, Reels, VIP, Followers)
 * - Server offline bo'lsa yoki xatolik bersa ham ilovani buzilmasligini (Zero Crash Guarantee) ta'minlovchi SafeCall mexanizmi
 * - Dinamik Server URL sozlamasi (Masalan: Node.js / Express, Django, Laravel, Spring Boot serverlarga to'g'ridan-to'g'ri ulanadi)
 */

// ======================= DTO MODELS =======================

data class ApiResponseWrapper<T>(
    val success: Boolean = true,
    val message: String? = null,
    val data: T? = null
)

data class AuthRequestDto(
    val phone: String,
    val fullName: String,
    val gender: String,
    val age: Int,
    val viloyat: String,
    val tuman: String,
    val mahalla: String,
    val bio: String,
    val telegram: String? = null,
    val instagram: String? = null,
    val tiktok: String? = null,
    val photoPath: String? = null,
    val birthDate: String? = null
)

data class AuthResponseDto(
    val token: String? = null,
    val userId: String? = null,
    val user: CandidateDto? = null
)

data class SendMessageDto(
    val receiverId: String,
    val text: String,
    val sharedReelId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class MessageResponseDto(
    val id: String,
    val senderId: String,
    val receiverId: String,
    val text: String,
    val timestamp: String,
    val isFromMe: Boolean = false,
    val reaction: String? = null
)

data class ReactionDto(
    val candidateId: String,
    val messageId: String,
    val emoji: String?
)

data class WallpaperDto(
    val candidateId: String,
    val wallpaperUrl: String?
)

data class CreatePostDto(
    val mediaUri: String,
    val title: String,
    val authorName: String,
    val authorPhotoUrl: String? = null,
    val musicTitle: String? = null,
    val musicArtist: String? = null,
    val musicAudioUrl: String? = null,
    val isVideo: Boolean = true
)

// ======================= RETROFIT INTERFACE =======================

interface VisolApiService {

    // --- AUTHENTICATION & PROFILE ---
    @POST("api/v1/auth/register")
    suspend fun registerUser(@Body request: AuthRequestDto): Response<ApiResponseWrapper<AuthResponseDto>>

    @POST("api/v1/auth/login")
    suspend fun loginUser(@Query("phone") phone: String): Response<ApiResponseWrapper<AuthResponseDto>>

    @GET("api/v1/profile/me")
    suspend fun getMyProfile(): Response<ApiResponseWrapper<CandidateDto>>

    @PUT("api/v1/profile/update")
    suspend fun updateProfile(@Body profile: CandidateDto): Response<ApiResponseWrapper<CandidateDto>>

    // --- CANDIDATES (NOMZODLAR) ---
    @GET("api/v1/candidates")
    suspend fun getCandidates(
        @Query("gender") gender: String? = null,
        @Query("viloyat") viloyat: String? = null,
        @Query("minAge") minAge: Int? = null,
        @Query("maxAge") maxAge: Int? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): Response<ApiResponseWrapper<List<CandidateDto>>>

    @GET("api/v1/candidates/{id}")
    suspend fun getCandidateDetails(@Path("id") id: String): Response<ApiResponseWrapper<CandidateDto>>

    // --- CHAT & DIRECT MESSAGING ---
    @GET("api/v1/chat/messages")
    suspend fun getChatMessages(
        @Query("candidateId") candidateId: String
    ): Response<ApiResponseWrapper<List<MessageResponseDto>>>

    @POST("api/v1/chat/send")
    suspend fun sendMessage(@Body message: SendMessageDto): Response<ApiResponseWrapper<MessageResponseDto>>

    @POST("api/v1/chat/reaction")
    suspend fun toggleMessageReaction(@Body reaction: ReactionDto): Response<ApiResponseWrapper<Boolean>>

    @POST("api/v1/chat/wallpaper")
    suspend fun setChatWallpaper(@Body wallpaper: WallpaperDto): Response<ApiResponseWrapper<Boolean>>

    // --- REELS & COMMUNITY POSTS ---
    @GET("api/v1/posts")
    suspend fun getPosts(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 30
    ): Response<ApiResponseWrapper<List<UserPost>>>

    @POST("api/v1/posts/create")
    suspend fun createPost(@Body post: CreatePostDto): Response<ApiResponseWrapper<UserPost>>

    @POST("api/v1/posts/{id}/like")
    suspend fun togglePostLike(@Path("id") postId: String): Response<ApiResponseWrapper<Map<String, Any>>>

    @POST("api/v1/posts/{id}/comment")
    suspend fun addPostComment(
        @Path("id") postId: String,
        @Query("text") text: String
    ): Response<ApiResponseWrapper<Map<String, Any>>>

    // --- FOLLOWERS & SOCIAL GRAPH ---
    @GET("api/v1/users/followers")
    suspend fun getFollowers(@Query("userId") userId: String): Response<ApiResponseWrapper<List<CandidateDto>>>

    @GET("api/v1/users/following")
    suspend fun getFollowing(@Query("userId") userId: String): Response<ApiResponseWrapper<List<CandidateDto>>>

    @POST("api/v1/users/follow")
    suspend fun toggleFollow(@Query("targetUserId") targetUserId: String): Response<ApiResponseWrapper<Map<String, Boolean>>>

    // --- VIP & MATCH STATUS ---
    @POST("api/v1/users/match-status")
    suspend fun updateMatchStatus(@Query("isMatched") isMatched: Boolean): Response<ApiResponseWrapper<Map<String, Any>>>

    @POST("api/v1/vip/verify")
    suspend fun verifyVipSubscription(@Query("transactionId") txId: String): Response<ApiResponseWrapper<Boolean>>
}

// ======================= API CLIENT FACTORY & SAFE RUNNER =======================

object BackendManager {
    private const val TAG = "BackendManager"
    private const val PREFS_NAME = "visol_backend_prefs"
    private const val KEY_BASE_URL = "custom_base_url"
    private const val KEY_AUTH_TOKEN = "jwt_auth_token"

    // Standart rasmiy server manzili
    var currentBaseUrl: String = "https://api.visol.app/"
        private set

    var authToken: String? = null
        private set

    private var activeApiService: VisolApiService? = null

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        currentBaseUrl = prefs.getString(KEY_BASE_URL, "https://api.visol.app/") ?: "https://api.visol.app/"
        authToken = prefs.getString(KEY_AUTH_TOKEN, null)
        rebuildService()
    }

    fun setCustomServerUrl(context: Context, url: String) {
        var cleanUrl = url.trim()
        if (cleanUrl.isNotBlank()) {
            if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                cleanUrl = "http://$cleanUrl"
            }
            if (!cleanUrl.endsWith("/")) {
                cleanUrl = "$cleanUrl/"
            }
            currentBaseUrl = cleanUrl
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_BASE_URL, currentBaseUrl)
                .apply()
            rebuildService()
        }
    }

    fun saveAuthToken(context: Context, token: String?) {
        authToken = token
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_AUTH_TOKEN, token)
            .apply()
        rebuildService()
    }

    private fun rebuildService() {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "VisolAndroidApp/2.0")

                authToken?.let { token ->
                    if (token.isNotBlank()) {
                        requestBuilder.header("Authorization", "Bearer $token")
                    }
                }
                chain.proceed(requestBuilder.build())
            }
            .addInterceptor(logging)
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        activeApiService = Retrofit.Builder()
            .baseUrl(currentBaseUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
            .build()
            .create(VisolApiService::class.java)
    }

    fun getService(): VisolApiService {
        if (activeApiService == null) {
            rebuildService()
        }
        return activeApiService!!
    }

    /**
     * Safe API Execution Wrapper:
     * Tarmoq uzilishi, server 404/500 xatosi yoki ulanish kechikishi holatlarida
     * xatolikni ushlaydi va ilovani crash bo'lishidan 100% asraydi.
     */
    suspend fun <T> safeApiCall(
        apiCall: suspend (VisolApiService) -> Response<ApiResponseWrapper<T>>
    ): ApiResult<T> = withContext(Dispatchers.IO) {
        try {
            val service = getService()
            val response = apiCall(service)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success && body.data != null) {
                    ApiResult.Success(body.data)
                } else {
                    ApiResult.Error(body?.message ?: "Serverdan bo'sh javob qaytdi")
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val message = try {
                    JSONObject(errorBody ?: "").optString("message", "Xatolik kodi: ${response.code()}")
                } catch (_: Exception) {
                    "Server xatosi (${response.code()}): ${response.message()}"
                }
                ApiResult.Error(message, response.code())
            }
        } catch (e: java.net.ConnectException) {
            Log.w(TAG, "Serverga ulanib bo'lmadi: ${e.message}")
            ApiResult.NetworkUnavailable("Serverga ulanib bo'lmadi. Lokal ma'lumotlar bilan ishlanmoqda.")
        } catch (e: java.net.SocketTimeoutException) {
            Log.w(TAG, "Server javob berish vaqti tugadi: ${e.message}")
            ApiResult.NetworkUnavailable("Internet tezligi past yoki server sekin javob beryapti.")
        } catch (e: Exception) {
            Log.e(TAG, "Kutilmagan xatolik: ${e.message}", e)
            ApiResult.Error(e.message ?: "Noma'lum xatolik yuz berdi")
        }
    }
}

sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()
    data class NetworkUnavailable(val message: String) : ApiResult<Nothing>()
}
