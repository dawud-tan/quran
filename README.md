# Al-Qur'an, Adzan & Kiblat

Aplikasi Android (Java, ditambah satu pustaka C++ kecil) yang menggabungkan:

- **pembaca Al-Qur'an** per surat atau per juz, dengan terjemahan per ayat;
- **adzan otomatis** lima waktu, dengan pengingat 10 menit sebelumnya;
- **arah kiblat** dengan kompas, ditambah **bayangan kiblat** (saat bayangan
  benda tegak jatuh tepat di garis kiblat, tanpa kompas sama sekali);
- **arah terbit dan tenggelam** matahari.

Semuanya luring. Satu-satunya pemakaian jaringan adalah mencari nama tempat
(Nominatim/OpenStreetMap) untuk baris kedua notifikasi adzan, dan alarm tidak
pernah menunggu jaringan itu.

Hanya Indonesia yang benar-benar didukung: arah kiblat dihitung khusus untuk
wilayah Indonesia, dan kriteria jadwalnya mengikuti Kemenag RI.

## Membangun

Butuh JDK 17, Android SDK (jalurnya di `local.properties`, tidak ikut repo),
serta NDK dan CMake dengan versi yang dipatok di `app/build.gradle`.

```bash
./gradlew :app:assembleDebug     # gerbang utama; ikut membangun kode C++
./gradlew :app:buildCMakeDebug   # C++ saja, keempat ABI — lebih cepat kalau hanya mengubah cpp/
./gradlew :app:assembleRelease   # R8 + shrinkResources + lintVital; APK belum ditandatangani
./gradlew :app:lintDebug         # laporan di app/build/reports/lint-results-debug.sarif
./gradlew :app:installDebug      # pasang ke ponsel yang tersambung
```

Selalu pakai `./gradlew`, bukan `gradle` yang terpasang global.

## Memeriksa tanpa ponsel

Tidak ada *test suite* Gradle. Yang ada dua pemeriksa angka yang berjalan di
luar Android:

**Hisab (jadwal salat, matahari, kiblat, weton)** — paket `hisab` sengaja bebas
Android, jadi bisa dikompilasi `javac` biasa:

```bash
javac -d build/uji-hisab app/src/main/java/com/quran/kiblat/salat/hisab/*.java alat/UjiHisab.java
java -cp build/uji-hisab UjiHisab
```

Keluar dengan kode 1 kalau ada angka yang bergeser dari patokan (jadwal
Jakarta, Ashar Hanafi, kerendahan ufuk Bandung, terbit/terbenam, weton,
bayangan kiblat setahun penuh). Jalankan setiap kali menyentuh paket `hisab`.

**Nada alarm** — `app/src/main/cpp/gelombang_t3.{h,cpp}` bebas Oboe/JNI/Android
dan bisa dikompilasi `g++ -std=c++17`. Cara merender dan angka patokannya ada
di `.claude/agents/build-verifier.md`.

Yang **tidak** bisa diperiksa tanpa ponsel: seluruh rantai alarm (alarm tepat
waktu, servis latar depan, layar penuh di atas layar kunci, tembus Jangan
Ganggu, pemasangan ulang sesudah boot), bunyi nada yang benar-benar keluar
dari pengeras suara, dan GPS.

## Struktur kode

Semua di `app/src/main/java/com/quran/kiblat/salat/`, dikelompokkan menurut
tugasnya:

```
AktivitasUtama.java      aktivitas peluncur: laci surat/juz, menu, navigasi
│
├── hisab/               hitungan murni, TANPA Android (diperiksa alat/UjiHisab.java)
│   ├── SolarPosition    port Java algoritma SPA NREL (Reda & Andreas 2004)
│   ├── PrayTime         jadwal salat, turunan praytimes.org
│   ├── MasukanSpa       satu-satunya tempat masukan SPA dirakit
│   ├── ArahMatahari     jam dan azimut terbit/terbenam
│   ├── BayanganKiblat   saat bayangan jatuh di garis kiblat
│   ├── Geodesic         azimut kiblat, geodesi Karney di elipsoid WGS84
│   ├── MedanMagnetBumi  deklinasi magnet (koreksi kompas ke utara sejati)
│   └── Weton            hari + pasaran Jawa
│
├── jadwal/              jadwal salat + setelannya
│   ├── JadwalSalat      lima waktu, ihtiyati, cara Ashar; merakit PrayTime
│   └── PengaturanAdzan  dialog "Pengaturan Adzan"
│
├── alarm/               rantai adzan
│   ├── PenjadwalAdzan   memasang satu-satunya alarm yang tertunda
│   ├── SiaranAlarm      dasar dua penerima alarm di bawah
│   ├── SiaranSepuluhMenitLalu → Servis10Menit   nada T3 / tarhim subuh
│   ├── SiaranNotifikasiAdzan  → ServisAdzan     adzan
│   ├── PembantuServis   bagian yang sama di kedua servis (pemutar, notifikasi)
│   ├── NadaT3           pembungkus JNI mesin nada di cpp/
│   ├── AktivitasDering  layar penuh saat alarm berbunyi
│   ├── SiaranMatikan    tombol "Matikan" dan notifikasi yang digeser
│   └── SiaranSehabisNyala  pasang ulang sesudah boot / izin / pembaruan aplikasi
│
├── izin/                Izin (satu-satunya penjawab "boleh atau tidak") + dialog Izin Aplikasi
├── lokasi/              Lokasi (baca, simpan, baca balik) + Alamat (Nominatim)
├── umum/                Pref (berkas SharedPreferences) + Latar (utas latar bersama)
│
└── ui/
    ├── ayat/            pembaca Qur'an: FragmenAyat, AdapterAyat, SumberQuran, PosisiBaca
    ├── kompas/          Kompas (sensor), FragmenKompas (dasar ketiga layar kompas),
    │                    PeringatanKompas
    ├── kiblat/          FragmenKiblat: arah kiblat, bayangan kiblat, jadwal hari ini
    └── matahari/        FragmenArahMatahari: satu kelas untuk arah terbit DAN tenggelam
```

Lainnya:

- `app/src/main/cpp/` — `libnada_t3.so`, pembangkit nada alarm 520 Hz (C++17 + Oboe).
- `app/src/main/assets/` — `daftar_surat.json` dan `Surat/<id>.json` (114 berkas).
- `res/font/lpmq.ttf` — huruf mushaf LPMQ untuk teks Arab.

### Konvensi penamaan

Nama kelas, variabel, komentar, dan teks layar berbahasa Indonesia. Teks layar
sengaja ditulis langsung di tata letak/kode, bukan di `strings.xml`.

| awalan | artinya | contoh |
| --- | --- | --- |
| `Aktivitas*` | Activity | `AktivitasUtama`, `AktivitasDering` |
| `Fragmen*` | Fragment | `FragmenKiblat` |
| `Servis*` | Service | `ServisAdzan` |
| `Siaran*` | BroadcastReceiver | `SiaranMatikan` |
| `Pengaturan*` | dialog setelan | `PengaturanAdzan`, `PengaturanIzin` |

Pengecualian: `SolarPosition`, `PrayTime`, dan `Geodesic` memakai nama dan
istilah aslinya, karena masing-masing port dari rujukan berbahasa Inggris
(NREL SPA, praytimes.org, Karney) dan harus tetap mudah dicocokkan dengannya.

## Alur jadwal salat

```
Lokasi.terakhir() ──► JadwalSalat.hitung()
                          │  MasukanSpa.buat()  →  SolarPosition.SPAData
                          │  JadwalSalat.penghitung()  →  PrayTime (+ ihtiyati, cara Ashar)
                          ▼
         7 slot, urutannya tetap (pakai konstanta JadwalSalat.SUBUH … ISYA):
         {subuh, terbit, dzuhur, ashar, terbenam, maghrib, isya}
            0      1       2       3       4         5        6
```

- Terbit, transit, dan terbenam dari SPA; subuh (−20°), ashar, dan isya (−18°)
  dari metode sudut waktu klasik. Maghrib tepat saat terbenam. Ini konvensi
  Kemenag RI.
- **Ihtiyati** (bawaan 2 menit, 0–10) ditambahkan ke kelima waktu salat, tetapi
  **tidak** ke terbit dan terbenam: keduanya patokan astronomis.
- Rujukan yang dicocokkan adalah **praytimes.org**. Kerendahan ufuk dihitung dari
  ketinggian, jadi ketinggian harus terhadap **permukaan laut (MSL)**, bukan
  elipsoid WGS84 — `Lokasi.simpan` yang menormalkannya.

## Rantai alarm

Hanya **satu** alarm yang tertunda pada satu waktu; setiap alarm yang berbunyi
memasang alarm berikutnya.

```
PenjadwalAdzan.cekJadwal()
  ├─ waktu aktif berikutnya: hari ini, kalau tidak ada besok
  ├─ masih > 10 menit → SiaranSepuluhMenitLalu → Servis10Menit  (nada T3, tarhim kalau subuh)
  └─ ≤ 10 menit       → SiaranNotifikasiAdzan  → ServisAdzan    (adzan)
                                 │
     setiap jalan keluar kedua servis berakhir di onDestroy, yang memanggil
     PenjadwalAdzan.segarkanJadwal() → cekJadwal() lagi
```

Nada 10 menit (selain subuh) berbunyi **terus sampai dimatikan** lewat tombol di
layar penuh, tombol "Matikan" di notifikasi, atau notifikasi yang digeser.

## Aturan yang jangan dilanggar

Setiap aturan di bawah pernah menjadi bug. Alasan lengkapnya ada di komentar
kode dan di `CLAUDE.md`.

1. **Rakit `PrayTime` hanya lewat `JadwalSalat.penghitung()`** (atau
   `JadwalSalat.hitung()`), jangan `new PrayTime()`. Kalau tidak, jadwal yang
   dibunyikan dan yang ditampilkan bisa berbeda.
2. **Periksa `SolarPosition.rtsSah()` sesudah menghitung jadwal.** Di lintang
   tinggi SPA mengisi −99999 dan hasilnya jadi jam ngawur. `JadwalSalat.hitung()`
   dan `ArahMatahari.hitung()` sudah mengembalikan `null` untuk itu.
3. **Paket `hisab` tidak boleh mengimpor apa pun dari Android/androidx.** Itulah
   yang membuatnya bisa diperiksa `alat/UjiHisab.java`.
4. **Jangan "merapikan" `SolarPosition`.** Ia port setia dari rujukan NREL; tabel
   suku-sukunya yang besar adalah data.
5. **Jebakan satuan:** `SolarPosition.eot()` dalam **menit**, rumus `PrayTime`
   dalam **jam** — karena itu `sunPosition()` membagi 60. Hapus pembagi itu dan
   semua waktu meleset 60 kali lipat.
6. **Servis alarm cukup bertipe `mediaPlayback`.** Menambah `location` membuatnya
   gagal dimulai dari latar (Android 14+) tepat saat adzan.
7. **Volume alarm dinaikkan sesudah `startForeground` dan dikembalikan sebelum
   servis turun dari latar depan** (aturan audio latar Android 17). Karena itu
   nada dimatikan lewat `Servis10Menit.matikan()`, bukan `stopService`.
8. **Jangan menaruh jaringan sebelum `setAlarmClock`.** Alarm dipasang dulu dengan
   alamat tersimpan; Nominatim dicari sesudahnya.
9. **Kedua penerima alarm tetap dua kelas.** Kode permintaan `PendingIntent`-nya
   sama, hanya komponennya yang membedakan, dan `cekJadwal` memasang yang satu
   sambil mencabut yang lain.
10. **Jangan meminta `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`** — tidak dibutuhkan
    (`setAlarmClock` sudah keluar dari Doze) dan dilarang Google Play.
11. **Lokasi cukup `getLastKnownLocation`.** Bergeser 1 km tidak mengubah satu
    detik pun dari ketujuh waktu; menunggu fix GPS baru hanya membuat servis
    menggantung.
12. **C++: `-ffast-math` jangan dinyalakan**, dan bangun untuk **keempat ABI**
    (armeabi-v7a yang 32-bit menangkap salah tipe `size_t`/`int64_t`).
13. **Alarm berbunyi ke nama kelas.** Mengganti nama atau memindahkan kelas
    `Siaran*` membuat alarm yang sudah terpasang di ponsel pengguna menuju
    komponen yang tidak ada. `SiaranSehabisNyala` memasang ulang lewat
    `MY_PACKAGE_REPLACED` sesudah aplikasi diperbarui, jadi pastikan aksi itu
    tetap terdaftar.

## Setelan tersimpan

Satu berkas SharedPreferences, `Pref.dari(context)` (nama berkasnya `"pref"`,
jangan diganti). Setiap kunci hanya dibaca dan ditulis kelas pemiliknya:

| kunci | pemilik | isi |
| --- | --- | --- |
| `latitude`, `longitude`, `altitude`, `accuracy` | `lokasi.Lokasi` | lokasi terakhir; ketinggian MSL |
| `alamat` | `lokasi.Alamat` | nama tempat terakhir dari Nominatim |
| `adzan_subuh` … `adzan_isya` | `jadwal.JadwalSalat` | waktu yang dibunyikan, bawaan semua hidup |
| `ihtiyati` | `jadwal.JadwalSalat` | 0–10 menit, bawaan 2 |
| `bayangan_ashar` | `jadwal.JadwalSalat` | 1 = jumhur (bawaan), 2 = Hanafi |
| `lokasi_latar`, `sudah_tanya_izin` | `izin.Izin` | sakelar lokasi latar; penjelasan izin sudah tampil |
| `sudah_baca_kompas` | `ui.kompas.PeringatanKompas` | spanduk kompas sudah dibaca |
| `mode`, `suratke`, `juzke`, `judul`, `bindingAdapterPosition` | `ui.ayat.PosisiBaca` | posisi baca terakhir |

## Izin

`izin.Izin` satu-satunya tempat yang memeriksa izin; dialog "Izin Aplikasi"
(`izin.PengaturanIzin`) menampilkan semuanya beserta akibatnya kalau ditolak.

| izin | kalau ditolak |
| --- | --- |
| Alarm & pengingat (`SCHEDULE_EXACT_ALARM`) | tidak ada adzan sama sekali — satu-satunya yang wajib |
| Notifikasi | adzan tetap berbunyi; nada 10 menit dibatasi 3 menit karena tidak ada tombol Matikan |
| Layar penuh | muncul sebagai notifikasi biasa |
| Lokasi saat dipakai | dipakai lokasi tersimpan, atau titik tengah Indonesia |
| Lokasi latar (pilihan, mati bawaan) | jadwal memakai lokasi saat aplikasi terakhir dibuka |

## Rencana (todo)

1. menu juz, satu ruqu
2. terjemahan quran yg bisa diexpand
3. ukuran ruku diperbesar, U+08D6
4. background gambar kompas di fragment terbit/tenggelam, bisa dibikin bundar?
5. Tombol icon speaker untuk hilang suara?
6. Penjelasan waktu matahari tergelincir/40/tenggelam/-18/-20 di setContentText?
