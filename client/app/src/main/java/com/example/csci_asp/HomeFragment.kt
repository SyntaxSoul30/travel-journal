package com.example.csci_asp

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.csci_asp.databinding.FragmentHomeBinding
import com.example.csci_asp.photos.DevicePhoto
import com.example.csci_asp.photos.DevicePhotoAdapter
import com.example.csci_asp.photos.PhotoDetailFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val photoAdapter by lazy { DevicePhotoAdapter { openPhotoDetail(it) } }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val granted = result.values.any { it }
            if (granted) {
                loadPhotos()
            } else {
                showPermissionDeniedState()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycler()
        determinePhotoAccess()
    }

    private fun setupRecycler() {
        binding.recyclerDevicePhotos.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = photoAdapter
            setHasFixedSize(true)
            isVisible = false
            isFocusable = false
            isFocusableInTouchMode = false
        }
    }

    private fun determinePhotoAccess() {
        if (hasPhotoPermission()) {
            loadPhotos()
        } else {
            permissionLauncher.launch(requiredPermissions())
        }
    }

    private fun hasPhotoPermission(): Boolean =
        requiredPermissions().all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }

    private fun requiredPermissions(): Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.ACCESS_MEDIA_LOCATION)
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.ACCESS_MEDIA_LOCATION)
    }

    private fun loadPhotos() {
        binding.textEmptyPhotos.apply {
            text = getString(R.string.device_photos_empty)
            isVisible = false
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val photos = withContext(Dispatchers.IO) {
                runCatching { queryDevicePhotos() }.getOrDefault(emptyList())
            }
            photoAdapter.submitList(photos)
            binding.recyclerDevicePhotos.isVisible = photos.isNotEmpty()
            binding.textEmptyPhotos.isVisible = photos.isEmpty()
        }
    }

    private fun showPermissionDeniedState() {
        binding.recyclerDevicePhotos.isVisible = false
        binding.textEmptyPhotos.apply {
            text = getString(R.string.device_photos_permission_denied)
            isVisible = true
        }
    }

    private fun queryDevicePhotos(): List<DevicePhoto> {
        val resolver = requireContext().contentResolver
        val photos = mutableListOf<DevicePhoto>()
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.MIME_TYPE
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
        val maxPhotos = 10

        resolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)

            while (cursor.moveToNext() && photos.size < maxPhotos) {
                val id = cursor.getLong(idColumn)
                val displayName = cursor.getString(nameColumn)
                val dateTaken = cursor.getLong(dateTakenColumn).takeIf { it > 0 }
                val dateAdded = cursor.getLong(dateAddedColumn).takeIf { it > 0 }
                val timestamp = dateTaken ?: dateAdded?.let { it * 1000 }
                val size = cursor.getLong(sizeColumn).takeIf { it > 0 }
                val width = cursor.getInt(widthColumn).takeIf { it > 0 }
                val height = cursor.getInt(heightColumn).takeIf { it > 0 }
                val mimeType = cursor.getString(mimeColumn)

                val uri = ContentUris.withAppendedId(collection, id)
                photos.add(
                    DevicePhoto(
                        uri = uri,
                        displayName = displayName,
                        dateTakenMillis = timestamp,
                        sizeBytes = size,
                        width = width,
                        height = height,
                        mimeType = mimeType
                    )
                )
            }
        }

        return photos
    }

    private fun openPhotoDetail(photo: DevicePhoto) {
        val args = bundleOf(PhotoDetailFragment.ARG_DEVICE_PHOTO to photo)
        findNavController().navigate(R.id.action_home_to_photoDetail, args)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerDevicePhotos.adapter = null
        _binding = null
    }
}
