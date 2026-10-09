package com.quran.kiblat.salat.umum;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Berkas SharedPreferences tunggal milik aplikasi.
 * <p>
 * Kuncinya sengaja tidak dikumpulkan di sini: setiap kunci dibaca dan ditulis
 * hanya oleh kelas pemiliknya, supaya nilai bawaan dan batasnya tidak tersebar.
 * <ul>
 * <li>{@code latitude}, {@code longitude}, {@code altitude}, {@code accuracy} —
 *     {@link com.quran.kiblat.salat.lokasi.Lokasi}</li>
 * <li>{@code alamat} — {@link com.quran.kiblat.salat.lokasi.Alamat}</li>
 * <li>{@code adzan_subuh} … {@code adzan_isya}, {@code ihtiyati},
 *     {@code bayangan_ashar} — {@link com.quran.kiblat.salat.jadwal.JadwalSalat}</li>
 * <li>{@code lokasi_latar}, {@code sudah_tanya_izin} —
 *     {@link com.quran.kiblat.salat.izin.Izin}</li>
 * <li>{@code sudah_baca_kompas} —
 *     {@link com.quran.kiblat.salat.ui.kompas.PeringatanKompas}</li>
 * <li>{@code mode}, {@code suratke}, {@code juzke}, {@code judul},
 *     {@code bindingAdapterPosition} —
 *     {@link com.quran.kiblat.salat.ui.ayat.PosisiBaca}</li>
 * </ul>
 */
public final class Pref {

    /**
     * Jangan diganti: semua setelan pengguna yang sudah terpasang tersimpan
     * dengan nama ini.
     */
    public static final String NAMA = "pref";

    private Pref() {
    }

    public static SharedPreferences dari(Context context) {
        return context.getSharedPreferences(NAMA, Context.MODE_PRIVATE);
    }
}
