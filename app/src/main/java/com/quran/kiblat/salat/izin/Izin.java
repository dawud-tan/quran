package com.quran.kiblat.salat.izin;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;

import com.quran.kiblat.salat.umum.Pref;

import java.util.ArrayList;

/**
 * Satu-satunya tempat yang menjawab "boleh atau tidak" untuk setiap izin yang
 * dipakai aplikasi, dan jalan ke layar setelannya masing-masing. Pembacaan
 * lokasinya sendiri ada di {@link com.quran.kiblat.salat.lokasi.Lokasi#terkini}.
 * <p>
 * Pembagiannya, dari yang paling penting:
 * <ul>
 * <li>Alarm tepat waktu — wajib. Tanpa ini tidak ada alarm yang dipasang, dan
 *     sejak Android 17 izin ini pula yang membolehkan servis adzan menyentuh
 *     volume alarm dari latar belakang.</li>
 * <li>Notifikasi, layar penuh, lokasi saat dipakai — menyempurnakan; tiap-tiap
 *     punya jalan mundur kalau ditolak.</li>
 * <li>Lokasi latar belakang — pilihan, mati kecuali dinyalakan sendiri oleh
 *     pengguna lewat {@link PengaturanIzin}.</li>
 * <li>Pengecualian optimasi baterai — tidak pernah diminta langsung.
 *     setAlarmClock() sudah membuat sistem keluar dari Doze sebelum alarmnya
 *     berbunyi, dan lokasinya cuma dibaca dari tembolok, jadi tidak ada fungsi
 *     inti yang terganggu Doze. Kebijakan Google Play melarang meminta
 *     pengecualian itu kalau fungsi intinya tidak terganggu.</li>
 * </ul>
 */
public final class Izin {

    /**
     * Pilihan pengguna untuk lokasi latar. Belum pernah disimpan = ikut izin
     * sistemnya, lihat {@link #lokasiLatarAktif}.
     */
    public static final String LOKASI_LATAR = "lokasi_latar";
    /**
     * Penanda bahwa penjelasan izin di awal sudah pernah ditampilkan sekali.
     */
    private static final String SUDAH_TANYA = "sudah_tanya_izin";

    private Izin() {
    }

    private static SharedPreferences pref(Context context) {
        return Pref.dari(context);
    }

    private static boolean diberikan(Context context, String izin) {
        return ContextCompat.checkSelfPermission(context, izin) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean alarmTepat(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                || context.getSystemService(AlarmManager.class).canScheduleExactAlarms();
    }

    /**
     * Mencakup izin POST_NOTIFICATIONS (Android 13+) dan juga sakelar
     * notifikasi aplikasi yang bisa dimatikan pengguna di versi berapa pun.
     */
    public static boolean notifikasi(Context context) {
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    /**
     * true kalau notifikasi di saluran ini benar-benar akan tampil. Kalau tidak,
     * tombol "Matikan" dan layar penuhnya tidak pernah sampai ke pengguna.
     */
    public static boolean notifikasiTampil(Context context, String saluran) {
        if (!notifikasi(context)) {
            return false;
        }
        NotificationChannel c = context.getSystemService(NotificationManager.class).getNotificationChannel(saluran);
        return c == null || c.getImportance() != NotificationManager.IMPORTANCE_NONE;
    }

    public static boolean layarPenuh(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                || context.getSystemService(NotificationManager.class).canUseFullScreenIntent();
    }

    public static boolean lokasi(Context context) {
        return diberikan(context, Manifest.permission.ACCESS_FINE_LOCATION)
                || diberikan(context, Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    /**
     * Lokasi perkiraan tidak membawa ketinggian, padahal ketinggian menggeser
     * terbit dan maghrib (Bandung 768 m: 4 menit).
     */
    public static boolean lokasiTepat(Context context) {
        return diberikan(context, Manifest.permission.ACCESS_FINE_LOCATION);
    }

    public static boolean layananLokasiHidup(Context context) {
        return LocationManagerCompat.isLocationEnabled(context.getSystemService(LocationManager.class));
    }

    /**
     * true kalau ACCESS_BACKGROUND_LOCATION tercantum di manifest. Menghapus
     * baris itu sudah cukup untuk membuat build tanpa lokasi latar — misalnya
     * kalau deklarasinya ditolak Google Play: sakelarnya ikut hilang dan jadwal
     * tetap berjalan dengan lokasi saat aplikasi dibuka.
     */
    public static boolean lokasiLatarTersedia(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    ? pm.getPackageInfo(context.getPackageName(), PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS))
                    : infoIzinLama(pm, context.getPackageName());
            if (info.requestedPermissions != null) {
                for (String izin : info.requestedPermissions) {
                    if (Manifest.permission.ACCESS_BACKGROUND_LOCATION.equals(izin)) {
                        return true;
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    private static PackageInfo infoIzinLama(PackageManager pm, String paket) throws PackageManager.NameNotFoundException {
        return pm.getPackageInfo(paket, PackageManager.GET_PERMISSIONS);
    }

    /**
     * Izin sistemnya saja, tanpa melihat pilihan pengguna.
     */
    public static boolean izinLokasiLatar(Context context) {
        return lokasi(context) && diberikan(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION);
    }

    /**
     * Sakelar lokasi latar: pengguna memilihnya dan izin sistemnya memang ada.
     * <p>
     * Kalau belum pernah disentuh, ikut izin yang sudah ada, supaya pemasangan
     * lama yang dulu sudah memberi "izinkan sepanjang waktu" tidak berubah
     * perilakunya. Pemasangan baru tidak pernah punya izin itu sampai pengguna
     * sendiri menyalakannya.
     */
    public static boolean lokasiLatarAktif(Context context) {
        return izinLokasiLatar(context) && pref(context).getBoolean(LOKASI_LATAR, true);
    }

    public static void setelLokasiLatar(Context context, boolean aktif) {
        pref(context).edit().putBoolean(LOKASI_LATAR, aktif).apply();
    }

    public static boolean bebasOptimasiBaterai(Context context) {
        return context.getSystemService(PowerManager.class).isIgnoringBatteryOptimizations(context.getPackageName());
    }

    public static boolean perluTanyaAwal(Context context) {
        return !pref(context).getBoolean(SUDAH_TANYA, false);
    }

    public static void tandaiSudahTanya(Context context) {
        pref(context).edit().putBoolean(SUDAH_TANYA, true).apply();
    }

    /**
     * Izin waktu-jalan yang ditanyakan di awal. Lokasi latar sengaja tidak ikut.
     */
    public static String[] izinAwal(Context context) {
        ArrayList<String> daftar = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && !diberikan(context, Manifest.permission.POST_NOTIFICATIONS)) {
            daftar.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (!lokasiTepat(context)) {
            daftar.add(Manifest.permission.ACCESS_FINE_LOCATION);
            daftar.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        return daftar.toArray(new String[0]);
    }

    private static Uri paket(Context context) {
        return Uri.parse("package:" + context.getPackageName());
    }

    public static Intent setelanAplikasi(Context context) {
        return new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, paket(context));
    }

    public static Intent setelanAlarmTepat(Context context) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                ? new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, paket(context))
                : setelanAplikasi(context);
    }

    public static Intent setelanLayarPenuh(Context context) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                ? new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, paket(context))
                : setelanAplikasi(context);
    }

    public static Intent setelanNotifikasi(Context context) {
        return new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
    }

    public static Intent setelanLayananLokasi() {
        return new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
    }

    /**
     * Membuka layar setelan; kalau layar itu tidak ada di ponsel ini (sebagian
     * ROM membuangnya), jatuh ke halaman info aplikasi.
     */
    public static void buka(Context context, Intent setelan) {
        try {
            context.startActivity(setelan);
        } catch (ActivityNotFoundException ex) {
            context.startActivity(setelanAplikasi(context));
        }
    }
}
