package com.alertyai.app.data.repository

import com.alertyai.app.data.model.*
import com.alertyai.app.network.RetrofitClient
import com.alertyai.app.network.TokenManager
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrgRepository @Inject constructor() {

    private val api = RetrofitClient.api

    suspend fun getMyOrganizations(context: Context): List<Organization> = withContext(Dispatchers.IO) {
        val token = TokenManager.getToken(context) ?: return@withContext emptyList()
        try {
            val response = api.getMyOrganizations("Bearer $token")
            if (response.isSuccessful) response.body()?.organizations ?: emptyList()
            else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun createOrganization(context: Context, name: String, description: String = ""): String? = withContext(Dispatchers.IO) {
        val token = TokenManager.getToken(context) ?: return@withContext null
        try {
            val body = mapOf("name" to name, "description" to description)
            val response = api.createOrganization("Bearer $token", body)
            if (response.isSuccessful) {
                response.body()?.get("join_code") as? String
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun joinByCode(context: Context, code: String): Result<String> = withContext(Dispatchers.IO) {
        val token = TokenManager.getToken(context) ?: return@withContext Result.failure(Exception("Not logged in"))
        try {
            val response = api.joinOrganizationByCode("Bearer $token", mapOf("code" to code))
            if (response.isSuccessful) {
                val msg = response.body()?.get("message") as? String ?: "Joined successfully!"
                Result.success(msg)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Invalid invite code"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getJoinCode(context: Context, orgId: String): String? = withContext(Dispatchers.IO) {
        val token = TokenManager.getToken(context) ?: return@withContext null
        try {
            val response = api.getJoinCode("Bearer $token", orgId)
            if (response.isSuccessful) response.body()?.get("join_code") as? String
            else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun regenerateJoinCode(context: Context, orgId: String): String? = withContext(Dispatchers.IO) {
        val token = TokenManager.getToken(context) ?: return@withContext null
        try {
            val response = api.regenerateJoinCode("Bearer $token", orgId)
            if (response.isSuccessful) response.body()?.get("join_code") as? String
            else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getOrgMembers(context: Context, orgId: String): List<OrgMember> = withContext(Dispatchers.IO) {
        val token = TokenManager.getToken(context) ?: return@withContext emptyList()
        try {
            val response = api.getOrgMembers("Bearer $token", orgId)
            if (response.isSuccessful) response.body()?.members ?: emptyList()
            else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun removeMember(context: Context, orgId: String, userId: String): Boolean = withContext(Dispatchers.IO) {
        val token = TokenManager.getToken(context) ?: return@withContext false
        try {
            val response = api.removeMember("Bearer $token", orgId, userId)
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getOrgTeams(context: Context, orgId: String): List<Team> = withContext(Dispatchers.IO) {
        val token = TokenManager.getToken(context) ?: return@withContext emptyList()
        try {
            val response = api.getOrgTeams("Bearer $token", orgId)
            if (response.isSuccessful) response.body()?.teams ?: emptyList()
            else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun createTeam(
        context: Context, 
        name: String, 
        description: String = "", 
        purpose: String = "other",
        memberEmails: List<String> = emptyList(),
        memberPhones: List<String> = emptyList()
    ): Boolean = withContext(Dispatchers.IO) {
        val token = TokenManager.getToken(context) ?: return@withContext false
        try {
            val body = mapOf(
                "name" to name, 
                "description" to description,
                "purpose" to purpose,
                "member_emails" to memberEmails,
                "member_phones" to memberPhones
            )
            val response = api.createTeam("Bearer $token", body)
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getTeams(context: Context): List<TeamDetailedResponse> = withContext(Dispatchers.IO) {
        val token = TokenManager.getToken(context) ?: return@withContext emptyList()
        try {
            val response = api.getTeams("Bearer $token")
            if (response.isSuccessful) response.body() ?: emptyList()
            else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getTeam(context: Context, teamId: String): TeamDetailedResponse? = withContext(Dispatchers.IO) {
        val token = TokenManager.getToken(context) ?: return@withContext null
        try {
            val response = api.getTeam("Bearer $token", teamId)
            if (response.isSuccessful) response.body()
            else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun joinTeamByCode(context: Context, code: String): Result<Team> = withContext(Dispatchers.IO) {
        val token = TokenManager.getToken(context) ?: return@withContext Result.failure(Exception("No token found"))
        try {
            val response = api.joinTeamByCode("Bearer $token", code)
            if (response.isSuccessful) {
                val teamResponse = response.body()
                if (teamResponse != null) {
                    Result.success(teamResponse.toTeam())
                } else {
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPendingInvitations(context: Context): List<PendingInvitation> = withContext(Dispatchers.IO) {
        val token = TokenManager.getToken(context) ?: return@withContext emptyList()
        try {
            val response = api.getPendingInvitations("Bearer $token")
            if (response.isSuccessful) response.body() ?: emptyList()
            else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun acceptInvitation(context: Context, invitationId: String): Boolean = withContext(Dispatchers.IO) {
        val token = TokenManager.getToken(context) ?: return@withContext false
        try {
            val response = api.acceptInvitation("Bearer $token", invitationId)
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun declineInvitation(context: Context, invitationId: String): Boolean = withContext(Dispatchers.IO) {
        val token = TokenManager.getToken(context) ?: return@withContext false
        try {
            val response = api.declineInvitation("Bearer $token", invitationId)
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getChatHistory(context: Context, teamId: String): List<TeamChatMessage> = withContext(Dispatchers.IO) {
        val token = TokenManager.getToken(context) ?: return@withContext emptyList()
        try {
            val response = api.getTeamChatHistory("Bearer $token", teamId, token)
            if (response.isSuccessful) response.body()?.messages ?: emptyList()
            else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getMentionSuggestions(context: Context, teamId: String): List<com.alertyai.app.network.MentionMember> = withContext(Dispatchers.IO) {
        val token = TokenManager.getToken(context) ?: return@withContext emptyList()
        try {
            val response = api.getMentionSuggestions("Bearer $token", teamId)
            if (response.isSuccessful) response.body()?.members ?: emptyList()
            else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun assignTask(context: Context, teamId: String, req: com.alertyai.app.network.AssignTaskRequest): Result<String> = withContext(Dispatchers.IO) {
        val token = TokenManager.getToken(context) ?: return@withContext Result.failure(Exception("Not logged in"))
        try {
            val response = api.assignTask("Bearer $token", teamId, req)
            if (response.isSuccessful) {
                Result.success(response.body()?.message ?: "Task assigned successfully")
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to assign task"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyAssignedTasks(context: Context, teamId: String): List<com.alertyai.app.network.TeamTask> = withContext(Dispatchers.IO) {
        val token = TokenManager.getToken(context) ?: return@withContext emptyList()
        try {
            val response = api.getMyAssignedTasks("Bearer $token")
            if (response.isSuccessful) {
                val tasks = response.body() ?: emptyList()
                tasks.filter { it.teamId == teamId }
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getTasksAssignedByMe(context: Context, teamId: String): List<com.alertyai.app.network.TeamTask> = withContext(Dispatchers.IO) {
        val token = TokenManager.getToken(context) ?: return@withContext emptyList()
        try {
            val response = api.getTasksAssignedByMe("Bearer $token")
            if (response.isSuccessful) {
                val tasks = response.body() ?: emptyList()
                tasks.filter { it.teamId == teamId }
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
