package com.quran.kiblat.salat.ui.matahari;

import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.quran.kiblat.salat.R;
import com.quran.kiblat.salat.databinding.FragmenArahMatahariBinding;
import com.quran.kiblat.salat.hisab.ArahMatahari;
import com.quran.kiblat.salat.hisab.Weton;
import com.quran.kiblat.salat.ui.kompas.FragmenKompas;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

/**
 * Arah dan waktu matahari terbit, atau terbenam. Satu kelas untuk kedua
 * layar: tujuan navigasi {@code nav_terbit} dan {@code nav_tenggelam} sama-sama
 * menunjuk ke sini dan hanya beda argumen {@link #ARG_TERBIT}. Dulu dua kelas
 * yang isinya hampir sama baris per baris.
 */
public class FragmenArahMatahari extends FragmenKompas {

    /**
     * Argumen navigasi, boolean: true = terbit, false = terbenam.
     */
    public static final String ARG_TERBIT = "terbit";

    private final DateTimeFormatter dtf = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM);
    private FragmenArahMatahariBinding binding;
    private boolean terbit;
    /**
     * Azimut matahari saat terbit/terbenam hari ini, atau NaN kalau hari ini
     * matahari tidak terbit/terbenam di lintang ini.
     */
    private double azimutMatahari = Double.NaN;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmenArahMatahariBinding.inflate(inflater, container, false);
        terbit = getArguments() == null || getArguments().getBoolean(ARG_TERBIT, true);

        binding.gambar.setImageResource(terbit ? R.drawable.sunrise : R.drawable.sunset);
        binding.judulArah.setText(terbit ? "Arah Terbit" : "Arah Tenggelam");
        binding.judulWaktu.setText(terbit ? "Waktu Terbit" : "Waktu Tenggelam");

        mulaiKompas(new Tampilan(binding.gambar, binding.gambar2, binding.koordinat,
                binding.akurasiGps, binding.mdpl, binding.sudutAzimut, binding.mikrotesla));
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    protected boolean layarKiblat() {
        // magnetometernya sama dengan layar kiblat, jadi masalahnya sama,
        // cuma patokan jaraknya yang beda
        return false;
    }

    @Override
    protected double azimutSasaran() {
        return azimutMatahari;
    }

    @Override
    protected void perbaruiData(Location lokasi) {
        ZonedDateTime sekarang = ZonedDateTime.now(ZoneId.systemDefault());
        binding.hari.setText(Weton.pemformat(
                        sekarang.getYear(),
                        sekarang.getMonth(),
                        sekarang.getDayOfMonth())
                .format(sekarang));

        ArahMatahari.Hasil hasil = ArahMatahari.hitung(sekarang, lokasi.getLatitude(),
                lokasi.getLongitude(), lokasi.getAltitude(), terbit);
        if (hasil == null) {
            //di lintang tinggi matahari bisa tidak terbit atau tidak terbenam seharian
            binding.waktuMatahari.setText("-");
            azimutMatahari = Double.NaN;
            return;
        }
        binding.waktuMatahari.setText(dtf.format(hasil.waktu()));
        azimutMatahari = hasil.azimut();
    }
}
