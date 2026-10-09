package com.quran.kiblat.salat.ui.kiblat;

import android.graphics.Paint;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.quran.kiblat.salat.databinding.FragmenKiblatBinding;
import com.quran.kiblat.salat.hisab.BayanganKiblat;
import com.quran.kiblat.salat.hisab.Geodesic;
import com.quran.kiblat.salat.hisab.Weton;
import com.quran.kiblat.salat.jadwal.JadwalSalat;
import com.quran.kiblat.salat.ui.kompas.FragmenKompas;
import com.quran.kiblat.salat.ui.kompas.PeringatanKompas;
import com.quran.kiblat.salat.umum.Latar;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Arah kiblat, bayangan kiblat, dan jadwal lima waktu hari ini.
 * Lokasi, kompas, dan baris-baris data umumnya diurus {@link FragmenKompas}.
 */
public class FragmenKiblat extends FragmenKompas {

    private FragmenKiblatBinding binding;
    private TextView[] barisAdzan;
    /**
     * Azimut kiblat dari lokasi saat ini, 0..360, atau NaN di luar Indonesia.
     */
    private double azimutKiblat = Double.NaN;
    /// Tanggal + posisi yang bayangan kiblatnya sudah dihitung, supaya tiap
    /// pembaruan GPS kecil tidak memicu pemindaian sehari penuh lagi.
    private String kunciBayangan;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmenKiblatBinding.inflate(inflater, container, false);
        // urutannya sama dengan JadwalSalat.namaAdzan()
        barisAdzan = new TextView[]{binding.sholatSubuh, binding.sholatDzuhur,
                binding.sholatAshar, binding.sholatMaghrib, binding.sholatIsya};

        mulaiKompas(new Tampilan(binding.gambar, binding.gambar2, binding.koordinat,
                binding.akurasiGps, binding.mdpl, binding.sudutAzimut, binding.mikrotesla));
        aturSpandukKompas();
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        barisAdzan = null;
    }

    @Override
    protected boolean layarKiblat() {
        return true;
    }

    @Override
    protected double azimutSasaran() {
        return azimutKiblat;
    }

    @Override
    protected void perbaruiData(Location lokasi) {
        double azimut = Geodesic.determineIndonesianQiblaDirection(lokasi.getLatitude(), lokasi.getLongitude());
        azimutKiblat = azimut < 0 ? azimut + 360 : azimut;

        aturBayanganKiblat(lokasi);
        aturJadwal(lokasi);
    }

    /**
     * Spanduk sekali-pakai gantinya dialog yang dulu menyembul dari onResume.
     * Dialog yang muncul sebelum penggunanya sempat melihat layar hampir pasti
     * ditutup tanpa dibaca; spanduk menunggu sampai dia sendiri yang tertarik.
     */
    private void aturSpandukKompas() {
        View spanduk = binding.spandukKompas.spandukKompas;
        if (!PeringatanKompas.perluSpanduk(sharedPref)) {
            spanduk.setVisibility(View.GONE);
            return;
        }
        spanduk.setVisibility(View.VISIBLE);

        binding.spandukKompas.spandukPesan.setOnClickListener(v -> {
            PeringatanKompas.tandaiSudahDibaca(sharedPref);
            spanduk.setVisibility(View.GONE);
            PeringatanKompas.tampilkan(requireContext(), -1, true);
        });
        binding.spandukKompas.spandukTutup.setOnClickListener(v -> {
            PeringatanKompas.tandaiSudahDibaca(sharedPref);
            spanduk.setVisibility(View.GONE);
        });
    }

    /**
     * Mencari saat bayangan benda tegak jatuh tepat di garis kiblat hari ini.
     * <p>
     * Dipindai di utas latar: satu hari berarti ribuan hitungan SPA, dan kalau
     * hari ini kosong pencariannya maju sampai sebulan ke depan.
     */
    private void aturBayanganKiblat(Location lokasi) {
        if (Double.isNaN(azimutKiblat)) {
            binding.bayanganKiblat.setText("-");
            kunciBayangan = null;
            return;
        }

        ZoneId zona = ZoneId.systemDefault();
        LocalDate hariIni = LocalDate.now(zona);
        // Dibulatkan ~100 m: lebih halus dari itu tidak pernah mengubah hasilnya.
        String kunci = String.format(Locale.ROOT, "%s|%.3f|%.3f",
                hariIni, lokasi.getLatitude(), lokasi.getLongitude());
        if (kunci.equals(kunciBayangan)) {
            return;
        }
        kunciBayangan = kunci;

        final double lat = lokasi.getLatitude();
        final double lon = lokasi.getLongitude();
        final double elevasi = lokasi.getAltitude();
        final double kiblat = azimutKiblat;
        binding.bayanganKiblat.setText("menghitung...");

        Latar.kerjakan(
                () -> BayanganKiblat.cariBerikutnya(lat, lon, elevasi, hariIni, zona, kiblat, 30),
                daftar -> {
                    if (binding == null) {
                        return;
                    }
                    if (daftar.isEmpty()) {
                        binding.bayanganKiblat.setText("tidak ada sebulan ini");
                        return;
                    }
                    BayanganKiblat.Saat saat = daftar.get(0);
                    String jam = saat.waktu.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                    // Bayangan yang menunjuk kiblat jauh lebih enak dipakai
                    // daripada bayangan yang membelakanginya, jadi bedanya
                    // disebutkan, bukan disamarkan.
                    String arah = saat.searahMatahari
                            ? "bayangan membelakangi kiblat"
                            : "bayangan menunjuk kiblat";
                    String hari = saat.waktu.toLocalDate().equals(hariIni)
                            ? ""
                            : saat.waktu.format(DateTimeFormatter.ofPattern("d MMM ")) + " ";
                    binding.bayanganKiblat.setText(String.format(Locale.getDefault(),
                            "%s%s (%s, matahari %.0f°)", hari, jam, arah, saat.tinggi));
                });
    }

    /**
     * Dirakit setiap kali, bukan sekali di onCreateView, supaya ihtiyati dan
     * cara hitung Ashar yang baru diubah lewat Pengaturan Adzan langsung
     * terpakai saat layar ini kembali tampil.
     */
    private void aturJadwal(Location lokasi) {
        ZonedDateTime sekarang = ZonedDateTime.now(ZoneId.systemDefault());
        binding.hari.setText(Weton.pemformat(
                        sekarang.getYear(),
                        sekarang.getMonth(),
                        sekarang.getDayOfMonth())
                .format(sekarang));

        List<ZonedDateTime> jadwal = JadwalSalat.hitung(sharedPref, lokasi, sekarang);
        if (jadwal == null) {
            for (TextView baris : barisAdzan) {
                baris.setText("-");
            }
            return;
        }

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("kk:mm:ss z");
        for (int i = 0; i < barisAdzan.length; i++) {
            barisAdzan[i].setText(jadwal.get(JadwalSalat.indeksAdzan(i)).format(dtf));
            //waktunya tetap ditampilkan, hanya dicoret kalau adzannya dimatikan lewat Pengaturan Adzan
            tandaiAdzan(barisAdzan[i], JadwalSalat.adzanAktif(sharedPref, i));
        }
    }

    private static void tandaiAdzan(TextView tampilan, boolean aktif) {
        tampilan.setAlpha(aktif ? 1f : 0.4f);
        tampilan.setPaintFlags(aktif
                ? tampilan.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG
                : tampilan.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
    }
}
