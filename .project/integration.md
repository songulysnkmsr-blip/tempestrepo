# Proje çalışma kaydı

Bu projeye devam ederken önce `.project/state.json` dosyasını oku.
Güncel özet ve kanıt durumunu görmek için proje kökünden çalıştır:

```sh
python3 .project/scripts/project.py context .
```

Windows PowerShell'de `python3` yerine `py -3` (veya Python 3.10+
olduğu doğrulanan `python`) kullan. macOS/Linux'ta `python3` kullan.

Yetkili kayıt `.project/state.json`; `.project/CONTEXT.md` türetilen görünümdür.
Şema 3 ontolojisi ve somut kayıtlar için `python3 .project/scripts/project.py
ontology .` çalıştır; `.project/ONTOLOJİ.md` bunun üretilmiş görünümüdür.
Görev/karar değişikliklerini `.project/scripts/project.py apply` ile, okuduğun
revizyonu `--expected-revision` olarak belirterek işle. Komut seçenekleri için
`python3 .project/scripts/project.py --help` kullan.
Önerileri kabul edilmiş karar sayma. Kontrol başarısını kullanıcı kabulü sayma.
Değişimin etkisini yazmadan görmek için `preview . --event <eylem.json>
--expected-revision <revizyon>` kullan. CLI yazıcıları işletim sistemi kilidiyle sıraya
girer; kayıt dosyasına dışarıdan veya elle paralel durum yazımı yapma.
