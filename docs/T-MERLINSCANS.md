# T-MERLINSCANS Görev Kanıtı

- **Görev:** T-MERLINSCANS (Merlin Scans uzantısını geliştir)
- **Tarih:** 2026-10-20
- **Geliştirici:** deepseek-gelistirici
- **Kaynak:** https://merlinscans.org/

## Mimari Özeti

Merlin Scans, OriManga ile aynı `wp-theme-init-manga` WordPress + UIkit altyapısını kullanır. Uzantı `HttpSource` üzerine kuruludur ve üç katmandan oluşur:

1. **WP REST API listeleme:** `popular`, `latest` ve `search` istekleri `/wp-json/wp/v2/manga` uç noktasına gider. Ortak parametreler `_embed=true`, `per_page=20` ve `page=N`; popüler sıralama `orderby=modified`, son güncelleme sıralaması `orderby=date`, her ikisinde `order=desc`; arama ise `search=<sorgu>` parametresini kullanır.
2. **DOM ayrıştırma:** Seri detayı ve bölüm listesi Jsoup ile `wp-theme-init-manga` seçicilerinden okunur. Sayfalama bilgisi `X-WP-TotalPages` başlığından, mevcut sayfa istek URL'sinin `page` parametresinden alınır.
3. **Okuyucu görselleri:** Bölüm sayfaları `#chapter-content img, .chapter-body img, .reading-content img` seçicisiyle toplanır; `img.site-logo` kesinlikle atlanır.

## Listeleme Akışı

| İşlem | Uç nokta / parametre | Ayrıştırma |
|---|---|---|
| Popüler | `/wp-json/wp/v2/manga?_embed=true&per_page=20&page=N&orderby=modified&order=desc` | `parseMangaJson` |
| Son güncellemeler | `/wp-json/wp/v2/manga?_embed=true&per_page=20&page=N&orderby=date&order=desc` | `parseMangaJson` |
| Arama | `/wp-json/wp/v2/manga?_embed=true&search=...&per_page=20&page=N` | `parseMangaJson` |

`parseMangaJson` yalnızca `[` ile başlayan gövdeyi dizi olarak işler. Boş gövde, WordPress hata nesnesi veya Cloudflare HTML yanıtı geldiğinde boş `MangasPage` döner; böylece ayrıştırma istisnası okuyucuya taşınmaz. Başlık `title.rendered` alanından okunur ve `cleanHtmlEntities` ile temizlenir; kapak `_embedded.wp:featuredmedia[0].source_url` alanından alınır.

## Detay ve Bölüm Ayrıştırma

- Başlık: `h1, .manga-title, h2.uk-h3`
- Kapak: `.story-cover-wrap img, .manga-item-grid img, img.image-3-4` (`src`, yoksa `data-src`)
- Açıklama: `.uk-text-justify, .manga-overview, .manga-description`
- Türler: `a[href*='/genre/'], a[href*='/kategori/'], a[href*='/tur/']`
- Yazar: `a[href*='/author/'], a[href*='/yazar/']`
- Çizer: `a[href*='/cizer/']` (yoksa yazar)
- Durum: sayfa metninde `Tamamlandı` / `Devam Ediyor`
- Bölümler: `.chapter-list .chapter-item` içindeki `a`, `h3` ve `time[datetime]`

Bölüm tarihi `yyyy-MM-dd'T'HH:mm:ssXXX` biçiminde ayrıştırılır; eksik veya geçersiz tarihte `0L` kullanılır.

## Harici CDN ve `data-referer` Çözümü

Merlin Scans bölüm görsellerinin bir bölümü `ragnarscans.net` gibi harici CDN'lerden sunulur. Bu sunucular yalnızca doğru `Referer` başlığıyla görsel döndürür; aksi halde `403` oluşur. Bu nedenle iki aşamalı bir çözüm uygulanır:

1. **`pageListParse`:** Her görsel için kaynak `img.absUrl("data-src")`, boşsa `img.absUrl("src")` olarak belirlenir. `img.attr("data-referer").trim()` boş değilse bu değer `Page(pages.size, dataReferer, src)` ile `page.url` alanına yazılır. `data-referer` yoksa `Page(pages.size, "", src)` kullanılır.
2. **`imageRequest`:** Görsel isteği `headers.newBuilder().set("Referer", referer)` ile yeniden üretilir. Referer seçimi şu sırayla yapılır:
   - `page.url` doluysa doğrudan `page.url` (CDN'in beklediği tam sayfa adresi),
   - değilse ve görsel host'u `merlinscans.org` dışındaysa `https://<görsel-host>/`,
   - aksi halde `https://merlinscans.org/`.

Bu akış hem `merlinscans.org` üzerindeki görselleri hem de `data-referer` taşıyan harici CDN görsellerini kapsar. `page.url` alanı Tachiyomi okuyucusunda sayfa adresi olarak değil, yalnızca bu uzantıya özgü Referer taşıyıcısı olarak kullanılır; `page.imageUrl` görselin gerçek adresidir.

## Oluşturulan / Güncellenen Dosyalar

- `src/tr/merlinscans/src/eu/kanade/tachiyomi/extension/tr/merlinscans/MerlinScans.kt` (`Jsoup.parse(..., response.request.url.toString())` baseUri ile)
- `src/tr/merlinscans/build.gradle.kts` (`versionCode = 140004`, `versionName = "1.4.4"`)
- `src/tr/merlinscans/AndroidManifest.xml` (`Tachiyomi: Merlin Scans`, `.MerlinScans`)
- `src/tr/merlinscans/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher.png` (48x48, 72x72, 96x96, 144x144, 192x192 PNG ikonları)
- `settings.gradle.kts` (`include(":src:tr:merlinscans")`)
- `tools/build_repo.py` (`EXTENSIONS_META["merlinscans"]` kaydı)
- `docs/T-MERLINSCANS.md` (bu belge)

## Doğrulama Ölçütleri

- `settings.gradle.kts` içinde `:src:tr:merlinscans` modülü tanımlı olmalı.
- `MerlinScans.kt` paket yolu ve sınıf adı `eu.kanade.tachiyomi.extension.tr.merlinscans.MerlinScans` ile eşleşmeli.
- Manifest sınıf meta-data değeri `.MerlinScans`, sürüm değeri `1.4.4` olmalı.
- `pageListParse` çıktısında `site-logo` görseli bulunmamalı; `data-referer` taşıyan görsellerde `Page.url` bu değeri içermeli.
- `imageRequest`, `page.url` dolu olduğunda `Referer` başlığını bu değere, harici host durumunda görsel origin'ine, dahili host durumunda `https://merlinscans.org/` değerine ayarlamalı.

Bu belge statik uygulama kanıtıdır. Canlı ağ davranışı (CDN yanıt kodları, gerçek bölüm sayısı) ayrı bir çalışma zamanı kontrol adımında doğrulanmalıdır; bu görevde canlı test sonucu iddia edilmemektedir.
