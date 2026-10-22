package com.example.csci_asp.photos.api

import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit service interface for photo API
 */
interface PhotoApiService {
    @POST("api/v1/getImageAndMetadata")
    suspend fun getImageAndMetadata(
        @Body request: ImageMetadataRequest
    ): ImageMetadataResponse

    @POST("api/v1/getAutoCaptions")
    suspend fun getAutoCaptions(
        @Body request: AutoCaptionsRequest
    ): AutoCaptionsResponse
}

