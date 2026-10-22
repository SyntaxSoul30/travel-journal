package com.example.demo.model.internal

data class CaptionRequest(
    val imageContentB64: String = "",
    val imageType: String = "",
    val fileName: String = "",
    val location: String = "",
    val capturedTs: String = ""
)

data class CaptionResponse(
    val caption: String = "",
    val summary: String = ""
)