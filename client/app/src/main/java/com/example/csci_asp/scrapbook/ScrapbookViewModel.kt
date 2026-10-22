package com.example.csci_asp.scrapbook

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map   // <-- add this import

class ScrapbookViewModel : ViewModel() {

    private val _selectedPhotos = MutableLiveData<List<Uri>>(emptyList())
    val selectedPhotos: LiveData<List<Uri>> = _selectedPhotos
    val shuffledPhotos: LiveData<List<Uri>> = _selectedPhotos.map { photos ->
        if (photos.isEmpty()) emptyList() else photos.shuffled()
    }

    private val _activeTemplateId = MutableLiveData(TemplateCatalog.default.id)
    val activeTemplate: LiveData<ScrapbookTemplate> =
        _activeTemplateId.map { templateId ->
            TemplateCatalog.findById(templateId)
        }

    fun setPhotos(uris: List<Uri>) {
        _selectedPhotos.value = uris.map { it }
    }

    fun selectTemplate(templateId: String) {
        if (_activeTemplateId.value != templateId) {
            _activeTemplateId.value = templateId
        }
    }
}
