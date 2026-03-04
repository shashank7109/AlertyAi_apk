package com.alertyai.app.network

import com.google.gson.annotations.SerializedName

// ── Auth ──────────────────────────────────────────────────────────────────────
data class LoginRequest(val email: String, val password: String)

// Sent to POST /api/oauth/google/mobile-token
data class GoogleIdTokenRequest(
    @SerializedName("id_token") val idToken: String
)

data class LoginResponse(
    val success: Boolean,
    val token: String? = null,
    @SerializedName("access_token") val accessToken: String? = null,
    val message: String? = null,
    val user: UserInfo? = null
) {
    val resolvedToken: String? get() = token ?: accessToken
}

data class UserInfo(
    val id: String? = null,
    @SerializedName("_id") val _id: String? = null,
    val email: String? = null,
    val name: String? = null,
    @SerializedName("full_name") val fullName: String? = null,
    @SerializedName("profile_picture") val profilePicture: String? = null
)

// ── Backend Task (from MongoDB via GET /api/tasks) ─────────────────────────────
data class BackendTask(
    @SerializedName("_id") val id: String = "",
    val title: String = "",
    val description: String? = null,
    val priority: String = "medium",
    val status: String = "pending",
    @SerializedName("due_date") val dueDate: String? = null,
    @SerializedName("ai_generated") val aiGenerated: Boolean = false,
    val source: String = "manual"
)

// ── Chat ──────────────────────────────────────────────────────────────────────
data class ChatResponse(
    val success: Boolean,
    val message: String = "",
    val response: String? = null,
    @SerializedName("task_created") val taskCreated: Boolean = false,
    val task: Map<String, Any>? = null
) {
    /** Returns the AI reply text regardless of which field the backend used */
    val reply: String get() = message.ifBlank { response ?: "" }
}

// ── Tasks from AI ─────────────────────────────────────────────────────────────
data class AiTaskResponse(
    val success: Boolean,
    val task: Map<String, Any>? = null,
    val message: String? = null,
    val transcript: String? = null,
    @SerializedName("task_data") val taskData: Map<String, Any>? = null
)
