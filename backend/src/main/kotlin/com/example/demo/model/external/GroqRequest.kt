package com.example.demo.model.external

import com.fasterxml.jackson.annotation.JsonProperty

data class GroqRequest(
    val model: String,
    val messages: List<GroqMessage>,
    @JsonProperty("max_tokens")
    val maxTokens: Int,
    @JsonProperty("response_format")
    val responseFormat: ResponseFormat
)

data class ResponseFormat(
    val type: String
)

data class GroqMessage(
    val role: String,
    val content: List<ContentPart>
)

data class ContentPart(
    val type: String,
    val text: String? = null,
    @JsonProperty("image_url")
    val imageUrl: ImageUrl? = null
)

data class ImageUrl(
    val url: String
)

data class GroqResponse(
    val choices: List<Choice>? = null
)

data class Choice(
    val message: GroqResponseMessage
)

data class GroqResponseMessage(
    val role: String,
    val content: String
)
