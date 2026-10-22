package com.example.csci_asp.places

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface PlacesApiService {
    @GET("maps/api/place/nearbysearch/json")
    suspend fun findNearbyPlaces(
        @Query("location") location: String, // e.g., "40.7128,-74.0060"
        @Query("radius") radius: Int = 500, // Search within 500 meters
        @Query("key") apiKey: String
    ): Response<PlacesResponse>
}