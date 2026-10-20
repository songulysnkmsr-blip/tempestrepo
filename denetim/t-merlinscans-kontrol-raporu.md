# Orvant Çiftli Kontrolcü Raporu — `T-MERLINSCANS — Merlin Scans (merlinscans.org) uzantısını geliştir ve repoya ekle`

- **Tarih:** `2026-10-20T20:56:53+03:00`
- **Çapraz Kontrol Kararı (Consensus):** **`PASS`** (GEÇTİ - ONAY)
- **Alt Ajan 1 (GLM Kontrolcü):** `PASS` · Model: `opencode/glm-5.3-flash-free` (`opencode-free`)
- **Alt Ajan 2 (DeepSeek Kontrolcü):** `PASS` · Model: `opencode/deepseek-v4.1-flash-free` (`opencode-free`)
- **İncelenen Dosyalar:** `src/tr/merlinscans/src/eu/kanade/tachiyomi/extension/tr/merlinscans/MerlinScans.kt`, `src/tr/merlinscans/build.gradle.kts`, `src/tr/merlinscans/AndroidManifest.xml`, `src/tr/merlinscans/res/mipmap-xxhdpi/ic_launcher.png`, `settings.gradle.kts`, `tools/build_repo.py`, `docs/T-MERLINSCANS.md`

## 1. Ölçüt Bazlı Çapraz Denetim Tablosu

| # | Kabul Ölçütü | GLM Kararı | DeepSeek Kararı | Ortak Hüküm | Bulgular (GLM & DeepSeek) |
|---|---|---|---|---|---|
| 0 | MerlinScans.kt (WP REST API + HTML parser + data-referer CDN başlık desteği), build.gradle.kts, AndroidManifest.xml, ikonlar, settings.gradle.kts ve tools/build_repo.py kayıtları eksiksiz oluşturuldu. | `PASS` | `PASS` | **`PASS`** | **GLM:** Tüm gerekli kayıtlar mevcut ve çapraz tutarlı: (1) MerlinScans.kt — WP REST API (/wp-json/wp/v2/manga + _embed/per_page/page, popular=modified, latest=date, search), Jsoup HTML parser (initmanga seçicileri) ve data-referer CDN başlık desteği (pageListParse'da Page(index, dataReferer, src); imageRequest'da page.url → host origin → baseUrl sıralı Referer seçimi) ölçütün üç şartını da karşılıyor; paket yolu eu.kanade.tachiyomi.extension.tr.merlinscans.MerlinScans doğrulandı. (2) build.gradle.kts versionCode=140004 ext-merlinscans nesne kaydıyla eşleşiyor. (3) AndroidManifest.xml .MerlinScans + 1.4.4 + extensionLib 1.4 + nsfw=0 tutarlı. (4) İkon kanıtı xxhdpi/ic_launcher.png (32186 bayt) doğrulanmış; mdpi/hdpi/xhdpi/xxxhdpi dokümanda listelenmiş. (5) settings.gradle.kts ':src:tr:merlinscans' include satırı eklenmiş, mevcut modüller korunmuş. (6) tools/build_repo.py EXTENSIONS_META['merlinscans'] (pkgSuffix tr.merlinscans, class tam yolu, baseUrl merlinscans.org = site-merlinscans.domain) kayıtlı. parseMangaJson'ın '[' kapısı WP hata nesnesi/Cloudflare/boş gövde uç durumlarını güvenle karşılıyor; X-WP-TotalPages + request page parametresi ile doğru sayfalama. Kritik mantık hatası, ontoloji tersliği veya eksik dosya yok. <br> **DeepSeek:** Tüm artefaktlar mevcut ve içerikli: (1) MerlinScans.kt tam HttpSource uygulaması — WP REST (/wp-json/wp/v2/manga, _embed/per_page/page/orderby/order/search, X-WP-TotalPages + queryParameter('page') ile sayfalama), Jsoup DOM ayrıştırma (baseUri'li), data-referer desteği (pageListParse'te Page(pages.size, dataReferer, src), imageRequest'te page.url -> harici host origin -> baseUrl/ önceliğiyle Referer) ve site-logo filtresi; TODO/placeholder yok, imageUrlParse yalnız bilinçli UnsupportedOperationException. (2) build.gradle.kts: namespace/applicationId eu.kanade.tachiyomi.extension.tr.merlinscans, versionCode 140004, versionName 1.4.4, doğru sourceSets ve compileOnly bağımlılıklar. (3) AndroidManifest.xml: label 'Tachiyomi: Merlin Scans', class '.MerlinScans', version '1.4.4', nsfw 0, ikon referansı @mipmap/ic_launcher. (4) res/mipmap-xxhdpi/ic_launcher.png gerçek ikili dosya (32186 bayt). (5) settings.gradle.kts içinde include(':src:tr:merlinscans'). (6) tools/build_repo.py EXTENSIONS_META['merlinscans'] kaydı name/lang/baseUrl/pkgSuffix/nsfw/class alanlarıyla build.gradle ile tutarlı. Sınırlamalar: build_repo.py alıntısı kısmi, ikonlardan yalnız xxhdpi kanıtlanmış, keystore parolası düz metin. |

## 2. GLM Kontrolcü (Mantık, Mimari & Ontoloji) Detaylı Çıktısı

## GLM Kontrolcü — T-MERLINSCANS Teknik İncelemesi

**Ölçüt [0] denetimi (dosya eksiksizliği ve tutarlılık):**

1. **MerlinScans.kt** — Paket yolu `eu.kanade.tachiyomi.extension.tr.merlinscans` ile sınıf adı `MerlinScans`, `ext-merlinscans` kaydı ve dokümandaki doğrulama ölçütleriyle birebir uyumlu. Üç katman mimari olarak tutarlı:
   - *WP REST API:* `popular` (`orderby=modified`), `latest` (`orderby=date`) ve `search` (`search=`) istekleri ortak `/wp-json/wp/v2/manga?_embed=true&per_page=20&page=N` sözleşmesine sadık; `wordpress_initmanga` ("site-merlinscans.engine") kaydıyla örtüşüyor.
   - *HTML parser:* Detay/bölüm Jsoup seçicileri (initmanga + UIkit) dokümantasyonla eşleşiyor; `SimpleDateFormat("...XXX")` ISO-8601 ofsetlerini aksi halde exception → `0L` fallback'i dökümanla uyumlu.
   - *data-referer CDN desteği (ölçütün açık şartı):* `pageListParse` içinde `Page(pages.size, dataReferer, src)` ile `page.url` taşıyıcı olarak kullanılıyor; `imageRequest` sırası (page.url → harici host origin → baseUrl) dokümandaki Referer matrisiyle eşleşiyor. `site-logo` filtresi de mevcut.

2. **Mantık / edge-case değerlendirmesi:**
   - `parseMangaJson`'ın `startsWith("[")` kapısı, WP hata nesnesi (`{`) ve Cloudflare HTML (`<`) yanıtları bölümünde istisna taşmasını engelliyor; `X-WP-TotalPages` yoksa `hasNextPage=false` güvenli davranış.
   - Sayfa indeksi `pages.size` ile sıralı üretiliyor, boş `src` atlanıyor; tarih fallback `0L` istisnaya dönmiyor. `imageUrlParse` doğru biçimde `UnsupportedOperationException` fırlatıyor.
   - Belirgin mantık hatası, yönsel (impact yönü) terslik veya `input_ids`/`output_ids` ontoloji ihlali bulunmadı.

3. **Kayıt seti eksiksizliği:** `build.gradle.kts` (versionCode 140004 = nesne kaydı; versionName 1.4.4 = manifest `tachiyomi.extension.version` + `build_repo.py` lib_version "1.4" zinciri tutarlı), `AndroidManifest.xml` (`.MerlinScans`, nsfw=0), ikon kanıtı (xxhdpi 32186 bayt; diğer yoğunluklar dokümanda listing edilmiş), `settings.gradle.kts` (`include(":src:tr:merlinscans")` eklenmiş, mevcut 4 modül bozulmamış) ve `tools/build_repo.py` içinde `EXTENSIONS_META["merlinscans"]` kaydı (pkgSuffix/class/baseUrl/domain ile örtüşüyor) — tüm parçalar mevcut ve birbirini tutuyor. Ölçüt **[0]** karşılanıyor.

**Uyarı düzeyi gözlemler (ölçütü bozmayan):** `isMinifyEnabled = true` ile manifes meta-data üzerinden çözülen sınıfın R8 tarafından stripped/renamed olma riski (proguard-rules.pro içeriği kanıtlanmadığından doğrulanamıyor); `data:` URI durumunda `toHttpUrl()` patlaması; kullanılmayan kotlinx-serialization eklentisi/bağımlılığı; imza parolalarının düz metin olması. Bunlar öneri düzeyindedir, kritik mantık hatası niteliği taşımaz.

```json
{
  "auditor": "glm-kontrolcu",
  "overall_verdict": "PASS",
  "criteria": [
    {
      "index": 0,
      "verdict": "PASS",
      "finding": "Tüm gerekli kayıtlar mevcut ve çapraz tutarlı: (1) MerlinScans.kt — WP REST API (/wp-json/wp/v2/manga + _embed/per_page/page, popular=modified, latest=date, search), Jsoup HTML parser (initmanga seçicileri) ve data-referer CDN başlık desteği (pageListParse'da Page(index, dataReferer, src); imageRequest'da page.url → host origin → baseUrl sıralı Referer seçimi) ölçütün üç şartını da karşılıyor; paket yolu eu.kanade.tachiyomi.extension.tr.merlinscans.MerlinScans doğrulandı. (2) build.gradle.kts versionCode=140004 ext-merlinscans nesne kaydıyla eşleşiyor. (3) AndroidManifest.xml .MerlinScans + 1.4.4 + extensionLib 1.4 + nsfw=0 tutarlı. (4) İkon kanıtı xxhdpi/ic_launcher.png (32186 bayt) doğrulanmış; mdpi/hdpi/xhdpi/xxxhdpi dokümanda listelenmiş. (5) settings.gradle.kts ':src:tr:merlinscans' include satırı eklenmiş, mevcut modüller korunmuş. (6) tools/build_repo.py EXTENSIONS_META['merlinscans'] (pkgSuffix tr.merlinscans, class tam yolu, baseUrl merlinscans.org = site-merlinscans.domain) kayıtlı. parseMangaJson'ın '[' kapısı WP hata nesnesi/Cloudflare/boş gövde uç durumlarını güvenle karşılıyor; X-WP-TotalPages + request page parametresi ile doğru sayfalama. Kritik mantık hatası, ontoloji tersliği veya eksik dosya yok."
    }
  ],
  "critical_issues": [],
  "recommendations": [
    "build.gradle.kts'te isMinifyEnabled=true; MerlinScans sınıfı yalnızca manifest meta-data dizesinden çözülür (kod referansı yok). rootProject proguard-rules.pro içinde '-keep class eu.kanade.tachiyomi.extension.tachiyomi.**' benzeri kural yoksa R8 sınıfı yeniden adlandırıp kaldırabilir — hem APK hem jars (build_repo.py fqn) bu sınıf adına bağlı. Keep kuralını bir kez doğrula veya minify'i false yap.",
    "imageRequest içinde imageUrl.toHttpUrl() data:/javascript: gibi şemalarda IllegalArgumentException fırlatır; src'si data: URI olan nadir görsellerde okuyucu çöker. runCatching / http(s) şema kontrolü ile korumaya alınabilir.",
    "kotlinx-serialization-json bağımlılığı, serialization plugin'i ve -opt-in bayrağı kodda hiç kullanılmıyor; ölü yapılandırma olarak kaldırılabilir (Karpathy: spekülatif ağırlık yok).",
    "hasNextPage tamamen X-WP-TotalPages başlığına bağımlı; örtee proxy başlığı silerse liste tek sayfada kalır. Gövde uzunluğu/per_page çıkarımıyla fallback düşünebilir.",
    "Durum tespiti tüm sayfa metninde 'Tamamlandı'/'Devam Ediyor' arıyor; alakasız metinde yanlış pozitif üretebilir. Durum rozeti gibi dar bir seçici varsa ona geçirilebilir.",
    "signingConfigs içinde 'tempestpass' düz metin; repo genel şablonu olsa da gradle property/env'e taşınması iyi uygulama olur."
  ]
}
```

## 3. DeepSeek Kontrolcü (Kod, Güvenlik & Somut Kanıt) Detaylı Çıktısı

## Teknik İnceleme — T-MERLINSCANS / Ölçüt [0]

### 1) MerlinScans.kt — derlenebilirlik ve API sözleşmesi
- **İmportlar ve kullanım tutarlı:** `GET`, `HttpSource`, `Headers`, `toHttpUrl`, `OkHttpClient`, `Request/Response`, `JSONArray`, `Jsoup`, `SimpleDateFormat`, `Locale`, `TimeUnit` — hepsi kodda karşılık buluyor; kullanılmayan/eksik import yok.
- **`HttpSource` sözleşmesi:** `name/baseUrl/lang/supportsLatest/client/headersBuilder` override'ları mevcut; `popularMangaRequest/Parse`, `latestUpdatesRequest/Parse`, `searchMangaRequest/Parse`, `mangaDetailsParse`, `chapterListParse`, `pageListParse`, `imageRequest`, `imageUrlParse` tamamlanmış. Placeholder/TODO/`throw NotImplementedError` yok (yalnız `imageUrlParse` bilinçli olarak `UnsupportedOperationException` atıyor; `imageRequest` override edildiği için bu yol kullanılmıyor — standart desen).
- **WP REST katmanı:** `/wp-json/wp/v2/manga` + `_embed`, `per_page`, `page`, `orderby`, `order`, `search` parametreleri doğru kuruluyor; `X-WP-TotalPages` başlığından `hasNextPage`, `response.request.url.queryParameter("page")` üzerinden mevcut sayfa okunuyor. `queryParameter`/`newBuilder` OkHttp 4.12 API'siyle uyumlu.
- **HTML katmanı:** `Jsoup.parse(body, response.request.url.toString())` ile baseUri verilmiş; `absUrl("src"/"data-src")`, `selectFirst`, `Elements.map/filter/distinct`, `hasClass` kullanımları Jsoup 1.17 ile geçerli.
- **`data-referer` CDN desteği:** `pageListParse` içinde `img.attr("data-referer").trim()` boş değilse `Page(pages.size, dataReferer, src)`, boşsa `Page(pages.size, "", src)`; `imageRequest` içinde `headers.newBuilder().set("Referer", ...)` ile öncelik sırası `page.url` → harici host origin → `baseUrl/` olarak uygulanmış. Ölçütte istenen üç davranış da kodda somut olarak var. `site-logo` görselleri `hasClass` ile atlanıyor.
- **Tarih ayrıştırma:** `yyyy-MM-dd'T'HH:mm:ssXXX` + `try/catch` fallback `0L` — istisna sızıntısı yok.

### 2) Dayanıklılık / küçük riskler (bloklayıcı değil)
- `obj.optString("link")` boş dönerse `url = ""` olur (WP her zaman `link` döndürdüğü için pratikte düşük risk, yine de `continue` koruması eklenebilir).
- `thumbnail_url = media?.optString("source_url")` alanı `null` yerine boş string üretebilir; `takeIf { it.isNotEmpty() }` daha temiz olur.
- `data-referer` değeri göreli bir yol ise `Referer` başlığına göreli değer yazılır; mutlak URL doğrulaması yok.
- `page.imageUrl!!` teorik NPE riski (bu kaynakta her `Page` imageUrl ile üretildiği için gerçekleşmez).
- `imageRequest` her çağrıda `baseUrl.toHttpUrl()` yeniden hesaplıyor — önemsiz ama `lazy` host alanına alınabilir.
- "Popüler" sıralaması `orderby=modified` ile yaklaşıklanıyor; WP REST'te gerçek popülerlik metriği olmadığı için kabul edilebilir bir tercih.

### 3) build.gradle.kts
- `namespace/applicationId` paketle birebir: `eu.kanade.tachiyomi.extension.tr.merlinscans`; `versionCode = 140004`, `versionName = "1.4.4"` — Orvant nesne kaydı ve manifest ile tutarlı. `sourceSets.main` içinde `manifest.srcFile`, `java.srcDirs("src")`, `res.srcDirs("res")` doğru. `compileOnly` bağımlılıklar (extensions-lib, jsoup, okhttp, org.json runtime) uzantı modeline uygun.
- **Güvenlik notu:** `storePassword = "tempestpass"` / `keyPassword = "tempestpass"` repo içinde düz metin. Bu, ekosistemde paylaşılan test anahtarı deseni olsa da sır sızıntısı sınıfına girer; ideal olarak `local.properties`/ortam değişkeninden okunmalı. Ayrıca `signingkey.p12` yoksa `signingConfigs.getByName("release")` yine de atanıyor → release derlemesi `storeFile` olmadan başarısız olur (build_repo akışı anahtarı sağlıyorsa sorun değil).

### 4) AndroidManifest.xml
- `application` etiketi, `android:icon="@mipmap/ic_launcher"`, `android:label="Tachiyomi: Merlin Scans"`, `tachiyomi.extension.class = ".MerlinScans"`, `tachiyomi.extension.version = "1.4.4"`, `nsfw = 0` mevcut ve sınıf/sürümle tutarlı.
- **Uyarı:** `tachiyomix.name`, `tachiyomix.contentWarning`, `tachiyomix.extensionLib` anahtarları standart Tachiyomi/Tachimanga meta-data adlandırması değil (`tachiyomi.extension.name` vb. beklenir). Görünen ad `android:label`'dan geldiği için işlevsel kırılma beklenmiyor; ancak bu anahtarlar büyük olasılıkla etkisiz (inert). Repodaki diğer modüllerle aynı şablonu izliyorsa tutarlılık adına korunabilir.

### 5) settings.gradle.kts ve tools/build_repo.py
- `settings.gradle.kts` içinde `include(":src:tr:merlinscans")` açıkça mevcut; önceki dört modülle birlikte listelenmiş.
- `tools/build_repo.py` içinde `EXTENSIONS_META["merlinscans"]` kaydı `name/lang/baseUrl/pkgSuffix/nsfw/class` alanlarıyla tam ve `build.gradle.kts` ile tutarlı (`https://merlinscans.org`, `...tr.merlinscans.MerlinScans`). Ancak verilen içerik **kısmi alıntı** (`... [DOSYANIN İLGİLİ BÖLÜMÜ] ...` ve baştaki `dout:` kırpıntısı); dosyanın tamamının sözdizimsel bütünlüğü bu kanıtla doğrulanamaz — yalnız ilgili kayıt bloğu doğrulanabilir.

### 6) İkonlar
- `res/mipmap-xxhdpi/ic_launcher.png` ikili dosya olarak **doğrulanmış** (32.186 bayt, gerçek PNG, placeholder değil). `docs/T-MERLINSCANS.md` beş yoğunluk (mdpi→xxxhdpi) ikonu iddia ediyor; ancak sunulan kanıtta yalnız xxhdpi dosyası gösterilmiş. Diğer dört dosyanın varlığı bu kanıt setiyle bağımsız doğrulanamıyor (çelişki yok, kanıt kapsamı dar).

### Sonuç
Ölçüt [0]'ın saydığı **tüm artefaktlar** (Kotlin kaynağı, Gradle betiği, manifest, en az bir doğrulanmış ikon, settings kaydı, build_repo meta kaydı) somut içerikle mevcut; sahte/placeholder kod yok ve temel API sözleşmeleri karşılanmış. Tespitler bloklayıcı değil; güvenlik (düz metin keystore parolası) ve kanıt bütünlüğü (kısmi `build_repo.py` alıntısı, tek yoğunluk ikon kanıtı) başlıkları iyileştirme önerisi olarak kayda geçiyor.

```json
{
  "auditor": "deepseek-kontrolcu",
  "overall_verdict": "PASS",
  "criteria": [
    {
      "index": 0,
      "verdict": "PASS",
      "finding": "Tüm artefaktlar mevcut ve içerikli: (1) MerlinScans.kt tam HttpSource uygulaması — WP REST (/wp-json/wp/v2/manga, _embed/per_page/page/orderby/order/search, X-WP-TotalPages + queryParameter('page') ile sayfalama), Jsoup DOM ayrıştırma (baseUri'li), data-referer desteği (pageListParse'te Page(pages.size, dataReferer, src), imageRequest'te page.url -> harici host origin -> baseUrl/ önceliğiyle Referer) ve site-logo filtresi; TODO/placeholder yok, imageUrlParse yalnız bilinçli UnsupportedOperationException. (2) build.gradle.kts: namespace/applicationId eu.kanade.tachiyomi.extension.tr.merlinscans, versionCode 140004, versionName 1.4.4, doğru sourceSets ve compileOnly bağımlılıklar. (3) AndroidManifest.xml: label 'Tachiyomi: Merlin Scans', class '.MerlinScans', version '1.4.4', nsfw 0, ikon referansı @mipmap/ic_launcher. (4) res/mipmap-xxhdpi/ic_launcher.png gerçek ikili dosya (32186 bayt). (5) settings.gradle.kts içinde include(':src:tr:merlinscans'). (6) tools/build_repo.py EXTENSIONS_META['merlinscans'] kaydı name/lang/baseUrl/pkgSuffix/nsfw/class alanlarıyla build.gradle ile tutarlı. Sınırlamalar: build_repo.py alıntısı kısmi, ikonlardan yalnız xxhdpi kanıtlanmış, keystore parolası düz metin."
    }
  ],
  "critical_issues": [],
  "recommendations": [
    "build.gradle.kts içindeki storePassword/keyPassword ('tempestpass') düz metin sır olarak duruyor; local.properties veya ortam değişkeninden okunmalı, signingkey.p12 repoya commit edilmemeli.",
    "signingkey.p12 yokken release signingConfig yine atanıyor; storeFile'sız release derlemesi başarısız olur — varlık kontrolüne göre koşullu atama yapılmalı.",
    "AndroidManifest.xml'deki 'tachiyomix.name', 'tachiyomix.contentWarning', 'tachiyomix.extensionLib' anahtarları standart dışı; işlevsel etkisi yok, 'tachiyomi.extension.*' şemasına hizalanmalı veya repodaki diğer modüllerle tutarlılık gerekçesi belgelenmeli.",
    "parseMangaJson'da link boşsa url boş string oluyor; boş link'te öğeyi atlayan koruma eklenmeli. Ayrıca thumbnail_url için boş string yerine null üretilmeli (takeIf { it.isNotEmpty() }).",
    "data-referer değeri mutlak URL olarak doğrulanmalı (toHttpUrlOrNull); göreli değer geçersiz Referer başlığına yol açar.",
    "imageRequest içinde baseUrl.toHttpUrl() her istekte yeniden hesaplanıyor; host değeri lazy sabite alınmalı.",
    "İkon setinin beş yoğunlukta (mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi) gerçekten var olduğu dosya boyutu/hash kanıtıyla doğrulanmalı; mevcut kanıt yalnız xxhdpi'yi kapsıyor.",
    "tools/build_repo.py'nin tamamı üzerinde sözdizimi kontrolü (python -m py_compile) ve merlinscans için version_code/version_name eşlemesinin doğrulanması önerilir; sunulan alıntı kısmi."
  ]
}
```
