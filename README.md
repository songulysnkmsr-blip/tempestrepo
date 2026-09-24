# Tachimanga Türkçe Manga Uzantı Deposu (Extensions Repo)

Bu depo, **iOS Tachimanga** ve **Android Mihon / Tachiyomi** için çalışan bağımsız Türkçe manga ve webtoon uzantılarını sunar.

## Desteklenen Siteler ve Durumları

| Site | Durum | Motor / Ayrıştırma |
|---|---|---|
| **Jura Tempest** (`https://juratempe.st`) | Aktif ✅ | SolidStart SSR + AVIF CDN desteği |
| **Mangtto** (`https://mangtto.com`) | Aktif ✅ | Next.js JSON API (düzeltilmiş şema) |
| **Ori Manga** (`https://orimanga.net`) | Aktif ✅ | WP REST API + WebP okuyucu |
| **Gölge Bahçesi** (`https://golgebahcesi.com`) | Aktif ✅ | REST API + Kilit Filtreleme (yalnızca açık bölümler) |

---

## iPhone'da Tachimanga'ya Nasıl Eklenir?

1. iPhone'unuzda **Tachimanga** uygulamasını açın.
2. Sağ alttan **Daha Fazla (More)** veya **Ayarlar (Settings)** sekmesine gidin.
3. **Uzantılar (Extensions)** -> **Uzantı Depoları (Extension Repositories)** bölümünü seçin.
4. Sağ üstteki **+ (Ekle)** butonuna dokunun.
5. Depo URL'si olarak aşağıdaki bağlantıyı yapıştırın:
   ```
   https://raw.githubusercontent.com/songulysnkmsr-blip/tempestrepo/repo/index.min.json
   ```
6. **Kaydet**'e dokunun. Uzantılar sekmesine döndüğünüzde **Jura Tempest**, **Mangtto**, **Ori Manga** ve **Gölge Bahçesi** uzantılarını göreceksiniz. İstediğinizi yükleyip hemen okumaya başlayabilirsiniz.

---

## Android (Mihon / Tachiyomi) İçin

1. **Göz At (Browse)** -> **Uzantılar (Extensions)** sekmesine gelin.
2. Sağ üstteki ayarlar veya uzantı depoları simgesine dokunun.
3. Aşağıdaki depo URL'sini ekleyin:
   ```
   https://raw.githubusercontent.com/songulysnkmsr-blip/tempestrepo/repo/index.min.json
   ```

---

## Geliştirici ve Derleme

- **Gradle / Kotlin:** Android Application + Kotlin 1.9 / 2.0
- **libVersion:** `1.4` (iOS Tachimanga ile tam uyumlu)
- **CI / CD:** GitHub Actions otomatik olarak `main` dalındaki değişiklikleri derler ve `repo` dalına dağıtır.
