# Tachimanga Türkçe Manga Uzantı Deposu — proje bağlamı

Revision: 19 · Yetkili kaynak: .project/state.json

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
- ext-juratempest → Uygular → site-juratempest
- ext-mangtto → Uygular → site-mangtto
- ext-orimanga → Uygular → site-orimanga
- ext-golgebahcesi → Uygular → site-golgebahcesi
- release-index → Yayınlar → ext-juratempest
- release-index → Yayınlar → ext-mangtto
- release-index → Yayınlar → ext-orimanga
- release-index → Yayınlar → ext-golgebahcesi

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
  - Kabul güncelliği: güncel · tamamlanma sayısı: 1
- T-JURATEMPEST [done] Jura Tempest uzantısını geliştir (kayıt: done)
  - Ölçüt: Jura Tempest SSR sayfaları ayrıştırılıyor, bölüm ve AVIF resim akışı sağlandı.
  - İlgili nesneler: site-juratempest, ext-juratempest
  - Girdiler: site-juratempest
  - Ürettiği nesneler: ext-juratempest
  - Etkin önkoşullar: T-INIT
  - Kabul güncelliği: güncel · tamamlanma sayısı: 1
- T-MANGTTO [done] Mangtto uzantısını geliştir ve API parametrelerini düzelt (kayıt: done)
  - Ölçüt: Mangtto JSON API istemcisi popular/latest/chapters/pages akışlarını doğru modeller.
  - İlgili nesneler: site-mangtto, ext-mangtto
  - Girdiler: site-mangtto
  - Ürettiği nesneler: ext-mangtto
  - Etkin önkoşullar: T-INIT
  - Kabul güncelliği: güncel · tamamlanma sayısı: 1
- T-ORIMANGA [done] Ori Manga uzantısını geliştir (kayıt: done)
  - Ölçüt: Ori Manga init-manga WordPress teması ayrıştırılıyor, WebP resimler yükleniyor.
  - İlgili nesneler: site-orimanga, ext-orimanga
  - Girdiler: site-orimanga
  - Ürettiği nesneler: ext-orimanga
  - Etkin önkoşullar: T-INIT
  - Kabul güncelliği: güncel · tamamlanma sayısı: 1
- T-GOLGEBAHCESI [done] Gölge Bahçesi uzantısını geliştir ve kilitli bölümleri filtrele (kayıt: done)
  - Ölçüt: Gölge Bahçesi API başlıkları ayarlandı, kilitli bölümler filtrelendi ve açık bölümler sağlandı.
  - İlgili nesneler: site-golgebahcesi, ext-golgebahcesi
  - Girdiler: site-golgebahcesi
  - Ürettiği nesneler: ext-golgebahcesi
  - Etkin önkoşullar: T-INIT
  - Kabul güncelliği: güncel · tamamlanma sayısı: 1
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
  - Kabul güncelliği: güncel · tamamlanma sayısı: 1
- T-GITHUB-CI [doing] GitHub Actions CI iş akışını ve GitHub repo senkronizasyonunu kur (kayıt: doing)
  - Ölçüt: GitHub Actions iş akışı tanımlandı ve GitHub deposuna senkronize edildi.
  - İlgili nesneler: release-index
  - Girdiler: release-index
  - Ürettiği nesneler: yok
  - Etkin önkoşullar: T-REPO-BUILD, T-GOLGEBAHCESI, T-JURATEMPEST, T-MANGTTO, T-ORIMANGA, T-INIT
  - Üretici bağı: ext-golgebahcesi ← T-GOLGEBAHCESI
  - Üretici bağı: ext-juratempest ← T-JURATEMPEST
  - Üretici bağı: ext-mangtto ← T-MANGTTO
  - Üretici bağı: ext-orimanga ← T-ORIMANGA
  - Üretici bağı: release-index ← T-INIT
  - Kabul güncelliği: henüz doğrulanmadı · tamamlanma sayısı: 0

## Çalışılabilir görevler

T-GITHUB-CI

## Uyarılar

- Yok.

## Onarım işlemleri

Bunlar öneridir; gerekçeyi değerlendir, actor ekle ve güncel revision ile uygula.
- Yok.

Kanıt hash'i dosya sürümünü denetler; kalite veya insan kabulünü ispatlamaz.
