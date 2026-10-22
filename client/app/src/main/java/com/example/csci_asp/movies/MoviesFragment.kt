package com.example.csci_asp.movies

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.csci_asp.R
import com.example.csci_asp.databinding.FragmentMoviesBinding
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.io.File

class MoviesFragment : Fragment() {

    private var _binding: FragmentMoviesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MoviesViewModel by viewModels()
    private lateinit var videoExporter: VideoExporter
    private var previewVideoFile: File? = null

    private val photoPickerLauncher =
        registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_SELECTION)) { uris ->
            if (!uris.isNullOrEmpty()) {
                applySelection(uris)
            }
        }

    private val documentPickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (!uris.isNullOrEmpty()) {
                retainPersistablePermissions(uris)
                applySelection(uris)
            }
        }

    private val audioPickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                retainPersistablePermission(uri)
                viewModel.setCustomMusic(uri)
            }
        }

    private val createDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("video/mp4")) { uri ->
            val previewFile = previewVideoFile
            if (uri == null || previewFile == null || !previewFile.exists()) {
                return@registerForActivityResult
            }
            exportPreviewToUri(uri, previewFile)
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMoviesBinding.inflate(inflater, container, false)
        videoExporter = VideoExporter(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        observeViewModel()
        renderMusicChips()
        setupVideoView()
    }

    private fun setupVideoView() {
        // Video will be controlled by play/pause button, no auto-play
        binding.videoPreview.setOnCompletionListener {
            updatePlayPauseButton()
        }
    }

    private fun setupListeners() {
        binding.buttonPickPhotos.setOnClickListener { launchPhotoPicker() }
        binding.buttonAddCustomMusic.setOnClickListener { launchAudioPicker() }
        binding.buttonCreateMovie.setOnClickListener { createMovie() }
        binding.buttonExportMovie.setOnClickListener { exportMovie() }
        binding.buttonPlayPause.setOnClickListener { togglePlayPause() }
    }

    private fun observeViewModel() {
        viewModel.selectedPhotos.observe(viewLifecycleOwner) { photos ->
            updateCreateButtonState()
        }

        viewModel.selectedMusic.observe(viewLifecycleOwner) { music ->
            updateMusicSelection(music)
            updateCreateButtonState()
        }

        viewModel.customMusicUri.observe(viewLifecycleOwner) { uri ->
            if (uri != null) {
                binding.textSelectedMusic.text = getString(R.string.movies_custom_music_selected)
                binding.textSelectedMusic.isVisible = true
                binding.chipGroupMusic.clearCheck()
            }
            updateCreateButtonState()
        }

        viewModel.previewVideoUri.observe(viewLifecycleOwner) { uri ->
            if (uri != null) {
                showPreview(uri)
            } else {
                hidePreview()
            }
        }
    }

    private fun renderMusicChips() {
        val group = binding.chipGroupMusic
        if (group.childCount == 0) {
            MusicCatalog.clips.forEach { clip ->
                val chip = Chip(requireContext()).apply {
                    id = View.generateViewId()
                    text = getString(clip.nameRes)
                    isCheckable = true
                    tag = clip.id
                }
                group.addView(chip)
            }
        }

        group.setOnCheckedStateChangeListener { chipGroup, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            val chip = chipGroup.findViewById<Chip>(checkedId)
            val clipId = chip?.tag as? String ?: return@setOnCheckedStateChangeListener
            val clip = MusicCatalog.findById(clipId)
            viewModel.selectMusic(clip)
        }
    }

    private fun updateMusicSelection(music: MusicClip?) {
        if (music == null) {
            binding.textSelectedMusic.isGone = true
            return
        }

        binding.textSelectedMusic.text = getString(music.nameRes)
        binding.textSelectedMusic.isVisible = true
    }

    private fun launchPhotoPicker() {
        val available = ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(requireContext())
        if (available) {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } else {
            documentPickerLauncher.launch(arrayOf("image/*"))
        }
    }

    private fun launchAudioPicker() {
        audioPickerLauncher.launch(arrayOf("video/mp4", "audio/*"))
    }

    private fun retainPersistablePermissions(uris: List<Uri>) {
        val resolver = requireContext().contentResolver
        uris.forEach { uri ->
            try {
                resolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // best effort
            }
        }
    }

    private fun retainPersistablePermission(uri: Uri) {
        val resolver = requireContext().contentResolver
        try {
            resolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // best effort
        }
    }

    private fun createMovie() {
        val photos = viewModel.selectedPhotos.value.orEmpty()
        val audioUri = viewModel.getSelectedAudioUri(requireContext())

        if (photos.isEmpty()) {
            Snackbar.make(binding.root, R.string.movies_no_photos, Snackbar.LENGTH_SHORT).show()
            return
        }

        if (audioUri == null) {
            Snackbar.make(binding.root, R.string.movies_no_music, Snackbar.LENGTH_SHORT).show()
            return
        }

        setCreateInProgress(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val previewFile = File(requireContext().cacheDir, "preview_movie_${System.currentTimeMillis()}.mp4")
                videoExporter.createVideo(photos, audioUri, previewFile)
                previewVideoFile = previewFile
                val previewUri = Uri.fromFile(previewFile)
                viewModel.setPreviewVideo(previewUri)
                Snackbar.make(binding.root, R.string.movies_preview_ready, Snackbar.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Snackbar.make(binding.root, R.string.movies_export_error, Snackbar.LENGTH_LONG).show()
            } finally {
                setCreateInProgress(false)
            }
        }
    }

    private fun showPreview(uri: Uri) {
        binding.previewSection.isVisible = true
        binding.videoPreview.setVideoURI(uri)
        binding.buttonExportMovie.isEnabled = true
        updatePlayPauseButton()
    }

    private fun hidePreview() {
        binding.previewSection.isGone = true
        binding.videoPreview.stopPlayback()
        binding.buttonExportMovie.isEnabled = false
    }

    private fun togglePlayPause() {
        if (binding.videoPreview.isPlaying) {
            binding.videoPreview.pause()
        } else {
            binding.videoPreview.start()
        }
        updatePlayPauseButton()
    }

    private fun updatePlayPauseButton() {
        val isPlaying = binding.videoPreview.isPlaying
        if (isPlaying) {
            binding.buttonPlayPause.setImageResource(android.R.drawable.ic_media_pause)
            binding.buttonPlayPause.contentDescription = getString(R.string.movies_pause)
        } else {
            binding.buttonPlayPause.setImageResource(android.R.drawable.ic_media_play)
            binding.buttonPlayPause.contentDescription = getString(R.string.movies_play)
        }
    }

    private fun exportMovie() {
        val previewFile = previewVideoFile
        if (previewFile == null || !previewFile.exists()) {
            Snackbar.make(binding.root, R.string.movies_no_preview, Snackbar.LENGTH_SHORT).show()
            return
        }

        val suggestedName = "movie_${System.currentTimeMillis()}.mp4"
        createDocumentLauncher.launch(suggestedName)
    }

    private fun exportPreviewToUri(uri: Uri, sourceFile: File) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                requireContext().contentResolver.openOutputStream(uri, "wt")?.use { output ->
                    sourceFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                } ?: throw IllegalStateException("Unable to open output stream")
                onExportSuccess(uri)
            } catch (e: Exception) {
                Snackbar.make(binding.root, R.string.movies_export_error, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun onExportSuccess(uri: Uri) {
        val readableName = uri.lastPathSegment ?: uri.toString()
        Snackbar.make(
            binding.root,
            getString(R.string.movies_export_success, readableName),
            Snackbar.LENGTH_LONG
        ).show()
        shareMovie(uri)
    }

    private fun shareMovie(uri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val targets = requireContext().packageManager
            .queryIntentActivities(shareIntent, PackageManager.MATCH_DEFAULT_ONLY)
        targets.forEach { resolveInfo ->
            requireContext().grantUriPermission(
                resolveInfo.activityInfo.packageName,
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

        try {
            startActivity(
                Intent.createChooser(
                    shareIntent,
                    getString(R.string.movies_share_movie)
                )
            )
        } catch (_: ActivityNotFoundException) {
            Snackbar.make(binding.root, R.string.movies_share_error, Snackbar.LENGTH_SHORT)
                .show()
        }
    }

    private fun updateCreateButtonState() {
        val hasPhotos = viewModel.selectedPhotos.value.orEmpty().isNotEmpty()
        val hasMusic = viewModel.selectedMusic.value != null || viewModel.customMusicUri.value != null
        binding.buttonCreateMovie.isEnabled = hasPhotos && hasMusic && !binding.progressCreate.isVisible
    }

    private fun setCreateInProgress(inProgress: Boolean) {
        binding.progressCreate.isVisible = inProgress
        binding.buttonCreateMovie.isEnabled = !inProgress
        updateCreateButtonState()
    }

    private fun applySelection(uris: List<Uri>) {
        val trimmed = if (uris.size > MAX_SELECTION) {
            _binding?.let {
                Snackbar.make(
                    it.root,
                    getString(R.string.scrapbook_max_selection, MAX_SELECTION),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
            uris.take(MAX_SELECTION)
        } else {
            uris
        }
        viewModel.setPhotos(trimmed.toList())
    }

    override fun onPause() {
        super.onPause()
        binding.videoPreview.pause()
    }

    override fun onResume() {
        super.onResume()
        // Don't auto-start video - user controls with play/pause button
        if (viewModel.previewVideoUri.value != null) {
            updatePlayPauseButton()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.videoPreview.stopPlayback()
        previewVideoFile?.delete()
        _binding = null
    }

    companion object {
        private const val MAX_SELECTION = 20
    }
}