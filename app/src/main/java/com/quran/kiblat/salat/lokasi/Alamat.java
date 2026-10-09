package com.quran.kiblat.salat.lokasi;

import android.content.SharedPreferences;

import com.quran.kiblat.salat.BuildConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

/**
 * Nama tempat untuk baris kedua notifikasi adzan, dari Nominatim
 * (OpenStreetMap). Satu-satunya pemakaian jaringan di aplikasi ini.
 * <p>
 * Alarm tidak pernah menunggu jaringan: yang dipakai saat memasang alarm
 * adalah alamat tersimpan ({@link #tersimpan}), dan {@link #cari} dijalankan
 * sesudahnya di utas latar.
 */
public final class Alamat {

    private static final String KUNCI = "alamat";
    private static final String TIDAK_DIKENAL = "Dimanapun Anda berada";
    private static final Pattern NAMA = Pattern.compile("(?<=\\\"name\\\":\\\")[^\\\"]*");
    private static final Pattern KABUPATEN = Pattern.compile("(?<=\\\"county\\\":\\\")[^\\\"]*");

    private Alamat() {
    }

    public static String tersimpan(SharedPreferences sharedPref) {
        return sharedPref.getString(KUNCI, TIDAK_DIKENAL);
    }

    public static void simpan(SharedPreferences sharedPref, String alamat) {
        sharedPref.edit().putString(KUNCI, alamat).apply();
    }

    /**
     * Memanggil jaringan, jadi jangan dari utas utama.
     *
     * @return null kalau gagal, supaya pemanggil bisa tetap memakai alamat
     * yang tersimpan
     */
    public static String cari(double lat, double lon) {
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

            String json = new String(bacaRespons(c), StandardCharsets.UTF_8);

            StringBuilder sb = new StringBuilder();
            Matcher nama = NAMA.matcher(json);
            if (nama.find()) {
                sb.append(nama.group(0)).append(", ");
            }
            Matcher kabupaten = KABUPATEN.matcher(json);
            if (kabupaten.find()) {
                sb.append(kabupaten.group(0));
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
    private static byte[] bacaRespons(HttpsURLConnection koneksi) throws IOException {
        try (InputStream masuk = koneksi.getInputStream()) {
            ByteArrayOutputStream hasil = new ByteArrayOutputStream();
            byte[] buff = new byte[4096];
            int terbaca;
            while ((terbaca = masuk.read(buff)) != -1) {
                hasil.write(buff, 0, terbaca);
            }
            return hasil.toByteArray();
        }
    }
}
