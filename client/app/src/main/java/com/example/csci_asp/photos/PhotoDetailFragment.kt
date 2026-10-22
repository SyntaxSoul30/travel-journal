package com.example.csci_asp.photos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.csci_asp.R
import com.example.csci_asp.databinding.FragmentPhotoDetailBinding
import com.example.csci_asp.photos.api.AutoCaptionsRequest
import com.example.csci_asp.photos.api.ImageMetadataRequest
import com.example.csci_asp.photos.api.PhotoApiClient
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class PhotoDetailFragment : Fragment() {

    private var _binding: FragmentPhotoDetailBinding? = null
    private val binding get() = _binding!!

    private val dateFormat = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhotoDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val devicePhoto = arguments?.getSerializable(ARG_DEVICE_PHOTO) as? DevicePhoto
        if (devicePhoto == null) {
            Snackbar.make(
                binding.root,
                getString(R.string.photo_detail_load_error),
                Snackbar.LENGTH_LONG
            ).show()
            return
        }

        renderPhoto(devicePhoto)
        
        // Fetch caption and summary from API
        fetchCaptionAndSummary(devicePhoto)

//        binding.buttonSendCaption.setOnClickListener {
//            Snackbar.make(
//                binding.root,
//                getString(R.string.photo_detail_caption_placeholder_action),
//                Snackbar.LENGTH_SHORT
//            ).show()
//        }
//
//        binding.buttonSendSummary.setOnClickListener {
//            Snackbar.make(
//                binding.root,
//                getString(R.string.photo_detail_summary_placeholder_action),
//                Snackbar.LENGTH_SHORT
//            ).show()
//        }
    }

    private fun renderPhoto(photo: DevicePhoto) {
        Glide.with(binding.imageFullPhoto)
            .load(photo.uri)
            .fitCenter()
            .into(binding.imageFullPhoto)

        val unknownValue = getString(R.string.common_unknown)

        binding.textMetadataFileName.text = getString(
            R.string.photo_detail_metadata_filename,
            photo.displayName ?: unknownValue
        )

        binding.textMetadataSize.text = getString(
            R.string.photo_detail_metadata_size,
            readableFileSize(photo.sizeBytes) ?: unknownValue
        )

        val dimensions = if (photo.width != null && photo.height != null) {
            "${photo.width} x ${photo.height}"
        } else {
            unknownValue
        }
        binding.textMetadataDimensions.text = getString(
            R.string.photo_detail_metadata_dimensions,
            dimensions
        )

        binding.textMetadataMime.text = getString(
            R.string.photo_detail_metadata_mime,
            photo.mimeType ?: unknownValue
        )

        binding.textMetadataDate.text = getString(
            R.string.photo_detail_metadata_date,
            photo.dateTakenMillis?.let { dateFormat.format(it) } ?: unknownValue
        )
    }

    private fun readableFileSize(bytes: Long?): String? {
        if (bytes == null || bytes <= 0) return null
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format(
            Locale.getDefault(),
            "%.1f %s",
            bytes / Math.pow(1024.0, digitGroups.toDouble()),
            units[digitGroups]
        )
    }

    /**
     * Fetches caption and summary from the API and updates the UI
     * First tries getImageAndMetadata, then falls back to getAutoCaptions if needed
     */
    private fun fetchCaptionAndSummary(photo: DevicePhoto) {
        // Get the file name from the photo
        val fileName = photo.displayName ?: return
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Try first API: getImageAndMetadata
                val response = withContext(Dispatchers.IO) {
                    PhotoApiClient.apiService.getImageAndMetadata(
                        ImageMetadataRequest(fileName = fileName)
                    )
                }
                
                // Check if caption and summary are empty
                val captionsAndSummaries = response.imageDetails.captionsAndSummaries
                val hasCaption = !captionsAndSummaries.caption.isNullOrBlank()
                val hasSummary = !captionsAndSummaries.summary.isNullOrBlank()
                
                if (hasCaption || hasSummary) {
                    // Update UI with fetched data from first API
                    captionsAndSummaries.caption?.let { caption ->
                        if (caption.isNotBlank()) {
                            binding.editCaption.setText(caption)
                        }
                    }
                    
                    captionsAndSummaries.summary?.let { summary ->
                        if (summary.isNotBlank()) {
                            binding.editSummary.setText(summary)
                        }
                    }
                } else {
                    // First API returned empty response, try fallback API
                    fetchAutoCaptions(photo)
                }
            } catch (e: Exception) {
                // First API failed, try fallback API
                fetchAutoCaptions(photo)
            }
        }
    }

    /**
     * Fetches auto-generated captions and summaries using the fallback API
     */
    private fun fetchAutoCaptions(photo: DevicePhoto) {
        val fileName = photo.displayName ?: return
        
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    // Convert image to base64
                    val imageBase64 = PhotoUtils.convertImageToBase64(photo.uri, requireContext())
                        ?: throw Exception("Failed to convert image to base64")
                    
                    // Extract image type from file name
                    val imageType = PhotoUtils.extractImageTypeFromFileName(fileName)
                        ?: throw Exception("Failed to extract image type from file name")
                    
                    // Format timestamp
                    val capturedTs = PhotoUtils.formatTimestampToISO8601(photo.dateTakenMillis)
                    
                    // Get location (currently returns null, can be enhanced later)
                    val location = PhotoUtils.getLocationFromExif(photo.uri, requireContext())
                    
                    // Call fallback API
                    PhotoApiClient.apiService.getAutoCaptions(
                        AutoCaptionsRequest(
                            imageContentB64 = imageBase64,
                            imageType = imageType,
                            fileName = fileName,
                            location = location,
                            capturedTs = capturedTs
                        )
                    )
                }
                
                // Update UI with fetched data from fallback API
                result.caption?.let { caption ->
                    if (caption.isNotBlank()) {
                        binding.editCaption.setText(caption)
                    }
                }
                
                result.summary?.let { summary ->
                    if (summary.isNotBlank()) {
                        binding.editSummary.setText(summary)
                    }
                }
            } catch (e: Exception) {
                // Handle error - show a message to user
                Snackbar.make(
                    binding.root,
                    "Failed to load caption/summary: ${e.message}",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_DEVICE_PHOTO = "arg_device_photo"
    }
}

