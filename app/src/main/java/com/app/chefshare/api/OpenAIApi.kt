package com.app.chefshare.api

import com.app.chefshare.models.ChatResponse
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface OpenAIApi {
    @POST("v1/chat/completions")
    @Headers(
        "Content-Type: application/json",
        "Accept: application/json"
    )
    fun generateRecipe(
            @Header("Authorization") apiKey: String,
        @Body body: RequestBody
    ): Call<ChatResponse>
}
