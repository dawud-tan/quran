package com.quran.kiblat.salat.hisab;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Bayang-bayang kiblat: saat azimut matahari kebetulan sama dengan azimut
 * kiblat (atau kebalikannya), bayangan benda tegak jatuh tepat pada garis
 * kiblat. Matahari sama sekali tidak terpengaruh medan magnet, jadi cara ini
 * melewati satu-satunya bagian rantai kiblat yang benar-benar tidak teliti:
 * sensor magnet ponsel, yang lazimnya meleset 5-15 derajat.
 * <p>
 * Ini bukan cuma dua hari Rasyd al-Kiblat setahun. Dua hari itu istimewa
 * karena matahari tepat di atas Ka'bah sehingga berlaku serentak di seluruh
 * dunia; syarat sebenarnya jauh lebih longgar, yakni azimut mataharinya saja
 * yang sama. Di Jakarta syarat longgar itu terpenuhi pada 299 dari 365 hari.
 * <p>
 * Seperti seluruh paket {@code hisab}, berkas ini sengaja tanpa kebergantungan
 * ke Android supaya bisa dikompilasi dan diperiksa angkanya dengan javac biasa
 * di luar Gradle (lihat alat/UjiHisab.java).
 */
public class BayanganKiblat {

    /**
     * Matahari di bawah 5 derajat terlalu rendah: bayangannya memanjang tak
     * keruan, dibiaskan atmosfer, dan gampang terhalang bangunan.
     */
    public static final double TINGGI_MIN = 5.0;

    /**
     * Di atas 75 derajat bayangannya terlalu pendek untuk dibidik: sedikit
     * salah membaca ujung bayangan sudah berarti banyak derajat.
     */
    public static final double TINGGI_MAKS = 75.0;

    /**
     * Selisih dua azimut, dilipat ke rentang -180..180.
     */
    private static double selisihAzimut(double a, double b) {
        double d = (a - b + 540.0) % 360.0 - 180.0;
        return d;
    }

    /**
     * Azimut dan tinggi matahari pada detik tertentu.
     */
    private static double[] matahari(double lat, double lon, double elevasi,
                                     ZonedDateTime t) {
        SolarPosition.SPAData spa = MasukanSpa.buat(t, lat, lon, elevasi, SolarPosition.SPA.SPA_ZA);
        SolarPosition.spaCalculate(spa);
        return new double[]{spa.azimuth, 90.0 - spa.zenith};
    }

    /**
     * Mencari saat bayangan kiblat pada satu tanggal.
     * <p>
     * Hari dipindai per menit untuk mengurung perubahan tanda selisih azimut,
     * lalu dipersempit dengan bagi dua sampai ketelitian satu detik. Dipindai
     * per menit, bukan lebih renggang, karena di lintang tropis azimut matahari
     * bisa berayun sangat cepat waktu melewati dekat zenit — langkah renggang
     * bisa melewatkan perpotongannya sama sekali.
     *
     * @param azimutKiblat azimut kiblat dari Geodesic, 0..360
     * @return saat-saat yang matahari nya cukup tinggi untuk dipakai, urut
     * menurut waktu; kosong kalau hari itu tidak ada.
     */
    public static List<Saat> cari(double lat, double lon, double elevasi,
                                  LocalDate tanggal, ZoneId zona,
                                  double azimutKiblat) {
        List<Saat> hasil = new ArrayList<>();
        double lawan = (azimutKiblat + 180.0) % 360.0;

        for (int putaran = 0; putaran < 2; putaran++) {
            double sasaran = (putaran == 0) ? azimutKiblat : lawan;
            boolean searah = (putaran == 0);

            ZonedDateTime awal = tanggal.atStartOfDay(zona);
            double[] sebelum = matahari(lat, lon, elevasi, awal);
            double dSebelum = selisihAzimut(sebelum[0], sasaran);

            for (int menit = 1; menit <= 24 * 60; menit++) {
                ZonedDateTime t = awal.plusMinutes(menit);
                double[] kini = matahari(lat, lon, elevasi, t);
                double dKini = selisihAzimut(kini[0], sasaran);

                // Perubahan tanda tanpa lompatan 180 derajat berarti azimutnya
                // benar-benar melewati sasaran di antara dua menit ini.
                if (dSebelum != 0 && dKini != 0
                        && Math.signum(dSebelum) != Math.signum(dKini)
                        && Math.abs(dSebelum) < 90 && Math.abs(dKini) < 90) {
                    ZonedDateTime tepat = persempit(lat, lon, elevasi,
                            t.minusMinutes(1), t, sasaran);
                    double[] p = matahari(lat, lon, elevasi, tepat);
                    if (p[1] >= TINGGI_MIN && p[1] <= TINGGI_MAKS) {
                        hasil.add(new Saat(tepat, p[1], searah));
                    }
                }
                dSebelum = dKini;
            }
        }

        hasil.sort((a, b) -> a.waktu.compareTo(b.waktu));
        return hasil;
    }

    /**
     * Bagi dua sampai selisih waktunya tinggal satu detik.
     */
    private static ZonedDateTime persempit(double lat, double lon, double elevasi,
                                           ZonedDateTime kiri, ZonedDateTime kanan,
                                           double sasaran) {
        double dKiri = selisihAzimut(matahari(lat, lon, elevasi, kiri)[0], sasaran);
        while (java.time.Duration.between(kiri, kanan).getSeconds() > 1) {
            ZonedDateTime tengah = kiri.plusSeconds(
                    java.time.Duration.between(kiri, kanan).getSeconds() / 2);
            double dTengah = selisihAzimut(matahari(lat, lon, elevasi, tengah)[0], sasaran);
            if (dTengah == 0) {
                return tengah;
            }
            if (Math.signum(dTengah) == Math.signum(dKiri)) {
                kiri = tengah;
                dKiri = dTengah;
            } else {
                kanan = tengah;
            }
        }
        return kiri;
    }

    /**
     * Seperti {@link #cari}, tapi kalau hari itu kosong, dicari maju sampai
     * {@code maksHari} hari ke depan. Di dekat ekuinoks memang ada rentang
     * berminggu-minggu yang azimut mataharinya tidak pernah sampai ke garis
     * kiblat, jadi tanpa ini layarnya cuma bilang "tidak ada" tanpa memberi
     * tahu kapan bisa.
     */
    public static List<Saat> cariBerikutnya(double lat, double lon, double elevasi,
                                            LocalDate mulai, ZoneId zona,
                                            double azimutKiblat, int maksHari) {
        for (int i = 0; i <= maksHari; i++) {
            List<Saat> hasil = cari(lat, lon, elevasi, mulai.plusDays(i), zona, azimutKiblat);
            if (!hasil.isEmpty()) {
                return hasil;
            }
        }
        return new ArrayList<>();
    }

    /**
     * Satu saat bayangan jatuh di garis kiblat.
     */
    public static class Saat {
        /**
         * Kapan persisnya, dibulatkan ke detik.
         */
        public final ZonedDateTime waktu;
        /**
         * Tinggi matahari saat itu, derajat.
         */
        public final double tinggi;
        /**
         * true  = matahari tepat di azimut kiblat, jadi bayangannya membelakangi
         * kiblat dan yang mengarah ke kiblat adalah arah matahari.
         * false = matahari di azimut lawan kiblat, jadi bayangannya sendiri
         * yang menunjuk kiblat. Ini yang lebih enak dipakai: tinggal
         * berdiri mengikuti garis bayangan.
         */
        public final boolean searahMatahari;

        Saat(ZonedDateTime waktu, double tinggi, boolean searahMatahari) {
            this.waktu = waktu;
            this.tinggi = tinggi;
            this.searahMatahari = searahMatahari;
        }
    }
}