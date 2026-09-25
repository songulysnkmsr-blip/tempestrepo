package eu.kanade.tachiyomi.extension.tr.mangtto

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.TimeUnit

class Mangtto : HttpSource() {

    override val name = "Mangtto"
    override val baseUrl = "https://mangtto.com"
    override val lang = "tr"
    override val supportsLatest = true

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun headersBuilder(): Headers.Builder = super.headersBuilder()
        .add("Referer", "$baseUrl/")
        .add("Origin", baseUrl)

    // Popular
    override fun popularMangaRequest(page: Int): Request {
        val skip = (page - 1) * 24
        return GET("$baseUrl/api/manga/populer?skip=$skip&take=24", headers)
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val data = json.decodeFromString<MangttoPopularData>(response.body?.string().orEmpty())
        val mangas = data.mangas.map { it.toSManga() }
        val hasNextPage = mangas.size >= 24
        return MangasPage(mangas, hasNextPage)
    }

    // Latest
    override fun latestUpdatesRequest(page: Int): Request {
        val skip = (page - 1) * 24
        return GET("$baseUrl/api/manga/latest?skip=$skip&take=24", headers)
    }

    override fun latestUpdatesParse(response: Response): MangasPage {
        val data = json.decodeFromString<MangttoLatestData>(response.body?.string().orEmpty())
        val mangas = data.chapters
            .mapNotNull { it.manga }
            .distinctBy { it.slug }
            .map { it.toSManga() }
        val hasNextPage = data.chapters.size >= 24
        return MangasPage(mangas, hasNextPage)
    }

    // Search
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        return GET("$baseUrl/api/manga/search?page=$page&q=$query", headers)
    }

    override fun searchMangaParse(response: Response): MangasPage {
        val data = json.decodeFromString<MangttoSearchData>(response.body?.string().orEmpty())
        val mangas = data.hits.map { it.document.toSManga() }
        val hasNextPage = mangas.size >= 24
        return MangasPage(mangas, hasNextPage)
    }

    // Details
    override fun mangaDetailsRequest(manga: SManga): Request {
        return GET("$baseUrl/api/manga/${manga.url}", headers)
    }

    override fun mangaDetailsParse(response: Response): SManga {
        val data = json.decodeFromString<MangttoDetailData>(response.body?.string().orEmpty())
        return data.toSManga()
    }

    // Chapters
    override fun chapterListRequest(manga: SManga): Request {
        return GET("$baseUrl/api/manga/${manga.url}/chapters?skip=0&take=1000", headers)
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val segments = response.request.url.pathSegments
        val mangaSlug = segments.getOrNull(segments.size - 2) ?: ""
        val data = json.decodeFromString<MangttoChapterPageData>(response.body?.string().orEmpty())

        return data.chapters
            .map { it.toSChapter(mangaSlug) }
            .sortedByDescending { it.chapter_number }
    }

    // Pages
    override fun pageListRequest(chapter: SChapter): Request {
        return GET("$baseUrl/api/manga/${chapter.url}", headers)
    }

    override fun pageListParse(response: Response): List<Page> {
        val data = json.decodeFromString<MangttoPageData>(response.body?.string().orEmpty())
        val upload = data.uploads.firstOrNull() ?: return emptyList()
        val fansubId = upload.fansubId ?: return emptyList()

        // Extract slug and chapter number from the request URL path
        // URL: /api/manga/<slug>/<chNum>
        val path = response.request.url.encodedPath
        val parts = path.trimEnd('/').split("/")
        val slug = parts.getOrNull(parts.size - 2) ?: ""
        val chNum = parts.lastOrNull() ?: ""

        if (slug.isEmpty() || chNum.isEmpty() || fansubId.isEmpty()) return emptyList()

        return (1..upload.fileLength).map { i ->
            val imgUrl = "${data.cdn}/manga/$slug/$chNum/$i-$fansubId.webp"
            Page(i - 1, "", imgUrl)
        }
    }

    override fun imageUrlParse(response: Response): String = throw UnsupportedOperationException()
}
