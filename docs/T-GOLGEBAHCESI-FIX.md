# T-GOLGEBAHCESI-FIX — Gölge Bahçesi REA Analizi ve Düzeltmeler

## Görev Özeti

Gölge Bahçesi (`golgebahcesi.com`) kaynağındaki bölüm açılamama ve yanlış
site URL'leri sorunları analiz edildi, kök nedenler REA (reverse engineering
analysis) ile tespit edildi ve uzantı kodu düzeltildi.

## REA Analizi

### Site mimarisi

- Site, içerikleri `https://api.golgebahcesi.com/api` üzerinden servis eden bir
  SPA + REST API yapısı kullanıyor. Sırası XHR ile çarpılan uçlar:
  - `/api/series?page=&limit=&sort=` (liste; `popular`, `updatedAt`, `search` sorguları)
  - `/api/series/<slug>` (dizilim detayı)
  - `/api/series/<slug>/chapters` (bölüm listesi)
  - `/api/chapters/<chapterId>` (bölüm sayfa listesi)

### Bölüm URL rotası

- Sitenin okuyucu rotası `/seri/<seriesSlug>/bolum-<num>` biçimindedir.
- Eski uzantı `WebView`/`getChapterUrl` için `/manga/<explorer>/bolum/<slug>`
  üretiyordu; bu rota site tarafından 404/yanlış yönlendiriliyordu.
- Bölüm numarası site rotasında tam sayılar için **noktasız** gösteriliyor
  (`bolum-12`), `12.5` gibi kesirli bölümler `bolum-12.5` olarak geçiyor.

### WASM DRM zinciri (`_kalinka` + `idec.bin`)

- Site, yöneticisi/koruma katmanı olarak bir **Turnstile benzeri köstebek
  analizi** ve sayfa görüntülerini istemci taraflı çözebilmek için bir WASM
  şifreleme katmanı kullanıyor.
- REA bulguları:
  - `_kalinka` modülü, tarayıcı parmak izi / istemci doğrulama denetimlerini
    yürüten bir denetim katmanıdır; tarayıcından gelen isteme,
    `X-Client-Check` başlığıyla bir durumu (`bot=false`, `evt=1`, `v=1`)
    bildiren `eyJib3RkIjpmYWxzZSwiZXZ0IjoxLCJ2IjoxfQ==` içeriği iletir.
  - Şifreli sayfalar CDN kalıcısı **`c2.skycdn.online`** üzerinden
    `*.enc` dosyaları olarak sunulmaktadır.
  - `idec.bin` adlı WASM modülü sayfa yükleme sırasında runtimede
    **ChaCha20** anahtarını çıkarıp galeriyi çözmektedir; istemcideki
    `canvas` görüntüsüne, birlikte galeri sekmesinde görünür hale gelmektedir.
- Sonuç: `<id>` ucu (`/api/chapters/<id>`) tamamen WASM katmanı ile
  korunan bölümde **boş `url` değerlerine veya `*.enc` dosyalarına sahip** bir
  `pages` dizisi dönmektedir. Zaten istemci için çözülen **kişisel anahtar
  uzantıda statik olarak yeniden üretilemez** — sayfa görüntüsü bundan başka
  kaynaktan doğrudan okunamaz.

## Yapılan Düzeltmeler

1. **`headersBuilder`** — API isteminin reddedilmemesi için `Origin`
   ve `Accept: application/json, text/plain, */*` başlıkları eklendi.
2. **`getMangaUrl`** — Mangaların site adresi `/manga/<slug>` yerine
   doğru rota olan **`/seri/<slug>`** olarak düzeltildi.
3. **`chapterListParse`** — Bölüm numarası tam sayıysa noktasız
   (`bolum-12`) formata çevrildi; `SChapter.url` artık
   `/<seriesSlug>/bolum-<numStr>/<chapterId>` biçiminde saklanıyor.
   `getChapterUrl` bu bilgilere dayanarak `/seri/<seriesSlug>/bolum-<num>`
   adresini üretiyor.
4. **`pageListParse`** —
   - Ana `/api/chapters/<id>` yanıtında açık sayfa varsa doğrudan
     döndürülüyor.
   - Açık sayfa yoksa (tüm boş veya `*.enc`), yedek olarak
     `/api/chapters/<seriesSlug>/chapter-<numStr>/manifest` ucu,
     `X-Client-Check: eyJib3RkIjpmYWxzZSwiZXZ0IjoxLCJ2IjoxfQ==` başlığıyla
     isteniyor; oradaki açık sayfa URL'leri devreye giriyor.
   - Bölüm tamamen WASM şifreliyse, daha önce döndürülen sahte
     `Page(0, chapterUrl, "")` sınırından **iyileştirilerek** (`imageUrlParse`
     `Not used.` istisnasına yıkılma) okunabilir bir hata iletisi fırlatılıyor:
     "Bu bölüm WASM şifrelemesi (DRM) kullanıyor. Sağ üstten WebView ile
     açarak okuyabilirsiniz." — Kullanıcı bu durumda bölümü webView'da
     açabilir, orada `idec.bin` ChaCha20 çözücü devrede olup bölümü
     görüntüleyebilir.
5. **Sürüm** — `versionCode = 140011`, `versionName = "1.4.11"`;
   `AndroidManifest.xml`'de `tachiyomi.extension.version = 1.4.11`.

## Bilinen Kısıtlar

- WASM şifreli bölümde sayfa görüntülerini uzantı içinde doğrudan
  göstermek, `idec.bin` ChaCha20 anahtarının kısa ömürlü ve istemci
  durumuna (`_kalinka`) bağlı olması nedeniyle mümkün değildir;
  bu tür bölümler için kullanıcı **WebView modunu** kullanmalıdır.
- Manifest (anlık) yedek ucu Orta katman yapı değişikliğine
  açık olabilir; başarısızsa hata iletisi WebView yönlendirmesine
  geri düşmektedir.
