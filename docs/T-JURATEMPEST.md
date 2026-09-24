# T-JURATEMPEST Doğrulama Raporu

## Yapılan İşlemler
1. `src/tr/juratempest` modülü oluşturuldu.
2. `build.gradle.kts` ve `AndroidManifest.xml` Tachiyomi 1.4 standartlarına uygun olarak tanımlandı.
3. `JuraTempest.kt` SSR tabanlı HTML ayrıştırıcısı yazıldı:
   - Popüler ve Son Eklenenler akışı ana sayfadan `a[href^="/explore/"]:has(img)` seçicisiyle çekiliyor.
   - Manga detayları (`og:title`, `og:image`, `og:description`) çıkarılıyor.
   - Bölüm listesi ve bölüm numaraları ayrıştırılıyor.
   - Okuma sayfaları `https://cdn.juratempe.st/*.avif` doğrudan sunucu kaynaklarından listeleniyor.
4. Logo ve ikon (`ic_launcher.png`) site kaynağından temin edildi.

## Doğrulama
Python simülasyonu ile `https://juratempe.st` üzerindeki kartlar, başlıklar, bölümler ve sayfa AVIF URL'leri test edildi ve 200 OK ile doğrulandı.
