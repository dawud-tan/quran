package com.quran.kiblat.salat;

import static java.util.Map.entry;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.location.LocationCompat;
import androidx.core.os.HandlerCompat;

import com.quran.kiblat.salat.ui.util.PrayTime;
import com.quran.kiblat.salat.ui.util.SolarPosition;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

public class Util {

    /**
     * Kunci pref penanda bahwa peringatan kompas sudah pernah dibaca sekali.
     */
    public static final String SUDAH_BACA_KOMPAS = "sudah_baca_kompas";
    private static final ExecutorService eks = Executors.newFixedThreadPool(2);
    private static final Handler hndlr = HandlerCompat.createAsync(Looper.getMainLooper());
    private static final Pattern nmPtrn = Pattern.compile("(?<=\\\"name\\\":\\\")[^\\\"]*");
    private static final Pattern cntyPtrn = Pattern.compile("(?<=\\\"county\\\":\\\")[^\\\"]*");
    private static final String ALAMAT_TIDAK_DIKENAL = "Dimanapun Anda berada";
    // 5 waktu yang dibunyikan. INDEKS_ADZAN menunjuk ke posisi di hasil PrayTime,
    // yang urutannya {subuh, terbit, dzuhur, ashar, terbenam, maghrib, isya}
    private static final String[] NAMA_ADZAN = {"subuh", "dzuhur", "ashar", "maghrib", "isya"};
    private static final int[] INDEKS_ADZAN = {0, 2, 3, 5, 6};
    private static final int IHTIYATI_BAWAAN = 2;   // menit
    private static final int IHTIYATI_MAKS = 10;    // menit
    private static final int BAYANGAN_BAWAAN = 1;   // jumhur
    private static final Locale INDONESIA = new Locale.Builder().setLanguage("id").setScript("Latn").setRegion("ID").build();
    private static final Map<Month, Integer> BULAN_BIASA = Map.ofEntries(entry(Month.JANUARY, 0), entry(Month.FEBRUARY, 4), entry(Month.MARCH, 1), entry(Month.APRIL, 0),
            entry(Month.MAY, 0), entry(Month.JUNE, 4), entry(Month.JULY, 4), entry(Month.AUGUST, 3), entry(Month.SEPTEMBER, 2), entry(Month.OCTOBER, 2), entry(Month.NOVEMBER, 1), entry(Month.DECEMBER, 1));
    private static final Map<Month, Integer> BULAN_KABISAT = Map.ofEntries(entry(Month.JANUARY, 1), entry(Month.FEBRUARY, 0), entry(Month.MARCH, 1), entry(Month.APRIL, 0),
            entry(Month.MAY, 0), entry(Month.JUNE, 4), entry(Month.JULY, 4), entry(Month.AUGUST, 3), entry(Month.SEPTEMBER, 2), entry(Month.OCTOBER, 2), entry(Month.NOVEMBER, 1), entry(Month.DECEMBER, 1));
    private static final Map<Integer, String> PASARAN = Map.ofEntries(entry(0, "Legi"), entry(1, "Pahing"), entry(2, "Pon"),
            entry(3, "Wage"), entry(4, "Kliwon"));

    public static String bacaBerkas(AssetManager assetManager, String fileName) throws IOException {
        InputStream is = assetManager.open(fileName);
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            is.transferTo(result);
        } else {
            byte[] buff = new byte[1000];
            int temp;
            while ((temp = is.read(buff)) != -1) {
                result.write(buff, 0, temp);
            }
        }
        is.close();

        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? result.toString(StandardCharsets.UTF_8) : result.toString();
    }

    public static int jumlahAdzan() {
        return NAMA_ADZAN.length;
    }

    public static String namaAdzan(int urutan) {
        return NAMA_ADZAN[urutan];
    }

    // baru dipasang = semua waktu menyala, sama seperti perilaku sebelum ada setelan ini
    public static boolean adzanAktif(SharedPreferences sharedPref, int urutan) {
        return sharedPref.getBoolean("adzan_" + NAMA_ADZAN[urutan], true);
    }

    // false kalau semua waktu dimatikan, artinya memang tidak ada alarm yang perlu dipasang
    public static boolean adaAdzanAktif(SharedPreferences sharedPref) {
        for (int i = 0; i < NAMA_ADZAN.length; i++) {
            if (adzanAktif(sharedPref, i)) {
                return true;
            }
        }
        return false;
    }

    public static int ihtiyatiMaks() {
        return IHTIYATI_MAKS;
    }

    // ihtiyati: menit pengaman yang ditambahkan ke waktu salat
    public static int ihtiyati(SharedPreferences sharedPref) {
        return Math.max(0, Math.min(IHTIYATI_MAKS, sharedPref.getInt("ihtiyati", IHTIYATI_BAWAAN)));
    }

    // panjang bayangan penanda Ashar: 1 = Syafi'i/Maliki/Hanbali, 2 = Hanafi
    public static int bayanganAshar(SharedPreferences sharedPref) {
        return sharedPref.getInt("bayangan_ashar", BAYANGAN_BAWAAN) == 2 ? 2 : 1;
    }

    // satu-satunya tempat PrayTime dirakit, supaya jadwal yang dibunyikan dan jadwal yang
    // ditampilkan di layar kiblat tidak pernah memakai setelan yang berbeda

    public static void simpanSetelanAdzan(SharedPreferences sharedPref, boolean[] aktif,
                                          int ihtiyati, int bayanganAshar) {
        SharedPreferences.Editor editor = sharedPref.edit();
        for (int i = 0; i < NAMA_ADZAN.length; i++) {
            editor.putBoolean("adzan_" + NAMA_ADZAN[i], aktif[i]);
        }
        editor.putInt("ihtiyati", Math.max(0, Math.min(IHTIYATI_MAKS, ihtiyati)));
        editor.putInt("bayangan_ashar", bayanganAshar == 2 ? 2 : 1);
        editor.apply();
    }

    /**
     * Menjalankan kerja berat di utas latar, hasilnya dikembalikan di utas
     * utama. Memakai ulang eks/hndlr yang sudah ada, supaya tidak ada kolam
     * utas baru cuma untuk satu perhitungan.
     */
    public static <T> void diLatar(Supplier<T> kerja, Consumer<T> selesai) {
        eks.execute(() -> {
            T hasil = kerja.get();
            hndlr.post(() -> selesai.accept(hasil));
        });
    }

    /**
     * Menampilkan keterbatasan sensor magnet ponsel.
     * <p>
     * Ini bukan peringatan basa-basi. Arah kiblatnya sendiri dihitung dengan
     * geodesi Karney di atas elipsoid WGS84, dan galat GPS beberapa meter cuma
     * menggeser azimutnya seperseratus ribu derajat. Yang menentukan ketepatan
     * di lapangan sepenuhnya sensor magnet, yang meleset ribuan kali lebih
     * besar — jadi satu-satunya hal berguna yang bisa dilakukan pengguna ada
     * di daftar ini.
     *
     * @param akurasi nilai SensorManager.SENSOR_STATUS_*, atau -1 kalau sedang
     *                tidak dipicu oleh perubahan akurasi sensor.
     */
    public static void peringatanKompas(Context context, int akurasi) {
        peringatanKompas(context, akurasi, true);
    }

    /**
     * @param kiblat true untuk layar kiblat, yang punya patokan jarak di
     *               Ka'bah; false untuk layar arah terbit/tenggelam, yang
     *               masalah kompasnya sama persis tapi patokannya beda.
     */
    public static void peringatanKompas(Context context, int akurasi, boolean kiblat) {
        StringBuilder isi = new StringBuilder();

        if (akurasi == SensorManager.SENSOR_STATUS_UNRELIABLE
                || akurasi == SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
            isi.append("Sensor magnet ponsel sedang melaporkan ketelitian rendah, ")
                    .append("jadi arah yang ditunjuk jarum saat ini belum bisa dipegang.\n\n");
        }

        if (kiblat) {
            isi.append("Sudut kiblat di layar ini dihitung dari titik lokasi Anda ke Ka'bah ")
                    .append("memakai geodesi Karney di atas elipsoid WGS84. Ketelitiannya jauh ")
                    .append("di bawah seperseribu derajat, dan meleset beberapa meter pun tidak ")
                    .append("mengubahnya.\n\n")
                    .append("Yang membatasi adalah sensor magnet (kompas) ponsel, yang lazimnya ")
                    .append("meleset 5\u00b0 sampai 15\u00b0. Meleset 1\u00b0 saja sudah bergeser ")
                    .append("sekitar 138 km di sekitar Ka'bah, jadi di sinilah seluruh ")
                    .append("ketepatannya ditentukan.\n\n");
        } else {
            isi.append("Azimut matahari di layar ini dihitung dengan algoritma SPA milik NREL, ")
                    .append("teliti sampai sekitar 0,0003\u00b0.\n\n")
                    .append("Yang membatasi adalah sensor magnet (kompas) ponsel, yang lazimnya ")
                    .append("meleset 5\u00b0 sampai 15\u00b0 \u2014 puluhan ribu kali lebih besar ")
                    .append("daripada galat hitungannya. Jadi angka arahnya boleh dipercaya; ")
                    .append("yang perlu diperlakukan hati-hati adalah ke mana jarumnya ")
                    .append("menunjuk.\n\n");
        }

        isi.append("Supaya selisihnya sekecil mungkin:\n")
                .append("\u2022 Menjauhlah dari besi beton, rangka baja, dan lantai bertulang. ")
                .append("Di dalam gedung bertingkat kompas hampir selalu menyimpang.\n")
                .append("\u2022 Jauhkan dari pengeras suara, magnet, dompet atau casing ")
                .append("bermagnet, dan dudukan ponsel bermagnet.\n")
                .append("\u2022 Jauhkan dari laptop, kulkas, dan dasbor mobil.\n")
                .append("\u2022 Kalibrasi dengan menggerakkan ponsel membentuk angka 8 ")
                .append("beberapa kali.\n")
                .append("\u2022 Pegang ponsel mendatar, lalu bandingkan hasilnya dari dua ")
                .append("atau tiga tempat yang berjauhan. Kalau ketiganya sama, barulah ")
                .append("angkanya bisa dipercaya.\n\n")
                .append("Cara yang benar-benar melewati masalah ini adalah memakai matahari, ")
                .append("yang sama sekali tidak terpengaruh medan magnet. Untuk ketepatan ")
                .append("'ainul ka'bah, pakailah baris \"bayangan kiblat\" di layar kiblat: ")
                .append("pada saat itu bayangan benda tegak jatuh tepat di garis kiblat, dan ")
                .append("kompas tidak dipakai sama sekali.");

        new AlertDialog.Builder(context)
                .setTitle(kiblat ? "Ketepatan Arah Kiblat" : "Ketepatan Arah Kompas")
                .setMessage(isi.toString())
                .setPositiveButton("Mengerti", null)
                .show();
    }

    /**
     * true kalau peringatan kompas belum pernah dibaca.
     */
    public static boolean perluSpandukKompas(SharedPreferences sharedPref) {
        return !sharedPref.getBoolean(SUDAH_BACA_KOMPAS, false);
    }

    /**
     * Menandai peringatan kompas sudah dibaca, supaya spanduknya tidak muncul lagi.
     */
    public static void tandaiSpandukKompas(SharedPreferences sharedPref) {
        sharedPref.edit().putBoolean(SUDAH_BACA_KOMPAS, true).apply();
    }

    public static PrayTime penghitungJadwal(SharedPreferences sharedPref) {
        int menit = ihtiyati(sharedPref);
        PrayTime prayers = new PrayTime();
        // urutan offset: {subuh, terbit, dzuhur, ashar, terbenam, maghrib, isya}.
        // Terbit dan terbenam sengaja tidak diberi ihtiyati, keduanya patokan astronomis
        prayers.tune(new int[]{menit, 0, menit, menit, 0, menit, menit});
        prayers.setAsrFactor(bayanganAshar(sharedPref));
        return prayers;
    }

    /**
     * Menyegarkan lokasi kalau boleh, lalu memasang ulang alarm berikutnya.
     * <p>
     * Satu-satunya jalan yang dipakai layar utama, kedua servis, dan penerima
     * siaran boot. Selesai seketika: lokasinya cuma dibaca dari tembolok
     * ({@link Izin#lokasiTerkini}), jadi tidak perlu WorkManager, tidak perlu
     * menunggu fix, dan tidak perlu keluar dari Doze — setAlarmClock sudah
     * mengurus itu.
     *
     * @param dariLatar true kalau tidak ada layar yang terlihat. Lokasi lalu
     *                  hanya dibaca kalau pengguna menyalakan lokasi latar;
     *                  kalau tidak, dipakai lokasi tersimpan terakhir, yaitu
     *                  lokasi saat aplikasi terakhir dibuka.
     */
    public static void segarkanJadwal(Context context, boolean dariLatar, boolean fromBooting) {
        Context aplikasi = context.getApplicationContext();
        SharedPreferences sharedPref = aplikasi.getSharedPreferences("pref", Context.MODE_PRIVATE);
        PowerManager.WakeLock wakeLock = aplikasi.getSystemService(PowerManager.class)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "quran:segarkanJadwal");
        try {
            wakeLock.acquire(60000);
            Location baru = Izin.lokasiTerkini(aplikasi, dariLatar);
            if (baru != null) {
                simpanLokasi(sharedPref, baru);
            }
            // dibaca balik dari simpanan, supaya ketinggian yang tidak dibawa fix
            // jaringan/perkiraan sudah terisi (lihat simpanLokasi)
            cekJadwal(aplikasi, lokasiTerakhir(sharedPref), fromBooting);
        } finally {
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
    }

    public static void cekJadwal(Context context, Location lokasiku, boolean fromBooting) {
        ZoneId z = ZoneId.systemDefault();
        ZonedDateTime sekarang = ZonedDateTime.now(z);
        SolarPosition.SPAData spa = new SolarPosition.SPAData();

        spa.year = sekarang.getYear();
        spa.month = sekarang.getMonthValue();
        spa.day = sekarang.getDayOfMonth();
        spa.hour = sekarang.getHour();
        spa.minute = sekarang.getMinute();
        spa.second = sekarang.getSecond();

        spa.timezone = sekarang.getOffset().getTotalSeconds() / 3600.0;

        spa.longitude = lokasiku.getLongitude();
        spa.latitude = lokasiku.getLatitude();

        spa.delta_ut1 = 0;
        spa.delta_t = SolarPosition.DELTA_T;
        if (LocationCompat.hasMslAltitude(lokasiku)) {
            spa.elevation = LocationCompat.getMslAltitudeMeters(lokasiku);
        } else {
            spa.elevation = lokasiku.getAltitude();
        }
        spa.pressure = 1000;
        spa.temperature = 27.7;
        spa.slope = 0;
        spa.azm_rotation = 180;
        spa.atmos_refract = 0.5667;
        spa.function = SolarPosition.SPA.SPA_ZA_RTS;

        SharedPreferences sharedPref = context.getSharedPreferences("pref", Context.MODE_PRIVATE);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        PrayTime prayers = penghitungJadwal(sharedPref);
        ArrayList<ZonedDateTime> prayerTimes = prayers.getDatePrayerTimes(spa);

        long current = sekarang.toInstant().toEpochMilli();
        Map<String, Object> peta = SolarPosition.rtsSah(spa)
                ? nextEpoch(current, prayerTimes, sharedPref)
                : null;

        long epoch = peta == null ? -1 : (Long) peta.get("epoch");
        if (epoch < 0) {
            // waktu aktif hari ini sudah lewat semua, lompat ke hari berikutnya
            ZonedDateTime besok = sekarang.plusDays(1).truncatedTo(ChronoUnit.DAYS);
            spa.year = besok.getYear();
            spa.month = besok.getMonthValue();
            spa.day = besok.getDayOfMonth();
            spa.hour = besok.getHour();
            spa.minute = besok.getMinute();
            spa.second = besok.getSecond();
            spa.timezone = besok.getOffset().getTotalSeconds() / 3600.0;
            prayerTimes = prayers.getDatePrayerTimes(spa);
            if (SolarPosition.rtsSah(spa)) {
                peta = nextEpoch(current, prayerTimes, sharedPref);
                epoch = (Long) peta.get("epoch");
            }
        }

        if (epoch < 0) {
            // tidak ada waktu yang dinyalakan, atau matahari memang tidak terbit/terbenam di
            // lintang ini hari itu: cabut alarm yang mungkin masih tergantung, jangan pasang apa pun
            batalkanAlarm(context, alarmManager, SiaranSepuluhMenitLalu.class);
            batalkanAlarm(context, alarmManager, SiaranNotifikasiAdzan.class);
            return;
        }

        long sepuluhMenitSebelum = epoch - 600000; //600000 milidetik = 10 menit

        // hanya satu dari dua siaran yang dipasang, satunya lagi dicabut supaya sisa jadwal
        // lama (misal waktunya baru saja dimatikan lewat setelan) tidak ikut berbunyi
        if (current > sepuluhMenitSebelum) {
            batalkanAlarm(context, alarmManager, SiaranSepuluhMenitLalu.class);
            pasangAlarm(context, alarmManager, sharedPref, SiaranNotifikasiAdzan.class, epoch, peta, lokasiku, fromBooting);
        } else {
            batalkanAlarm(context, alarmManager, SiaranNotifikasiAdzan.class);
            pasangAlarm(context, alarmManager, sharedPref, SiaranSepuluhMenitLalu.class, sepuluhMenitSebelum, peta, lokasiku, fromBooting);
        }
    }

    private static void pasangAlarm(Context context, AlarmManager alarmManager, SharedPreferences sharedPref,
                                    Class<?> siaran, long kapan, Map<String, Object> peta,
                                    Location lokasiku, boolean fromBooting) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            return;
        }

        // alarm dipasang lebih dulu memakai alamat yang tersimpan. Kalau menunggu balasan
        // nominatim, jaringan yang lambat ikut menunda setAlarmClock, padahal alamat cuma
        // dipakai sebagai baris kedua notifikasi
        String alamatTersimpan = sharedPref.getString("alamat", ALAMAT_TIDAK_DIKENAL);
        setelAlarm(context, alarmManager, siaran, kapan, peta, alamatTersimpan, fromBooting);

        eks.execute(() -> {
            String alamat = cariAlamat(lokasiku.getLatitude(), lokasiku.getLongitude());
            if (alamat == null || alamat.equals(alamatTersimpan)) {
                return;
            }
            sharedPref.edit().putString("alamat", alamat).apply();
            hndlr.post(() -> {
                // penjadwalan yang lebih baru mungkin sudah mencabut alarm ini selagi
                // menunggu jaringan; jangan dihidupkan lagi
                PendingIntent masihAda = PendingIntent
                        .getBroadcast(context,
                                1,
                                new Intent(context, siaran),
                                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
                if (masihAda != null) {
                    setelAlarm(context, alarmManager, siaran, kapan, peta, alamat, fromBooting);
                }
            });
        });
    }

    private static void setelAlarm(Context context, AlarmManager alarmManager, Class<?> siaran,
                                   long kapan, Map<String, Object> peta, String lokasitks, boolean fromBooting) {
        Intent notifyIntent = new Intent(context, siaran);
        notifyIntent.putExtra("nama", String.valueOf(peta.get("nama")));
        notifyIntent.putExtra("waktu", String.valueOf(peta.get("waktu")));
        notifyIntent.putExtra("lokasi", lokasitks);
        notifyIntent.putExtra("fromBooting", fromBooting);

        PendingIntent pendingIntent = PendingIntent
                .getBroadcast(context,
                        1,
                        notifyIntent,
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager.AlarmClockInfo ac = new AlarmManager.AlarmClockInfo(kapan, pendingIntent);
        alarmManager.setAlarmClock(ac, pendingIntent);
    }

    private static void batalkanAlarm(Context context, AlarmManager alarmManager, Class<?> siaran) {
        PendingIntent pendingIntent = PendingIntent
                .getBroadcast(context,
                        1,
                        new Intent(context, siaran),
                        PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    // waktu aktif terdekat setelah current, atau epoch -1 kalau tidak ada lagi hari itu
    private static Map<String, Object> nextEpoch(long current, ArrayList<ZonedDateTime> prayerTimes, SharedPreferences sharedPref) {
        Map<String, Object> peta = new HashMap<>();
        peta.put("nama", "subuh");
        peta.put("waktu", "-");
        peta.put("epoch", -1L);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("kk:mm:ss");

        for (int i = 0; i < NAMA_ADZAN.length; i++) {
            if (!adzanAktif(sharedPref, i)) {
                continue;
            }
            ZonedDateTime zdt = prayerTimes.get(INDEKS_ADZAN[i]);
            long waktu = zdt.toInstant().toEpochMilli();
            if (current - waktu < 0) {
                peta.put("nama", NAMA_ADZAN[i]);
                peta.put("waktu", dtf.format(zdt));
                peta.put("epoch", waktu);
                return peta;
            }
        }
        return peta;
    }

    // null kalau gagal, supaya pemanggil bisa tetap memakai alamat yang tersimpan
    private static String cariAlamat(double lat, double lon) {
        HttpsURLConnection c = null;
        try {
            URL u = new URL("https://nominatim.openstreetmap.org/reverse?format=geocodejson&lat=" + lat + "&lon=" + lon + "&zoom=13&layer=address&email=muhammad.dawud91%40gmail.com");
            c = (HttpsURLConnection) u.openConnection();
            //verifikasi sertifikat dan nama host dibiarkan bawaan platform: nominatim memakai
            //sertifikat yang sah, jadi tidak ada alasan mematikannya
            c.setConnectTimeout(10000);
            c.setReadTimeout(10000);
            c.setRequestProperty("Accept", "application/json");
            c.setRequestProperty("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7");
            //syarat pemakaian nominatim: setiap permintaan harus bisa dikenali
            c.setRequestProperty("User-Agent", BuildConfig.APPLICATION_ID + "/" + BuildConfig.VERSION_NAME);

            if (c.getResponseCode() / 100 != 2) {
                return null;
            }

            String json = new String(readResponse(c), StandardCharsets.UTF_8);

            StringBuilder sb = new StringBuilder();
            Matcher nmMtch = nmPtrn.matcher(json);
            if (nmMtch.find()) {
                sb.append(nmMtch.group(0)).append(", ");
            }
            Matcher cntyMtch = cntyPtrn.matcher(json);
            if (cntyMtch.find()) {
                sb.append(cntyMtch.group(0));
            }

            String alamat = sb.toString().trim();
            return alamat.isEmpty() ? null : alamat;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        } finally {
            if (c != null) {
                try {
                    c.disconnect();
                } catch (Exception ignored) {
                }
            }
        }
    }

    // dibaca sampai habis, bukan sampai getContentLength(): respons ber-chunk tidak
    // mengirim Content-Length dan nilainya -1
    private static byte[] readResponse(HttpsURLConnection connection) throws IOException {
        try (InputStream inputStream = connection.getInputStream()) {
            ByteArrayOutputStream hasil = new ByteArrayOutputStream();
            byte[] buff = new byte[4096];
            int terbaca;
            while ((terbaca = inputStream.read(buff)) != -1) {
                hasil.write(buff, 0, terbaca);
            }
            return hasil.toByteArray();
        }
    }

    public static String getLatitudeAsDMS(Location location, int decimalPlace) {
        String strLatitude = Location.convert(location.getLatitude(), Location.FORMAT_SECONDS);
        strLatitude = replaceDelimiters(strLatitude, decimalPlace);
        strLatitude = strLatitude + " LS";
        return strLatitude;
    }

    public static String getLongitudeAsDMS(Location location, int decimalPlace) {
        String strLongitude = Location.convert(location.getLongitude(), Location.FORMAT_SECONDS);
        strLongitude = replaceDelimiters(strLongitude, decimalPlace);
        strLongitude = strLongitude + " BT";
        return strLongitude;
    }

    @NonNull
    private static String replaceDelimiters(String str, int decimalPlace) {
        str = str.replaceFirst(":", "°");
        str = str.replaceFirst(":", "'");
        int pointIndex = str.indexOf(".");
        int endIndex = pointIndex + 1 + decimalPlace;
        if (endIndex < str.length()) {
            str = str.substring(0, endIndex);
        }
        str = str + "″";
        return str;
    }

    public static Location lokasiTerakhir(SharedPreferences sharedPref) {
        Location lokasi = new Location("lokasiku");
        lokasi.setLatitude(Double.parseDouble(sharedPref.getString("latitude", "-2.548925")));
        lokasi.setLongitude(Double.parseDouble(sharedPref.getString("longitude", "118.014864")));
        lokasi.setAltitude(Double.parseDouble(sharedPref.getString("altitude", "0")));
        lokasi.setAccuracy(Float.parseFloat(sharedPref.getString("accuracy", "1000000")));
        return lokasi;
    }

    public static void simpanLokasi(SharedPreferences sharedPref, Location lokasi) {
        // GPS memberi tinggi terhadap elipsoid WGS84, sedangkan kerendahan ufuk butuh tinggi
        // terhadap permukaan laut. Selisihnya di Indonesia bisa puluhan meter, jadi yang
        // disimpan selalu versi MSL supaya sama dengan yang dipakai saat menghitung jadwal.
        //
        // Fix jaringan dan lokasi perkiraan (izin lokasi kasar) tidak membawa ketinggian sama
        // sekali. Tidak diketahui bukan berarti nol: di Bandung (768 m) menganggapnya nol
        // memundurkan terbit 4 menit dan memajukan maghrib 4 menit, jadi ketinggian terakhir
        // yang benar-benar terukur tetap dipakai.
        double ketinggian;
        if (LocationCompat.hasMslAltitude(lokasi)) {
            ketinggian = LocationCompat.getMslAltitudeMeters(lokasi);
        } else if (lokasi.hasAltitude()) {
            ketinggian = lokasi.getAltitude();
        } else {
            ketinggian = Double.parseDouble(sharedPref.getString("altitude", "0"));
        }

        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("latitude", Double.toString(lokasi.getLatitude()));
        editor.putString("longitude", Double.toString(lokasi.getLongitude()));
        editor.putString("altitude", Double.toString(ketinggian));
        editor.putString("accuracy", Float.toString(lokasi.getAccuracy()));
        editor.apply();
    }

    public static DateTimeFormatter tanggalWeton(final int tahun, final Month bulan, final int tanggal) {
        boolean kabisat = (tahun & 3) == 0 && ((tahun % 25) != 0 || (tahun & 15) == 0);
        int jumlahKabisat = tahun / 4 - tahun / 100 * 25;

        int nilai_bulan = kabisat ? BULAN_KABISAT.get(bulan) : BULAN_BIASA.get(bulan);
        return new DateTimeFormatterBuilder()
                .appendPattern("eeee")
                .appendLiteral(" ")
                .appendLiteral(PASARAN.get(Math.floorMod(jumlahKabisat + tanggal - nilai_bulan, 5)))
                .toFormatter(INDONESIA);
    }

    public static boolean isAirplaneModeOff(Context context) {
        return Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) == 0;
    }

}