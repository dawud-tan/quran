package com.quran.kiblat.salat.ui.ayat;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.quran.kiblat.salat.Util;
import com.quran.kiblat.salat.databinding.FragmenAyatBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class FragmenAyat extends Fragment {

    private static final Ayat BASMALLAH = new Ayat(
            0,
            0,
            "Al-Fatihah",
            0,
            "\u0628\u0650\u0633\u0652\u0645\u0650 \u0627\u0644\u0644\u0651\u0670\u0647\u0650 \u0627\u0644\u0631\u0651\u064e\u062d\u0652\u0645\u0670\u0646\u0650 \u0627\u0644\u0631\u0651\u064e\u062d\u0650\u064a\u0652\u0645\u0650",
            "Dengan nama Allah Yang Maha Pengasih, Maha Penyayang."
    );
    private FragmenAyatBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmenAyatBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        Locale locale = new Locale.Builder().setLanguageTag("ar-SA-u-nu-arab").build();
        NumberFormat numberFormat = NumberFormat.getNumberInstance(locale);
        SharedPreferences sharedPref = requireContext().getSharedPreferences("pref", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        int id_surat = getArguments().getInt("id_surat");
        int id_juz = getArguments().getInt("id_juz");

        List<Ayat> daftarAyat = null;
        if (id_surat > 0 && id_juz == 0) {
            editor.putString("mode", "id_surat");
            editor.apply();

            String judul = getArguments().getString("judul");
            daftarAyat = daftarAyat(requireContext(), id_surat, judul);
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar())
                    .setTitle("Surat " + judul);
        } else if (id_surat == 0 && id_juz > 0) {
            editor.putString("mode", "id_juz");
            editor.apply();
            daftarAyat = daftarJuz(requireContext(), id_juz);
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar())
                    .setTitle("Juz ke " + numberFormat.format(id_juz));
        } else {
            editor.putString("mode", "id_surat");
            editor.apply();

            String judul = "Al-Fatihah";
            daftarAyat = daftarAyat(requireContext(), 1, judul);
            Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar())
                    .setTitle("Surat " + judul);
        }

        boolean nightModeFlags = (requireContext().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        AdapaterAyat adapaterAyat = new AdapaterAyat(daftarAyat, numberFormat, requireContext(), nightModeFlags);
        binding.daftarAngka.setHasFixedSize(true);
        binding.daftarAngka.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
        binding.daftarAngka.setAdapter(adapaterAyat);

        int bindingAdapterPosition = getArguments().getInt("bindingAdapterPosition");
        if (bindingAdapterPosition > 0) {
            binding.daftarAngka.smoothScrollToPosition(bindingAdapterPosition);
        }
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public List<Ayat> daftarAyat(Context context, int id, String judul) {
        List<Ayat> daftarAyat = null;
        try {
            daftarAyat = new ArrayList<>();
            if (id > 1 && id != 9) {
                daftarAyat.add(BASMALLAH);
            }

            String json = Util.bacaBerkas(context.getAssets(), "Surat/" + id + ".json");
            JSONObject jo = new JSONObject(json);
            JSONArray jarray = jo.getJSONArray("data");
            int jumlahAyat = jarray.length();
            for (short i = 0; i < jumlahAyat; i++) {
                JSONObject jo1 = jarray.getJSONObject(i);
                daftarAyat.add(new Ayat(
                        jo1.getInt("juz_id"),
                        id,
                        judul,
                        jo1.getInt("aya_number"),
                        jo1.getString("aya_text"),
                        jo1.getString("translation_aya_text")
                ));

            }
        } catch (IOException | JSONException ex) {
            return null;
        }
        return daftarAyat;
    }

    public List<Ayat> daftarJuz(Context context, int id) {
        List<Ayat> daftarAyat = null;
        try {
            daftarAyat = new ArrayList<>();
            int ayatSebelum = 0;

            JSONArray daftarSurat = daftarSurat(requireContext());
            int jumlahSurat = daftarSurat.length();

            for (short suratKe = 1; suratKe <= jumlahSurat; suratKe++) {
                JSONObject jo4 = daftarSurat.getJSONObject(suratKe - 1);
                String json = Util.bacaBerkas(context.getAssets(), "Surat/" + suratKe + ".json");
                JSONObject jo = new JSONObject(json);
                JSONArray jarray = jo.getJSONArray("data");
                int jumlahAyat = jarray.length();
                for (short i = 0; i < jumlahAyat; i++) {
                    JSONObject jo1 = jarray.getJSONObject(i);

                    int id_juz = jo1.getInt("juz_id");
                    if (id_juz == id) {
                        if (suratKe > 1 && suratKe != 9 && ((jo1.getInt("aya_number") - ayatSebelum < 0) || i == 0)) {
                            daftarAyat.add(BASMALLAH);
                        }

                        daftarAyat.add(new Ayat(
                                jo1.getInt("juz_id"),
                                suratKe,
                                jo4.getString("surat_name"),
                                jo1.getInt("aya_number"),
                                jo1.getString("aya_text"),
                                jo1.getString("translation_aya_text")
                        ));

                        ayatSebelum = jo1.getInt("aya_number");
                    }
                }
            }
        } catch (IOException | JSONException ex) {
            return null;
        }
        return daftarAyat;
    }

    public JSONArray daftarSurat(Context context) {
        JSONArray jarray = null;
        try {
            String json = Util.bacaBerkas(context.getAssets(), "daftar_surat.json");
            jarray = new JSONArray(json);
        } catch (IOException | JSONException ignored) {
            return null;
        }
        return jarray;
    }

}