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

    // ── Organizations & Teams ──────────────────────────────────────────────────
    @GET("api/orgs/my")
    suspend fun getMyOrganizations(
        @Header("Authorization") bearer: String
    ): Response<OrgListResponse>

    @POST("api/orgs/")
    suspend fun createOrganization(
        @Header("Authorization") bearer: String,
        @Body body: Map<String, String>
    ): Response<Map<String, Any>>

    @POST("api/orgs/join")
    suspend fun joinOrganizationByCode(
        @Header("Authorization") bearer: String,
        @Body body: Map<String, String>  // { "code": "XXXX" }
    ): Response<Map<String, Any>>

    @GET("api/orgs/{org_id}/join-code")
    suspend fun getJoinCode(
        @Header("Authorization") bearer: String,
        @Path("org_id") orgId: String
    ): Response<Map<String, Any>>

    @POST("api/orgs/{org_id}/regenerate-code")
    suspend fun regenerateJoinCode(
        @Header("Authorization") bearer: String,
        @Path("org_id") orgId: String
    ): Response<Map<String, Any>>

    @GET("api/orgs/{org_id}/members")
    suspend fun getOrgMembers(
        @Header("Authorization") bearer: String,
        @Path("org_id") orgId: String
    ): Response<OrgMembersResponse>

    @DELETE("api/orgs/{org_id}/members/{user_id}")
    suspend fun removeMember(
        @Header("Authorization") bearer: String,
        @Path("org_id") orgId: String,
        @Path("user_id") userId: String
    ): Response<Map<String, Any>>

    @GET("api/orgs/{org_id}/teams")
    suspend fun getOrgTeams(
        @Header("Authorization") bearer: String,
        @Path("org_id") orgId: String
    ): Response<TeamListResponse>

    @POST("api/orgs/{org_id}/teams")
    suspend fun createTeam(
        @Header("Authorization") bearer: String,
        @Path("org_id") orgId: String,
        @Body body: Map<String, String>
    ): Response<Map<String, Any>>

    @GET("api/chat/history/{org_id}")
    suspend fun getTeamChatHistory(
        @Header("Authorization") bearer: String,
        @Path("org_id") orgId: String,
        @Query("token") token: String
    ): Response<ChatHistoryResponse>

    // ── Profile ────────────────────────────────────────────────────────────────
    @GET("api/auth/profile")
    suspend fun getProfile(
        @Header("Authorization") bearer: String
    ): Response<UserInfo>

    @PUT("api/auth/profile")
    suspend fun updateProfile(
        @Header("Authorization") bearer: String,
        @Body body: UserUpdateRequest
    ): Response<UserInfo>

    // ── Mentions ──────────────────────────────────────────────────────────────
    @GET("api/chat/mentions/{org_id}")
    suspend fun getMentionSuggestions(
        @Header("Authorization") bearer: String,
        @Path("org_id") orgId: String
    ): Response<MentionSuggestionsResponse>

    // ── Task Assignment ────────────────────────────────────────────────────────
    @POST("api/orgs/{org_id}/assign-task")
    suspend fun assignTask(
        @Header("Authorization") bearer: String,
        @Path("org_id") orgId: String,
        @Body body: AssignTaskRequest
    ): Response<AssignTaskResponse>

    @GET("api/orgs/{org_id}/my-tasks")
    suspend fun getMyAssignedTasks(
        @Header("Authorization") bearer: String,
        @Path("org_id") orgId: String
    ): Response<TeamTasksResponse>

    @GET("api/orgs/{org_id}/assigned-by-me")
    suspend fun getTasksAssignedByMe(
        @Header("Authorization") bearer: String,
        @Path("org_id") orgId: String
    ): Response<TeamTasksResponse>
}
