package eu.kanade.tachiyomi.extension.tr.golgebahcesi

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class GolgeBahcesi : HttpSource() {

    override val name = "Gölge Bahçesi"

    override val baseUrl = "https://golgebahcesi.com"

    private val apiBaseUrl = "https://api.golgebahcesi.com/api"

    override val lang = "tr"

    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
        .add("Referer", "$baseUrl/")
        .add("Origin", baseUrl)

    // Popular Manga
    override fun popularMangaRequest(page: Int): Request {
        val url = "$apiBaseUrl/series".toHttpUrl().newBuilder()
            .addQueryParameter("page", page.toString())
            .addQueryParameter("limit", "24")
            .addQueryParameter("sort", "popular")
            .build()
        return GET(url.toString(), headers)
    }

    override fun popularMangaParse(response: Response): MangasPage = parseSeriesList(response)

    // Latest Manga
    override fun latestUpdatesRequest(page: Int): Request {
        val url = "$apiBaseUrl/series".toHttpUrl().newBuilder()
            .addQueryParameter("page", page.toString())
            .addQueryParameter("limit", "24")
            .addQueryParameter("sort", "updatedAt")
            .build()
        return GET(url.toString(), headers)
    }

    override fun latestUpdatesParse(response: Response): MangasPage = parseSeriesList(response)

    // Search Manga
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url = "$apiBaseUrl/series".toHttpUrl().newBuilder()
            .addQueryParameter("page", page.toString())
            .addQueryParameter("limit", "24")
            .addQueryParameter("search", query)
            .build()
        return GET(url.toString(), headers)
    }

    override fun searchMangaParse(response: Response): MangasPage = parseSeriesList(response)

    private fun parseSeriesList(response: Response): MangasPage {
        val json = JSONObject(response.body.string())
        val data = json.optJSONArray("data") ?: JSONArray()
        val pagination = json.optJSONObject("pagination")

        val hasNextPage = if (pagination != null) {
            val currentPage = pagination.optInt("currentPage", 1)
            val totalPages = pagination.optInt("totalPages", 1)
            currentPage < totalPages
        } else {
            false
        }

        val mangas = mutableListOf<SManga>()
        for (i in 0 until data.length()) {
            val obj = data.getJSONObject(i)
            val manga = SManga.create().apply {
                url = obj.optString("slug")
                title = obj.optString("title")
                thumbnail_url = obj.optString("coverImage").takeIf { it.isNotBlank() }
            }
            mangas.add(manga)
        }

        return MangasPage(mangas, hasNextPage)
    }

    // Manga Details
    override fun getMangaUrl(manga: SManga): String = "$baseUrl/manga/${manga.url}"

    override fun mangaDetailsRequest(manga: SManga): Request =
        GET("$apiBaseUrl/series/${manga.url}", headers)

    override fun mangaDetailsParse(response: Response): SManga {
        val obj = JSONObject(response.body.string())
        return SManga.create().apply {
            url = obj.optString("slug")
            title = obj.optString("title")
            thumbnail_url = obj.optString("coverImage").takeIf { it.isNotBlank() }
            description = obj.optString("description").takeIf { it.isNotBlank() }
            author = obj.optString("author").takeIf { it.isNotBlank() }
            artist = obj.optString("artist").takeIf { it.isNotBlank() } ?: author

            val genresList = mutableListOf<String>()
            val genresArr = obj.optJSONArray("genres")
            if (genresArr != null) {
                for (i in 0 until genresArr.length()) {
                    genresList.add(genresArr.getString(i))
                }
            }
            val type = obj.optString("type")
            if (type.isNotBlank()) {
                genresList.add(type.lowercase().replaceFirstChar { it.uppercase() })
            }
            genre = genresList.distinct().joinToString(", ")

            status = when (obj.optString("status")) {
                "ONGOING" -> SManga.ONGOING
                "COMPLETED" -> SManga.COMPLETED
                "HIATUS" -> SManga.ON_HIATUS
                else -> SManga.UNKNOWN
            }
            initialized = true
        }
    }

    // Chapter List
    override fun chapterListRequest(manga: SManga): Request =
        GET("$apiBaseUrl/series/${manga.url}/chapters", headers)

    override fun chapterListParse(response: Response): List<SChapter> {
        val arr = JSONArray(response.body.string())
        val chapters = mutableListOf<SChapter>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        for (i in 0 until arr.length()) {
            val ch = arr.getJSONObject(i)

            // D-LOCKED-CHAPTERS: Yalnızca açık (ücretsiz) bölümler listelenir
            val isLocked = ch.optBoolean("isLocked", false)
            if (isLocked) continue

            val chapter = SChapter.create().apply {
                val seriesSlug = ch.optString("seriesSlug")
                val chapterSlug = ch.optString("slug")
                url = "/$seriesSlug/$chapterSlug"

                name = ch.optString("title").ifBlank { "Bölüm ${ch.optDouble("number", 0.0)}" }
                chapter_number = ch.optDouble("number", 0.0).toFloat()

                val dateStr = ch.optString("releaseDate").ifBlank { ch.optString("createdAt") }
                date_upload = if (dateStr.isNotBlank()) {
                    try {
                        val cleanDate = if (dateStr.length >= 19) dateStr.substring(0, 19) else dateStr
                        dateFormat.parse(cleanDate)?.time ?: 0L
                    } catch (_: Exception) {
                        0L
                    }
                } else {
                    0L
                }
            }
            chapters.add(chapter)
        }

        return chapters
    }

    override fun getChapterUrl(chapter: SChapter): String {
        val segments = "$baseUrl${chapter.url}".toHttpUrl().pathSegments
        return if (segments.size >= 2) {
            "$baseUrl/manga/${segments[0]}/bolum/${segments[1]}"
        } else {
            "$baseUrl${chapter.url}"
        }
    }

    // Page List
    override fun pageListRequest(chapter: SChapter): Request {
        return GET(getChapterUrl(chapter), headers)
    }

    override fun pageListParse(response: Response): List<Page> {
        val html = response.body.string()
        val pages = mutableListOf<Page>()

        // Search for skycdn images in chapter page
        val pattern = Pattern.compile("""https://c2\.skycdn\.online/series/[^\s"'<>]+\.(?:webp|jpg|png|jpeg)""")
        val matcher = pattern.matcher(html)
        val seenUrls = mutableSetOf<String>()

        while (matcher.find()) {
            val imgUrl = matcher.group()
            if (!imgUrl.contains("series-thumbnail") && seenUrls.add(imgUrl)) {
                pages.add(Page(pages.size, "", imgUrl))
            }
        }

        return pages
    }

    override fun imageUrlParse(response: Response): String {
        throw UnsupportedOperationException("Not used.")
    }
}
