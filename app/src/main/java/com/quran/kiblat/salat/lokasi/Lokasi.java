package com.quran.kiblat.salat.lokasi;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;

import androidx.core.location.LocationCompat;

import com.quran.kiblat.salat.izin.Izin;
import com.quran.kiblat.salat.umum.Pref;

/**
 * Lokasi pengguna: membaca dari ponsel, menyimpan, dan membaca balik.
 * <p>
 * Aturan pentingnya: lokasi yang dipakai menghitung jadwal <b>selalu</b>
 * dibaca balik dari simpanan ({@link #terakhir}), bukan langsung dari fix
 * yang baru datang. Hanya dengan begitu ketinggian yang tidak dibawa fix
 * jaringan/perkiraan terisi dari simpanan, dan jadwal yang dibunyikan sama
 * persis dengan jadwal yang ditampilkan.
 */
public final class Lokasi {

    /**
     * Titik tengah Indonesia, dipakai kalau belum pernah ada lokasi sama
     * sekali. Untuk Jakarta jadwalnya meleset 37–52 menit.
     */
    private static final String LINTANG_BAWAAN = "-2.548925";
    private static final String BUJUR_BAWAAN = "118.014864";

    private static final String LINTANG = "latitude";
    private static final String BUJUR = "longitude";
    private static final String KETINGGIAN = "altitude";
    private static final String AKURASI = "accuracy";

    private Lokasi() {
    }

    /**
     * Membaca lokasi ponsel kalau boleh, menyimpannya, lalu mengembalikan
     * lokasi tersimpan. Tidak pernah null: kalau tidak boleh atau belum ada,
     * yang dikembalikan lokasi tersimpan terakhir (atau titik tengah
     * Indonesia).
     *
     * @param dariLatar lihat {@link #terkini}
     */
    public static Location perbarui(Context context, boolean dariLatar) {
        SharedPreferences sharedPref = Pref.dari(context);
        Location baru = terkini(context, dariLatar);
        if (baru != null) {
            simpan(sharedPref, baru);
        }
        return terakhir(sharedPref);
    }

    /**
     * Lokasi terakhir yang sudah diketahui ponsel, kalau boleh dibaca dari
     * tempat pemanggilnya; null kalau tidak boleh atau belum ada, dan pemanggil
     * memakai {@link #terakhir}.
     * <p>
     * Sengaja getLastKnownLocation, bukan meminta fix baru: GPS tidak pernah
     * dinyalakan, jadi tidak ada yang perlu membangunkan ponsel dari Doze dan
     * tidak ada yang bisa menggantung. Jadwal salat juga nyaris tidak peka
     * terhadap posisi — digeser 1 km, ketujuh waktunya tidak berubah satu detik
     * pun; sampai sekitar 40 km paling banyak satu menit. Bandingkan dengan
     * ihtiyati yang 2 menit.
     *
     * @param dariLatar true dari servis dan penerima siaran, yang tidak punya
     *                  layar terlihat: lokasi hanya dibaca kalau sakelar lokasi
     *                  latar menyala. false dari layar aplikasi: cukup izin
     *                  lokasi biasa.
     */
    public static Location terkini(Context context, boolean dariLatar) {
        if (dariLatar ? !Izin.lokasiLatarAktif(context) : !Izin.lokasi(context)) {
            return null;
        }
        LocationManager lm = context.getSystemService(LocationManager.class);
        Location gps = dariPenyedia(lm, LocationManager.GPS_PROVIDER);
        return gps != null ? gps : dariPenyedia(lm, LocationManager.NETWORK_PROVIDER);
    }

    @SuppressLint("MissingPermission") // sudah diperiksa di terkini
    private static Location dariPenyedia(LocationManager lm, String penyedia) {
        try {
            return lm.isProviderEnabled(penyedia) ? lm.getLastKnownLocation(penyedia) : null;
        } catch (RuntimeException ex) {
            // penyedia tidak ada di perangkat ini, atau izinnya baru saja dicabut
            return null;
        }
    }

    /**
     * Lokasi tersimpan terakhir. Ketinggiannya sudah MSL.
     */
    public static Location terakhir(SharedPreferences sharedPref) {
        Location lokasi = new Location("lokasiku");
        lokasi.setLatitude(Double.parseDouble(sharedPref.getString(LINTANG, LINTANG_BAWAAN)));
        lokasi.setLongitude(Double.parseDouble(sharedPref.getString(BUJUR, BUJUR_BAWAAN)));
        lokasi.setAltitude(Double.parseDouble(sharedPref.getString(KETINGGIAN, "0")));
        lokasi.setAccuracy(Float.parseFloat(sharedPref.getString(AKURASI, "1000000")));
        return lokasi;
    }

    public static void simpan(SharedPreferences sharedPref, Location lokasi) {
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
            ketinggian = Double.parseDouble(sharedPref.getString(KETINGGIAN, "0"));
        }

        sharedPref.edit()
                .putString(LINTANG, Double.toString(lokasi.getLatitude()))
                .putString(BUJUR, Double.toString(lokasi.getLongitude()))
                .putString(KETINGGIAN, Double.toString(ketinggian))
                .putString(AKURASI, Float.toString(lokasi.getAccuracy()))
                .apply();
    }

    /**
     * Ketinggian MSL kalau lokasinya membawa itu, kalau tidak ketinggian
     * apa adanya. Lokasi dari {@link #terakhir} sudah MSL.
     */
    public static double ketinggianMsl(Location lokasi) {
        return LocationCompat.hasMslAltitude(lokasi)
                ? LocationCompat.getMslAltitudeMeters(lokasi)
                : lokasi.getAltitude();
    }

    /**
     * Misalnya "6°12'31.68″ LS, 106°50'44.16″ BT".
     */
    public static String teksKoordinat(Location lokasi) {
        return derajatMenitDetik(lokasi.getLatitude(), "LU", "LS") + ", "
                + derajatMenitDetik(lokasi.getLongitude(), "BT", "BB");
    }

    /**
     * Arah mata angin ditulis sebagai akhiran, jadi angkanya tanpa tanda
     * minus. Dulu tandanya ikut tertulis dan akhirannya selalu "LS", jadi
     * Aceh dan Sulawesi Utara tertulis di lintang selatan.
     */
    private static String derajatMenitDetik(double derajat, String positif, String negatif) {
        String dms = Location.convert(Math.abs(derajat), Location.FORMAT_SECONDS)
                .replaceFirst(":", "°")
                .replaceFirst(":", "'");
        return dms + "″ " + (derajat < 0 ? negatif : positif);
    }
}
