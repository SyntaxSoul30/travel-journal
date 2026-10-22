package com.example.csci_asp.photos

import android.net.Uri
import java.io.Serializable

data class DevicePhoto(
    val uri: Uri,
    val displayName: String?,
    val dateTakenMillis: Long?,
    val sizeBytes: Long?,
    val width: Int?,
    val height: Int?,
    val mimeType: String?
) : Serializable

