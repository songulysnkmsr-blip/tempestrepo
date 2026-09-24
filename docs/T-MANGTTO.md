# T-MANGTTO Doğrulama Raporu

## Yapılan İşlemler
1. `src/tr/mangtto` modülü kuruldu.
2. `build.gradle.kts`, `AndroidManifest.xml` ve `Dto.kt` veri modelleri yapılandırıldı.
3. Keiyoushi'de 400 Bad Request hatasına sebep olan `chapter` API parametresi ve bölüm numarası veri tipleri düzeltildi.
4. `populer`, `latest`, `chapters` ve `uploads` API uçları Kotlin serialization ve OkHttp ile tam uyumlu hale getirildi.
5. Resim tesliminde CDN URL şablonu (`${cdn}/manga/$slug/$chNum/$i-$fansubId.webp`) oluşturuldu.
6. İkon `res/mipmap-xxhdpi/ic_launcher.png` olarak kaydedildi.

## Doğrulama
Canlı testlerde API yanıtları (`/api/manga/populer`, `/api/manga/latest`, `/api/manga/{slug}/{chapter}`) ve WebP resim URL'leri 200 OK ile doğrulandı.
