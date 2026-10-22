package com.example.csci_asp.movies

import androidx.annotation.RawRes
import com.example.csci_asp.R

data class MusicClip(
    val id: String,
    val nameRes: Int,
    val descriptionRes: Int,
    @RawRes val rawResId: Int
)

object MusicCatalog {
    val clips = listOf(
        MusicClip(
            id = "upbeat",
            nameRes = R.string.music_clip_upbeat,
            descriptionRes = R.string.music_clip_upbeat_description,
            rawResId = R.raw.upbeat
        ),
        MusicClip(
            id = "romantic",
            nameRes = R.string.music_clip_romantic,
            descriptionRes = R.string.music_clip_romantic_description,
            rawResId = R.raw.romantic
        ),
        MusicClip(
            id = "beats",
            nameRes = R.string.music_clip_beats,
            descriptionRes = R.string.music_clip_beats_description,
            rawResId = R.raw.beats
        ),
        MusicClip(
            id = "mellow",
            nameRes = R.string.music_clip_mellow,
            descriptionRes = R.string.music_clip_mellow_description,
            rawResId = R.raw.mellow
        ),
        MusicClip(
            id = "playful",
            nameRes = R.string.music_clip_playful,
            descriptionRes = R.string.music_clip_playful_description,
            rawResId = R.raw.playful
        )
    )

    fun findById(id: String): MusicClip? = clips.find { it.id == id }
}

