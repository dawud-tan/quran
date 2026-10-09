package com.quran.kiblat.salat.hisab;

import java.time.ZonedDateTime;

/**
 * Satu-satunya tempat masukan SPA dirakit.
 * <p>
 * Dulu blok isian {@link SolarPosition.SPAData} yang sama ditulis ulang di
 * lima tempat (penjadwal alarm dua kali, layar kiblat, layar terbit, layar
 * tenggelam, bayangan kiblat). Konstanta yang bukan masukan pengguna —
 * delta T, tekanan, suhu, refraksi — sekarang hanya ditulis di sini.
 * <p>
 * Seperti seluruh paket {@code hisab}, berkas ini tanpa Android supaya bisa
 * dikompilasi dan diperiksa angkanya dengan {@code javac} biasa.
 */
public final class MasukanSpa {

    /**
     * Tekanan udara, milibar. Hanya memengaruhi refraksi pada tinggi/azimut
     * matahari, tidak pada terbit/terbenam (yang memakai {@link #REFRAKSI}).
     */
    public static final double TEKANAN = 1000;
    /**
     * Suhu udara, derajat Celsius. Sama seperti {@link #TEKANAN}.
     */
    public static final double SUHU = 27.7;
    /**
     * Refraksi atmosfer di ufuk saat terbit/terbenam, derajat. 0,5667 adalah
     * nilai baku SPA.
     */
    public static final double REFRAKSI = 0.5667;

    private MasukanSpa() {
    }

    /**
     * @param waktu    saat yang dihitung, lengkap dengan zona waktunya
     * @param lintang  derajat, negatif di selatan khatulistiwa
     * @param bujur    derajat, negatif di barat Greenwich
     * @param elevasi  meter di atas permukaan laut (MSL), bukan elipsoid WGS84
     *                 — kerendahan ufuk dihitung dari sini
     * @param fungsi   keluaran yang diminta dari SPA
     */
    public static SolarPosition.SPAData buat(ZonedDateTime waktu, double lintang, double bujur,
                                             double elevasi, SolarPosition.SPA fungsi) {
        SolarPosition.SPAData spa = new SolarPosition.SPAData();
        spa.year = waktu.getYear();
        spa.month = waktu.getMonthValue();
        spa.day = waktu.getDayOfMonth();
        spa.hour = waktu.getHour();
        spa.minute = waktu.getMinute();
        spa.second = waktu.getSecond();
        spa.timezone = waktu.getOffset().getTotalSeconds() / 3600.0;

        spa.latitude = lintang;
        spa.longitude = bujur;
        spa.elevation = elevasi;

        spa.delta_ut1 = 0;
        spa.delta_t = SolarPosition.DELTA_T;
        spa.pressure = TEKANAN;
        spa.temperature = SUHU;
        spa.atmos_refract = REFRAKSI;
        // Bidang permukaan datar menghadap selatan. Dua nilai ini hanya
        // dipakai untuk sudut datang (SPA_ZA_INC / SPA_ALL), yang tidak
        // pernah dibaca aplikasi ini.
        spa.slope = 0;
        spa.azm_rotation = 180;
        spa.function = fungsi;
        return spa;
    }
}
