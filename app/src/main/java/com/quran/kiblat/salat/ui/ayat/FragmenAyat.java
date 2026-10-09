package com.quran.kiblat.salat.ui.ayat;

import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.quran.kiblat.salat.databinding.FragmenAyatBinding;
import com.quran.kiblat.salat.umum.Pref;

import java.text.NumberFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Daftar ayat satu surat atau satu juz. Argumennya dari {@link PosisiBaca}.
 */
public class FragmenAyat extends Fragment {

    private FragmenAyatBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmenAyatBinding.inflate(inflater, container, false);

        NumberFormat angkaArab = NumberFormat.getNumberInstance(
                new Locale.Builder().setLanguageTag("ar-SA-u-nu-arab").build());
        SharedPreferences sharedPref = Pref.dari(requireContext());
        Bundle argumen = requireArguments();

        int idSurat = argumen.getInt(PosisiBaca.ARG_SURAT);
        int idJuz = argumen.getInt(PosisiBaca.ARG_JUZ);

        List<Ayat> daftarAyat;
        String judulLayar;
        if (idSurat == 0 && idJuz > 0) {
            PosisiBaca.catatMode(sharedPref, true);
            daftarAyat = SumberQuran.ayatJuz(requireContext(), idJuz);
            judulLayar = "Juz ke " + angkaArab.format(idJuz);
        } else {
            // surat yang diminta, atau Al-Fatihah kalau argumennya tidak sah
            boolean sah = idSurat > 0 && idJuz == 0;
            String judul = sah ? argumen.getString(PosisiBaca.ARG_JUDUL) : "Al-Fatihah";
            PosisiBaca.catatMode(sharedPref, false);
            daftarAyat = SumberQuran.ayatSurat(requireContext(), sah ? idSurat : 1, judul);
            judulLayar = "Surat " + judul;
        }
        ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(judulLayar);
        }
        if (daftarAyat == null) {
            // berkas assets rusak: daftar kosong lebih baik daripada mogok
            daftarAyat = Collections.emptyList();
        }

        boolean malam = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        binding.daftarAngka.setHasFixedSize(true);
        binding.daftarAngka.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
        binding.daftarAngka.setAdapter(new AdapterAyat(daftarAyat, angkaArab, sharedPref, malam));

        int posisi = argumen.getInt(PosisiBaca.ARG_POSISI);
        if (posisi > 0) {
            binding.daftarAngka.smoothScrollToPosition(posisi);
        }
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
