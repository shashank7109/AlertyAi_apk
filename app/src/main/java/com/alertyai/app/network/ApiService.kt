package com.alertyai.app.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*
import com.alertyai.app.data.model.TeamDetailedResponse

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

    // ── Delete a task on the backend (204 on success) ─────────────────────────
    @POST("api/tasks/{task_id}/complete")
    suspend fun completeBackendTask(
        @Header("Authorization") bearer: String,
        @Path("task_id") taskId: String
    ): Response<com.alertyai.app.network.BackendTask>

    @DELETE("api/tasks/{task_id}")
    suspend fun deleteBackendTask(
        @Header("Authorization") bearer: String,
        @Path("task_id") taskId: String
    ): Response<Unit>

    @PUT("api/tasks/{task_id}")
    suspend fun updateBackendTask(
        @Header("Authorization") bearer: String,
        @Path("task_id") taskId: String,
        @Body request: BackendTaskUpdate
    ): Response<BackendTask>

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

    @GET("api/teams/{team_id}")
    suspend fun getTeam(
        @Header("Authorization") bearer: String,
        @Path("team_id") teamId: String
    ): Response<TeamDetailedResponse>

    @GET("api/teams")
    suspend fun getTeams(
        @Header("Authorization") bearer: String
    ): Response<List<TeamDetailedResponse>>

    @GET("api/teams/invitations/pending")
    suspend fun getPendingInvitations(
        @Header("Authorization") bearer: String
    ): Response<List<com.alertyai.app.data.model.PendingInvitation>>

    @POST("api/teams/invitations/{invitation_id}/accept")
    suspend fun acceptInvitation(
        @Header("Authorization") bearer: String,
        @Path("invitation_id") invitationId: String
    ): Response<Any>

    @POST("api/teams/invitations/{invitation_id}/decline")
    suspend fun declineInvitation(
        @Header("Authorization") bearer: String,
        @Path("invitation_id") invitationId: String
    ): Response<Any>

    @POST("api/teams/")
    suspend fun createTeam(
        @Header("Authorization") bearer: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<com.alertyai.app.data.model.TeamDetailedResponse>

    @DELETE("api/teams/{teamId}")
    suspend fun deleteTeam(
        @Header("Authorization") bearer: String,
        @Path("teamId") teamId: String
    ): Response<Unit>

    @FormUrlEncoded
    @POST("api/teams/join/code")
    suspend fun joinTeamByCode(
        @Header("Authorization") bearer: String,
        @Field("code") code: String
    ): Response<com.alertyai.app.data.model.TeamDetailedResponse>

    @DELETE("api/teams/{teamId}/members/{userId}")
    suspend fun removeTeamMember(
        @Header("Authorization") bearer: String,
        @Path("teamId") teamId: String,
        @Path("userId") userId: String
    ): Response<Unit>

    @PATCH("api/teams/{teamId}/members/{userId}/role")
    suspend fun updateMemberRole(
        @Header("Authorization") bearer: String,
        @Path("teamId") teamId: String,
        @Path("userId") userId: String,
        @Body body: Map<String, String>
    ): Response<Map<String, String>>

    @GET("api/chat/history/{team_id}")
    suspend fun getTeamChatHistory(
        @Header("Authorization") bearer: String,
        @Path("team_id") teamId: String,
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
    @GET("api/chat/mentions/{team_id}")
    suspend fun getMentionSuggestions(
        @Header("Authorization") bearer: String,
        @Path("team_id") teamId: String
    ): Response<MentionSuggestionsResponse>

    // ── Task Assignment ────────────────────────────────────────────────────────
    @POST("api/teams/{team_id}/tasks")
    suspend fun assignTask(
        @Header("Authorization") bearer: String,
        @Path("team_id") teamId: String,
        @Body body: AssignTaskRequest
    ): Response<AssignTaskResponse>

    @GET("api/teams/tasks/pending")
    suspend fun getMyAssignedTasks(
        @Header("Authorization") bearer: String
    ): Response<List<TeamTask>>

    @GET("api/teams/tasks/assigned")
    suspend fun getTasksAssignedByMe(
        @Header("Authorization") bearer: String
    ): Response<List<TeamTask>>
}
