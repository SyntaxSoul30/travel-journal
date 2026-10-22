package com.example.csci_asp.scrapbook

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.csci_asp.R

data class ScrapbookTemplate(
    val id: String,
    @StringRes val labelRes: Int,
    @DrawableRes val backgroundRes: Int,
    val photosPerPage: Int,
    val columns: Int,
    val rotationPattern: List<Float> = emptyList()
)

object TemplateCatalog {
    val templates: List<ScrapbookTemplate> = listOf(
        ScrapbookTemplate(
            id = "floral",
            labelRes = R.string.scrapbook_template_floral,
            backgroundRes = R.drawable.bg_template_floral,
            photosPerPage = 4,
            columns = 2,
            rotationPattern = listOf(-4f, 2.5f, -2.5f, 5f)
        ),
        ScrapbookTemplate(
            id = "music",
            labelRes = R.string.scrapbook_template_music,
            backgroundRes = R.drawable.bg_template_music,
            photosPerPage = 4,
            columns = 2,
            rotationPattern = listOf(-4f, 2.5f, -2.5f, 5f)
        ),
        ScrapbookTemplate(
            id = "tech",
            labelRes = R.string.scrapbook_template_tech,
            backgroundRes = R.drawable.bg_template_tech,
            photosPerPage = 4,
            columns = 2,
            rotationPattern = listOf(-4f, 2.5f, -2.5f, 5f)
        ),
        ScrapbookTemplate(
            id = "travel",
            labelRes = R.string.scrapbook_template_travel,
            backgroundRes = R.drawable.bg_template_travel,
            photosPerPage = 4,
            columns = 2,
            rotationPattern = listOf(-4f, 2.5f, -2.5f, 5f)
        )
    )

    val default: ScrapbookTemplate = templates.first()

    fun findById(id: String): ScrapbookTemplate =
        templates.firstOrNull { it.id == id } ?: default
}

