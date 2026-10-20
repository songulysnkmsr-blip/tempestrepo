# Orvant Çiftli Kontrolcü Raporu — `T-GOLGEBAHCESI-FIX — Gölge Bahçesi şifreli bölüm çökme ve URL yönlendirme sorunlarını gider`

- **Tarih:** `2026-10-20T20:47:48+03:00`
- **Çapraz Kontrol Kararı (Consensus):** **`PASS`** (GEÇTİ - ONAY)
- **Alt Ajan 1 (GLM Kontrolcü):** `PASS` · Model: `opencode/glm-5.3-flash-free` (`opencode-free`)
- **Alt Ajan 2 (DeepSeek Kontrolcü):** `PASS` · Model: `opencode/deepseek-v4.1-flash-free` (`opencode-free`)
- **İncelenen Dosyalar:** `src/tr/golgebahcesi/src/eu/kanade/tachiyomi/extension/tr/golgebahcesi/GolgeBahcesi.kt`, `src/tr/golgebahcesi/build.gradle.kts`, `src/tr/golgebahcesi/AndroidManifest.xml`, `docs/T-GOLGEBAHCESI-FIX.md`

## 1. Ölçüt Bazlı Çapraz Denetim Tablosu

| # | Kabul Ölçütü | GLM Kararı | DeepSeek Kararı | Ortak Hüküm | Bulgular (GLM & DeepSeek) |
|---|---|---|---|---|---|
| 0 | GolgeBahcesi.kt içinde şifreli (.enc) bölümlerde imageUrlParse UnsupportedOperationException çökmesi giderilip açıklayıcı hata mesajı, manifest yedek ucu ve doğru WebView URL yapısı (/seri/<slug>/bolum-<num>) sağlandı, Origin/Accept başlıkları eklendi ve sürüm 1.4.11'e (140011) yükseltildi. | `PASS` | `PASS` | **`PASS`** | **GLM:** [0] Tam karşılandı: (1) pageListParse, boş URL ve *.enc sayfaları encCount ile izleyip açık sayfa yoksa imageUrlParse'a ulaşmadan (satır: throw Exception "Bu bölüm WASM şifrelemesi (DRM) kullanıyor...") okunabilir hata fırlatıyor; (2) manifest yedek ucu /api/chapters/<seriesSlug>/chapter-<numStr>/manifest, X-Client-Check başlığıyla ve try/catch + use{} bloğunda çağrılıyor, başarısızlıkta WebView hata mesajına düşüyor; (3) WebView/reader URL yapısı /seri/<slug>/bolum-<num> doğru: getMangaUrl (+getChapterUrl, pageListParse webViewUrl) noktasız tam sayı ve noktalı kesir (12.5) formunu kotlin %1.0==0 kontrolleriyle üretiyor; (4) headersBuilder'da Origin: https://golgebahcesi.com ve Accept: application/json, text/plain, */* mevcut; (5) build.gradle.kts versionCode=140011 / versionName=1.4.11 ile AndroidManifest.xml tachiyomi.extension.version=1.4.11 tutarlı. <br> **DeepSeek:** GolgeBahcesi.kt: (a) .enc/boş URL'ler parsePagesArray içinde encCount'a ayrılıp Page üretilmiyor, eski Page(0, chapterUrl, "") deseni kaldırıldığı için imageUrlParse UnsupportedOperationException yolu erişilemez; açık sayfa yoksa 'Bu bölüm WASM şifrelemesi (DRM) kullanıyor. Sağ üstten WebView ile açarak okuyabilirsiniz: <url>' mesajı fırlatılıyor. (b) Manifest yedeği GET("$apiBaseUrl/chapters/$seriesSlug/chapter-$numStr/manifest") + X-Client-Check başlığı, isSuccessful kontrolü ve try/catch ile mevcut. (c) WebView URL'si "$baseUrl/seri/$seriesSlug/bolum-$numStr" (tam sayıda noktasız); getChapterUrl de /seri/<slug>/bolum-<num> üretiyor, pageListRequest son segmentten chapterId alıyor. (d) headersBuilder'da Origin=baseUrl ve Accept=application/json, text/plain, */* eklendi (Referer dahil), manifest isteği bunları miras alıyor. (e) build.gradle.kts versionCode=140011 / versionName=1.4.11 ve AndroidManifest tachiyomi.extension.version=1.4.11 ile üç yerde tutarlı. Kod eksiksiz, placeholder yok. |

## 2. GLM Kontrolcü (Mantık, Mimari & Ontoloji) Detaylı Çıktısı

## Teknik İnceleme — T-GOLGEBAHCESI-FIX

**Ölçüt [0] alt bileşenlerini tek tek doğruladım:**

1. **Şifreli (.enc) bölüm çökmesinin giderilmesi:** `pageListParse` içinde `parsePagesArray` boş URL'lerin ve `.enc` ile biten URL'lerin sayısını `encCount` ile takip ediyor; açık sayfa yoksa artık `Page(0, chapterUrl, "")` gibi sahte bir Page üretilmiyor (öyle olsaydı `imageUrlParse`'taki `UnsupportedOperationException("Not used.")` çökmeye yol açardı). Bunun yerine `encCount > 0` durumunda WebView yönlendirmeli Türkçe hata mesajı fırlatılıyor. `imageUrlParse` yalnızca standart "Not used." korumas olarak kalıyor ve bu akışta hiç çağrılmıyor. ✓

2. **Manifest yedek ucu:** `$apiBaseUrl/chapters/$seriesSlug/chapter-$numStr/manifest` ucu, `X-Client-Check: eyJib3RkIjpmYWxzZSwiZXZ0IjoxLCIyIjo...` (bot=false/evt=1/v=1) başlığıyla çağrılıyor; `try/catch` ve `use { }` ile kaynak sızıntısı yok; manifest başarısızsa veya manifest sayfaları da `.enc`/boşsa mevcut düşüş (fall-through) doğru biçimde WebView hata mesajına gidiyor. `seriesSlug` boşsa yedek denemesi koşullu olarak atlanıyor (gerekli koruma). ✓

3. **Doğru WebView URL yapısı:** `getMangaUrl` → `/seri/<slug>`; `chapterListParse` tam sayıları noktasız fmt (`bolum-12`) yazıyor, `12.5` gibi kesirleri `bolum-12.5` olarak bırakıyor (`numObj % 1.0 == 0.0` denetimi doğru; `Double.toString` Kotlin/Java'da her zaman `.` kullanır, yerellik sorun değildir). `SChapter.url` = `/<seriesSlug>/bolum-<numStr>/<chapterId>` ve `getChapterUrl` bunu `/seri/<seriesSlug>/bolum-<num>` karşılığına dönüştürüyor; 1'den az segment varsa `baseUrl` düşüşü var. `pageListParse` içindeki hata mesajındaki WebView URL'i de aynı `/seri/$seriesSlug/bolum-$numStr` yapısını kullanıyor (yoksa `baseUrl`). ✓

4. **Origin/Accept başlıkları:** `headersBuilder`'da `.add("Origin", baseUrl)` ve `.add("Accept", "application/json, text/plain, */*")` mevcut. ✓

5. **Sürüm yükseltmesi:** `build.gradle.kts` → `versionCode = 140011`, `versionName = "1.4.11"`; `AndroidManifest.xml` → `tachiyomi.extension.version = 1.4.11`. Üç kayıt tutarlı. ✓

**Küçük uç durum notları (ölçüt kırıcı değil):**
- `optDouble("number", 0.0)` sayı alanı yok/kirli ise `bolum-0` üretir ve manifest isteği zararsızca başarısız olur — kabul edilebilir düşüş.
- `Double.toString` çok büyük (≥1e7) sayılarda bilimsel gösterim verir; pratikte bölüm numaraları küçük olduğundan etkisiz.
- `pageListParse`'ta yanıt gövdesi JSON'a ayrıştırılemazsa (boş/bozuk) genel JSONException fırlar; bu, söz konusu çökme (`imageUrlParse`) ölçütünü etkilemez ama mesajda `encCount`-tabanlı senaryodaki okunabilir istisnadan bağımsız olarak külçe (noise) olur.
- `client.newCall(...).execute()` senkron ikinci ağ çağrısı `pageListParse` içinde yapılıyor; Tachiyomi/HttpSource bellek modelinde yaygın kabul gören bir kalıptır ancak zaman aşımını uzatır.

```json
{
  "auditor": "glm-kontrolcu",
  "overall_verdict": "PASS",
  "criteria": [
    {
      "index": 0,
      "verdict": "PASS",
      "finding": "[0] Tam karşılandı: (1) pageListParse, boş URL ve *.enc sayfaları encCount ile izleyip açık sayfa yoksa imageUrlParse'a ulaşmadan (satır: throw Exception \"Bu bölüm WASM şifrelemesi (DRM) kullanıyor...\") okunabilir hata fırlatıyor; (2) manifest yedek ucu /api/chapters/<seriesSlug>/chapter-<numStr>/manifest, X-Client-Check başlığıyla ve try/catch + use{} bloğunda çağrılıyor, başarısızlıkta WebView hata mesajına düşüyor; (3) WebView/reader URL yapısı /seri/<slug>/bolum-<num> doğru: getMangaUrl (+getChapterUrl, pageListParse webViewUrl) noktasız tam sayı ve noktalı kesir (12.5) formunu kotlin %1.0==0 kontrolleriyle üretiyor; (4) headersBuilder'da Origin: https://golgebahcesi.com ve Accept: application/json, text/plain, */* mevcut; (5) build.gradle.kts versionCode=140011 / versionName=1.4.11 ile AndroidManifest.xml tachiyomi.extension.version=1.4.11 tutarlı."
    }
  ],
  "critical_issues": [],
  "recommendations": [
    "optDouble(\"number\") kirlilik/girilmiş data durumunda bolum-0 ürettebilir; manifest isteği yapılacaksa numStr.isBlank() guard'ına ek olarak numObj > 0 koşulu da eklenebilir.",
    "pageListParse'ta JSON gövdesi ayrıştırılamazsa genel JSONException fırlıyor; okunabilir hata istisnasını yakalamak ve aynı WebView yönlendirme mesajı standartlaştırmak UX için güzel olurdu (ölçüt kırıcı değil).",
    "Hata mesajındaki 'Sağ üstten WebView' ibaresi istemciye özgü UI diline dayanıyor; farklı okuyucu çatallarında konum değişebilir, düşük öncelikli not."
  ]
}
```

## 3. DeepSeek Kontrolcü (Kod, Güvenlik & Somut Kanıt) Detaylı Çıktısı

## Teknik İnceleme — T-GOLGEBAHCESI-FIX

**Kod bütünlüğü:** `GolgeBahcesi.kt` eksiksiz, derlenebilir görünümde. Kullanılan tüm import'lar (`GET`, `toHttpUrl`, `Headers`, `JSONArray/JSONObject`, `SimpleDateFormat`, `TimeUnit` vb.) gerçekten referanslanıyor; yarım bırakılmış fonksiyon, TODO veya sahte dönüş yok. `headersBuilder()` imzası (`Headers.Builder`), `getMangaUrl`/`getChapterUrl`/`imageUrlParse` override'ları extensions-lib 1.4.1 sözleşmesiyle uyumlu.

**Ölçüt [0] alt kalemleri:**

1. **`.enc` çökme yolu:** `pageListParse` içinde `parsePagesArray` boş `url` ve `.enc` uzantılı girdileri `encCount` sayacına aktarıp `Page` üretmiyor; böylece eski `Page(0, chapterUrl, "")` desenindeki boş `imageUrl` → `imageUrlParse` → `UnsupportedOperationException` zinciri artık kurulamıyor. Açık sayfa yoksa `throw Exception("Bu bölüm WASM şifrelemesi (DRM) kullanıyor...")` ile **açıklayıcı** mesaj ve WebView adresi veriliyor. `imageUrlParse`'teki `"Not used."` istisnası ölü yol olarak kalmış (zararsız, standart desen).
2. **Manifest yedek ucu:** `"$apiBaseUrl/chapters/$seriesSlug/chapter-$numStr/manifest"` + `X-Client-Check: eyJib3RkIjpmYWxzZSwiZXZ0IjoxLCJ2IjoxfQ==` başlığı ile `try/catch` sarmalı ve `isSuccessful` kontrolü mevcut; hata durumunda aşağıdaki WebView mesajına düşülüyor (sessiz yutma yok).
3. **WebView URL yapısı:** `"$baseUrl/seri/$seriesSlug/bolum-$numStr"` — tam sayı bölümler `%1.0 == 0.0` kontrolüyle noktasız (`bolum-12`), kesirler `12.5` olarak üretiliyor. `getChapterUrl` de `/<slug>/bolum-<num>/<id>` ayrıştırmasıyla `/seri/<slug>/bolum-<num>` döndürüyor; `pageListRequest` son segmentten `chapterId` alıyor. Tutarlı.
4. **Origin/Accept:** `headersBuilder` içinde `Origin: baseUrl`, `Accept: application/json, text/plain, */*`, ayrıca `Referer` var; manifest isteği `headers.newBuilder()` ile bunları miras alıyor.
5. **Sürüm:** `versionCode = 140011`, `versionName = "1.4.11"`, `AndroidManifest.xml` içinde `tachiyomi.extension.version = 1.4.11` — üçü birbiriyle tutarlı.

**Tespit edilen riskler (ölçüt dışı / bloklayıcı değil):**

- `seriesSlug`, `/api/chapters/<id>` yanıtından `optString` ile alınıyor; API bu alanı döndürmezse `webViewUrl` sessizce `baseUrl`'e düşüyor ve kullanıcı bölüme değil ana sayfaya yönlendiriliyor. Slug zaten `SChapter.url` içinde mevcut olduğundan `pageListRequest`/`pageListParse` çiftine taşınabilir.
- Eski sürümden kütüphanede kalan `SChapter.url` kayıtları (örn. `/manga/<slug>/bolum/<id>`) için `getChapterUrl` migrasyon koruması yok; `parts[1]` "bolum-" ile başlamıyorsa `/seri/<slug>/bolum` gibi eksik URL üretilir.
- `JSONObject(response.body?.string().orEmpty())` ve `JSONArray(...)` çağrıları boş/bozuk gövdede `JSONException` fırlatır; `popular/latest/search/details/chapterList` yollarında savunma yok.
- Manifest ucu ve `X-Client-Check` sabiti yalnız REA dokümanına dayanıyor; derleme (`assembleRelease`) veya canlı uç doğrulama çıktısı paylaşılmamış. Kabul ölçütü kod düzeyinde karşılanıyor, uç düzeyinde kanıt yok.
- `build.gradle.kts` içindeki `"tempestpass"` parolası şablonun bilinen varsayılanı ve anahtar dosyası repoda değil; gerçek sır sızıntısı sayılmaz, yine de CI'da ortam değişkenine taşınması önerilir.

**Sonuç:** Ölçüt [0]'ın tüm alt gereksinimleri kodda somut olarak mevcut; placeholder/yarım kod yok, çökme yolu kapatılmış. PASS.

```json
{
  "auditor": "deepseek-kontrolcu",
  "overall_verdict": "PASS",
  "criteria": [
    {
      "index": 0,
      "verdict": "PASS",
      "finding": "GolgeBahcesi.kt: (a) .enc/boş URL'ler parsePagesArray içinde encCount'a ayrılıp Page üretilmiyor, eski Page(0, chapterUrl, \"\") deseni kaldırıldığı için imageUrlParse UnsupportedOperationException yolu erişilemez; açık sayfa yoksa 'Bu bölüm WASM şifrelemesi (DRM) kullanıyor. Sağ üstten WebView ile açarak okuyabilirsiniz: <url>' mesajı fırlatılıyor. (b) Manifest yedeği GET(\"$apiBaseUrl/chapters/$seriesSlug/chapter-$numStr/manifest\") + X-Client-Check başlığı, isSuccessful kontrolü ve try/catch ile mevcut. (c) WebView URL'si \"$baseUrl/seri/$seriesSlug/bolum-$numStr\" (tam sayıda noktasız); getChapterUrl de /seri/<slug>/bolum-<num> üretiyor, pageListRequest son segmentten chapterId alıyor. (d) headersBuilder'da Origin=baseUrl ve Accept=application/json, text/plain, */* eklendi (Referer dahil), manifest isteği bunları miras alıyor. (e) build.gradle.kts versionCode=140011 / versionName=1.4.11 ve AndroidManifest tachiyomi.extension.version=1.4.11 ile üç yerde tutarlı. Kod eksiksiz, placeholder yok."
    }
  ],
  "critical_issues": [],
  "recommendations": [
    "seriesSlug API yanıtında yoksa webViewUrl baseUrl'e düşüyor; slug'ı SChapter.url'den türetip pageListParse'e taşıyarak hata mesajındaki yönlendirmeyi garantileyin.",
    "Eski sürümden kalan SChapter.url formatları için getChapterUrl'de 'bolum-' önek kontrolü ve migrasyon fallback'i ekleyin.",
    "JSONObject/JSONArray ayrıştırmalarını boş gövde ve JSONException için try/catch veya optJSONObject ile koruyun.",
    "Manifest ucu ve X-Client-Check sabitinin gerçekten çalıştığını canlı istek veya en azından assembleRelease çıktısıyla belgeleyin; şu an kanıt yalnız REA dokümanı.",
    "imageUrlParse'teki kullanılmayan 'Not used.' override'ı kaldırılabilir veya neden ölü olduğu yorumla belirtilebilir.",
    "Signing parolasını CI ortam değişkenine taşıyın (şablon varsayılanı olsa da repoda düz metin duruyor)."
  ]
}
```
