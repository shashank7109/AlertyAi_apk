package com.alertyai.app.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ── Auth: email + password ─────────────────────────────────────────────────
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    // ── Auth: Google Sign-In (sends Google ID Token → gets AlertyAI JWT) ───────
    @POST("api/oauth/google/mobile-token")
    suspend fun googleMobileSignIn(
        @Body body: GoogleIdTokenRequest
    ): Response<LoginResponse>

    // ── Sync: fetch all tasks from backend (MongoDB) for Room DB merge ─────────
    @GET("api/tasks")
    suspend fun getBackendTasks(
        @Header("Authorization") bearer: String
    ): Response<List<BackendTask>>

    // ── AI Chat (backend uses POST /chat with message as query param) ──────────
    @POST("api/v2/chat")
    suspend fun chat(
        @Header("Authorization") bearer: String,
        @Query("message") message: String
    ): Response<ChatResponse>

    // ── AI Task from Text (backend uses POST with content as query param) ──────
    @POST("api/v2/tasks/from-text")
    suspend fun createTaskFromText(
        @Header("Authorization") bearer: String,
        @Query("content") content: String,
        @Query("language") language: String = "en"
    ): Response<AiTaskResponse>

    // ── AI Task from Screenshot/OCR (POST multipart) ──────────────────────────
    @Multipart
    @POST("api/v2/tasks/from-screenshot")
    suspend fun createTaskFromScreenshot(
        @Header("Authorization") bearer: String,
        @Part image: MultipartBody.Part,
        @Part("language") language: RequestBody,
        @Part("extract_only") extractOnly: RequestBody
    ): Response<AiTaskResponse>

    // ── AI Task from Voice (POST multipart) ───────────────────────────────────
    @Multipart
    @POST("api/v2/tasks/from-voice")
    suspend fun createTaskFromVoice(
        @Header("Authorization") bearer: String,
        @Part audio: MultipartBody.Part,
        @Part("language") language: RequestBody,
        @Part("extract_only") extractOnly: RequestBody
    ): Response<AiTaskResponse>
}
