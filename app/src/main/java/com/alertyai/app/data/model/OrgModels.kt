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
    val id: String = "",
    @SerializedName("org_id") val orgId: String = "",
    val name: String = "",
    val description: String? = "",
    @SerializedName("created_by") val createdBy: String = "",
    @SerializedName("created_at") val createdAt: String = ""
)

data class OrgMember(
    val id: String = "",
    @SerializedName("org_id") val orgId: String = "",
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
    @SerializedName("org_id") val orgId: String = "",
    @SerializedName("sender_email") val senderEmail: String = "",
    @SerializedName("sender_name") val senderName: String = "",
    val text: String = "",
    val timestamp: String = ""
)
