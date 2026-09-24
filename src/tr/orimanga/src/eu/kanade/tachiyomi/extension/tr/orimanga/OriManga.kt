package eu.kanade.tachiyomi.extension.tr.orimanga

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
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
import org.jsoup.Jsoup
import rx.Observable
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class OriManga : HttpSource() {

    override val name = "Ori Manga"

    override val baseUrl = "https://orimanga.net"

    override val lang = "tr"

    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
        .add("Referer", "$baseUrl/")

    // Popular Manga
    override fun popularMangaRequest(page: Int): Request {
        val url = "$baseUrl/wp-json/wp/v2/manga".toHttpUrl().newBuilder()
            .addQueryParameter("_embed", "true")
            .addQueryParameter("per_page", "20")
            .addQueryParameter("page", page.toString())
            .addQueryParameter("orderby", "modified")
            .addQueryParameter("order", "desc")
            .build()
        return GET(url.toString(), headers)
    }

    override fun popularMangaParse(response: Response): MangasPage = parseMangaJson(response)

    // Latest Manga
    override fun latestUpdatesRequest(page: Int): Request {
        val url = "$baseUrl/wp-json/wp/v2/manga".toHttpUrl().newBuilder()
            .addQueryParameter("_embed", "true")
            .addQueryParameter("per_page", "20")
            .addQueryParameter("page", page.toString())
            .addQueryParameter("orderby", "date")
            .addQueryParameter("order", "desc")
            .build()
        return GET(url.toString(), headers)
    }

    override fun latestUpdatesParse(response: Response): MangasPage = parseMangaJson(response)

    // Search Manga
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url = "$baseUrl/wp-json/wp/v2/manga".toHttpUrl().newBuilder()
            .addQueryParameter("_embed", "true")
            .addQueryParameter("search", query)
            .addQueryParameter("per_page", "20")
            .addQueryParameter("page", page.toString())
            .build()
        return GET(url.toString(), headers)
    }

    override fun searchMangaParse(response: Response): MangasPage = parseMangaJson(response)

    private fun parseMangaJson(response: Response): MangasPage {
        val body = response.body.string()
        if (body.isBlank() || body.startsWith("{") && body.contains("\"code\"")) {
            return MangasPage(emptyList(), false)
        }

        val totalPages = response.header("X-WP-TotalPages")?.toIntOrNull() ?: 1
        val currentPage = response.request.url.queryParameter("page")?.toIntOrNull() ?: 1
        val hasNextPage = currentPage < totalPages

        val jsonArray = JSONArray(body)
        val mangas = mutableListOf<SManga>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val manga = SManga.create().apply {
                val fullUrl = obj.optString("link")
                url = fullUrl.removePrefix(baseUrl)

                val rawTitle = obj.optJSONObject("title")?.optString("rendered") ?: ""
                title = cleanHtmlEntities(rawTitle)

                val embedded = obj.optJSONObject("_embedded")
                val media = embedded?.optJSONArray("wp:featuredmedia")?.optJSONObject(0)
                thumbnail_url = media?.optString("source_url")
            }
            mangas.add(manga)
        }

        return MangasPage(mangas, hasNextPage)
    }

    // Manga Details
    override fun mangaDetailsParse(response: Response): SManga {
        val document = Jsoup.parse(response.body.string())
        val manga = SManga.create()

        manga.title = cleanHtmlEntities(
            document.selectFirst("h1, .manga-title, h2.uk-h3")?.text() ?: ""
        )

        manga.thumbnail_url = document.selectFirst(".story-cover-wrap img, .manga-item-grid img, img.image-3-4")
            ?.let { it.absUrl("src").ifEmpty { it.absUrl("data-src") } }

        manga.description = document.select(".uk-text-justify, .manga-overview, .manga-description")
            .text().trim()

        val genres = document.select("a[href*='/genre/'], a[href*='/kategori/'], a[href*='/tur/']")
            .map { it.text().trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        manga.genre = genres.joinToString(", ")

        manga.author = document.select("a[href*='/author/'], a[href*='/yazar/']")
            .firstOrNull()?.text()?.trim()

        manga.artist = document.select("a[href*='/cizer/']")
            .firstOrNull()?.text()?.trim() ?: manga.author

        val pageText = document.text()
        manga.status = when {
            pageText.contains("Tamamlandı", ignoreCase = true) -> SManga.COMPLETED
            pageText.contains("Devam Ediyor", ignoreCase = true) -> SManga.ONGOING
            else -> SManga.UNKNOWN
        }

        return manga
    }

    // Chapter List
    override fun chapterListParse(response: Response): List<SChapter> {
        val document = Jsoup.parse(response.body.string())
        val chapters = mutableListOf<SChapter>()

        val items = document.select(".chapter-list .chapter-item")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ROOT)

        for (item in items) {
            val link = item.selectFirst("a") ?: continue
            val href = link.attr("href")
            if (href.isBlank()) continue

            val chapter = SChapter.create().apply {
                url = href.removePrefix(baseUrl)
                name = item.selectFirst("h3")?.text()?.trim()
                    ?: link.text().trim()

                val timeAttr = item.selectFirst("time")?.attr("datetime")
                date_upload = if (!timeAttr.isNullOrBlank()) {
                    try {
                        dateFormat.parse(timeAttr)?.time ?: 0L
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

    // Page List
    override fun pageListParse(response: Response): List<Page> {
        val document = Jsoup.parse(response.body.string())
        val images = document.select("#chapter-content img, .chapter-body img")
        val pages = mutableListOf<Page>()

        for ((index, img) in images.withIndex()) {
            val src = img.absUrl("src").ifEmpty { img.absUrl("data-src") }
            if (src.isNotBlank()) {
                pages.add(Page(index, "", src))
            }
        }

        return pages
    }

    override fun imageUrlParse(response: Response): String {
        throw UnsupportedOperationException("Not used.")
    }

    private fun cleanHtmlEntities(str: String): String {
        return str
            .replace("&#8217;", "'")
            .replace("&#8216;", "'")
            .replace("&#039;", "'")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace(Regex("<[^>]+>"), "")
            .trim()
    }
}
