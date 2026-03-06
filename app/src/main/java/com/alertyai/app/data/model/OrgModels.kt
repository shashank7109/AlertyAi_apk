package com.alertyai.app.data.model

import com.google.gson.annotations.SerializedName

data class Organization(
    val id: String = "",
    val name: String = "",
    val description: String? = "",
    @SerializedName("owner_id") val ownerId: String = "",
    @SerializedName("join_code") val joinCode: String = "",
    @SerializedName("my_role") val myRole: String = "member",
    @SerializedName("created_at") val createdAt: String = ""
) {
    val isAdmin: Boolean get() = myRole == "admin"
}

data class Team(
    @SerializedName("_id", alternate = ["id"]) val id: String = "",
    @SerializedName("team_id") val teamId: String = "",
    val name: String = "",
    val description: String? = "",
    @SerializedName("created_by") val createdBy: String = "",
    @SerializedName("created_at") val createdAt: String = ""
)

data class OrgMember(
    val id: String = "",
    @SerializedName("team_id") val teamId: String = "",
    @SerializedName("user_id") val userId: String = "",
    val email: String = "",
    val name: String = "",
    val role: String = "member",
    @SerializedName("joined_at") val joinedAt: String = ""
) {
    val isAdmin: Boolean get() = role == "admin"
    val displayName: String get() = name.ifBlank { email }
    val initials: String get() = displayName.take(1).uppercase()
}

data class TeamChatMessage(
    val id: String? = null,
    @SerializedName("team_id") val teamId: String = "",
    @SerializedName("sender_email") val senderEmail: String = "",
    @SerializedName("sender_name") val senderName: String = "",
    val text: String = "",
    val timestamp: String = ""
)

data class TeamTask(
    val id: String = "",
    val title: String = "",
    val description: String? = "",
    val status: String = "pending",
    val priority: String = "medium",
    @SerializedName("assigned_to") val assignedTo: String? = null,
    @SerializedName("assigned_to_name") val assignedToName: String? = null,
    @SerializedName("assigned_by") val assignedBy: String? = null,
    @SerializedName("assigned_by_name") val assignedByName: String? = null,
    val deadline: String? = null,
    @SerializedName("progress_percentage") val progressPercentage: Int = 0,
    @SerializedName("task_type") val taskType: String = "",
    val subtasks: List<String>? = null,
    @SerializedName("created_at") val createdAt: String = ""
)

data class TeamDetailedResponse(
    @SerializedName("_id", alternate = ["id"]) val id: String = "",
    @SerializedName("team_id") val teamId: String = "",
    val name: String = "",
    val description: String? = "",
    val purpose: String = "other",
    @SerializedName("leader_id") val leaderId: String = "",
    @SerializedName("co_leaders") val coLeaders: List<String> = emptyList(),
    val members: List<OrgMember> = emptyList(),
    val tasks: List<TeamTask> = emptyList(),
    @SerializedName("invite_link") val inviteLink: String? = null,
    @SerializedName("invite_token") val inviteToken: String? = null,
    @SerializedName("created_at") val createdAt: String = ""
) {
    fun toTeam(): Team = Team(
        id = id,
        teamId = teamId,
        name = name,
        description = description,
        createdBy = leaderId,
        createdAt = createdAt
    )
}

data class PendingInvitation(
    @SerializedName("_id") val id: String = "",
    @SerializedName("team_id") val teamId: String = "",
    @SerializedName("team_name") val teamName: String = "",
    @SerializedName("invited_by") val invitedBy: String = "",
    @SerializedName("invited_by_name") val invitedByName: String = "",
    val status: String = "pending"
)
