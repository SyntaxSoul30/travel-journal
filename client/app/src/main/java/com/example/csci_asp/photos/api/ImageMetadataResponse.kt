package com.example.csci_asp.photos.api

/**
 * Response model for image and metadata API
 */
data class ImageMetadataResponse(
    val imageDetails: ImageDetails
)

data class ImageDetails(
    val imageContentB64: String,
    val metadata: ImageMetadata,
    val captionsAndSummaries: CaptionsAndSummaries
)

data class ImageMetadata(
    val fileName: String,
    val location: String?,
    val imgType: String?,
    val capturedTs: String?
)

data class CaptionsAndSummaries(
    val caption: String?,
    val summary: String?
)

