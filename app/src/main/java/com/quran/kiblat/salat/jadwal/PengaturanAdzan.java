package com.quran.kiblat.salat.jadwal;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.quran.kiblat.salat.R;
import com.quran.kiblat.salat.alarm.PenjadwalAdzan;
import com.quran.kiblat.salat.databinding.PengaturanAdzanBinding;
import com.quran.kiblat.salat.lokasi.Lokasi;
import com.quran.kiblat.salat.umum.Pref;

import java.util.Locale;

//pilih waktu mana saja yang dibunyikan, berapa menit ihtiyati, dan cara hitung Ashar,
//tanpa perlu copot pemasangan
public class PengaturanAdzan extends DialogFragment {

    public static final String TAG = "pengaturan_adzan";

    private PengaturanAdzanBinding ikatan;
    private MaterialCheckBox[] centang;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context konteks = requireContext().getApplicationContext();
        SharedPreferences sharedPref = Pref.dari(konteks);

        ikatan = PengaturanAdzanBinding.inflate(getLayoutInflater());

        //nilai awal dari simpanan, lalu ditimpa centangan yang belum sempat disimpan
        //kalau layar kebetulan diputar selagi dialog terbuka
        boolean[] aktif = new boolean[JadwalSalat.jumlahAdzan()];
        for (int i = 0; i < aktif.length; i++) {
            aktif[i] = JadwalSalat.adzanAktif(sharedPref, i);
        }
        int ihtiyati = JadwalSalat.ihtiyati(sharedPref);
        int bayangan = JadwalSalat.bayanganAshar(sharedPref);
        if (savedInstanceState != null) {
            boolean[] tersimpan = savedInstanceState.getBooleanArray("aktif");
            if (tersimpan != null && tersimpan.length == aktif.length) {
                aktif = tersimpan;
            }
            ihtiyati = savedInstanceState.getInt("ihtiyati", ihtiyati);
            bayangan = savedInstanceState.getInt("bayangan_ashar", bayangan);
        }

        centang = new MaterialCheckBox[aktif.length];
        for (int i = 0; i < aktif.length; i++) {
            String nama = JadwalSalat.namaAdzan(i);
            MaterialCheckBox kotak = new MaterialCheckBox(ikatan.daftarAdzan.getContext());
            kotak.setText(nama.substring(0, 1).toUpperCase(Locale.ROOT) + nama.substring(1));
            kotak.setChecked(aktif[i]);
            centang[i] = kotak;
            ikatan.daftarAdzan.addView(kotak);
        }

        ikatan.ihtiyati.setValueTo(JadwalSalat.ihtiyatiMaks());
        ikatan.ihtiyati.setValue(ihtiyati);
        tulisIhtiyati(ihtiyati);
        ikatan.ihtiyati.addOnChangeListener((slider, nilai, dariPengguna) -> tulisIhtiyati((int) nilai));

        ikatan.caraAshar.check(bayangan == 2 ? R.id.bayangan_dua : R.id.bayangan_satu);

        return new MaterialAlertDialogBuilder(requireActivity())
                .setTitle("Pengaturan Adzan")
                .setView(ikatan.getRoot())
                .setPositiveButton("Simpan", (dialog, which) -> {
                    boolean[] pilihan = new boolean[centang.length];
                    for (int i = 0; i < centang.length; i++) {
                        pilihan[i] = centang[i].isChecked();
                    }
                    JadwalSalat.simpanSetelan(sharedPref, pilihan,
                            (int) ikatan.ihtiyati.getValue(), bayanganTerpilih());
                    //jadwal terdekat dihitung ulang supaya waktu yang baru dimatikan tidak jadi
                    //berbunyi, yang baru dinyalakan langsung terpasang, dan ihtiyati baru terpakai
                    Location lokasi = Lokasi.terakhir(sharedPref);
                    PenjadwalAdzan.cekJadwal(konteks, lokasi);
                })
                .setNegativeButton("Batal", null)
                .create();
    }

    private int bayanganTerpilih() {
        return ikatan.caraAshar.getCheckedRadioButtonId() == R.id.bayangan_dua ? 2 : 1;
    }

    private void tulisIhtiyati(int menit) {
        ikatan.judulIhtiyati.setText("Ihtiyati: " + menit + " menit");
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (ikatan == null) {
            return;
        }
        boolean[] pilihan = new boolean[centang.length];
        for (int i = 0; i < centang.length; i++) {
            pilihan[i] = centang[i].isChecked();
        }
        outState.putBooleanArray("aktif", pilihan);
        outState.putInt("ihtiyati", (int) ikatan.ihtiyati.getValue());
        outState.putInt("bayangan_ashar", bayanganTerpilih());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ikatan = null;
        centang = null;
    }
}
