package com.argon.ph03n1x.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class ArgonRepository(private val quotaManager: QuotaManager) {

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.argon-ai.io/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(ArgonApiService::class.java)

    sealed class Result {
        data class Success(val reply: String) : Result()
        data class Error(val message: String) : Result()
        object ApiKeyRequired : Result()
    }

    suspend fun sendMessage(userPrompt: String): Result = withContext(Dispatchers.IO) {
        val bearer = quotaManager.resolveBearerToken()
        if (bearer == null) {
            return@withContext Result.ApiKeyRequired
        }

        try {
            val request = ArgonChatRequest(prompt = userPrompt)
            val response = api.chatCompletions(authorization = bearer, body = request)

            if (response.error != null) {
                return@withContext Result.Error(response.error.message ?: "Unknown API error")
            }

            val text = response.choices?.firstOrNull()?.text?.trim()
                ?: return@withContext Result.Error("Empty response from Argon 4.8")

            if (!quotaManager.hasUserApiKey()) {
                quotaManager.incrementMessageCount()
            }

            Result.Success(text)
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network failure")
        }
    }

    fun getQuotaBadge(): String = quotaManager.getQuotaBadgeText()
    fun isApiKeyRequired(): Boolean = quotaManager.resolveBearerToken() == null
    fun saveApiKey(key: String) = quotaManager.saveUserApiKey(key)
    fun hasUserKey(): Boolean = quotaManager.hasUserApiKey()
}