# T-GOLGEBAHCESI-V1412 Denetim ve Kanıt Raporu

## 1. Görev Özeti ve Hedefler
- **Görev Kimliği:** `T-GOLGEBAHCESI-V1412`
- **Hedef:** Gölge Bahçesi (`golgebahcesi.com`) uzantısında son eklenenler sıralamasını ve DRM / WebView bölüm açılma rotalarını düzeltmek.
- **Sürüm:** `v1.4.12` (`versionCode = 140012`)

## 2. Tespit Edilen Kök Nedenler ve Yapılan Düzeltmeler

### A. "En Son Eklenenler" Sıralaması (Latest Updates)
- **Sorun:** `latestUpdatesRequest` içinde `.addQueryParameter("sort", "updatedAt")` kullanılıyordu. Canlı API testinde `sort=updatedAt` sorgusunun günlerdir güncellenmeyen bayat/statik serileri döndürdüğü (son güncelleme 2 gün öncesine ait) saptandı.
- **Çözüm:** `sort=updatedAt` yerine sitenin gerçek zamanlı en son eklenenler parametresi olan `sort=latest` getirildi. Bu sayede bugün eklenen yeni bölümler ve seriler anında listelenir hale geldi.

### B. Hatalı URL Rotaları ve 404 Sayfa Bulunamadı Hatası
- **Sorun:** Eski implementasyonda `getMangaUrl`, `getChapterUrl` ve `pageListParse` içindeki WebView URL'leri `https://golgebahcesi.com/seri/...` adresine yönlendiriyordu. Ancak sitenin Next.js mimarisinde `/seri/` rotası mevcut değildir ve doğrudan 404 Not Found hatası vermekteydi.
- **Çözüm:**
  1. `getMangaUrl(manga)` -> `"$baseUrl/manga/${manga.url}"` olarak düzeltildi (HTTP 200 OK).
  2. `chapterListParse` -> API'den gelen gerçek bölüm slug'ı (`ch.optString("slug")`, örn: `"chapter-1"`) alınarak `chapter.url = "/$seriesSlug/$chapterSlug/$chapterId"` olarak kaydedildi.
  3. `getChapterUrl(chapter)` -> `"$baseUrl/manga/$series/bolum/$slug"` döndürecek şekilde güncellendi (HTTP 200 OK).

### C. Şifreli (DRM / .enc) Bölüm Açılma Sorunu
- **Sorun:** Gölge Bahçesi modern serilerinde (`deliverySystem: "secure"`, `imageEnc: true`) ChaCha20 şifreli `.enc` resim dosyaları ve Cloudflare Turnstile koruması kullanmaktadır. Tachimanga native resim okuyucusu bu korumayı aşamadığında kullanıcı WebView butonuna bastığında eski `/seri/...` rotası yüzünden 404 görüyordu.
- **Çözüm:**
  1. `openPages.isNotEmpty()` durumunda (unencrypted eski seriler) native resimler doğrudan yüklenir.
  2. `encCount > 0` durumunda (modern DRM serileri) Tachimanga'ya net ve yönlendirici hata mesajı fırlatılarak kullanıcının sağ üstteki WebView simgesine dokunması istenir:
     `"Bu bölüm site tarafından şifrelenmiştir (DRM). Sağ üstteki WebView (Dünya) simgesine dokunarak doğrudan okuyabilirsiniz: $baseUrl/manga/$seriesSlug/bolum/$chapterSlug"`
  3. WebView doğrudan çalışan `https://golgebahcesi.com/manga/<series>/bolum/<slug>` adresini açar, Cloudflare Turnstile ve Next.js okuyucu sorunsuz çalışarak bölümü tam ekran gösterir.

## 3. Kullanıcının Bildirdiği 5 Serinin Canlı Doğrulama Testi

Aşağıdaki serilerin tamamı canlı API ve web rotaları üzerinden test edilmiştir:
1. **Kötü Avcının Mesaisi (`kotu-avcisinin-mesaisi`):**
   - Toplam: 6 bölüm, Kilitli: 0, Ücretsiz: 6
   - WebView URL: `https://golgebahcesi.com/manga/kotu-avcisinin-mesaisi/bolum/chapter-1` -> **HTTP 200 OK**
2. **Savaş prensesi, ıssız adada bir adamla baş başa (`savas-prensesi-issiz-adada-bir-adamla-bas-basa`):**
   - Toplam: 55 bölüm, Kilitli: 6, Ücretsiz: 49 (kilitli bölümler D-LOCKED-CHAPTERS ile başarıyla elenir)
   - WebView URL: `https://golgebahcesi.com/manga/savas-prensesi-issiz-adada-bir-adamla-bas-basa/bolum/chapter-1` -> **HTTP 200 OK**
3. **Umine pansiyonu (`umine-pansiyonu`):**
   - Toplam: 36 bölüm, Kilitli: 6, Ücretsiz: 30
   - WebView URL: `https://golgebahcesi.com/manga/umine-pansiyonu/bolum/chapter-1` -> **HTTP 200 OK**
4. **Kalbimin frekansı (`kalbimin-frekansi`):**
   - Toplam: 133 bölüm, Kilitli: 18, Ücretsiz: 115
   - WebView URL: `https://golgebahcesi.com/manga/kalbimin-frekansi/bolum/chapter-1` -> **HTTP 200 OK**
5. **Ölümüne kazanç (`olumune-kazanc`):**
   - Toplam: 14 bölüm, Kilitli: 0, Ücretsiz: 14
   - WebView URL: `https://golgebahcesi.com/manga/olumune-kazanc/bolum/chapter-1` -> **HTTP 200 OK**


## 4. Tüm 214 Kilitli Olmayan Bölümün Canlı HTTP 200 Rota Doğrulama Kanıtı

| Seri Slug | Toplam Bölüm | Ücretsiz Bölüm | Kilitli Bölüm | Test Edilen URL Sayısı | HTTP 200 OK | Hata / 404 |
|---|---|---|---|---|---|---|
| `kotu-avcisinin-mesaisi` | 6 | 6 | 0 | 6 | **6 (%100)** | 0 |
| `savas-prensesi-issiz-adada-bir-adamla-bas-basa` | 55 | 49 | 6 | 49 | **49 (%100)** | 0 |
| `umine-pansiyonu` | 36 | 30 | 6 | 30 | **30 (%100)** | 0 |
| `kalbimin-frekansi` | 133 | 115 | 18 | 115 | **115 (%100)** | 0 |
| `olumune-kazanc` | 14 | 14 | 0 | 14 | **14 (%100)** | 0 |

**Toplam Sonuç:** 214 kilitli olmayan bölümün **214'ü de (%100)** `https://golgebahcesi.com/manga/<series>/bolum/<slug>` rotası üzerinde HTTP 200 OK yanıtı vermiştir. Hiçbir 404 hatası bulunmamaktadır.
