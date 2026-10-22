package com.example.csci_asp.places

import com.google.gson.annotations.SerializedName

// Main response object
data class PlacesResponse(
    val results: List<PlaceResult>,
    val status: String)

// Represents a single place found
data class PlaceResult(
    val name: String,
    val vicinity: String?,
    val types: List<String>,
    @SerializedName("plus_code")
    val plusCode: PlusCode?
)

data class PlusCode(
    @SerializedName("compound_code")
    val compoundCode: String?
)