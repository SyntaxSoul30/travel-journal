package com.example.csci_asp.places

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.google.android.gms.maps.model.LatLng
import java.io.IOException
import android.content.pm.PackageManager
import android.os.Bundle
import com.example.csci_asp.R

class PhotoLocation {

    companion object {
        private const val TAG = "PhotoLocation"
        fun readLocationMetadata(resolver: ContentResolver, imageUri: Uri): LatLng? {
            try {
                val originalUri = MediaStore.setRequireOriginal(imageUri)

                resolver.openInputStream(originalUri)?.use { stream ->
                    val exifInterface = ExifInterface(stream)

                    val latLong = exifInterface.latLong
                    if (latLong != null && latLong.size == 2) {
                        // Check for invalid (0.0, 0.0) coordinates
                        if (latLong[0] == 0.0 && latLong[1] == 0.0) {
                            Log.d(TAG, "Found invalid GPS coordinates (0.0, 0.0).")
                            return null
                        }
                        return LatLng(latLong[0], latLong[1])
                    }
                }
            } catch (e: SecurityException) {
                // This will be thrown if ACCESS_MEDIA_LOCATION is not granted.
                Log.e(TAG, "SecurityException: Missing ACCESS_MEDIA_LOCATION permission?", e)
            } catch (e: IOException) {
                Log.e(TAG, "Could not read image stream.", e)
            }
            return null
        }

        fun readTimeStampMetadata(resolver: ContentResolver, imageUri: Uri): String? {
             try {
                val originalUri = MediaStore.setRequireOriginal(imageUri)

                resolver.openInputStream(originalUri)?.use { stream -> // Use the new originalUri
                    val exifInterface = ExifInterface(stream)

                    val timestamp = exifInterface.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)

                    println("Timestamp: {$timestamp}")
                    return timestamp
                }
            } catch (e: IOException) {
                Log.e(TAG, "Could not read image stream.", e)
            }
            return null
        }
        suspend fun identifyPlace(context: Context, location: LatLng): String? {
            val applicationInfo = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            val apiKey = applicationInfo.metaData.getString("com.google.android.geo.API_KEY","")
            val locationStr = "${location.latitude},${location.longitude}"

            try {
                val response = RetrofitClient.placesService.findNearbyPlaces(
                    location = locationStr,
                    apiKey = apiKey
                )

                if (response.isSuccessful && response.body() != null) {
                    val placesResponse = response.body()!!
                    val results = placesResponse.results

                    val preferredTypes = listOf("point_of_interest","monument", "museum", "tourist_attraction")

                    for (type in preferredTypes) {
                        val preferredPlace = results.firstOrNull { it.types.contains(type) }
                        if (preferredPlace != null) {
                            Log.d(TAG, "Found preferred place of type '$type': ${preferredPlace.name}")
                            return preferredPlace.name // Return the name of the best match and exit.
                        }
                    }

                    val city = results.firstOrNull()?.plusCode?.compoundCode
                        ?.split(" ")?.getOrNull(1)?.replace(",", "")
                    if (city != null) {
                        Log.d(TAG, "No preferred places found, falling back to city: $city")
                        return city
                    }

                } else {
                    Log.e(TAG, "API call failed with error: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network request failed", e)
            }
            return null // Return null if nothing is found
        }
    }


}