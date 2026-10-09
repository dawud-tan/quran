package com.quran.kiblat.salat.jadwal;

import android.content.SharedPreferences;
import android.location.Location;

import com.quran.kiblat.salat.hisab.MasukanSpa;
import com.quran.kiblat.salat.hisab.PrayTime;
import com.quran.kiblat.salat.hisab.SolarPosition;
import com.quran.kiblat.salat.lokasi.Lokasi;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Jadwal salat beserta setelannya: waktu mana yang dibunyikan, ihtiyati, dan
 * cara hitung Ashar.
 * <p>
 * Satu-satunya tempat {@link PrayTime} dirakit ({@link #penghitung}), supaya
 * jadwal yang dibunyikan alarm dan jadwal yang ditampilkan di layar kiblat
 * tidak pernah memakai setelan yang berbeda. Jangan memanggil
 * {@code new PrayTime()} di tempat lain.
 * <p>
 * Hasil {@link PrayTime} selalu tujuh slot dengan urutan tetap, pakai
 * konstanta di bawah untuk mengambilnya.
 */
public final class JadwalSalat {

    public static final int SUBUH = 0;
    public static final int TERBIT = 1;
    public static final int DZUHUR = 2;
    public static final int ASHAR = 3;
    public static final int TERBENAM = 4;
    public static final int MAGHRIB = 5;
    public static final int ISYA = 6;

    // 5 waktu yang dibunyikan. INDEKS_ADZAN menunjuk ke slot di hasil PrayTime.
    // Menambah waktu keenam cukup di dua larik ini; dialog Pengaturan Adzan
    // membuat kotak centangnya sendiri dari sini.
    private static final String[] NAMA_ADZAN = {"subuh", "dzuhur", "ashar", "maghrib", "isya"};
    private static final int[] INDEKS_ADZAN = {SUBUH, DZUHUR, ASHAR, MAGHRIB, ISYA};

    private static final int IHTIYATI_BAWAAN = 2;   // menit
    private static final int IHTIYATI_MAKS = 10;    // menit
    private static final int BAYANGAN_BAWAAN = 1;   // jumhur

    private static final String AWALAN_ADZAN = "adzan_";
    private static final String IHTIYATI = "ihtiyati";
    private static final String BAYANGAN_ASHAR = "bayangan_ashar";

    private JadwalSalat() {
    }

    public static int jumlahAdzan() {
        return NAMA_ADZAN.length;
    }

    public static String namaAdzan(int urutan) {
        return NAMA_ADZAN[urutan];
    }

    /**
     * Slot hasil {@link PrayTime} untuk adzan ke-{@code urutan}.
     */
    public static int indeksAdzan(int urutan) {
        return INDEKS_ADZAN[urutan];
    }

    // baru dipasang = semua waktu menyala, sama seperti perilaku sebelum ada setelan ini
    public static boolean adzanAktif(SharedPreferences sharedPref, int urutan) {
        return sharedPref.getBoolean(AWALAN_ADZAN + NAMA_ADZAN[urutan], true);
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

    /**
     * Menit pengaman yang ditambahkan ke lima waktu salat. Bukan koreksi
     * hitungan: hitungannya sudah teliti, ini kehati-hatian yang disengaja.
     */
    public static int ihtiyati(SharedPreferences sharedPref) {
        return Math.max(0, Math.min(IHTIYATI_MAKS, sharedPref.getInt(IHTIYATI, IHTIYATI_BAWAAN)));
    }

    /**
     * Panjang bayangan penanda Ashar: 1 = Syafi'i/Maliki/Hanbali, 2 = Hanafi.
     * Faktor 2 memundurkan Ashar sekitar satu jam.
     */
    public static int bayanganAshar(SharedPreferences sharedPref) {
        return sharedPref.getInt(BAYANGAN_ASHAR, BAYANGAN_BAWAAN) == 2 ? 2 : 1;
    }

    public static void simpanSetelan(SharedPreferences sharedPref, boolean[] aktif,
                                     int ihtiyati, int bayanganAshar) {
        SharedPreferences.Editor editor = sharedPref.edit();
        for (int i = 0; i < NAMA_ADZAN.length; i++) {
            editor.putBoolean(AWALAN_ADZAN + NAMA_ADZAN[i], aktif[i]);
        }
        editor.putInt(IHTIYATI, Math.max(0, Math.min(IHTIYATI_MAKS, ihtiyati)));
        editor.putInt(BAYANGAN_ASHAR, bayanganAshar == 2 ? 2 : 1);
        editor.apply();
    }

    /**
     * {@link PrayTime} dengan ihtiyati dan cara hitung Ashar pengguna.
     */
    public static PrayTime penghitung(SharedPreferences sharedPref) {
        int menit = ihtiyati(sharedPref);
        PrayTime prayers = new PrayTime();
        // urutan offset: {subuh, terbit, dzuhur, ashar, terbenam, maghrib, isya}.
        // Terbit dan terbenam sengaja tidak diberi ihtiyati, keduanya patokan astronomis
        prayers.tune(new int[]{menit, 0, menit, menit, 0, menit, menit});
        prayers.setAsrFactor(bayanganAshar(sharedPref));
        return prayers;
    }

    /**
     * Ketujuh waktu untuk tanggal {@code hari}, dengan setelan pengguna.
     *
     * @param hari menentukan tanggal dan zona waktunya
     * @return tujuh slot (lihat konstanta kelas ini), atau null kalau di
     * lintang itu matahari tidak terbit/terbenam pada tanggal tersebut — SPA
     * lalu mengisi -99999 sebagai penanda, yang kalau diteruskan jadi jam ngawur
     */
    public static List<ZonedDateTime> hitung(SharedPreferences sharedPref, Location lokasi,
                                             ZonedDateTime hari) {
        SolarPosition.SPAData spa = MasukanSpa.buat(hari, lokasi.getLatitude(),
                lokasi.getLongitude(), Lokasi.ketinggianMsl(lokasi), SolarPosition.SPA.SPA_ZA_RTS);
        List<ZonedDateTime> waktu = penghitung(sharedPref).getDatePrayerTimes(spa);
        // rtsSah baru bermakna SESUDAH getDatePrayerTimes, yang mengisi terbit/terbenamnya
        return SolarPosition.rtsSah(spa) ? waktu : null;
    }
}
