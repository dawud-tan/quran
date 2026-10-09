package com.quran.kiblat.salat.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.PowerManager;

import com.quran.kiblat.salat.izin.Izin;
import com.quran.kiblat.salat.jadwal.JadwalSalat;
import com.quran.kiblat.salat.lokasi.Alamat;
import com.quran.kiblat.salat.lokasi.Lokasi;
import com.quran.kiblat.salat.umum.Latar;
import com.quran.kiblat.salat.umum.Pref;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Memasang satu-satunya alarm adzan yang tertunda.
 * <pre>
 * cekJadwal()
 *   ├─ waktu aktif berikutnya: hari ini, kalau tidak ada besok, kalau tidak ada tidak apa-apa
 *   ├─ masih &gt; 10 menit → SiaranSepuluhMenitLalu → Servis10Menit (nada T3 / tarhim)
 *   └─ ≤ 10 menit       → SiaranNotifikasiAdzan  → ServisAdzan    (adzan)
 * </pre>
 * Setiap jalan keluar kedua servis berakhir di onDestroy, yang memanggil
 * {@link #segarkanJadwal} lagi — begitulah rantainya bersambung.
 */
public final class PenjadwalAdzan {

    /**
     * Nama ekstra Intent yang dibawa alarm sampai ke servis. Jangan diganti:
     * alarm yang dipasang versi sebelumnya membawa nama-nama ini juga.
     */
    static final String EKSTRA_NAMA = "nama";
    static final String EKSTRA_WAKTU = "waktu";
    static final String EKSTRA_LOKASI = "lokasi";

    /**
     * Kedua PendingIntent alarm memakai kode yang sama tetapi komponennya
     * beda, jadi tetap dua alarm yang terpisah.
     */
    private static final int KODE_PERMINTAAN = 1;
    private static final long SEPULUH_MENIT = 10 * 60 * 1000L;

    private PenjadwalAdzan() {
    }

    /**
     * Menyegarkan lokasi kalau boleh, lalu memasang ulang alarm berikutnya.
     * <p>
     * Satu-satunya jalan yang dipakai layar utama, kedua servis, dan penerima
     * siaran boot. Selesai seketika: lokasinya cuma dibaca dari tembolok
     * ({@link Lokasi#terkini}), jadi tidak perlu WorkManager, tidak perlu
     * menunggu fix, dan tidak perlu keluar dari Doze — setAlarmClock sudah
     * mengurus itu.
     *
     * @param dariLatar true kalau tidak ada layar yang terlihat. Lokasi lalu
     *                  hanya dibaca kalau pengguna menyalakan lokasi latar;
     *                  kalau tidak, dipakai lokasi tersimpan terakhir, yaitu
     *                  lokasi saat aplikasi terakhir dibuka.
     */
    public static void segarkanJadwal(Context context, boolean dariLatar) {
        Context aplikasi = context.getApplicationContext();
        PowerManager.WakeLock wakeLock = aplikasi.getSystemService(PowerManager.class)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "quran:segarkanJadwal");
        try {
            wakeLock.acquire(60000);
            // dibaca balik dari simpanan, supaya ketinggian yang tidak dibawa fix
            // jaringan/perkiraan sudah terisi (lihat Lokasi.simpan)
            cekJadwal(aplikasi, Lokasi.perbarui(aplikasi, dariLatar));
        } finally {
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
        }
    }

    /**
     * Memasang alarm untuk waktu aktif berikutnya, dan mencabut alarm yang
     * tidak dipakai lagi. Dipanggil langsung oleh Pengaturan Adzan sesudah
     * disimpan, supaya waktu yang baru dimatikan tidak jadi berbunyi.
     */
    public static void cekJadwal(Context context, Location lokasiku) {
        Context aplikasi = context.getApplicationContext();
        SharedPreferences sharedPref = Pref.dari(aplikasi);
        AlarmManager alarmManager = aplikasi.getSystemService(AlarmManager.class);

        ZonedDateTime sekarang = ZonedDateTime.now(ZoneId.systemDefault());
        long kini = sekarang.toInstant().toEpochMilli();

        WaktuAdzan berikut = berikutnya(sharedPref, lokasiku, sekarang, kini);
        if (berikut == null) {
            // waktu aktif hari ini sudah lewat semua, lompat ke hari berikutnya
            ZonedDateTime besok = sekarang.plusDays(1).truncatedTo(ChronoUnit.DAYS);
            berikut = berikutnya(sharedPref, lokasiku, besok, kini);
        }

        if (berikut == null) {
            // tidak ada waktu yang dinyalakan, atau matahari memang tidak terbit/terbenam di
            // lintang ini hari itu: cabut alarm yang mungkin masih tergantung, jangan pasang apa pun
            batalkanAlarm(aplikasi, alarmManager, SiaranSepuluhMenitLalu.class);
            batalkanAlarm(aplikasi, alarmManager, SiaranNotifikasiAdzan.class);
            return;
        }

        long sepuluhMenitSebelum = berikut.epoch() - SEPULUH_MENIT;

        // hanya satu dari dua siaran yang dipasang, satunya lagi dicabut supaya sisa jadwal
        // lama (misal waktunya baru saja dimatikan lewat setelan) tidak ikut berbunyi
        if (kini > sepuluhMenitSebelum) {
            batalkanAlarm(aplikasi, alarmManager, SiaranSepuluhMenitLalu.class);
            pasangAlarm(aplikasi, alarmManager, sharedPref, SiaranNotifikasiAdzan.class,
                    berikut.epoch(), berikut, lokasiku);
        } else {
            batalkanAlarm(aplikasi, alarmManager, SiaranNotifikasiAdzan.class);
            pasangAlarm(aplikasi, alarmManager, sharedPref, SiaranSepuluhMenitLalu.class,
                    sepuluhMenitSebelum, berikut, lokasiku);
        }
    }

    /**
     * Waktu aktif terdekat sesudah {@code kini} pada tanggal {@code hari},
     * atau null kalau tidak ada lagi hari itu.
     */
    private static WaktuAdzan berikutnya(SharedPreferences sharedPref, Location lokasi,
                                         ZonedDateTime hari, long kini) {
        List<ZonedDateTime> jadwal = JadwalSalat.hitung(sharedPref, lokasi, hari);
        if (jadwal == null) {
            return null;
        }
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("kk:mm:ss");
        for (int i = 0; i < JadwalSalat.jumlahAdzan(); i++) {
            if (!JadwalSalat.adzanAktif(sharedPref, i)) {
                continue;
            }
            ZonedDateTime zdt = jadwal.get(JadwalSalat.indeksAdzan(i));
            long epoch = zdt.toInstant().toEpochMilli();
            if (epoch > kini) {
                return new WaktuAdzan(JadwalSalat.namaAdzan(i), dtf.format(zdt), epoch);
            }
        }
        return null;
    }

    private static void pasangAlarm(Context context, AlarmManager alarmManager, SharedPreferences sharedPref,
                                    Class<?> siaran, long kapan, WaktuAdzan waktu, Location lokasiku) {
        if (!Izin.alarmTepat(context)) {
            return;
        }

        // alarm dipasang lebih dulu memakai alamat yang tersimpan. Kalau menunggu balasan
        // nominatim, jaringan yang lambat ikut menunda setAlarmClock, padahal alamat cuma
        // dipakai sebagai baris kedua notifikasi
        String alamatTersimpan = Alamat.tersimpan(sharedPref);
        setelAlarm(context, alarmManager, siaran, kapan, waktu, alamatTersimpan);

        Latar.jalankan(() -> {
            String alamat = Alamat.cari(lokasiku.getLatitude(), lokasiku.getLongitude());
            if (alamat == null || alamat.equals(alamatTersimpan)) {
                return;
            }
            Alamat.simpan(sharedPref, alamat);
            Latar.keUtama(() -> {
                // penjadwalan yang lebih baru mungkin sudah mencabut alarm ini selagi
                // menunggu jaringan; jangan dihidupkan lagi
                if (alarmYangAda(context, siaran) != null) {
                    setelAlarm(context, alarmManager, siaran, kapan, waktu, alamat);
                }
            });
        });
    }

    private static void setelAlarm(Context context, AlarmManager alarmManager, Class<?> siaran,
                                   long kapan, WaktuAdzan waktu, String alamat) {
        Intent niat = new Intent(context, siaran)
                .putExtra(EKSTRA_NAMA, waktu.nama())
                .putExtra(EKSTRA_WAKTU, waktu.jam())
                .putExtra(EKSTRA_LOKASI, alamat);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, KODE_PERMINTAAN, niat,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(kapan, pendingIntent), pendingIntent);
    }

    private static void batalkanAlarm(Context context, AlarmManager alarmManager, Class<?> siaran) {
        PendingIntent pendingIntent = alarmYangAda(context, siaran);
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    /**
     * PendingIntent alarm yang sedang terpasang untuk {@code siaran}, atau
     * null kalau tidak ada. Ekstra tidak ikut menentukan identitasnya.
     */
    private static PendingIntent alarmYangAda(Context context, Class<?> siaran) {
        return PendingIntent.getBroadcast(context, KODE_PERMINTAAN, new Intent(context, siaran),
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
    }

    /**
     * @param nama  "subuh", "dzuhur", ...
     * @param jam   untuk ditampilkan, "kk:mm:ss"
     * @param epoch milidetik
     */
    private record WaktuAdzan(String nama, String jam, long epoch) {
    }
}
