package com.quran.kiblat.salat.hisab;

import java.time.LocalTime;
import java.time.ZonedDateTime;

/**
 * Jam dan azimut matahari saat terbit atau terbenam, untuk layar arah
 * terbit/tenggelam.
 * <p>
 * Tanpa Android, seperti seluruh paket {@code hisab}.
 */
public final class ArahMatahari {

    private ArahMatahari() {
    }

    /**
     * @param sekarang menentukan tanggal dan zona waktunya; jamnya sendiri
     *                 tidak berpengaruh
     * @param terbit   true untuk terbit, false untuk terbenam
     * @return null kalau matahari tidak terbit/terbenam pada tanggal itu
     * (lintang tinggi; SPA mengisi -99999 sebagai penanda)
     */
    public static Hasil hitung(ZonedDateTime sekarang, double lintang, double bujur,
                               double elevasi, boolean terbit) {
        SolarPosition.SPAData spa = MasukanSpa.buat(sekarang, lintang, bujur, elevasi,
                SolarPosition.SPA.SPA_ZA_RTS);
        SolarPosition.spaCalculate(spa);
        if (!SolarPosition.rtsSah(spa)) {
            return null;
        }

        // jam pecahan dari SPA, misalnya 5.9264 = 05:55:35
        double jamPecahan = terbit ? spa.sunrise : spa.sunset;
        int jam = (int) jamPecahan;
        double menitPecahan = (jamPecahan - jam) * 60.0;
        int menit = (int) menitPecahan;
        double detik = (menitPecahan - menit) * 60.0;

        // Azimutnya dihitung ulang tepat pada saat terbit/terbenam itu,
        // dengan detik pecahannya, bukan pada jam saat layar dibuka.
        spa.hour = jam;
        spa.minute = menit;
        spa.second = detik;
        spa.function = SolarPosition.SPA.SPA_ZA;
        SolarPosition.spaCalculate(spa);

        return new Hasil(LocalTime.of(jam, menit, (int) detik), spa.azimuth);
    }

    /**
     * @param waktu  jam setempat, dipotong ke detik
     * @param azimut derajat dari utara sejati, searah jarum jam
     */
    public record Hasil(LocalTime waktu, double azimut) {
    }
}
