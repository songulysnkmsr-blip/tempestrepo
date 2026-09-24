package eu.kanade.tachiyomi.extension.tr.juratempest

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import rx.Observable
import java.util.concurrent.TimeUnit

class JuraTempest : ParsedHttpSource() {

    override val name = "Jura Tempest"
    override val baseUrl = "https://juratempe.st"
    override val lang = "tr"
    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun headersBuilder(): Headers.Builder = super.headersBuilder()
        .add("Referer", "$baseUrl/")
        .add("Origin", baseUrl)

    // Popular Manga
    override fun popularMangaRequest(page: Int): Request = GET(baseUrl, headers)

    override fun popularMangaSelector(): String = "a[href^=\"/explore/\"]:has(img)"

    override fun popularMangaFromElement(element: Element): SManga = SManga.create().apply {
        val href = element.attr("href")
        url = href.split("/").take(3).joinToString("/")
        val img = element.selectFirst("img")
        title = img?.attr("alt")?.takeIf { it.isNotBlank() } ?: element.text().trim()
        thumbnail_url = img?.absUrl("src")
    }

    override fun popularMangaNextPageSelector(): String? = null

    // Latest Updates
    override fun latestUpdatesRequest(page: Int): Request = GET(baseUrl, headers)

    override fun latestUpdatesSelector(): String = "a[href^=\"/explore/\"]:has(img)"

    override fun latestUpdatesFromElement(element: Element): SManga = popularMangaFromElement(element)

    override fun latestUpdatesNextPageSelector(): String? = null

    // Search
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        return GET(baseUrl, headers)
    }

    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> {
        return client.newCall(searchMangaRequest(page, query, filters))
            .asObservableSuccess()
            .map { response ->
                val document = response.asJsoup()
                val q = query.lowercase().trim()
                val mangas = document.select(popularMangaSelector())
                    .map { popularMangaFromElement(it) }
                    .distinctBy { it.url }

                val filtered = if (q.isNotEmpty()) {
                    mangas.filter { it.title.lowercase().contains(q) }
                } else {
                    mangas
                }
                MangasPage(filtered, false)
            }
    }

    override fun searchMangaSelector(): String = throw UnsupportedOperationException()
    override fun searchMangaFromElement(element: Element): SManga = throw UnsupportedOperationException()
    override fun searchMangaNextPageSelector(): String? = null

    // Manga Details
    override fun mangaDetailsParse(document: Document): SManga = SManga.create().apply {
        title = document.selectFirst("h1")?.text()?.trim()
            ?: document.selectFirst("meta[property=\"og:title\"]")?.attr("content")
            ?: ""
        thumbnail_url = document.selectFirst("meta[property=\"og:image\"]")?.attr("content")
            ?: document.selectFirst("img[src*=\"cdn.juratempe.st\"]")?.absUrl("src")
        description = document.selectFirst("meta[property=\"og:description\"]")?.attr("content")
            ?: document.select("p").firstOrNull { it.text().length > 30 }?.text()
        status = SManga.ONGOING
    }

    // Chapter List
    override fun chapterListSelector(): String = "a[href^=\"/explore/\"]"

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        val mangaUrl = response.request.url.encodedPath
        val chapterLinks = document.select("a[href^=\"$mangaUrl/\"]")

        return chapterLinks
            .filter { it.attr("href") != mangaUrl }
            .map { chapterFromElement(it) }
            .distinctBy { it.url }
            .sortedByDescending { it.chapter_number }
    }

    override fun chapterFromElement(element: Element): SChapter = SChapter.create().apply {
        url = element.attr("href")
        val rawText = element.text().trim()
        name = rawText
        val numberRegex = Regex("""(?:bölüm|chapter)?\s*([0-9]+(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE)
        val match = numberRegex.find(rawText)
        chapter_number = match?.groupValues?.get(1)?.toFloatOrNull() ?: -1f
    }

    // Pages
    override fun pageListParse(document: Document): List<Page> {
        val images = document.select("img[src*=\"cdn.juratempe.st\"]")
        return images.mapIndexed { index, element ->
            Page(index, "", element.absUrl("src"))
        }
    }

    override fun imageUrlParse(document: Document): String = throw UnsupportedOperationException()
}
