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
    val username: String? = null,
    @SerializedName("mobile_number") val mobileNumber: String? = null,
    @SerializedName("profile_picture") val profilePicture: String? = null
)

data class UserUpdateRequest(
    val name: String? = null,
    val username: String? = null,
    @SerializedName("mobile_number") val mobileNumber: String? = null,
    @SerializedName("profile_picture") val profilePicture: String? = null
)

// ── Backend Task (from MongoDB via GET /api/tasks) ─────────────────────────────
data class BackendTask(
    @SerializedName("_id") val id: String = "",
    val title: String = "",
    val description: String? = null,
    val priority: String = "medium",
    val status: String = "pending",
    @SerializedName("due_date")  val dueDate: String? = null,
    @SerializedName("due_time")  val dueTime: String? = null,   // "HH:mm" e.g. "05:00"
    val category: String? = null,
    val tags: List<String>? = null,
    @SerializedName("ai_generated") val aiGenerated: Boolean = false,
    val subtasks: List<String>? = null,
    val source: String = "manual",
    @SerializedName("is_team_task") val isTeamTask: Boolean = false,
    @SerializedName("team_name")    val teamName: String? = null,
    // Recurring task fields
    @SerializedName("recurrence_type") val recurrenceType: String = "none",
    @SerializedName("completed_dates") val completedDates: List<String>? = null,
    @SerializedName("last_completed_at") val lastCompletedAt: String? = null
) {
    /** Returns true if this recurring task was already completed today */
    fun isCompletedToday(): Boolean {
        if (recurrenceType == "none" || recurrenceType.isNullOrEmpty()) return false
        val today = java.time.LocalDate.now().toString() // YYYY-MM-DD
        return completedDates?.contains(today) == true
    }

    val isRecurring: Boolean get() = recurrenceType != "none" && !recurrenceType.isNullOrEmpty()
}


// ── Backend Task Update ────────────────────────────────────────────────────────
data class BackendTaskUpdate(
    val title: String? = null,
    val description: String? = null,
    val priority: String? = null,
    val status: String? = null,
    @SerializedName("due_date") val dueDate: String? = null,
    @SerializedName("due_time") val dueTime: String? = null
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
) {
    /** Pull subtasks from the task map (backend returns List<String> under "subtasks") */
    @Suppress("UNCHECKED_CAST")
    fun getSubtasks(): List<String> {
        val src = task ?: taskData ?: return emptyList()
        return (src["subtasks"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
    }

    fun getDescription(): String {
        val src = task ?: taskData ?: return ""
        return (src["description"] as? String)
            ?: (src["note"] as? String)
            ?: ""
    }
}

// ── Organizations & Teams ───────────────────────────────────────────────────
data class OrgListResponse(
    val success: Boolean,
    val organizations: List<com.alertyai.app.data.model.Organization> = emptyList()
)

data class TeamListResponse(
    val success: Boolean,
    val teams: List<com.alertyai.app.data.model.Team> = emptyList()
)

data class ChatHistoryResponse(
    val success: Boolean,
    val messages: List<com.alertyai.app.data.model.TeamChatMessage> = emptyList()
)

data class OrgMembersResponse(
    val success: Boolean,
    val members: List<com.alertyai.app.data.model.OrgMember> = emptyList()
)

data class MentionSuggestionsResponse(
    val success: Boolean,
    val members: List<MentionMember> = emptyList()
)

data class MentionMember(
    @SerializedName("user_id") val userId: String,
    val username: String,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("profile_picture") val profilePicture: String? = null
)

// ── Task Assignment ────────────────────────────────────────────────────────────
data class AssignTaskRequest(
    val title: String,
    val description: String = "",
    val priority: String = "medium",
    @SerializedName("assigned_to") val assignedTo: String,
    val deadline: String? = null,
    @SerializedName("reminder_frequency") val reminderFrequency: String = "daily",
    @SerializedName("reminder_time") val reminderTime: String? = null
)

data class AssignTaskResponse(
    val success: Boolean,
    @SerializedName("task_id") val taskId: String? = null,
    val message: String? = null
)

data class TeamTask(
    val id: String = "",
    @SerializedName("team_id") val teamId: String = "",
    val title: String = "",
    val description: String? = "",
    val priority: String = "normal",
    val status: String = "pending",
    @SerializedName("assignee_email") val assigneeEmail: String = "",
    @SerializedName("assigned_by_email") val assignedByEmail: String = "",
    @SerializedName("due_date") val dueDate: String? = null,
    @SerializedName("task_type") val taskType: String = "",
    val subtasks: List<String>? = null,
    @SerializedName("created_at") val createdAt: String = ""
)

data class TeamTasksResponse(
    val success: Boolean,
    val tasks: List<TeamTask> = emptyList()
)
