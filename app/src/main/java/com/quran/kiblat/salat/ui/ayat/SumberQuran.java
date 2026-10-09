package com.quran.kiblat.salat.ui.ayat;

import android.content.Context;
import android.content.res.AssetManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Membaca teks Al-Qur'an dari assets.
 * <ul>
 * <li>{@code assets/daftar_surat.json} — 114 surat: {@code id},
 *     {@code surat_name}, {@code surat_text}</li>
 * <li>{@code assets/Surat/<id>.json} — {@code data[]} berisi {@code juz_id},
 *     {@code aya_number}, {@code aya_text}, {@code translation_aya_text}</li>
 * </ul>
 * Semua method mengembalikan null kalau berkasnya tidak terbaca.
 */
public final class SumberQuran {

    /**
     * Basmalah yang disisipkan di awal setiap surat, kecuali Al-Fatihah (di
     * sana basmalah adalah ayat pertamanya) dan At-Taubah.
     */
    private static final Ayat BASMALAH = new Ayat(
            0,
            0,
            "Al-Fatihah",
            0,
            "بِسْمِ اللّٰهِ الرَّحْمٰنِ الرَّحِيْمِ",
            "Dengan nama Allah Yang Maha Pengasih, Maha Penyayang."
    );
    public static final int JUMLAH_JUZ = 30;

    private SumberQuran() {
    }

    public static JSONArray daftarSurat(Context context) {
        try {
            return new JSONArray(bacaBerkas(context.getAssets(), "daftar_surat.json"));
        } catch (IOException | JSONException ignored) {
            return null;
        }
    }

    /**
     * Seluruh ayat satu surat.
     *
     * @param id    1..114
     * @param judul nama surat, ikut disimpan di setiap ayat
     */
    public static List<Ayat> ayatSurat(Context context, int id, String judul) {
        try {
            List<Ayat> daftarAyat = new ArrayList<>();
            if (pakaiBasmalah(id)) {
                daftarAyat.add(BASMALAH);
            }

            JSONArray jarray = dataSurat(context, id);
            int jumlahAyat = jarray.length();
            for (int i = 0; i < jumlahAyat; i++) {
                daftarAyat.add(ayat(jarray.getJSONObject(i), id, judul));
            }
            return daftarAyat;
        } catch (IOException | JSONException ex) {
            return null;
        }
    }

    /**
     * Seluruh ayat satu juz, lintas surat.
     *
     * @param id 1..30
     */
    public static List<Ayat> ayatJuz(Context context, int id) {
        try {
            List<Ayat> daftarAyat = new ArrayList<>();
            int ayatSebelum = 0;

            JSONArray daftarSurat = daftarSurat(context);
            if (daftarSurat == null) {
                return null;
            }
            int jumlahSurat = daftarSurat.length();

            for (int suratKe = 1; suratKe <= jumlahSurat; suratKe++) {
                String judul = daftarSurat.getJSONObject(suratKe - 1).getString("surat_name");
                JSONArray jarray = dataSurat(context, suratKe);
                int jumlahAyat = jarray.length();
                for (int i = 0; i < jumlahAyat; i++) {
                    JSONObject jo = jarray.getJSONObject(i);
                    if (jo.getInt("juz_id") != id) {
                        continue;
                    }
                    // awal surat baru di dalam juz ini
                    if (pakaiBasmalah(suratKe) && ((jo.getInt("aya_number") - ayatSebelum < 0) || i == 0)) {
                        daftarAyat.add(BASMALAH);
                    }
                    daftarAyat.add(ayat(jo, suratKe, judul));
                    ayatSebelum = jo.getInt("aya_number");
                }
            }
            return daftarAyat;
        } catch (IOException | JSONException ex) {
            return null;
        }
    }

    private static boolean pakaiBasmalah(int idSurat) {
        return idSurat > 1 && idSurat != 9;
    }

    private static JSONArray dataSurat(Context context, int id) throws IOException, JSONException {
        return new JSONObject(bacaBerkas(context.getAssets(), "Surat/" + id + ".json")).getJSONArray("data");
    }

    private static Ayat ayat(JSONObject jo, int idSurat, String judul) throws JSONException {
        return new Ayat(
                jo.getInt("juz_id"),
                idSurat,
                judul,
                jo.getInt("aya_number"),
                jo.getString("aya_text"),
                jo.getString("translation_aya_text"));
    }

    private static String bacaBerkas(AssetManager assetManager, String namaBerkas) throws IOException {
        try (InputStream masuk = assetManager.open(namaBerkas)) {
            ByteArrayOutputStream hasil = new ByteArrayOutputStream();
            byte[] buff = new byte[4096];
            int terbaca;
            while ((terbaca = masuk.read(buff)) != -1) {
                hasil.write(buff, 0, terbaca);
            }
            return hasil.toString(StandardCharsets.UTF_8.name());
        }
    }
}
