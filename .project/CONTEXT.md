# Tachimanga Türkçe Manga Uzantı Deposu — proje bağlamı

Revision: 53 · Yetkili kaynak: .project/state.json

Bu görünüm türetilmiştir. Güncel kanıt kontrolü için context komutunu çalıştır.

Amaç: Tachimanga ve Mihon için çalışan bağımsız Türkçe manga eklenti deposu sağlamak
Hedef kitle: Tachimanga iOS ve Mihon kullanıcıları

## Kapsam

- Jura Tempest (juratempe.st) eklentisi
- Mangtto (mangtto.com) eklentisi
- Ori Manga (orimanga.net) eklentisi
- Gölge Bahçesi (golgebahcesi.com) eklentisi
- index.min.json ve repo dağıtımı

## Kapsam dışı

- Üçüncü taraf sitelerin sunucu taraflı içerik engellemeleri

## Kısıtlar

- Gölge Bahçesi'nde kilitli (coin/VIP) bölümler filtrelenecek, sadece açık bölümler sunulacak
- Tachimanga ile tam uyumlu index.min.json ve index.json formatı üretilecek

## Açık sorular

- Yok.

## Nesneler ve ilişkiler

- site-juratempest (source_site): Jura Tempest
- site-mangtto (source_site): Mangtto
- site-orimanga (source_site): Ori Manga
- site-golgebahcesi (source_site): Gölge Bahçesi
- ext-juratempest (extension_module): Jura Tempest Eklentisi
- ext-mangtto (extension_module): Mangtto Eklentisi
- ext-orimanga (extension_module): Ori Manga Eklentisi
- ext-golgebahcesi (extension_module): Gölge Bahçesi Eklentisi
- release-index (repo_index): Tachimanga Uyumlu Dağıtım İndeksi
- site-merlinscans (source_site): Merlin Scans
- ext-merlinscans (extension_module): Merlin Scans Eklentisi
- ext-juratempest → Uygular → site-juratempest
- ext-mangtto → Uygular → site-mangtto
- ext-orimanga → Uygular → site-orimanga
- ext-golgebahcesi → Uygular → site-golgebahcesi
- release-index → Yayınlar → ext-juratempest
- release-index → Yayınlar → ext-mangtto
- release-index → Yayınlar → ext-orimanga
- release-index → Yayınlar → ext-golgebahcesi
- ext-merlinscans → Uygular → site-merlinscans
- release-index → Yayınlar → ext-merlinscans

### Somut nesne değerleri

- site-juratempest: {"domain": "juratempe.st", "engine": "solidstart_ssr", "status": "active"}
- site-mangtto: {"domain": "mangtto.com", "engine": "nextjs_api", "status": "active"}
- site-orimanga: {"domain": "orimanga.net", "engine": "wordpress_initmanga", "status": "active"}
- site-golgebahcesi: {"domain": "golgebahcesi.com", "engine": "nextjs_encrypted", "status": "active"}
- ext-juratempest: {"pkg": "eu.kanade.tachiyomi.extension.tr.juratempest", "version_code": 1}
- ext-mangtto: {"pkg": "eu.kanade.tachiyomi.extension.tr.mangtto", "version_code": 1}
- ext-orimanga: {"pkg": "eu.kanade.tachiyomi.extension.tr.orimanga", "version_code": 1}
- ext-golgebahcesi: {"pkg": "eu.kanade.tachiyomi.extension.tr.golgebahcesi", "version_code": 1}
- release-index: {"status": "pending"}
- site-merlinscans: {"domain": "merlinscans.org", "engine": "wordpress_initmanga", "status": "active"}
- ext-merlinscans: {"pkg": "eu.kanade.tachiyomi.extension.tr.merlinscans", "version_code": 140004}

Türler ve bağlantı kuralları: `ontology` komutu / `ONTOLOJİ.md`.

## Kararlar

- D-LOCKED-CHAPTERS [accepted]: Gölge Bahçesi'nde coin/VIP ile kilitli bölümler filtrelenecek, yalnızca ücretsiz ve açık bölümler listelenecek.
  Gerekçe: Kullanıcı tercihi ve DRM/şifreli kilit engeli sebebiyle açık bölümlerin kesintisiz okunması hedeflenir.; kaynak: Kullanıcı mesajı: 'sadece açık olanlar okunsun'; kabul eden: user

## Görevler

- T-INIT [done] Proje ve Gradle repository çatısını kur (kayıt: done)
  - Ölçüt: Gradle derleme altyapısı, eklenti şablonu ve dizin yapısı hazır.
  - İlgili nesneler: release-index
  - Girdiler: yok
  - Ürettiği nesneler: release-index
  - Etkin önkoşullar: yok
  - Kabul güncelliği: güncel · tamamlanma sayısı: 2
- T-JURATEMPEST [done] Jura Tempest uzantısını geliştir (kayıt: done)
  - Ölçüt: Jura Tempest SSR sayfaları ayrıştırılıyor, bölüm ve AVIF resim akışı sağlandı.
  - İlgili nesneler: site-juratempest, ext-juratempest
  - Girdiler: site-juratempest
  - Ürettiği nesneler: ext-juratempest
  - Etkin önkoşullar: T-INIT
  - Kabul güncelliği: güncel · tamamlanma sayısı: 2
- T-MANGTTO [done] Mangtto uzantısını geliştir ve API parametrelerini düzelt (kayıt: done)
  - Ölçüt: Mangtto JSON API istemcisi popular/latest/chapters/pages akışlarını doğru modeller.
  - İlgili nesneler: site-mangtto, ext-mangtto
  - Girdiler: site-mangtto
  - Ürettiği nesneler: ext-mangtto
  - Etkin önkoşullar: T-INIT
  - Kabul güncelliği: güncel · tamamlanma sayısı: 2
- T-ORIMANGA [done] Ori Manga uzantısını geliştir (kayıt: done)
  - Ölçüt: Ori Manga init-manga WordPress teması ayrıştırılıyor, WebP resimler yükleniyor.
  - İlgili nesneler: site-orimanga, ext-orimanga
  - Girdiler: site-orimanga
  - Ürettiği nesneler: ext-orimanga
  - Etkin önkoşullar: T-INIT
  - Kabul güncelliği: güncel · tamamlanma sayısı: 2
- T-GOLGEBAHCESI [done] Gölge Bahçesi uzantısını geliştir ve kilitli bölümleri filtrele (kayıt: done)
  - Ölçüt: Gölge Bahçesi API başlıkları ayarlandı, kilitli bölümler filtrelendi ve açık bölümler sağlandı.
  - İlgili nesneler: site-golgebahcesi, ext-golgebahcesi
  - Girdiler: site-golgebahcesi
  - Ürettiği nesneler: ext-golgebahcesi
  - Etkin önkoşullar: T-INIT
  - Kabul güncelliği: güncel · tamamlanma sayısı: 2
- T-REPO-BUILD [done] Repository indeks ve APK derleme mantığını kur (kayıt: done)
  - Ölçüt: index.min.json, index.json ve repo.json Tachimanga standartlarında üretildi.
  - İlgili nesneler: release-index, ext-juratempest, ext-mangtto, ext-orimanga, ext-golgebahcesi
  - Girdiler: ext-juratempest, ext-mangtto, ext-orimanga, ext-golgebahcesi
  - Ürettiği nesneler: yok
  - Etkin önkoşullar: T-JURATEMPEST, T-MANGTTO, T-ORIMANGA, T-GOLGEBAHCESI
  - Üretici bağı: ext-golgebahcesi ← T-GOLGEBAHCESI
  - Üretici bağı: ext-juratempest ← T-JURATEMPEST
  - Üretici bağı: ext-mangtto ← T-MANGTTO
  - Üretici bağı: ext-orimanga ← T-ORIMANGA
  - Kabul güncelliği: güncel · tamamlanma sayısı: 2
- T-GITHUB-CI [done] GitHub Actions CI iş akışını ve GitHub repo senkronizasyonunu kur (kayıt: done)
  - Ölçüt: GitHub Actions iş akışı tanımlandı ve GitHub deposuna senkronize edildi.
  - İlgili nesneler: release-index
  - Girdiler: release-index
  - Ürettiği nesneler: yok
  - Etkin önkoşullar: T-REPO-BUILD, T-GOLGEBAHCESI, T-JURATEMPEST, T-MANGTTO, T-MERLINSCANS, T-ORIMANGA, T-INIT
  - Üretici bağı: ext-golgebahcesi ← T-GOLGEBAHCESI
  - Üretici bağı: ext-juratempest ← T-JURATEMPEST
  - Üretici bağı: ext-mangtto ← T-MANGTTO
  - Üretici bağı: ext-merlinscans ← T-MERLINSCANS
  - Üretici bağı: ext-orimanga ← T-ORIMANGA
  - Üretici bağı: release-index ← T-INIT
  - Kabul güncelliği: güncel · tamamlanma sayısı: 1
- T-GOLGEBAHCESI-FIX [needs_review] Gölge Bahçesi şifreli bölüm çökme ve URL yönlendirme sorunlarını gider (kayıt: done)
  - Ölçüt: GolgeBahcesi.kt içinde şifreli (.enc) bölümlerde imageUrlParse UnsupportedOperationException çökmesi giderilip açıklayıcı hata mesajı, manifest yedek ucu ve doğru WebView URL yapısı (/seri/<slug>/bolum-<num>) sağlandı, Origin/Accept başlıkları eklendi ve sürüm 1.4.11'e (140011) yükseltildi.
  - İlgili nesneler: site-golgebahcesi, ext-golgebahcesi
  - Girdiler: site-golgebahcesi
  - Ürettiği nesneler: yok
  - Etkin önkoşullar: yok
  - Kabul güncelliği: yeniden inceleme gerekli · tamamlanma sayısı: 1
  - Kontrol: stale evidence: src/tr/golgebahcesi/src/eu/kanade/tachiyomi/extension/tr/golgebahcesi/GolgeBahcesi.kt
  - Kontrol: stale evidence: src/tr/golgebahcesi/build.gradle.kts
  - Kontrol: stale evidence: src/tr/golgebahcesi/AndroidManifest.xml
- T-MERLINSCANS [done] Merlin Scans (merlinscans.org) uzantısını geliştir ve repoya ekle (kayıt: done)
  - Ölçüt: MerlinScans.kt (WP REST API + HTML parser + data-referer CDN başlık desteği), build.gradle.kts, AndroidManifest.xml, ikonlar, settings.gradle.kts ve tools/build_repo.py kayıtları eksiksiz oluşturuldu.
  - İlgili nesneler: site-merlinscans, ext-merlinscans
  - Girdiler: site-merlinscans
  - Ürettiği nesneler: ext-merlinscans
  - Etkin önkoşullar: yok
  - Kabul güncelliği: güncel · tamamlanma sayısı: 1
- T-GOLGEBAHCESI-V1412 [done] Gölge Bahçesi son eklenenler sıralamasını ve DRM / WebView rotalarını düzelt (v1.4.12) (kayıt: done)
  - Ölçüt: En son eklenenler sort=latest ile güncel bölümleri ve serileri listelemeli.
  - Ölçüt: Kötü Avcının Mesaisi, Savaş prensesi, Umine pansiyonu, Kalbimin frekansı, Ölümüne kazanç serileri ve tüm kilitli olmayan bölümler doğru webview/bölüm URL rotasına (/manga/<series>/bolum/<slug>) yönlendirilmeli, 404 hatası vermemeli.
  - Ölçüt: Android manifest ve build.gradle.kts v1.4.12 (140012) olarak sürüm yükseltilmeli ve kod derlenebilir olmalı.
  - İlgili nesneler: site-golgebahcesi, ext-golgebahcesi
  - Girdiler: site-golgebahcesi
  - Ürettiği nesneler: yok
  - Etkin önkoşullar: yok
  - Kabul güncelliği: güncel · tamamlanma sayısı: 1

## Çalışılabilir görevler

Şu anda çalışılabilir görev yok.

## Uyarılar

- T-GOLGEBAHCESI-FIX: stale evidence: src/tr/golgebahcesi/src/eu/kanade/tachiyomi/extension/tr/golgebahcesi/GolgeBahcesi.kt
- T-GOLGEBAHCESI-FIX: stale evidence: src/tr/golgebahcesi/build.gradle.kts
- T-GOLGEBAHCESI-FIX: stale evidence: src/tr/golgebahcesi/AndroidManifest.xml

## Onarım işlemleri

Bunlar öneridir; gerekçeyi değerlendir, actor ekle ve güncel revision ile uygula.
- reopen_task → T-GOLGEBAHCESI-FIX: Recorded completion needs review: stale evidence: src/tr/golgebahcesi/src/eu/kanade/tachiyomi/extension/tr/golgebahcesi/GolgeBahcesi.kt; stale evidence: src/tr/golgebahcesi/build.gradle.kts; stale evidence: src/tr/golgebahcesi/AndroidManifest.xml

Kanıt hash'i dosya sürümünü denetler; kalite veya insan kabulünü ispatlamaz.
