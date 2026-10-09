package com.quran.kiblat.salat.hisab;

import static java.util.Map.entry;

import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;
import java.util.Map;

/**
 * Hari dan pasaran Jawa (weton), misalnya "Sabtu Pahing".
 * <p>
 * Tanpa Android, seperti seluruh paket {@code hisab}.
 */
public final class Weton {

    private static final Locale INDONESIA = new Locale.Builder()
            .setLanguage("id").setScript("Latn").setRegion("ID").build();
    private static final Map<Month, Integer> BULAN_BIASA = Map.ofEntries(
            entry(Month.JANUARY, 0), entry(Month.FEBRUARY, 4), entry(Month.MARCH, 1),
            entry(Month.APRIL, 0), entry(Month.MAY, 0), entry(Month.JUNE, 4),
            entry(Month.JULY, 4), entry(Month.AUGUST, 3), entry(Month.SEPTEMBER, 2),
            entry(Month.OCTOBER, 2), entry(Month.NOVEMBER, 1), entry(Month.DECEMBER, 1));
    private static final Map<Month, Integer> BULAN_KABISAT = Map.ofEntries(
            entry(Month.JANUARY, 1), entry(Month.FEBRUARY, 0), entry(Month.MARCH, 1),
            entry(Month.APRIL, 0), entry(Month.MAY, 0), entry(Month.JUNE, 4),
            entry(Month.JULY, 4), entry(Month.AUGUST, 3), entry(Month.SEPTEMBER, 2),
            entry(Month.OCTOBER, 2), entry(Month.NOVEMBER, 1), entry(Month.DECEMBER, 1));
    private static final String[] PASARAN = {"Legi", "Pahing", "Pon", "Wage", "Kliwon"};

    private Weton() {
    }

    /**
     * Pemformat "nama hari + pasaran" untuk tanggal itu. Pasarannya dihitung
     * dari tanggalnya, jadi pemformat ini hanya benar untuk tanggal yang sama.
     */
    public static DateTimeFormatter pemformat(int tahun, Month bulan, int tanggal) {
        boolean kabisat = (tahun & 3) == 0 && ((tahun % 25) != 0 || (tahun & 15) == 0);
        int jumlahKabisat = tahun / 4 - tahun / 100 * 25;

        int nilaiBulan = kabisat ? BULAN_KABISAT.get(bulan) : BULAN_BIASA.get(bulan);
        return new DateTimeFormatterBuilder()
                .appendPattern("eeee")
                .appendLiteral(" ")
                .appendLiteral(PASARAN[Math.floorMod(jumlahKabisat + tanggal - nilaiBulan, 5)])
                .toFormatter(INDONESIA);
    }
}
