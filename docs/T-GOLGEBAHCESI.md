# T-GOLGEBAHCESI Görev Kanıtı

- **Görev:** T-GOLGEBAHCESI (Gölge Bahçesi uzantısını geliştir ve kilitli bölümleri filtrele)
- **Tarih:** 2026-09-25
- **Geliştirici:** antigravity
- **Karar Bağlantısı:** `D-LOCKED-CHAPTERS` ("sadece açık olanlar okunsun")

## Gerçekleştirilen İşlemler

1. **API ve DRM Analizi:**
   - Gölge Bahçesi Next.js mimarisine sahip olup `https://api.golgebahcesi.com/api` REST endpoint'lerini kullanmaktadır.
   - Doğru sorgulama için `Origin: https://golgebahcesi.com` ve `Referer: https://golgebahcesi.com/` başlıkları zorunludur.
   - Yeni güvenli bölümlerde WASM/OffscreenCanvas koruması bulunmaktadır.
   - Kilitli bölümlerde (`isLocked: true`, `lock.type: coin`) kullanıcı kararı `D-LOCKED-CHAPTERS` doğrultusunda tam filtreleme uygulanmış; yalnızca `isLocked: false` olan açık ve ücretsiz bölümler listeye dahil edilmiştir.
   - Bölüm okuma linkleri Tachimanga'nın WebView okuyucusuyla uyumlu olarak `/manga/{seriesSlug}/bolum/{chapterSlug}` yapısına yönlendirilmiştir.
   - CDN üzerinden doğrudan sunulan resimler regex deseniyle ayrıştırılmıştır.

2. **Oluşturulan Dosyalar:**
   - `src/tr/golgebahcesi/build.gradle.kts`: `libVersion = "1.4"` ve `versionCode = 1` Android eklenti yapılandırması.
   - `src/tr/golgebahcesi/AndroidManifest.xml`: `.GolgeBahcesi` meta-data tanımı.
   - `src/tr/golgebahcesi/res/mipmap-xxhdpi/ic_launcher.png`: Gölge Bahçesi resmi logosu.
   - `src/tr/golgebahcesi/src/eu/kanade/tachiyomi/extension/tr/golgebahcesi/GolgeBahcesi.kt`: Kilit filtrelemeli ve API entegrasyonlu HttpSource sınıfı.

3. **Doğrulama Sonuçları:**
   - Popüler seriler: 24 seri başarıyla çekildi (Örn: Gölgelerdeki Hükümdar Olmak İstiyorum).
   - Seri detayları: Açıklama, durum, türler ve yazarlar alındı.
   - Bölüm listesi ve kilit testi: Kilitli bölümler filtrelenerek sadece açık bölümler listelendi.
   - Resim çıkarma: Doğrudan CDN resimleri 50 sayfa olarak başarıyla ayrıştırıldı.
