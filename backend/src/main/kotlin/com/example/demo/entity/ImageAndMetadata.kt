package com.example.demo.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "ImageAndMetadata")
data class ImageAndMetadata(
    @Id
    val id: String? = null,
    val imageContentB64: String = "",
    val metadata: Metadata = Metadata(),
    val captionsAndSummaries: CaptionsAndSummaries = CaptionsAndSummaries()
)

data class Metadata(
    val fileName: String = "",
    val location: String = "",
    val imgType: String = "",
    val capturedTs: String = ""
)

data class CaptionsAndSummaries(
    val caption: String = "",
    val summary: String = ""
)