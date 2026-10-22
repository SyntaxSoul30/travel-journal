package com.example.csci_asp.movies

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MoviesViewModel : ViewModel() {

    private val _selectedPhotos = MutableLiveData<List<Uri>>(emptyList())
    val selectedPhotos: LiveData<List<Uri>> = _selectedPhotos

    private val _selectedMusic = MutableLiveData<MusicClip?>(null)
    val selectedMusic: LiveData<MusicClip?> = _selectedMusic

    private val _customMusicUri = MutableLiveData<Uri?>(null)
    val customMusicUri: LiveData<Uri?> = _customMusicUri

    private val _previewVideoUri = MutableLiveData<Uri?>(null)
    val previewVideoUri: LiveData<Uri?> = _previewVideoUri

    fun setPhotos(uris: List<Uri>) {
        _selectedPhotos.value = uris.toList()
    }

    fun selectMusic(musicClip: MusicClip?) {
        _selectedMusic.value = musicClip
        if (musicClip != null) {
            _customMusicUri.value = null
        }
    }

    fun setCustomMusic(uri: Uri?) {
        _customMusicUri.value = uri
        if (uri != null) {
            _selectedMusic.value = null
        }
    }

    fun setPreviewVideo(uri: Uri?) {
        _previewVideoUri.value = uri
    }

    fun getSelectedAudioUri(context: Context): Uri? {
        // Return custom music URI if available, otherwise create URI from selected music clip's raw resource
        return _customMusicUri.value ?: _selectedMusic.value?.let { clip ->
            Uri.parse("android.resource://${context.packageName}/${clip.rawResId}")
        }
    }
}

