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

    suspend fun createTeam(context: Context, orgId: String, name: String, description: String = ""): Boolean = withContext(Dispatchers.IO) {
        val token = TokenManager.getToken(context) ?: return@withContext false
        try {
            val body = mapOf("name" to name, "description" to description)
            val response = api.createTeam("Bearer $token", orgId, body)
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getChatHistory(context: Context, orgId: String): List<TeamChatMessage> = withContext(Dispatchers.IO) {
        val token = TokenManager.getToken(context) ?: return@withContext emptyList()
        try {
            val response = api.getTeamChatHistory("Bearer $token", orgId, token)
            if (response.isSuccessful) response.body()?.messages ?: emptyList()
            else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getMentionSuggestions(context: Context, orgId: String): List<com.alertyai.app.network.MentionMember> = withContext(Dispatchers.IO) {
        val token = TokenManager.getToken(context) ?: return@withContext emptyList()
        try {
            val response = api.getMentionSuggestions("Bearer $token", orgId)
            if (response.isSuccessful) response.body()?.members ?: emptyList()
            else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
