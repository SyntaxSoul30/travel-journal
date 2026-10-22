package com.example.csci_asp.photos.api

/**
 * Request model for getting auto-generated captions and summaries
 */
data class AutoCaptionsRequest(
    val imageContentB64: String,
    val imageType: String,
    val fileName: String,
    val location: String?,
    val capturedTs: String?
)

