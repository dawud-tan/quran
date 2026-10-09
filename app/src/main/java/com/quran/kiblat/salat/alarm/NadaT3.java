package com.quran.kiblat.salat.alarm;

/**
 * Pembungkus tipis mesin nada asli (src/main/cpp): gelombang kotak 520 Hz
 * pita-terbatas dengan pola isyarat T3 — 0,5 detik nyala, 0,5 diam, 0,5 nyala,
 * 0,5 diam, 0,5 nyala, 1,5 diam; satu putaran tepat 4 detik.
 * <p>
 * Menggantikan res/raw/t3_441khz.ogg dan t3_48khz.ogg. Nadanya dibangkitkan
 * pada laju cuplik asli perangkat, berapa pun itu, jadi tidak ada lagi
 * pemilihan berkas 44,1 kHz atau 48 kHz — dan tidak ada lagi cacat mampatan
 * ogg: harmonisa genap sekarang di -176 dB, dulu -64 dB.
 * <p>
 * Bukan kelas yang aman dipakai beberapa utas sekaligus. Servis10Menit
 * memakainya hanya dari utas utama.
 */
public final class NadaT3 {
    /**
     * Nilai {@code putaran} yang berarti berbunyi terus sampai dimatikan.
     */
    public static final int TANPA_BATAS = 0;

    static {
        System.loadLibrary("nada_t3");
    }

    private long penunjuk;

    public NadaT3() {
        penunjuk = buat();
    }

    private static native long buat();

    private static native long mulai(long penunjuk, int putaran);

    private static native void berhenti(long penunjuk);

    private static native boolean sedangMain(long penunjuk);

    private static native void hapus(long penunjuk);

    /**
     * Membunyikan pola sebanyak {@code putaran} kali, atau terus-menerus
     * kalau {@code putaran} adalah {@link #TANPA_BATAS}.
     *
     * <p>Sisi asli tidak memanggil balik ke Java dari utas audionya, jadi
     * pemanggil yang menjadwalkan sendiri apa yang terjadi sesudahnya.
     *
     * @return {@code > 0} lama pemutaran dalam milidetik; {@code 0} berhasil
     * tapi tanpa batas, jadi tidak ada yang perlu dijadwalkan;
     * {@code < 0} gagal.
     */
    public long mulai(int putaran) {
        return penunjuk == 0 ? -1 : mulai(penunjuk, putaran);
    }

    public void berhenti() {
        if (penunjuk != 0) {
            berhenti(penunjuk);
        }
    }

    public boolean sedangMain() {
        return penunjuk != 0 && sedangMain(penunjuk);
    }

    /**
     * Membubarkan mesinnya. Sesudah ini objeknya tidak bisa dipakai lagi.
     */
    public void lepas() {
        if (penunjuk != 0) {
            hapus(penunjuk);
            penunjuk = 0;
        }
    }
}