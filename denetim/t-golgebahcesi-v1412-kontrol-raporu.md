# Orvant Çiftli Kontrolcü Raporu — `T-GOLGEBAHCESI-V1412 — Gölge Bahçesi son eklenenler sıralamasını ve DRM / WebView rotalarını düzelt (v1.4.12)`

- **Tarih:** `2026-10-20T22:18:37+03:00`
- **Çapraz Kontrol Kararı (Consensus):** **`WARN`** (GEÇTİ - ONAY)
- **Alt Ajan 1 (GLM Kontrolcü):** `WARN` · Model: `opencode/glm-5.3-flash-free` (`opencode-free`)
- **Alt Ajan 2 (DeepSeek Kontrolcü):** `WARN` · Model: `opencode/deepseek-v4.1-flash-free` (`opencode-free`)
- **İncelenen Dosyalar:** `src/tr/golgebahcesi/src/eu/kanade/tachiyomi/extension/tr/golgebahcesi/GolgeBahcesi.kt`, `src/tr/golgebahcesi/build.gradle.kts`, `src/tr/golgebahcesi/AndroidManifest.xml`, `docs/T-GOLGEBAHCESI-V1412.md`

## 1. Ölçüt Bazlı Çapraz Denetim Tablosu

| # | Kabul Ölçütü | GLM Kararı | DeepSeek Kararı | Ortak Hüküm | Bulgular (GLM & DeepSeek) |
|---|---|---|---|---|---|
| 0 | En son eklenenler sort=latest ile güncel bölümleri ve serileri listelemeli. | `PASS` | `PASS` | **`PASS`** | **GLM:** latestUpdatesRequest açıkça sort=latest kullanıyor (popular için sort=popular ile ayrışmış); parseSeriesList data+pagination ile hasNextPage'i currentPage<totalPages üzerinden güvenli hesaplıyor. Kod ölçütle birebir uyumlu; canlı davranış docs §2.A kaydına dayanıyor. <br> **DeepSeek:** GolgeBahcesi.kt latestUpdatesRequest içinde sort=latest kullanıyor; latestUpdatesParse -> parseSeriesList data ve pagination alanlarını okuyor. docs/T-GOLGEBAHCESI-V1412.md §2.A eski sort=updatedAt değerinin sort=latest ile değiştirildiğini doğruluyor. Canlı API yanıtı veya curl çıktısı denetim paketinde olmadığı için sunucu tarafındaki sıralama etkisi bağımsız doğrulanamadı. |
| 1 | Kötü Avcının Mesaisi, Savaş prensesi, Umine pansiyonu, Kalbimin frekansı, Ölümüne kazanç serileri ve tüm kilitli olmayan bölümler doğru webview/bölüm URL rotasına (/manga/<series>/bolum/<slug>) yönlendirilmeli, 404 hatası vermemeli. | `PASS` | `WARN` | **`WARN`** | **GLM:** Rota zinciri tutarlı: chapterListParse '/<series>/<slug>/<id>' saklıyor, getChapterUrl '$baseUrl/manga/<series>/bolum/<slug>' üretiyor, DRM exception'ındaki WebView URL'i aynı şablonu kullanıyor; /seri/ rotası koddan tamamen arındırılmış, isLocked filtresiyle kilitli bölümler eleniyor. 5 seri sayıları (6+49+30+115+14=214) docs §4 tablosuyla aritmetik uyumlu; 214/214 HTTP 200 kanıtı docs §4 kaydına dayanıyor, betik dosyası denetime sunulmadı. <br> **DeepSeek:** getMangaUrl /manga/<slug>, getChapterUrl /manga/<series>/bolum/<slug> üretiyor; chapterListParse isLocked bölümleri eliyor ve chapter.url değerini /<seriesSlug>/<chapterSlug>/<chapterId> olarak saklıyor. docs §3-§4 tablosunda 6+49+30+115+14=214 ücretsiz bölüm ve 30 kilitli bölüm aritmetiği tutarlı. Ancak chapterListParse seriesSlug alanını her chapter nesnesinden okuyor; API bu alanı döndürmezse continue ile tüm ücretsiz bölümler sessizce elenir. Ayrıca test_all_214_chapters.py ve ham HTTP 200 logları denetim paketinde yok; 214/214 beyanı bağımsız olarak yeniden üretilemiyor. |
| 2 | Android manifest ve build.gradle.kts v1.4.12 (140012) olarak sürüm yükseltilmeli ve kod derlenebilir olmalı. | `PASS` | `PASS` | **`PASS`** | **GLM:** build.gradle.kts: versionCode=140012, versionName=1.4.12; AndroidManifest: tachiyomi.extension.version=1.4.12; docs §1 ile üçlü tutarlı. Kullanılan API'ler extensions-lib 1.4.1/okhttp 4.12.0 ile uyumlu, import seti eksiksiz; tek girinti kusuru derlemeyi etkilemez. <br> **DeepSeek:** build.gradle.kts versionCode=140012 ve versionName=1.4.12; AndroidManifest.xml meta-data tachiyomi.extension.version=1.4.12. Görünen Kotlin kodunda HttpSource abstract üyelerinin tamamı uygulanmış, getMangaUrl/getChapterUrl imzaları ve importlar tutarlı; belirgin sözdizimi veya tip hatası yok. Ancak sağlanan .kt metni DOSYA KISALTILDI işareti ve tekrarlanan kuyruk içeriyor; bu nedenle tam dosyanın derlenebilirliği bu paketle bağımsız doğrulanamıyor. Orvant ext-golgebahcesi kaydındaki version_code=1 de 140012 ile senkron değil. |

## 2. GLM Kontrolcü (Mantık, Mimari & Ontoloji) Detaylı Çıktısı

## GLM Kontrolcü Teknik İncelemesi — T-GOLGEBAHCESI-V1412

**Kapsam:** Sunulan 4 dosya içeriği (GolgeBahcesi.kt, build.gradle.kts, AndroidManifest.xml, docs raporu) 3 kabul ölçütü üzerinden, araç çağrımı yapılmadan, salt-okunur bağımsız denetimle incelendi.

**Ölçüt [0] — sort=latest:** `latestUpdatesRequest` içinde `$apiBaseUrl/series` çağrısına `addQueryParameter("sort", "latest")` açıkça eklenmiş (GolgeBahcesi.kt, Latest Manga bloğu). `popularMangaRequest` ile ayrıştırılmış (`sort=popular`), `searchMangaRequest` ayrı `search` parametresi kullanıyor — üç istek tek `parseSeriesList` üzerinden `data` dizisi + `pagination.currentPage < totalPages` ile hasNextPage hesaplıyor. Sıfır/eksik `pagination` durumunda `false` fallback'i güvenli. Kod düzeyinde ölçüt tutarlı.

**Ölçüt [1] — WebView/bölüm rotası:** Rota zinciri tek doğrultuda:
- `getMangaUrl` → `"$baseUrl/manga/${manga.url}"` (dokümandaki 404 veren `/seri/` rotası koddan tamamen arındırılmış; tüm dosyada `seri/` geçişi yok).
- `chapterListParse`, `url = "/$seriesSlug/$chapterSlug/$chapterId"` formatında saklıyor; kilitli bölümler `isLocked` filtresiyle eleniyor (D-LOCKED-CHAPTERS).
- `getChapterUrl`, `trim('/').split('/')` ile `[seriesSlug, chapterSlug, chapterId]` ayrıştırıp `"$baseUrl/manga/$series/bolum/$slug"` döndürüyor — `/manga/<series>/bolum/<slug>` ölçütle birebir uyumlu.
- DRM dalındaki WebView URL'i (`pageListParse` exception metni) aynı rotayı `seriesSlug`/`chapterSlug` ile üretiyor; slug boşsa `baseUrl` fallback'i 404 dışı güvenli davranış.
- docs §4'teki 214/214 HTTP 200 tablosu, kodda üretilen URL şablonu ve 5 serinin serihatlarıyla (kilitli/bölüm sayıları: 6+49+30+115+14=214) aritmetik olarak tutarlı. Canlı doğrulamayı bu salt-okunur denetimde yeniden çalıştıramam; kanıt docs §4 kaydına dayanıyor, kod-şablon tutarlılığı ise doğrudan doğrulanabilir.

**Ölçüt [2] — Sürüm ve derlenebilirlik:** `build.gradle.kts` → `versionCode = 140012`, `versionName = "1.4.12"`; `AndroidManifest.xml` → `tachiyomi.extension.version = 1.4.12` — üç değer ve docs §1 (140012) tutarlı. Kotlin kodunda kullanılan tüm API'ler (`network.cloudflareClient`, `Page(index, url, imageUrl)`, `optDouble`, `toHttpUrl`, `JSONArray/JSONObject`) extensions-lib 1.4.1 + okhttp 4.12.0 bildirimleriyle uyumlu; import listesi eksiksiz; `pageListParse` içindeki tek satırlık girinti bozukluğu (`val json = ...`) derlemeyi etkilemez (Kotlin girinti-duyarsız). `imageUrlParse` стандарт `UnsupportedOperationException`. Derlenebilirlik kod düzeyinde engel içermiyor.

**Tespit Edilen Uç Durumlar (kritik değil, öneri düzeyi):**
1. **Karışık DRM:** `openPages.isNotEmpty()` → açık sayfalar döner, `.enc` sayfaları sessizce düşer. Kısmi şifreli bir bölümde sayfa kaybı kullanıcıya sinyalsiz olur.
2. **Boş pages dizisi:** `openPages` boş **ve** `encCount == 0` (API'nin sayfa dizisini hiç döndürmediği Turnstile-gated durum) → `emptyList()` dönüyor, kullanıcıya WebView ipucu verilmiyor; DRM yönlendirmesi bu dalı kapsamıyor.
3. 214/214 kanıtı için `test_all_214_chapters.py` betiğinin kendisi ve ham HTTP yanıtları docs'a dahil edilmemiş; yeniden üretilebilirlik bağımsız denetim için zayıf halka.

Bu bulgular üç ölçütü de geçersiz kılmıyor; kritik mantık hatası, yarış durumu veya ontoloji ihlali (`input_ids`/`output_ids` sınırları görev tanımıyla uyumlu, tek girdi nesnesi `site-golgebahcesi` ile çalışma alanı doğru) saptanmadı.

```json
{
  "auditor": "glm-kontrolcu",
  "overall_verdict": "WARN",
  "criteria": [
    {
      "index": 0,
      "verdict": "PASS",
      "finding": "latestUpdatesRequest açıkça sort=latest kullanıyor (popular için sort=popular ile ayrışmış); parseSeriesList data+pagination ile hasNextPage'i currentPage<totalPages üzerinden güvenli hesaplıyor. Kod ölçütle birebir uyumlu; canlı davranış docs §2.A kaydına dayanıyor."
    },
    {
      "index": 1,
      "verdict": "PASS",
      "finding": "Rota zinciri tutarlı: chapterListParse '/<series>/<slug>/<id>' saklıyor, getChapterUrl '$baseUrl/manga/<series>/bolum/<slug>' üretiyor, DRM exception'ındaki WebView URL'i aynı şablonu kullanıyor; /seri/ rotası koddan tamamen arındırılmış, isLocked filtresiyle kilitli bölümler eleniyor. 5 seri sayıları (6+49+30+115+14=214) docs §4 tablosuyla aritmetik uyumlu; 214/214 HTTP 200 kanıtı docs §4 kaydına dayanıyor, betik dosyası denetime sunulmadı."
    },
    {
      "index": 2,
      "verdict": "PASS",
      "finding": "build.gradle.kts: versionCode=140012, versionName=1.4.12; AndroidManifest: tachiyomi.extension.version=1.4.12; docs §1 ile üçlü tutarlı. Kullanılan API'ler extensions-lib 1.4.1/okhttp 4.12.0 ile uyumlu, import seti eksiksiz; tek girinti kusuru derlemeyi etkilemez."
    }
  ],
  "critical_issues": [],
  "recommendations": [
    "Karışık DRM uç durumu: bir bölümde hem açık hem .enc sayfa varsa openPages kısmi dönüyor ve şifreli sayfalar sessizce düşüyor; encCount>0 + openPages.isNotEmpty kombinasyonunda kullanıcı uyarısı veya tam DRM yönlendirmesi düşünülebilir.",
    "pages dizisi tamamen boş/eksik geldiğinde (openPages boş ve encCount==0) emptyList() dönülüyor; Turnstile-gated DRM durumlarında kullanıcı 'no pages' görür, WebView ipucu bu dalı da kapsayacak şekilde genişletilebilir.",
    "test_all_214_chapters.py betiği ve ham HTTP yanıt kanıtı docs/T-GOLGEBAHCESI-V1412.md içine eklenerek 214/214 doğrulamanın bağımsız yeniden üretilebilirliği sağlansın.",
    "chapter_number fallback'i 0.0 olan bölümler okuyucu sıralamasını bozabilir; number alanı eksikse slug/title'dan çıkarım veya varsayılan alternatif değerlendirilebilir.",
    "Küçük kod hijyeni: pageListParse içindeki 'val json = ...' satırının girinti düzeltmesi ve number→slug fallback mantığındaki (chapterListParse içindeki çift hesaplama) tekrarın tek yardımcı fonksiyona alınması."
  ]
}
```

## 3. DeepSeek Kontrolcü (Kod, Güvenlik & Somut Kanıt) Detaylı Çıktısı

## Teknik İnceleme

İnceleme yalnızca sağlanan dosya metinleri üzerinden yapıldı; canlı HTTP, derleme veya test betiği çalıştırılmadı.

### [0] En son eklenenler / `sort=latest`
- `GolgeBahcesi.kt` içindeki `latestUpdatesRequest`, `/api/series` çağrısına `sort=latest` ekliyor.
- `latestUpdatesParse` → `parseSeriesList`; `data` dizisi ve `pagination.currentPage/totalPages` alanları okunuyor.
- `docs/T-GOLGEBAHCESI-V1412.md` §2.A, eski `sort=updatedAt` değerinin `sort=latest` ile değiştirildiğini belirtiyor.
- Kod ölçütü karşılıyor. Ancak canlı API yanıtı/curl çıktısı denetim paketinde yok; `sort=latest` parametresinin sunucu tarafındaki gerçek sıralama etkisi bağımsız doğrulanamadı.

### [1] Bölüm/webview rotaları ve DRM
- `getMangaUrl` → `$baseUrl/manga/${manga.url}`.
- `chapterListParse` → `isLocked` olan bölümler eleniyor; `chapter.url = "/$seriesSlug/$chapterSlug/$chapterId"` saklanıyor.
- `getChapterUrl` → `/manga/<series>/bolum/<slug>` üretiyor; `pageListParse` içindeki DRM mesajı da aynı rotayı kullanıyor.
- `docs` §3–§4 tablosundaki 5 seri ve `6+49+30+115+14=214` ücretsiz bölüm aritmetiği tutarlı; 30 kilitli bölüm de toplamla uyumlu.
- Ancak iki önemli risk var:
  1. `seriesSlug`, her chapter nesnesinden `ch.optString("seriesSlug")` ile okunuyor. API bu alanı döndürmezse `if (chapterId.isBlank() || seriesSlug.isBlank()) continue` satırı **tüm ücretsiz bölümleri sessizce eler**. `chapterListRequest(manga)` zaten `manga.url` üzerinden seri slug’ını biliyor; buradan fallback verilmesi gerekir.
  2. `test_all_214_chapters.py` ve ham HTTP 200 logları denetim paketinde yok. §4 tablosu kendi kendine raporlanmış durumda; testlerin uzantının gerçek `chapterListParse` çıktısını mı yoksa elle kurulmuş URL’leri mi doğruladığı belirsiz.
- Ayrıca karışık açık + `.enc` sayfalarda yalnızca açık sayfalar dönüyor; bölüm sessizce eksik gösterilebilir.

### [2] Sürüm ve derlenebilirlik
- `build.gradle.kts`: `versionCode = 140012`, `versionName = "1.4.12"`.
- `AndroidManifest.xml`: `tachiyomi.extension.version = "1.4.12"`.
- Görünen Kotlin kodunda `HttpSource`’un tüm abstract üyeleri uygulanmış; `getMangaUrl`/`getChapterUrl` imzaları ve importlar tutarlı; belirgin sözdizimi/tip hatası yok.
- Ancak sağlanan `.kt` metni `[DOSYA KISALTILDI - TAMAMI PROJE DİZİNİNDE]` işareti ve tekrarlanan bir kuyruk içeriyor. “Tam dosya içerikleri kesintisiz sunulmuştur” iddiası bu paketle çelişiyor; tam dosyanın derlenebilirliği bağımsız doğrulanamıyor.
- Orvant `ext-golgebahcesi` kaydındaki `version_code` hâlâ `1`; `build.gradle.kts` ile senkron değil.

### Güvenlik ve dayanıklılık notları
- `build.gradle.kts` içinde `storePassword`/`keyPassword` düz metin. Tachiyomi ekosisteminde standart test anahtarı olsa da sır olarak işaretlenmeli; CI/env veya repo dışı keystore tercih edilmeli.
- `JSONObject(response.body?.string().orEmpty())` boş/geçersiz gövdede `JSONException` fırlatabilir.
- API’den gelen slug’lar path segmenti olarak encode edilmiyor; göreli kapak URL’leri mutlaklaştırılmıyor.

### Sonuç
Kod, [0] ve [2]’nin dosya düzeyindeki gereksinimlerini karşılıyor; [1]’in rota mantığı doğru. Ancak 214/214 kanıtı ve tam dosya sunumu eksik, ayrıca `seriesSlug` bağımlılığı nedeniyle bölüm listesinin boş kalma riski var. Genel değerlendirme **WARN**.

```json
{
  "auditor": "deepseek-kontrolcu",
  "overall_verdict": "WARN",
  "criteria": [
    {
      "index": 0,
      "verdict": "PASS",
      "finding": "GolgeBahcesi.kt latestUpdatesRequest içinde sort=latest kullanıyor; latestUpdatesParse -> parseSeriesList data ve pagination alanlarını okuyor. docs/T-GOLGEBAHCESI-V1412.md §2.A eski sort=updatedAt değerinin sort=latest ile değiştirildiğini doğruluyor. Canlı API yanıtı veya curl çıktısı denetim paketinde olmadığı için sunucu tarafındaki sıralama etkisi bağımsız doğrulanamadı."
    },
    {
      "index": 1,
      "verdict": "WARN",
      "finding": "getMangaUrl /manga/<slug>, getChapterUrl /manga/<series>/bolum/<slug> üretiyor; chapterListParse isLocked bölümleri eliyor ve chapter.url değerini /<seriesSlug>/<chapterSlug>/<chapterId> olarak saklıyor. docs §3-§4 tablosunda 6+49+30+115+14=214 ücretsiz bölüm ve 30 kilitli bölüm aritmetiği tutarlı. Ancak chapterListParse seriesSlug alanını her chapter nesnesinden okuyor; API bu alanı döndürmezse continue ile tüm ücretsiz bölümler sessizce elenir. Ayrıca test_all_214_chapters.py ve ham HTTP 200 logları denetim paketinde yok; 214/214 beyanı bağımsız olarak yeniden üretilemiyor."
    },
    {
      "index": 2,
      "verdict": "PASS",
      "finding": "build.gradle.kts versionCode=140012 ve versionName=1.4.12; AndroidManifest.xml meta-data tachiyomi.extension.version=1.4.12. Görünen Kotlin kodunda HttpSource abstract üyelerinin tamamı uygulanmış, getMangaUrl/getChapterUrl imzaları ve importlar tutarlı; belirgin sözdizimi veya tip hatası yok. Ancak sağlanan .kt metni DOSYA KISALTILDI işareti ve tekrarlanan kuyruk içeriyor; bu nedenle tam dosyanın derlenebilirliği bu paketle bağımsız doğrulanamıyor. Orvant ext-golgebahcesi kaydındaki version_code=1 de 140012 ile senkron değil."
    }
  ],
  "critical_issues": [],
  "recommendations": [
    "chapterListParse içinde seriesSlug için manga.url tabanlı bir fallback ekleyin; aksi halde chapter API'sinde seriesSlug yoksa kilitli olmayan tüm bölümler sessizce kaybolur.",
    "test_all_214_chapters.py dosyasını ve ham HTTP durum kodlarını (214/214) denetim/teslim paketine ekleyin; yalnızca özet tablo bağımsız kanıt için yeterli değil.",
    "GolgeBahcesi.kt dosyasının kesintisiz tam sürümünü ve CI derleme logunu ekleyin; mevcut metin kesinti işareti ve tekrarlanan blok içeriyor.",
    "Karışık açık/.enc sayfa durumunda yalnızca açık sayfaları döndürmek yerine bölümün eksik olduğunu belirten açık bir hata üretin.",
    "JSONObject(response.body?.string().orEmpty()) çağrılarını boş veya geçersiz gövde için koruyun; şu haliyle boş yanıt JSONException fırlatır.",
    "build.gradle.kts içindeki storePassword/keyPassword değerlerini CI ortam değişkenlerine veya repo dışı keystore yapılandırmasına taşıyın.",
    "API'den gelen slug ve göreli kapak URL'lerini path segmenti olarak encode edin ve kapak URL'lerini mutlaklaştırın.",
    "Orvant ext-golgebahcesi version_code kaydını 140012 olarak güncelleyin ve docs kanıtını output_ids/evidence olarak kaydedin."
  ]
}
```
