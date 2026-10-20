package com.argon.ph03n1x.data

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

data class ArgonChatRequest(
    val model: String = "argon-4.8",
    val prompt: String,
    val max_tokens: Int = 1024,
    val temperature: Double = 0.7
)

data class ArgonChatResponse(
    val id: String? = null,
    val choices: List<Choice>? = null,
    val error: ErrorBody? = null
)

data class Choice(
    val text: String? = null,
    val index: Int? = null
)

data class ErrorBody(
    val message: String? = null,
    val type: String? = null
)

interface ArgonApiService {
    @POST("v1/chat/completions")
    suspend fun chatCompletions(
        @Header("Authorization") authorization: String,
        @Body body: ArgonChatRequest
    ): ArgonChatResponse
}