package com.example.demo.model.internal

import com.example.demo.entity.CaptionsAndSummaries
import com.example.demo.entity.Metadata

data class GetImageAndMetadataResponse(
    val imageDetails: ImageDetails
)

data class ImageDetails(
    val imageContentB64: String = "",
    val metadata: Metadata? = null,
    val captionsAndSummaries: CaptionsAndSummaries? = null
)