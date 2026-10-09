# CLAUDE.md

Guidance for Claude Code when working in this repository.

## What this is

A single-module Android app (Java, no Kotlin, plus one small C++ library) that combines a
Qur'an reader with a prayer-time alarm, a qibla compass, and sunrise/sunset direction finders.
Everything is offline except one reverse-geocode call for the notification subtitle.

- Module `:app`, namespace + applicationId `com.quran.kiblat.salat`
- `minSdk 29`, `targetSdk`/`compileSdk 37`, Java 17 toolchain, core library desugaring on
- View binding; Navigation component; Material 3; Media3 (ExoPlayer + MediaSession); WorkManager
- One native library, `libnada_t3.so` (`app/src/main/cpp`, C++17 + Oboe), which synthesises the
  520 Hz alarm tone. NDK and CMake versions are pinned in `app/build.gradle`.
- **Identifiers, comments, and UI strings are in Indonesian.** Match that when adding code
  (`Servis*` = service, `Siaran*` = broadcast receiver, `Fragmen*` = fragment,
  `Aktivitas*` = activity, `jadwal` = schedule, `lokasi` = location, `waktu` = time).

## Build and verify

```bash
./gradlew :app:assembleDebug     # main gate; pulls in the native build itself
./gradlew :app:buildCMakeDebug   # C++ only, all four ABIs — quicker when iterating on cpp/
./gradlew :app:assembleRelease   # runs R8 + shrinkResources + lintVital; unsigned
./gradlew :app:lintDebug         # report at app/build/reports/lint-results-debug.sarif
```

- Always use the wrapper. The SDK path comes from `local.properties` (not in VCS).
- **There is no test suite** — only `app/src/main` exists. Never report tests as passing.
- Lint has a large pre-existing warning backlog (HardcodedText, UnusedResources,
  IconDuplicates, SetTextI18n). Zero errors and 123 warnings today; report only *new* findings.
  The `GradleDependency` "newer version available" notices drift as libraries release, so the
  count can move without any code change — diff the SARIF against a lint of `HEAD` rather than
  trusting the number. Lint does not look at C++ at all.
- The native build needs `android.buildFeatures.prefab = true` — Oboe ships as a prefab package
  inside its AAR. Without it CMake fails at `find_package(oboe REQUIRED CONFIG)` with
  "Could not find a package configuration file provided by oboe". That flag is the fix; nothing
  about `CMAKE_PREFIX_PATH` is.
- Build C++ for **all four ABIs**, not just arm64: armeabi-v7a is 32-bit and is what catches
  `size_t`/`int64_t` narrowing.
- `PrayTime` and `SolarPosition` have no Android dependencies, so they can be compiled
  and run with plain `javac`/`java` outside Gradle to check prayer-time changes numerically.
  That is the only practical way to verify the astronomy without a device.
- `cpp/gelombang_t3.{h,cpp}` is deliberately free of Oboe, JNI and Android for exactly the same
  reason: `g++ -std=c++17` compiles it as-is, so the alarm tone can be rendered to raw floats and
  measured. `.claude/agents/build-verifier.md` has the harness and the reference numbers.

## The prayer-time pipeline

```
Location ──► SolarPosition.SPAData ──► PrayTime.getDatePrayerTimes()
                                            │
                                            ├─ SPA (NREL, Reda & Andreas 2004): terbit, transit, terbenam
                                            └─ classic hour-angle method: subuh (−20°), ashar, isya (−18°)
                                            ▼
                    ArrayList<ZonedDateTime> — fixed 7 slots, order matters:
                    {subuh, terbit, dzuhur, ashar, terbenam, maghrib, isya}
                              0      1       2       3       4         5        6
```

- `SolarPosition` is a Java port of the NREL SPA reference implementation. Keep the port
  faithful; the giant `SUKU2_*` term tables are data, not code to refactor.
- `PrayTime.methodParams["falakiyah"]` = `{fajr 20°, maghrib selector 0, maghrib 0, isha selector 0, isha 18°}`
  — the Kemenag RI convention. Maghrib selector 0 + value 0 means "Maghrib exactly at sunset".
- **Build every `PrayTime` through `Util.penghitungJadwal(sharedPref)`**, never `new PrayTime()`
  directly. It applies the user's ihtiyati and Asr factor, and it is what keeps the schedule
  that rings and the schedule shown on the qibla screen from drifting apart.
- **Ihtiyati** is the deliberate safety margin added to the five prayers, not a fudge factor.
  It is a pref (0–10 min, default 2) applied as `{n, 0, n, n, 0, n, n}` — terbit and terbenam
  deliberately get **0**, they are astronomical reference points, not prayer times. (Kemenag
  practice would pull terbit *earlier* by the same margin; this app has never done that, so
  changing the 2nd slot to `-n` is a behaviour change, not a bug fix.)
- `PrayTime.getDatePrayerTimes` anchors its Julian day to **0h UT of the local date minus
  longitude/15**, because `computeTime(G, t)` treats `t` as a day fraction from local midnight.
  Do not anchor it to the wall-clock moment the calculation happens to run.
- `SolarPosition.calculateEotAndSunRiseTransitSet` applies **dip of horizon** from
  `spa.elevation`, so sunrise/sunset/maghrib shift by minutes in highland cities. Elevation
  must be metres above **mean sea level**, not the WGS84 ellipsoid — `Util.simpanLokasi`
  normalises via `LocationCompat` before persisting.
- **The reference this app matches is praytimes.org** (the library NU Online is understood to
  use). `dipOfHorizon` deliberately uses praytimes.js's `0.0347 * sqrt(h)` rather than the
  classical falak `0.0293 * sqrt(h)`, and the `JDate` anchoring mirrors praytimes.js's
  `julian(y,m,d) - lng/(15*24)`. The one deliberate divergence: the solar model here is the
  NREL SPA (~0.0003° accurate) instead of praytimes.js's low-precision approximation
  (~1 arcmin), which is worth a few seconds. Do not downgrade it to match byte-for-byte.
- Asr shadow factor is a pref: 1 = Shafi'i **and Maliki and Hanbali** (default), 2 = Hanafi.
  Factor 2 moves Ashar roughly an hour later, so it is very visible — do not change the default.
- **Unit trap at the SPA/praytimes seam.** `SolarPosition.eot()` returns the equation of time in
  **minutes of time** (hence its `4.0 *` factor: 1° = 4 min, and `limitMinutes` bounds it to
  ±20 min). The praytimes.org formulas that `PrayTime` inherits — `computeMidDay`'s `12 - T`
  and everything derived from it — work in **hours**. That is the only reason
  `sunPosition()` ends with `... / 60.0`. Remove that divisor and every time is off by a
  factor of 60; it is the seam to check first if a port of this code goes wrong.
- Only Indonesia is really supported. At high latitudes the SPA returns the `-99999` sentinel
  for rise/transit/set; call `SolarPosition.rtsSah(spa)` **after** `getDatePrayerTimes` and
  bail out, or that value becomes a nonsense time (and `LocalTime.of` throws).
  `Util.cekJadwal` and all three compass fragments already do this.

## The alarm chain

Only **one** alarm is ever pending. Each firing reschedules the next one.

```
Util.cekJadwal(context, lokasi, fromBooting)
  ├─ nextEpoch()  → next *enabled* prayer today, else tomorrow, else nothing
  ├─ >10 min away → setAlarmClock ─► SiaranSepuluhMenitLalu ─► Servis10Menit  (T3 tone / tarhim)
  └─ ≤10 min away → setAlarmClock ─► SiaranNotifikasiAdzan  ─► ServisAdzan    (adzan)
                                            │
        every way out of either service ends in onDestroy, which calls
        Util.segarkanJadwal(this, dariLatar = true, …) → cekJadwal again
```

- Both services are foreground services of type **`mediaPlayback` only**, with a full-screen
  intent into `RingActivity`, `setBypassDnd(true)`, and (in `Servis10Menit`) the alarm audio
  stream forced to max volume and restored afterwards.
- **Do not add `location` back to the service type.** Since Android 14 a `location`-type
  foreground service cannot be *started from the background* without
  `ACCESS_BACKGROUND_LOCATION`, and these services are only ever started from an alarm — so it
  threw `SecurityException` at adzan time for everyone who didn't pick "Allow all the time".
  The location read is `getLastKnownLocation`, which needs no service type at all.
- **Android 17 background-audio rule** (all apps; stricter at targetSdk 37): playback and
  `setStreamVolume` from a non-visible app are *silently ignored* unless a non-short foreground
  service is running, and at targetSdk 37 it must also have while-in-use capability — waived
  only for `USAGE_ALARM` streams **when the exact-alarm permission is granted**. Two
  consequences in the code: volume is raised and ExoPlayer is prepared only *after*
  `startForeground`, and the volume is restored *before* the service leaves the foreground —
  in `onDestroy` it is already too late and the alarm volume would stay at max.
- **The non-subuh tone rings until the user stops it**, not for a fixed 3 minutes. Three ways
  out — the full-screen `RingActivity` button, the notification's "Matikan" action, and swiping
  the notification — all go through `Servis10Menit.matikan()`, which sends `AKSI_MATIKAN` *to the
  service* rather than calling `stopService`, so the volume is restored while it is still in
  the foreground (see the Android 17 rule above). Because nothing else ends it, `Servis10Menit`
  re-derives the schedule **when the tone starts** as well as in `onDestroy`, so the chain is
  already armed even if the service is killed.
- The one exception to "rings until stopped": if the notification cannot show
  (`Izin.notifikasiTampil` false — permission denied or channel blocked) there is no Matikan
  button and no full-screen intent either, so the tone is bounded to 45 cycles (3 min).
- Subuh is the other bounded case: one cycle of tone, then the tarhim through ExoPlayer, then
  `panggil10Menit()` when it ends. That last step used to be missing — both branches of the old
  listener excluded subuh, so the service hung until something else destroyed it.
- `ServisAdzan` still plays its audio through ExoPlayer. `Servis10Menit` does **not**: its T3
  tone comes from `NadaT3` (native, below), and ExoPlayer there is left holding only the subuh
  tarhim. Anything in that service that asks ExoPlayer "is something still playing?" has to ask
  `nada.sedangMain()` too — `onTaskRemoved` does, and would otherwise kill a ringing alarm the
  moment the app is swiped from recents, because an empty ExoPlayer reports `getMediaItemCount()
  == 0`.
- `SiaranSehabisNyala` re-arms inline after `BOOT_COMPLETED`, and also on
  `SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`, so granting exact alarms in system settings
  arms the adzan without reopening the app. There is no WorkManager any more: the refresh is a
  cached-location read plus `cekJadwal`, which finishes immediately, and the old expedited
  `LocationWorker` had no `getForegroundInfo()`, which crashes on Android 10–11.
- The two `PendingIntent`s both use request code 1 but differ by component, so they are
  distinct. `cekJadwal` sets one and explicitly cancels the other — keep that invariant, or
  a stale alarm from a prayer the user just muted will still fire.
- The alarm is set **immediately** using the cached `alamat` pref, and the Nominatim lookup
  runs afterwards on `Util.eks`, re-setting the alarm only if the address actually changed and
  the `PendingIntent` still exists. Never move the network back in front of `setAlarmClock`.

## The alarm tone (`app/src/main/cpp`)

`Servis10Menit` used to play `res/raw/t3_441khz.ogg` / `t3_48khz.ogg`, picking between them by
querying the built-in speaker's sample rates. Both files are gone. The tone is now synthesised:

```
NadaT3.java ──JNI──► NadaT3 (nada_t3.cpp)  ── Oboe stream, Usage::Alarm
                          └─ gelombang::bangunLetupan()  (gelombang_t3.cpp)
```

- **`gelombang_t3.{h,cpp}`** is the waveform, and nothing else — no Oboe, no JNI, no Android, so
  it compiles under plain `g++` and can be checked numerically. `POLA` is the single source of
  truth for the rhythm: 0.5 s on, 0.5 off, 0.5 on, 0.5 off, 0.5 on, 1.5 off — the ISO 8201 /
  NFPA 72 "T3" evacuation signal, exactly 4 s per cycle.
- 520 Hz is deliberate (NFPA 72: low tones wake sleepers far better than high ones) and is
  declared as an **integer**, because the whole precision argument rests on `520/sampleRate`
  reducing to an exact rational `p/q`. The sampled wave then repeats exactly every `q` frames
  (1200 at 48 kHz, 2205 at 44.1 kHz) and the phase of frame *n* is the integer `(n*p) mod q`.
  There is no floating-point phase accumulator anywhere, so nothing drifts, and burst 45 is
  bit-identical to burst 1.
- **The reference is Nayuki's `GenerateSquareWave.java`**, and the output is meant to *be* its
  output. That fixes three things that would otherwise look arbitrary: the **cosine** form
  (`a_k = sin(k*d*pi)*2/(k*pi)`, `x = a_0 + sum a_k*cos(k*phi)`, so the burst starts on the
  plateau rather than at a zero crossing), the harmonic count (`sampleRate/(frequency*2)`,
  matching `kMaks`), and `AMPLITUDO = 0.5`, which is Nayuki's inherent amplitude — the
  coefficients are used unscaled there. Diffing the two sample by sample is the real regression
  test; outside the edge fades they agree to a quarter of a float ulp.
- The wave is **band-limited**: truncated at the last harmonic below Nyquist, summed
  high-k-first with Neumaier compensation. That is why `-ffast-math` must stay off — it is
  licensed to delete exactly that compensation.
- Peak is 0.5896 (`AMPLITUDO` x 1.179 Gibbs overshoot) and burst RMS 0.4947. That is about 4 dB
  quieter than the ogg files it replaced, which measured RMS 0.78 and clipped at 1.0. If the
  alarm needs to be louder, `AMPLITUDO` is the only knob; 0.84 is where clipping starts.
- All of that is paid **once**, in `mulai()`, on the calling thread. `onAudioReady` copies whole
  chunks out of the finished burst buffer or fills zeros; it has no locks, no allocation, no
  logging, and no per-frame arithmetic at all.
- `setUsage(oboe::Usage::Alarm)` is not optional. Without it the tone lands on the media stream,
  the forced-to-max *alarm* volume does nothing, and a user who has silenced media hears nothing.
- The native side never calls back into Java from the audio thread. `mulai(putaran)` returns the
  exact duration in ms and `Servis10Menit` posts a delayed `Runnable` for what comes next; with
  `NadaT3.TANPA_BATAS` it returns 0 instead and loops until `berhenti()`, so there is nothing to
  schedule. A negative return means the stream would not open.
- Adding a sixth prayer, changing the rhythm, or changing the tone means editing `POLA` /
  `FREKUENSI` in `gelombang_t3.h` and re-running the offline harness. Nothing else.

## Preferences (`getSharedPreferences("pref", MODE_PRIVATE)`)

| key | meaning |
| --- | --- |
| `latitude` / `longitude` / `altitude` / `accuracy` | last known fix, stored as strings; altitude is MSL. A fix with no altitude (network provider, or "approximate" location) keeps the previous altitude — unknown is not zero, and zero moves Bandung's maghrib 4 min early |
| `alamat` | last resolved reverse-geocode string, used so alarms never wait on the network |
| `adzan_subuh` … `adzan_isya` | per-prayer on/off, **default true** so existing installs are unchanged |
| `ihtiyati` | safety minutes added to the five prayers, 0–10, default 2 |
| `bayangan_ashar` | Asr shadow factor, 1 (default) or 2 |
| `mode`, `suratke`, `juzke`, `judul`, `bindingAdapterPosition` | last reading position |
| `sudah_baca_kompas` | the compass-accuracy dialog has been shown once; see below |
| `lokasi_latar` | user's choice for background location. **Absent = follow the existing grant**, so old installs that already allowed "all the time" keep working; read only through `Izin.lokasiLatarAktif` |
| `sudah_tanya_izin` | the one-time permission explanation has been shown |

`Util.adzanAktif` / `ihtiyati` / `bayanganAshar` / `simpanSetelanAdzan` / `jumlahAdzan` /
`namaAdzan` are the only supported way to read or write these; `PengaturanAdzan` (options menu →
"Pengaturan Adzan") is the UI, and it calls `cekJadwal` on save so the pending alarm is
re-derived immediately. Its prayer checkboxes are generated from `Util.namaAdzan()` at runtime,
so adding a sixth entry to `NAMA_ADZAN`/`INDEKS_ADZAN` needs no layout change.
`FragmenKiblat` re-reads all three settings in `aturJadwal`, which `onResume` calls, so closing
the dialog refreshes the displayed schedule.

## Bayangan kiblat (the sun-shadow qibla check)

`ui/util/BayanganKiblat.java` finds the moments when the sun's azimuth equals the qibla azimuth
(shadow points *away* from the qibla) or its reciprocal (shadow points *at* it). At those
moments a vertical object's shadow lies on the qibla line and **the magnetometer is not used at
all** — which is the entire point, since the compass is the only inaccurate link in the chain.

- **This is not just the two Rashdul Qibla dates.** Those are the special case where the sun is
  directly *over* the Kaaba, so a shadow works worldwide at one instant. The general condition is
  much weaker — only the azimuth has to match — and in Jakarta it is met on **299 of 365 days**.
  The gap is a few weeks around each equinox, which is why `cariBerikutnya` searches forward.
- Android-free, like `PrayTime`/`SolarPosition`, so it is verifiable with plain `javac`. Scanning
  a year and re-deriving the sun's azimuth at every returned instant gives a worst-case error of
  0.0057 degrees — that is the one-second search resolution (the azimuth sweeps ~0.0042 deg/s),
  not a modelling error. The four Rashdul Qibla dates fall out of the general scan on their own;
  that is the cheapest regression test.
- The day is scanned **per minute** before bisecting. Do not widen that step: at tropical
  latitudes the solar azimuth swings very fast near the zenith crossing, and a coarser scan can
  step straight over a crossing.
- `TINGGI_MIN`/`TINGGI_MAKS` (5 and 75 degrees) reject unusable geometry — too low means
  refraction and obstructions, too high means the shadow is too short to aim along.
- One scan is thousands of SPA evaluations, so `FragmenKiblat` runs it through `Util.diLatar`
  (the existing executor and handler, not a new pool) and caches on date plus position rounded to
  ~100 m.

## Compass accuracy

`Util.peringatanKompas` explains, in Indonesian, that the qibla *bearing* is exact (Karney on
WGS84; a few metres of GPS error moves it by ~1e-5 degrees) and that everything is decided by the
magnetometer instead, which typically misses by 5-15 degrees — where **1 degree is about 138 km
at the Kaaba**. Keep that ratio in the text; it is the whole reason the dialog exists.

Three triggers, all in place: once per install on first opening `FragmenKiblat`
(`peringatanKompasSekali`, guarded by the `sudah_baca_kompas` pref), whenever the magnetometer
reports `SENSOR_STATUS_ACCURACY_LOW` or `UNRELIABLE` via `onAccuracyChanged` (once per visit,
with an extra opening line), and on demand from the options menu.

`FragmenTerbit` and `FragmenTenggelam` show it too, on the accuracy trigger only, passing
`kiblat = false`: same magnetometer and same problem, but the "138 km at the Kaaba" yardstick
does not apply there, so that paragraph is swapped for the SPA's own accuracy. The first-run
banner lives only on the qibla screen; the pref is shared, so reading it anywhere silences it
everywhere.

The first-run path is a **banner** (`spanduk_kompas.xml`, included at the top of
`fragmen_kiblat.xml`), not a dialog fired from `onResume`. A dialog that appears before the user
has seen anything gets dismissed unread, which defeats the purpose. Tapping it opens the full
explanation; either action sets `sudah_baca_kompas`.

## UI layout

`AktivitasUtama` hosts a `DrawerLayout` + `NavHostFragment`. The drawer menu is built at
runtime from `assets/daftar_surat.json`: group 1 = surah, group 2 = juz, and the two groups are
swapped by removing the other (`daftarMenu.removeGroup(n)`). Destinations: `FragmenAyat`
(default), `FragmenKiblat` (also shows the 5 prayer times), `FragmenTerbit`, `FragmenTenggelam`.

Qur'an text lives in `assets/Surat/<id>.json` (114 files) and is rendered with `res/font/lpmq.ttf`.

The three compass fragments share one tilt-compensated orientation algorithm (gravity × magnetic
field cross products) plus `MedanMagnetBumi` for magnetic declination. That code is duplicated
three times — if you touch one copy, check the other two.

All three layouts are wrapped in a `NestedScrollView` with `fillViewport="true"`, which keeps
tall screens looking exactly as before and makes short ones scroll instead of clipping.
`fragmen_kiblat.xml` is the one that actually needed it — banner, 220dp compass, then 13 data
rows down to isya, against 7 rows in the other two — but they share the wrap so the three stay
interchangeable. Every child inside is `wrap_content` or a fixed dp — **keep it that way**: one
`match_parent` or `0dp` height in there and the content either collapses or becomes unreachable,
because a scroll view gives its child an unbounded height.

## Permissions

`Izin.java` is the only place that answers "is this allowed?", and the only route to each
settings screen. `PengaturanIzin` (options menu → "Izin Aplikasi") shows every permission with
its state, what changes if it is denied, the fallback, and a button. Nothing is re-asked on
cold start: `AktivitasUtama.mulai()` explains the missing ones **once** (`sudah_tanya_izin`),
and afterwards the only reminder is a Snackbar when adzan is enabled but exact alarms are
denied — i.e. nothing would ring at all.

| permission | status | if denied |
| --- | --- | --- |
| `SCHEDULE_EXACT_ALARM` | required; settings screen only | nothing is scheduled. Also gates the Android 17 `USAGE_ALARM` audio waiver |
| `POST_NOTIFICATIONS` | asked once | adzan still sounds; the 10-min tone is bounded to 3 min (no Matikan) |
| `USE_FULL_SCREEN_INTENT` | settings screen only (Play auto-grants declared alarm apps) | plain heads-up notification |
| fine/coarse location | asked once | last saved location, or the centre of Indonesia (37–52 min off for Jakarta) |
| `ACCESS_BACKGROUND_LOCATION` | **opt-in switch**, off by default, with Play's prominent disclosure | schedule uses the location from the last time the app was opened |
| battery optimisation | **never requested** | — |

- **`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` is deliberately gone.** `setAlarmClock` makes the
  system leave Doze before the alarm fires, starting a foreground service from an exact alarm
  is already exempt from background-start limits, and the location read never turns on a
  radio. So Doze does not impair the core function, and Play forbids asking for the exemption
  in that case. Users on aggressive OEMs are pointed at App info → Battery instead, which needs
  no permission. Don't add it back.
- Background location is the one Play-declared permission left (declaration form + video).
  Deleting its `<uses-permission>` line is enough to ship without it:
  `Izin.lokasiLatarTersedia` reads the manifest and the switch disappears.
- Location reads go through `Izin.lokasiTerkini(context, dariLatar)`: `dariLatar = true` from
  services/receivers (requires the switch), `false` from screens (foreground permission is
  enough). The three compass fragments used to call `getLastKnownLocation` unguarded and threw
  `SecurityException` with location denied.
- Unused-app hibernation (Android 12+) resets permissions **and stops alarms**, and a ringing
  adzan does not count as usage. `PengaturanIzin` shows that state too.

## Known rough edges

- `Util` holds a static `ExecutorService` and `Handler` for the whole process.
- `spa.pressure = 1000` / `temperature = 27.7` are hardcoded; they only affect the
  zenith/azimuth path, not sunrise/sunset.
- The compass fragments still call the deprecated `getDefaultDisplay()` below API 30 (API 29
  only, given `minSdk`), with `@SuppressWarnings("deprecation")` on that one `Display`
  declaration. javac warns on any call to a `@Deprecated` method whether or not an
  `SDK_INT` check guards it, so the annotation is the only way to silence it; keep it on the
  declaration, not the method. `app/build.gradle` passes `-Xlint:unchecked -Xlint:deprecation`
  and the Java build is otherwise warning-free, so any new warning is a real finding. Check it
  with `./gradlew :app:compileDebugJavaWithJavac --rerun`, because a task that is up to date
  prints nothing.
- UI strings are inline Indonesian by choice, so `HardcodedText` and `SetTextI18n` warnings are
  expected for any new layout or `setText`. Follow the convention rather than fighting it.
- The MediaStyle notification's transport controls only mean anything for the subuh tarhim,
  since the T3 tone does not go through the `MediaSession`. That is why the notification carries
  an explicit "Matikan" action: an alarm wants "stop", not "pause", and it has to be reachable
  even though the session has nothing playing. Reviving a real pause button would mean wrapping
  `NadaT3` in a `SimpleBasePlayer` and swapping it into the session — possible, but it buys a
  control with the wrong semantics.
- `Izin.lokasiTerkini` deliberately uses `getLastKnownLocation`, never a fresh GPS fix. Prayer times
  barely depend on position: shifting 1 km in any direction moves **none** of the seven times by
  a single second, and 10 km moves one slot by one minute — against a default ihtiyati of two
  minutes and a schedule displayed to the minute. The old code waited on a real fix with no
  timeout, which could hold the service open indefinitely for accuracy that never becomes
  visible. Re-measure with the `PrayTime` harness before reintroducing it.
