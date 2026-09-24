package eu.kanade.tachiyomi.extension.tr.mangtto

import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.Serializable

@Serializable
class MangttoPopularData(
    val mangas: List<MangttoManga> = emptyList(),
    val total: Int = 0,
)

@Serializable
class MangttoLatestData(
    val chapters: List<MangttoLatestChapter> = emptyList(),
    val total: Int = 0,
)

@Serializable
class MangttoLatestChapter(
    val chapter: Float? = null,
    val manga: MangttoManga? = null,
)

@Serializable
class MangttoSearchData(
    val hits: List<MangttoSearchHit> = emptyList(),
    val estimatedTotalHits: Int = 0,
)

@Serializable
class MangttoSearchHit(
    val document: MangttoManga,
)

@Serializable
class MangttoManga(
    val title: String = "",
    val slug: String = "",
    val coverImage: String? = null,
) {
    fun toSManga(): SManga = SManga.create().apply {
        this.title = this@MangttoManga.title
        this.url = this@MangttoManga.slug
        this.thumbnail_url = this@MangttoManga.coverImage
    }
}

@Serializable
class MangttoDetailData(
    val slug: String = "",
    val title: String = "",
    val status: String? = null,
    val description: String? = null,
    val coverImage: String? = null,
    val genres: List<MangttoGenre> = emptyList(),
) {
    fun toSManga(): SManga = SManga.create().apply {
        this.title = this@MangttoDetailData.title
        this.url = this@MangttoDetailData.slug
        this.thumbnail_url = this@MangttoDetailData.coverImage
        this.description = this@MangttoDetailData.description
        this.status = when (this@MangttoDetailData.status) {
            "FINISHED" -> SManga.COMPLETED
            "RELEASING" -> SManga.ONGOING
            "HIATUS" -> SManga.ON_HIATUS
            "CANCELLED" -> SManga.CANCELLED
            else -> SManga.UNKNOWN
        }
        this.genre = this@MangttoDetailData.genres.joinToString { it.name }
        this.initialized = true
    }
}

@Serializable
class MangttoGenre(val name: String = "")

@Serializable
class MangttoChapterPageData(
    val chapters: List<MangttoChapter> = emptyList(),
    val total: Int = 0,
)

@Serializable
class MangttoChapter(
    val id: String? = null,
    val chapter: Float = 0f,
) {
    fun toSChapter(mangaSlug: String): SChapter = SChapter.create().apply {
        val chapterStr = if (chapter % 1.0f == 0.0f) chapter.toInt().toString() else chapter.toString()
        name = "Bölüm $chapterStr"
        chapter_number = chapter
        url = "$mangaSlug/$chapterStr"
    }
}

@Serializable
class MangttoPageData(
    val cdn: String = "",
    val uploads: List<MangttoUpload> = emptyList(),
)

@Serializable
class MangttoUpload(
    val fansubId: String? = null,
    val fileLength: Int = 0,
)
