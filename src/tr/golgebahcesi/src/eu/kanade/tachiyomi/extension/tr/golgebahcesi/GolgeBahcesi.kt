package eu.kanade.tachiyomi.extension.tr.golgebahcesi

import android.webkit.CookieManager
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class GolgeBahcesi : HttpSource() {

    override val name = "Gölge Bahçesi"
    override val baseUrl = "https://golgebahcesi.com"
    private val apiBaseUrl = "https://api.golgebahcesi.com/api"
    override val lang = "tr"
    override val supportsLatest = true

    private val cookieSyncInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        try {
            val cookieManager = CookieManager.getInstance()
            val mainCookies = cookieManager.getCookie(baseUrl)
            if (!mainCookies.isNullOrBlank()) {
                val existingCookie = originalRequest.header("Cookie")
                val mergedCookies = if (existingCookie.isNullOrBlank()) {
                    mainCookies
                } else {
                    "$existingCookie; $mainCookies"
                }
                requestBuilder.header("Cookie", mergedCookies)
            }
        } catch (_: Exception) {
            // Ignore if CookieManager not available
        }

        val response = chain.proceed(requestBuilder.build())

        if (response.code == 403) {
            val bodyString = response.peekBody(1024).string()
            if (bodyString.contains("challenge") || bodyString.contains("turnstile") || bodyString.contains("Just a moment")) {
                throw Exception("Cloudflare doğrulaması gerekiyor. Lütfen seriyi WebView (küre simgesi) ile açıp doğrulamayı tamamlayın.")
            }
        }

        response
    }

    override val client = network.cloudflareClient.newBuilder()
        .addInterceptor(cookieSyncInterceptor)
        .rateLimit(2)
        .build()

    override fun headersBuilder(): Headers.Builder = super.headersBuilder()
        .add("Referer", "$baseUrl/")

    private fun getSeriesSlug(url: String): String {
        val cleanUrl = url.substringBefore('?').substringBefore('#').trim().trim('/')
        val extracted = when {
            cleanUrl.contains("/manga/") -> cleanUrl.substringAfterLast("/manga/")
            cleanUrl.contains("/series/") -> cleanUrl.substringAfterLast("/series/")
            cleanUrl.startsWith("manga/") -> cleanUrl.removePrefix("manga/")
            cleanUrl.startsWith("series/") -> cleanUrl.removePrefix("series/")
            else -> cleanUrl
        }
        return extracted.substringBefore('/')
    }

    private fun getChapterIdentifier(url: String): String {
        val parts = url.substringBefore('?').substringBefore('#').trim().trim('/').split('/')
            .filterNot { it == "v3" || it == "v2" || it == "manga" || it == "bolum" || it == "series" }
        val idPart = parts.firstOrNull { it.length == 24 && it.all { c -> c.isDigit() || c in 'a'..'f' || c in 'A'..'F' } }
        if (idPart != null) return idPart
        val nonIdParts = parts.filterNot { it.length == 24 && it.all { c -> c.isDigit() || c in 'a'..'f' || c in 'A'..'F' } }
        if (nonIdParts.size >= 2) return "${nonIdParts[0]}/${nonIdParts[1]}"
        return parts.lastOrNull() ?: url.trim('/')
    }

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
        val json = JSONObject(response.body?.string().orEmpty())
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
            val slug = obj.optString("slug").ifBlank { obj.optString("id") }.trim('/')
            val manga = SManga.create().apply {
                url = slug
                title = obj.optString("title")
                thumbnail_url = obj.optString("coverImage").takeIf { it.isNotBlank() }
            }
            mangas.add(manga)
        }

        return MangasPage(mangas, hasNextPage)
    }

    // Manga Details
    override fun getMangaUrl(manga: SManga): String {
        val slug = getSeriesSlug(manga.url)
        return "$baseUrl/manga/$slug"
    }

    override fun mangaDetailsRequest(manga: SManga): Request {
        val slug = getSeriesSlug(manga.url)
        return GET("$apiBaseUrl/series/$slug", headers)
    }

    override fun mangaDetailsParse(response: Response): SManga {
        val obj = JSONObject(response.body?.string().orEmpty())
        return SManga.create().apply {
            val slug = obj.optString("slug").ifBlank { obj.optString("id") }.trim('/')
            url = slug
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
    override fun chapterListRequest(manga: SManga): Request {
        val slug = getSeriesSlug(manga.url)
        return GET("$apiBaseUrl/series/$slug/chapters", headers)
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val arr = JSONArray(response.body?.string().orEmpty())
        val chapters = mutableListOf<SChapter>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        for (i in 0 until arr.length()) {
            val ch = arr.getJSONObject(i)

            val seriesSlug = ch.optString("seriesSlug")
            val chapterSlug = ch.optString("slug")
            val chapterId = ch.optString("id")
            if (chapterId.isBlank()) continue

            val isEncrypted = ch.optBoolean("imageEnc", false) || ch.optString("deliverySystem") == "secure"

            val isLocked = ch.optBoolean("isLocked", false)
            val lockObj = ch.optJSONObject("lock")
            val lockType = lockObj?.optString("type", "none") ?: "none"
            val unlockAtStr = lockObj?.optString("unlockAt")

            val isTimeUnlocked = if (!unlockAtStr.isNullOrBlank()) {
                try {
                    val cleanUnlock = if (unlockAtStr.length >= 19) unlockAtStr.substring(0, 19) else unlockAtStr
                    dateFormat.parse(cleanUnlock)?.time?.let { System.currentTimeMillis() >= it } ?: false
                } catch (_: Exception) {
                    false
                }
            } else {
                false
            }

            val isActuallyLocked = isLocked && lockType != "none" && !isTimeUnlocked

            val rawTitle = ch.optString("title").ifBlank { "Bölüm ${ch.optDouble("number", 0.0)}" }

            val displayName = when {
                isActuallyLocked -> "🔒 $rawTitle"
                isEncrypted -> "🌐 [Web] $rawTitle"
                else -> rawTitle
            }

            val chapter = SChapter.create().apply {
                // v3 prefix ensures Mihon disk cache is invalidated for all previous chapters
                url = "/v3/$seriesSlug/$chapterSlug/$chapterId"
                name = displayName
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
        val parts = chapter.url.substringBefore('?').substringBefore('#').trim('/').split('/')
            .filterNot { it == "v3" || it == "v2" || it == "manga" || it == "bolum" || it == "series" }
        val nonIdParts = parts.filterNot { it.length == 24 && it.all { c -> c.isDigit() || c in 'a'..'f' || c in 'A'..'F' } }
        return if (nonIdParts.size >= 2) {
            val series = nonIdParts[0]
            val slug = nonIdParts[1]
            "$baseUrl/manga/$series/bolum/$slug"
        } else if (parts.size >= 2) {
            "$baseUrl/manga/${parts[0]}/bolum/${parts[1]}"
        } else {
            baseUrl
        }
    }

    // Page List - use /api/chapters/<id> directly
    override fun pageListRequest(chapter: SChapter): Request {
        val chapterIdentifier = getChapterIdentifier(chapter.url)
        return GET("$apiBaseUrl/chapters/$chapterIdentifier", headers)
    }

    override fun pageListParse(response: Response): List<Page> {
        val json = JSONObject(response.body?.string().orEmpty())
        val pagesArr = json.optJSONArray("pages")

        val seriesSlug = json.optString("seriesSlug")
        val chapterSlug = json.optString("slug")
        val chapterWebUrl = if (seriesSlug.isNotBlank() && chapterSlug.isNotBlank()) {
            "$baseUrl/manga/$seriesSlug/bolum/$chapterSlug"
        } else {
            baseUrl
        }

        val isLocked = json.optBoolean("isLocked", false)
        val lockObj = json.optJSONObject("lock")
        val lockType = lockObj?.optString("type", "none") ?: "none"
        if (isLocked && lockType != "none") {
            return listOf(Page(0, chapterWebUrl, chapterWebUrl))
        }

        val isEncrypted = json.optBoolean("imageEnc", false) || json.optString("deliverySystem") == "secure"
        val hasEncryptedPages = pagesArr != null && (0 until pagesArr.length()).any {
            pagesArr.getJSONObject(it).optString("url").contains(".enc")
        }

        // Korumalı bölümlerde tek sayfa olarak chapterWebUrl verilir.
        // Mihon okuyucusunda 'WebView ile aç' tıklandığında skycdn yerine doğrudan sitenin bölüm sayfası açılır!
        if (isEncrypted || hasEncryptedPages) {
            return listOf(Page(0, chapterWebUrl, chapterWebUrl))
        }

        if (pagesArr == null || pagesArr.length() == 0) {
            return listOf(Page(0, chapterWebUrl, chapterWebUrl))
        }

        val skycdnBase = "https://c2.skycdn.online"
        val pages = mutableListOf<Page>()

        for (i in 0 until pagesArr.length()) {
            val pageObj = pagesArr.getJSONObject(i)
            val rawUrl = pageObj.optString("url")
            if (rawUrl.isBlank() || rawUrl.contains(".enc")) continue

            val fullUrl = when {
                rawUrl.startsWith("http://") || rawUrl.startsWith("https://") -> rawUrl
                rawUrl.startsWith("//") -> "https:$rawUrl"
                rawUrl.startsWith("/") -> "$skycdnBase$rawUrl"
                else -> "$skycdnBase/$rawUrl"
            }

            pages.add(Page(pages.size, chapterWebUrl, fullUrl))
        }

        if (pages.isEmpty()) {
            return listOf(Page(0, chapterWebUrl, chapterWebUrl))
        }

        return pages
    }

    override fun imageRequest(page: Page): Request {
        val url = page.imageUrl?.takeIf { it.isNotBlank() } ?: page.url
        return GET(url, headers)
    }

    override fun imageUrlParse(response: Response): String {
        throw UnsupportedOperationException("Not used.")
    }
}
