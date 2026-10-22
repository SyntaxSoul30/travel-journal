package com.example.csci_asp.scrapbook

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
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.csci_asp.R
import com.example.csci_asp.databinding.FragmentScrapbookBinding
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import kotlin.math.max

class ScrapbookFragment : Fragment() {

    private var _binding: FragmentScrapbookBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ScrapbookViewModel by viewModels()
    private val photoAdapter = PhotoPreviewAdapter()
    private lateinit var staggeredLayoutManager: StaggeredGridLayoutManager
    private lateinit var pdfExporter: PdfExporter
    private var pendingPhotos: List<Uri>? = null
    private var pendingTemplate: ScrapbookTemplate? = null
    private var currentTemplateColumns: Int = MIN_COLUMNS
    private var currentPhotoCount: Int = 0

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

    private val createDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
            val photos = pendingPhotos
            val template = pendingTemplate
            if (uri == null || photos == null || template == null) {
                pendingPhotos = null
                pendingTemplate = null
                return@registerForActivityResult
            }
            exportToUri(uri, photos, template)
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScrapbookBinding.inflate(inflater, container, false)
        pdfExporter = PdfExporter(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycler()
        setupListeners()
        observeViewModel()
        renderTemplateChips()
    }

    private fun setupRecycler() {
        staggeredLayoutManager = StaggeredGridLayoutManager(
            MIN_COLUMNS,
            StaggeredGridLayoutManager.VERTICAL
        ).apply {
            gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS
        }
        binding.recyclerPhotos.apply {
            layoutManager = staggeredLayoutManager
            adapter = photoAdapter
        }
    }

    private fun setupListeners() {
        binding.buttonPickPhotos.setOnClickListener { launchPhotoPicker() }
        binding.buttonExportPdf.setOnClickListener { exportScrapbook() }
    }

    private fun observeViewModel() {
        viewModel.shuffledPhotos.observe(viewLifecycleOwner) { photos ->
            photoAdapter.submitList(photos)
        }

        viewModel.selectedPhotos.observe(viewLifecycleOwner) { photos ->
            val hasPhotos = photos.isNotEmpty()
            currentPhotoCount = photos.size
            applySpanCount()
            binding.textEmptyState.isGone = hasPhotos
            binding.recyclerPhotos.isVisible = hasPhotos
            updateExportState(hasPhotos)
        }

        viewModel.activeTemplate.observe(viewLifecycleOwner) { template ->
            selectTemplateChip(template.id)
            currentTemplateColumns = max(template.columns, 1)
            applySpanCount()
            photoAdapter.setRotationPattern(template.rotationPattern)
        }
    }

    private fun renderTemplateChips() {
        val group = binding.chipGroupTemplates
        if (group.childCount == 0) {
            TemplateCatalog.templates.forEach { template ->
                val chip = Chip(requireContext()).apply {
                    id = View.generateViewId()
                    text = getString(template.labelRes)
                    isCheckable = true
                    tag = template.id
                }
                group.addView(chip)
            }
        }

        group.setOnCheckedStateChangeListener { chipGroup, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            val chip = chipGroup.findViewById<Chip>(checkedId)
            val templateId = chip?.tag as? String ?: return@setOnCheckedStateChangeListener
            viewModel.selectTemplate(templateId)
        }

        viewModel.activeTemplate.value?.let { selectTemplateChip(it.id) }
            ?: selectTemplateChip(TemplateCatalog.default.id)
    }

    private fun selectTemplateChip(templateId: String) {
        val group = binding.chipGroupTemplates
        (0 until group.childCount)
            .map { group.getChildAt(it) as? Chip }
            .firstOrNull { it?.tag == templateId }
            ?.let { chip ->
                if (!chip.isChecked) {
                    chip.isChecked = true
                }
            }
    }

    private fun applySpanCount() {
        val baseColumns = max(currentTemplateColumns, MIN_COLUMNS)
        val desiredColumns = when {
            currentPhotoCount in 1 until MIN_COLUMNS -> currentPhotoCount
            else -> baseColumns
        }
        staggeredLayoutManager.spanCount = max(1, desiredColumns)
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

    private fun exportScrapbook() {
        val photos = viewModel.selectedPhotos.value.orEmpty()
        val template = viewModel.activeTemplate.value ?: TemplateCatalog.default

        if (photos.isEmpty()) {
            Snackbar.make(binding.root, R.string.scrapbook_no_photos, Snackbar.LENGTH_SHORT).show()
            return
        }

        pendingPhotos = photos
        pendingTemplate = template
        val suggestedName = "scrapbook_${System.currentTimeMillis()}.pdf"
        createDocumentLauncher.launch(suggestedName)
    }

    private fun exportToUri(uri: Uri, photos: List<Uri>, template: ScrapbookTemplate) {
        setExportInProgress(true)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                requireContext().contentResolver.openOutputStream(uri, "wt")?.use { stream ->
                    pdfExporter.export(photos, template, stream)
                } ?: throw IllegalStateException("Unable to open output stream")
                onExportSuccess(uri)
            } catch (_: Exception) {
                Snackbar.make(binding.root, R.string.scrapbook_export_error, Snackbar.LENGTH_LONG).show()
            } finally {
                setExportInProgress(false)
                pendingPhotos = null
                pendingTemplate = null
            }
        }
    }

    private fun onExportSuccess(uri: Uri) {
        val readableName = uri.lastPathSegment ?: uri.toString()
        Snackbar.make(
            binding.root,
            getString(R.string.scrapbook_export_success, readableName),
            Snackbar.LENGTH_LONG
        ).show()
        sharePdf(uri)
    }

    private fun sharePdf(uri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
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
                    getString(R.string.scrapbook_share_pdf)
                )
            )
        } catch (_: ActivityNotFoundException) {
            Snackbar.make(binding.root, R.string.scrapbook_share_error, Snackbar.LENGTH_SHORT)
                .show()
        }
    }

    private fun updateExportState(canExport: Boolean) {
        binding.buttonExportPdf.isEnabled = canExport && !binding.progressExport.isVisible
    }

    private fun setExportInProgress(inProgress: Boolean) {
        binding.progressExport.isVisible = inProgress
        binding.buttonExportPdf.isEnabled = !inProgress && viewModel.selectedPhotos.value.orEmpty().isNotEmpty()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        pendingPhotos = null
        pendingTemplate = null
    }

    companion object {
        private const val MAX_SELECTION = 20
        private const val MIN_COLUMNS = 3
    }
}

