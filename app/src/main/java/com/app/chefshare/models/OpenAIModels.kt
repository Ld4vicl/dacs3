package com.app.chefshare.models

data class ChatMessage(
    val role: String,
    val content: String
)

data class ChatChoice(
    val message: ChatMessage
)

data class ChatResponse(
    val choices: List<ChatChoice>
)

data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>
)
