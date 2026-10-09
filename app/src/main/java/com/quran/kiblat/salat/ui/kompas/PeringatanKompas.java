package com.quran.kiblat.salat.ui.kompas;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.SensorManager;

import androidx.appcompat.app.AlertDialog;

/**
 * Penjelasan keterbatasan sensor magnet ponsel.
 * <p>
 * Ini bukan peringatan basa-basi. Arah kiblatnya sendiri dihitung dengan
 * geodesi Karney di atas elipsoid WGS84, dan galat GPS beberapa meter cuma
 * menggeser azimutnya seperseratus ribu derajat. Yang menentukan ketepatan
 * di lapangan sepenuhnya sensor magnet, yang meleset ribuan kali lebih
 * besar — jadi satu-satunya hal berguna yang bisa dilakukan pengguna ada
 * di daftar ini. Perbandingan "1 derajat = 138 km di Ka'bah" adalah inti
 * dialognya; jangan dibuang.
 * <p>
 * Muncul dari tiga jalan: spanduk sekali-pakai di layar kiblat, saat
 * magnetometer melaporkan ketelitian rendah (sekali per kunjungan), dan dari
 * menu "Ketepatan Kompas".
 */
public final class PeringatanKompas {

    /**
     * Penanda bahwa peringatan kompas sudah pernah dibaca. Dipakai bersama
     * ketiga layar kompas: dibaca di mana pun, spanduknya hilang di mana-mana.
     */
    private static final String SUDAH_BACA = "sudah_baca_kompas";

    private PeringatanKompas() {
    }

    /**
     * @param akurasi nilai SensorManager.SENSOR_STATUS_*, atau -1 kalau sedang
     *                tidak dipicu oleh perubahan akurasi sensor.
     * @param kiblat  true untuk layar kiblat, yang punya patokan jarak di
     *                Ka'bah; false untuk layar arah terbit/tenggelam, yang
     *                masalah kompasnya sama persis tapi patokannya beda.
     */
    public static void tampilkan(Context context, int akurasi, boolean kiblat) {
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
                    .append("meleset 5° sampai 15°. Meleset 1° saja sudah bergeser ")
                    .append("sekitar 138 km di sekitar Ka'bah, jadi di sinilah seluruh ")
                    .append("ketepatannya ditentukan.\n\n");
        } else {
            isi.append("Azimut matahari di layar ini dihitung dengan algoritma SPA milik NREL, ")
                    .append("teliti sampai sekitar 0,0003°.\n\n")
                    .append("Yang membatasi adalah sensor magnet (kompas) ponsel, yang lazimnya ")
                    .append("meleset 5° sampai 15° — puluhan ribu kali lebih besar ")
                    .append("daripada galat hitungannya. Jadi angka arahnya boleh dipercaya; ")
                    .append("yang perlu diperlakukan hati-hati adalah ke mana jarumnya ")
                    .append("menunjuk.\n\n");
        }

        isi.append("Supaya selisihnya sekecil mungkin:\n")
                .append("• Menjauhlah dari besi beton, rangka baja, dan lantai bertulang. ")
                .append("Di dalam gedung bertingkat kompas hampir selalu menyimpang.\n")
                .append("• Jauhkan dari pengeras suara, magnet, dompet atau casing ")
                .append("bermagnet, dan dudukan ponsel bermagnet.\n")
                .append("• Jauhkan dari laptop, kulkas, dan dasbor mobil.\n")
                .append("• Kalibrasi dengan menggerakkan ponsel membentuk angka 8 ")
                .append("beberapa kali.\n")
                .append("• Pegang ponsel mendatar, lalu bandingkan hasilnya dari dua ")
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
    public static boolean perluSpanduk(SharedPreferences sharedPref) {
        return !sharedPref.getBoolean(SUDAH_BACA, false);
    }

    /**
     * Menandai peringatan kompas sudah dibaca, supaya spanduknya tidak muncul lagi.
     */
    public static void tandaiSudahDibaca(SharedPreferences sharedPref) {
        sharedPref.edit().putBoolean(SUDAH_BACA, true).apply();
    }
}
