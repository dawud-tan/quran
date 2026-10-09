import com.quran.kiblat.salat.hisab.ArahMatahari;
import com.quran.kiblat.salat.hisab.BayanganKiblat;
import com.quran.kiblat.salat.hisab.Geodesic;
import com.quran.kiblat.salat.hisab.MasukanSpa;
import com.quran.kiblat.salat.hisab.PrayTime;
import com.quran.kiblat.salat.hisab.SolarPosition;
import com.quran.kiblat.salat.hisab.Weton;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Pemeriksaan angka paket {@code hisab} tanpa perangkat dan tanpa Gradle.
 * <p>
 * Paket hisab sengaja bebas Android, jadi bisa dikompilasi apa adanya:
 * <pre>
 * javac -d build/uji-hisab app/src/main/java/com/quran/kiblat/salat/hisab/*.java alat/UjiHisab.java
 * java -cp build/uji-hisab UjiHisab
 * </pre>
 * Keluar dengan kode 1 kalau ada angka yang bergeser dari patokan. Jalankan
 * setiap kali mengubah apa pun di paket hisab. Patokannya adalah keluaran
 * aplikasi sebelum dirapikan (dan, untuk bayangan kiblat, angka di CLAUDE.md);
 * kalau perubahannya memang disengaja, perbarui patokan di sini bersama
 * alasannya.
 */
public class UjiHisab {

    // Jakarta, Monas
    private static final double LINTANG = -6.2088;
    private static final double BUJUR = 106.8456;
    private static final double ELEVASI = 8;
    private static final ZoneId WIB = ZoneId.of("Asia/Jakarta");

    private static int gagal = 0;

    public static void main(String[] args) {
        // PrayTime memakai zona bawaan JVM untuk hasilnya
        TimeZone.setDefault(TimeZone.getTimeZone(WIB));

        double kiblat = Geodesic.determineIndonesianQiblaDirection(LINTANG, BUJUR);
        kiblat = kiblat < 0 ? kiblat + 360 : kiblat;
        dekat("azimut kiblat Jakarta", kiblat, 295.0247, 0.0001);
        cek("di luar Indonesia NaN", Double.isNaN(Geodesic.determineIndonesianQiblaDirection(21.4, 39.8)));

        // Setelan bawaan JadwalSalat.penghitung: ihtiyati 2 menit, Ashar jumhur.
        ZonedDateTime hari = ZonedDateTime.of(2026, 5, 27, 13, 37, 21, 0, WIB);
        sama("jadwal Jakarta 27 Mei 2026", jadwal(hari, 1),
                "04:35 05:56 11:52 15:14 17:44 17:46 18:59");
        sama("Ashar Hanafi 27 Mei 2026", jadwal(hari, 2).split(" ")[3], "16:07");
        // Kerendahan ufuk: di Bandung (768 m) maghrib sekitar 4,3 menit lebih lambat
        // daripada di 0 m — 5 menit setelah dibulatkan ke menit.
        sama("maghrib Bandung 768 m", jadwal(-6.9175, 107.6191, 768, hari, 1).split(" ")[5], "17:46");
        sama("maghrib Bandung 0 m", jadwal(-6.9175, 107.6191, 0, hari, 1).split(" ")[5], "17:41");

        ArahMatahari.Hasil terbit = ArahMatahari.hitung(hari, LINTANG, BUJUR, ELEVASI, true);
        ArahMatahari.Hasil terbenam = ArahMatahari.hitung(hari, LINTANG, BUJUR, ELEVASI, false);
        sama("jam terbit 27 Mei 2026", terbit.waktu().toString(), "05:55:35");
        dekat("azimut terbit 27 Mei 2026", terbit.azimut(), 68.716475, 0.00001);
        sama("jam terbenam 27 Mei 2026", terbenam.waktu().toString(), "17:44:03");
        dekat("azimut terbenam 27 Mei 2026", terbenam.azimut(), 291.361207, 0.00001);

        sama("weton 10 Okt 2026",
                Weton.pemformat(2026, Month.OCTOBER, 10).format(LocalDate.of(2026, 10, 10)),
                "Sabtu Kliwon");

        // Bayangan kiblat. Rashdul qiblat 27/28 Mei dan 15/16 Juli harus muncul
        // sendiri dari pemindaian umum.
        sama("bayangan 27 Mei 2026", saat(LocalDate.of(2026, 5, 27), kiblat), "2026-05-27 16:18:16 searah");
        sama("bayangan 16 Juli 2026", saat(LocalDate.of(2026, 7, 16), kiblat), "2026-07-16 16:27:14 searah");
        sama("bayangan 28 Sep 2026 (celah ekuinoks)", saat(LocalDate.of(2026, 9, 28), kiblat),
                "2026-10-26 10:40:02 lawan");

        int hariAda = 0;
        double galatMaks = 0;
        for (LocalDate d = LocalDate.of(2026, 1, 1); d.getYear() == 2026; d = d.plusDays(1)) {
            List<BayanganKiblat.Saat> daftar = BayanganKiblat.cari(LINTANG, BUJUR, ELEVASI, d, WIB, kiblat);
            if (!daftar.isEmpty()) {
                hariAda++;
            }
            for (BayanganKiblat.Saat s : daftar) {
                double sasaran = s.searahMatahari ? kiblat : (kiblat + 180) % 360;
                double galat = Math.abs((azimutMatahari(s.waktu) - sasaran + 540) % 360 - 180);
                galatMaks = Math.max(galatMaks, galat);
            }
        }
        cek("hari bayangan kiblat 2026 = 299 (dapat " + hariAda + ")", hariAda == 299);
        cek(String.format(Locale.ROOT, "galat azimut bayangan <= 0.006 (dapat %.4f)", galatMaks),
                galatMaks <= 0.006);

        if (gagal > 0) {
            System.out.println(gagal + " pemeriksaan GAGAL");
            System.exit(1);
        }
        System.out.println("semua pemeriksaan lolos");
    }

    private static String jadwal(ZonedDateTime hari, int ashar) {
        return jadwal(LINTANG, BUJUR, ELEVASI, hari, ashar);
    }

    private static String jadwal(double lat, double lon, double elevasi, ZonedDateTime hari, int ashar) {
        PrayTime p = new PrayTime();
        p.tune(new int[]{2, 0, 2, 2, 0, 2, 2});
        p.setAsrFactor(ashar);
        SolarPosition.SPAData spa = MasukanSpa.buat(hari, lat, lon, elevasi, SolarPosition.SPA.SPA_ZA_RTS);
        StringBuilder sb = new StringBuilder();
        for (ZonedDateTime w : p.getDatePrayerTimes(spa)) {
            sb.append(sb.length() == 0 ? "" : " ").append(w.format(DateTimeFormatter.ofPattern("HH:mm")));
        }
        if (!SolarPosition.rtsSah(spa)) {
            cek("rtsSah " + hari.toLocalDate(), false);
        }
        return sb.toString();
    }

    private static String saat(LocalDate mulai, double kiblat) {
        List<BayanganKiblat.Saat> daftar = BayanganKiblat.cariBerikutnya(LINTANG, BUJUR, ELEVASI, mulai, WIB, kiblat, 30);
        if (daftar.isEmpty()) {
            return "tidak ada";
        }
        BayanganKiblat.Saat s = daftar.get(0);
        return s.waktu.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                + (s.searahMatahari ? " searah" : " lawan");
    }

    private static double azimutMatahari(ZonedDateTime t) {
        SolarPosition.SPAData spa = MasukanSpa.buat(t, LINTANG, BUJUR, ELEVASI, SolarPosition.SPA.SPA_ZA);
        SolarPosition.spaCalculate(spa);
        return spa.azimuth;
    }

    private static void sama(String nama, String dapat, String harap) {
        cek(nama + ": " + dapat + (dapat.equals(harap) ? "" : " (harusnya " + harap + ")"), dapat.equals(harap));
    }

    private static void dekat(String nama, double dapat, double harap, double toleransi) {
        cek(String.format(Locale.ROOT, "%s: %.6f%s", nama, dapat,
                Math.abs(dapat - harap) <= toleransi ? "" : " (harusnya " + harap + ")"),
                Math.abs(dapat - harap) <= toleransi);
    }

    private static void cek(String nama, boolean lolos) {
        System.out.println((lolos ? "  ok    " : "  GAGAL ") + nama);
        if (!lolos) {
            gagal++;
        }
    }
}
