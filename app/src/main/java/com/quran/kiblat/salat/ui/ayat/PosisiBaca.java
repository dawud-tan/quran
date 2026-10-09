package com.quran.kiblat.salat.ui.ayat;

import android.content.SharedPreferences;
import android.os.Bundle;

/**
 * Argumen navigasi {@link FragmenAyat} dan posisi baca terakhir.
 * <p>
 * Nama kunci pref dan nilai {@code mode} sengaja tidak diubah: pemasangan
 * lama sudah menyimpannya.
 */
public final class PosisiBaca {

    /**
     * Argumen navigasi. Tepat satu dari {@link #ARG_SURAT} dan
     * {@link #ARG_JUZ} yang lebih dari nol.
     */
    public static final String ARG_SURAT = "id_surat";
    public static final String ARG_JUZ = "id_juz";
    public static final String ARG_JUDUL = "judul";
    public static final String ARG_POSISI = "bindingAdapterPosition";

    private static final String MODE = "mode";
    private static final String MODE_SURAT = "id_surat";
    private static final String MODE_JUZ = "id_juz";
    private static final String SURAT_KE = "suratke";
    private static final String JUZ_KE = "juzke";
    private static final String JUDUL = "judul";
    private static final String POSISI = "bindingAdapterPosition";

    private PosisiBaca() {
    }

    public static Bundle argumenSurat(int idSurat, String judul) {
        Bundle b = new Bundle();
        b.putInt(ARG_SURAT, idSurat);
        b.putString(ARG_JUDUL, judul);
        return b;
    }

    public static Bundle argumenJuz(int idJuz) {
        Bundle b = new Bundle();
        b.putInt(ARG_JUZ, idJuz);
        return b;
    }

    /**
     * Argumen untuk layar pertama saat aplikasi dibuka: posisi yang terakhir
     * ditandai, atau Al-Fatihah kalau belum pernah membaca.
     */
    public static Bundle argumenAwal(SharedPreferences sharedPref) {
        String mode = sharedPref.getString(MODE, null);
        if (mode == null) {
            return argumenSurat(1, "Al-Fatihah");
        }
        Bundle b = mode.equals(MODE_SURAT)
                ? argumenSurat(sharedPref.getInt(SURAT_KE, 0), sharedPref.getString(JUDUL, null))
                : argumenJuz(sharedPref.getInt(JUZ_KE, 0));
        b.putInt(ARG_POSISI, sharedPref.getInt(POSISI, 0));
        return b;
    }

    /**
     * Mengingat apakah yang terakhir dibuka daftar surat atau daftar juz.
     */
    static void catatMode(SharedPreferences sharedPref, boolean juz) {
        sharedPref.edit().putString(MODE, juz ? MODE_JUZ : MODE_SURAT).apply();
    }

    /**
     * Menu "Tandai terakhir dibaca".
     *
     * @param posisi urutan baris di daftar yang sedang tampil
     */
    static void tandai(SharedPreferences sharedPref, int posisi, Ayat ayat) {
        sharedPref.edit()
                .putInt(POSISI, posisi)
                .putInt(JUZ_KE, ayat.juzke())
                .putInt(SURAT_KE, ayat.suratke())
                .putString(JUDUL, ayat.judul())
                .apply();
    }
}
