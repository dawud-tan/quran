package com.quran.kiblat.salat.izin;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.IntentCompat;
import androidx.core.content.PackageManagerCompat;
import androidx.core.content.UnusedAppRestrictionsConstants;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.util.concurrent.ListenableFuture;
import com.quran.kiblat.salat.R;
import com.quran.kiblat.salat.alarm.PenjadwalAdzan;
import com.quran.kiblat.salat.databinding.BarisIzinBinding;
import com.quran.kiblat.salat.databinding.PengaturanIzinBinding;

//semua izin di satu tempat: keadaannya, akibatnya kalau ditolak beserta gantinya, dan
//jalan untuk mengubahnya. Pengganti permintaan izin yang dulu diulang setiap aplikasi dibuka
public class PengaturanIzin extends DialogFragment {

    public static final String TAG = "pengaturan_izin";

    private static final String[] LOKASI = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private PengaturanIzinBinding ikatan;
    /// Izin lokasi biasa sedang diminta sebagai langkah pertama menyalakan lokasi latar.
    private boolean lanjutKeLatar;
    /// Keadaan izin lokasi waktu terakhir dilihat, supaya jadwal dihitung ulang begitu
    /// izinnya baru saja diberikan — termasuk lewat layar setelan sistem.
    private boolean lokasiTadi;

    private final ActivityResultLauncher<String[]> mintaLokasi = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), hasil -> sesudahIzinLokasi());
    private final ActivityResultLauncher<String> mintaLokasiLatar = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), this::sesudahIzinLokasiLatar);
    private final ActivityResultLauncher<String> mintaNotifikasi = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), boleh -> {
                if (!boleh) {
                    tawarkanSetelan("Notifikasi masih ditolak. Izinkan lewat setelan notifikasi aplikasi.",
                            Izin.setelanNotifikasi(requireContext()));
                }
                perbarui();
            });
    // dokumentasinya meminta startActivityForResult, bukan startActivity
    private final ActivityResultLauncher<Intent> bukaSetelanJeda = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), hasil -> perbarui());

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context konteks = requireContext();
        if (savedInstanceState != null) {
            lanjutKeLatar = savedInstanceState.getBoolean("lanjutKeLatar");
        }
        lokasiTadi = Izin.lokasi(konteks);

        ikatan = PengaturanIzinBinding.inflate(getLayoutInflater());

        isi(ikatan.barisAlarm, "Alarm & pengingat",
                "Wajib agar adzan berbunyi. Tanpa izin ini adzan dan pengingat 10 menit sama sekali "
                        + "tidak dijadwalkan, dan tidak ada gantinya. Kalau memang tidak ingin adzan, "
                        + "matikan semua waktu di Pengaturan Adzan.");
        tampil(ikatan.barisAlarm, Build.VERSION.SDK_INT >= Build.VERSION_CODES.S);
        ikatan.barisAlarm.tombol.setOnClickListener(v ->
                Izin.buka(requireActivity(), Izin.setelanAlarmTepat(konteks)));

        isi(ikatan.barisNotifikasi, "Notifikasi",
                "Menampilkan adzan beserta tombol Matikan. Kalau ditolak, adzan tetap berbunyi, tetapi "
                        + "nada pengingat 10 menit hanya berbunyi 3 menit lalu berhenti sendiri, karena "
                        + "tidak ada tombol untuk mematikannya.");
        ikatan.barisNotifikasi.tombol.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    && ContextCompat.checkSelfPermission(konteks, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                mintaNotifikasi.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                Izin.buka(requireActivity(), Izin.setelanNotifikasi(konteks));
            }
        });

        isi(ikatan.barisLayarPenuh, "Layar penuh",
                "Menampilkan layar adzan di atas layar kunci. Kalau ditolak, adzan tetap berbunyi dan "
                        + "muncul sebagai notifikasi biasa, lengkap dengan tombol Matikan.");
        tampil(ikatan.barisLayarPenuh, Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE);
        ikatan.barisLayarPenuh.tombol.setOnClickListener(v ->
                Izin.buka(requireActivity(), Izin.setelanLayarPenuh(konteks)));

        isi(ikatan.barisLokasi, "Lokasi saat aplikasi dipakai",
                "Dipakai untuk jadwal salat dan arah kiblat, dibaca setiap kali aplikasi dibuka. Kalau "
                        + "ditolak, dipakai lokasi tersimpan terakhir — atau titik tengah Indonesia kalau "
                        + "belum pernah ada, yang untuk Jakarta meleset 37–52 menit. Pilih lokasi tepat: "
                        + "lokasi perkiraan tidak membawa ketinggian, padahal di dataran tinggi seperti "
                        + "Bandung ketinggian menggeser terbit dan maghrib 4 menit.");
        ikatan.barisLokasi.tombol.setOnClickListener(v -> {
            if (Izin.lokasi(konteks) && !Izin.layananLokasiHidup(konteks)) {
                Izin.buka(requireActivity(), Izin.setelanLayananLokasi());
            } else {
                // belum diizinkan, atau baru perkiraan: Android 12+ menawarkan naik ke lokasi tepat
                mintaLokasi.launch(LOKASI);
            }
        });

        isi(ikatan.barisLokasiLatar, "Lokasi di latar belakang",
                "Mati: jadwal adzan memakai lokasi saat aplikasi terakhir dibuka. Dalam jarak sekitar "
                        + "40 km selisihnya paling banyak 1 menit, tetapi pindah kota tanpa membuka aplikasi "
                        + "menggeser adzan — Jakarta ke Bandung 1–5 menit, ke Surabaya 21–26 menit.\n"
                        + "Hidup: 10 menit sebelum adzan, aplikasi membaca lokasi terakhir yang sudah "
                        + "diketahui ponsel. GPS tidak dinyalakan, jadi tidak menambah pemakaian baterai.");
        tampil(ikatan.barisLokasiLatar, Izin.lokasiLatarTersedia(konteks));
        ikatan.barisLokasiLatar.sakelar.setText("Perbarui lokasi sebelum adzan");
        ikatan.barisLokasiLatar.sakelar.setVisibility(View.VISIBLE);
        // klik, bukan OnCheckedChangeListener: perbarui() mengubah centangnya dari kode
        ikatan.barisLokasiLatar.sakelar.setOnClickListener(v -> {
            if (ikatan.barisLokasiLatar.sakelar.isChecked()) {
                // baru benar-benar hidup kalau izinnya diberikan
                ikatan.barisLokasiLatar.sakelar.setChecked(false);
                jelaskanLokasiLatar();
            } else {
                Izin.setelLokasiLatar(konteks, false);
                perbarui();
            }
        });

        isi(ikatan.barisJeda, "Jeda saat jarang dibuka",
                "Kalau aplikasi tidak dibuka selama beberapa bulan, Android bisa mencabut semua izinnya "
                        + "dan menghentikan alarm. Adzan yang berbunyi tidak dihitung sebagai membuka "
                        + "aplikasi. Matikan \"Jeda aktivitas aplikasi jika tidak digunakan\" supaya adzan "
                        + "tidak berhenti diam-diam.");
        tampil(ikatan.barisJeda, false); // ditampilkan sesudah keadaannya diketahui
        ikatan.barisJeda.tombol.setOnClickListener(v -> bukaSetelanJeda.launch(
                IntentCompat.createManageUnusedAppRestrictionsIntent(konteks, konteks.getPackageName())));

        isi(ikatan.barisBaterai, "Optimasi baterai",
                "Biasanya tidak perlu diubah. Adzan dipasang sebagai alarm jam, yang membangunkan ponsel "
                        + "dari mode hemat daya (Doze) sebelum waktunya tiba. Ubah hanya kalau adzan sering "
                        + "tidak berbunyi — beberapa merek ponsel menutup paksa aplikasi di latar belakang. "
                        + "Di halaman info aplikasi pilih Baterai, lalu Tidak dibatasi.");
        ikatan.barisBaterai.tombol.setOnClickListener(v ->
                Izin.buka(requireActivity(), Izin.setelanAplikasi(konteks)));

        return new MaterialAlertDialogBuilder(requireActivity())
                .setTitle("Izin Aplikasi")
                .setView(ikatan.getRoot())
                .setPositiveButton("Tutup", null)
                .create();
    }

    @Override
    public void onResume() {
        super.onResume();
        // kembali dari layar setelan sistem
        perbarui();
    }

    private void perbarui() {
        if (ikatan == null) {
            return;
        }
        Context k = requireContext();

        keadaan(ikatan.barisAlarm, Izin.alarmTepat(k), "diizinkan", "belum diizinkan", "Izinkan");
        keadaan(ikatan.barisNotifikasi, Izin.notifikasi(k), "diizinkan", "mati", "Izinkan");
        keadaan(ikatan.barisLayarPenuh, Izin.layarPenuh(k), "diizinkan", "belum diizinkan", "Izinkan");

        BarisIzinBinding lokasi = ikatan.barisLokasi;
        boolean bolehLokasi = Izin.lokasi(k);
        if (!bolehLokasi) {
            keadaan(lokasi, false, "", "belum diizinkan", "Izinkan");
        } else if (!Izin.layananLokasiHidup(k)) {
            keadaan(lokasi, false, "", "lokasi ponsel mati", "Nyalakan");
        } else if (!Izin.lokasiTepat(k)) {
            keadaan(lokasi, false, "", "perkiraan", "Pakai lokasi tepat");
        } else {
            keadaan(lokasi, true, "tepat", "", "");
        }
        if (bolehLokasi && !lokasiTadi) {
            // baru saja diizinkan: alarm berikutnya langsung memakai lokasi sungguhan
            PenjadwalAdzan.segarkanJadwal(k, false);
        }
        lokasiTadi = bolehLokasi;

        boolean latar = Izin.lokasiLatarAktif(k);
        ikatan.barisLokasiLatar.sakelar.setChecked(latar);
        ikatan.barisLokasiLatar.status.setText(latar ? "hidup" : "mati");
        warnai(ikatan.barisLokasiLatar.status, null);

        boolean bebas = Izin.bebasOptimasiBaterai(k);
        ikatan.barisBaterai.status.setText(bebas ? "tidak dibatasi" : "dioptimalkan");
        warnai(ikatan.barisBaterai.status, null);
        ikatan.barisBaterai.tombol.setText("Buka info aplikasi");
        ikatan.barisBaterai.tombol.setVisibility(View.VISIBLE);

        ListenableFuture<Integer> jeda = PackageManagerCompat.getUnusedAppRestrictionsStatus(k);
        jeda.addListener(() -> {
            int s;
            try {
                s = jeda.get();
            } catch (Exception ex) {
                s = UnusedAppRestrictionsConstants.ERROR;
            }
            tampilkanJeda(s);
        }, ContextCompat.getMainExecutor(k));
    }

    private void tampilkanJeda(int s) {
        if (ikatan == null) {
            return;
        }
        boolean berlaku = s == UnusedAppRestrictionsConstants.DISABLED
                || s == UnusedAppRestrictionsConstants.API_30_BACKPORT
                || s == UnusedAppRestrictionsConstants.API_30
                || s == UnusedAppRestrictionsConstants.API_31;
        tampil(ikatan.barisJeda, berlaku);
        keadaan(ikatan.barisJeda, s == UnusedAppRestrictionsConstants.DISABLED,
                "dimatikan", "aktif", "Buka setelan");
    }

    private void jelaskanLokasiLatar() {
        // Pengungkapan yang disyaratkan Google Play sebelum meminta lokasi latar: menyebut
        // "lokasi", "saat aplikasi ditutup atau tidak digunakan", fiturnya, dan ke mana datanya
        // pergi. Harus ada persetujuan yang disengaja; menutup dialog bukan persetujuan.
        new MaterialAlertDialogBuilder(requireActivity())
                .setTitle("Lokasi di latar belakang")
                .setMessage("Aplikasi " + getString(R.string.app_name) + " mengumpulkan data lokasi untuk "
                        + "menyesuaikan jadwal adzan dengan tempat Anda berada, bahkan saat aplikasi "
                        + "ditutup atau tidak digunakan.\n\n"
                        + "Caranya: 10 menit sebelum setiap adzan, aplikasi membaca lokasi terakhir yang "
                        + "sudah diketahui ponsel, tanpa menyalakan GPS. Koordinat itu disimpan di ponsel "
                        + "ini, dan dikirim ke OpenStreetMap (Nominatim) hanya untuk mencari nama tempat "
                        + "yang ditampilkan di notifikasi. Lokasi tidak dipakai untuk iklan dan tidak "
                        + "dibagikan ke pihak lain.\n\n"
                        + "Di layar berikutnya pilih \"Izinkan sepanjang waktu\". Fitur ini bisa "
                        + "dimatikan lagi kapan saja di sini.")
                .setPositiveButton("Setuju", (d, w) -> mulaiLokasiLatar())
                .setNegativeButton("Tidak", null)
                .show();
    }

    private void mulaiLokasiLatar() {
        Context k = requireContext();
        if (Izin.izinLokasiLatar(k)) {
            Izin.setelLokasiLatar(k, true);
            perbarui();
        } else if (!Izin.lokasi(k)) {
            // Android 11+ menolak meminta lokasi latar sebelum lokasi biasa diberikan
            lanjutKeLatar = true;
            mintaLokasi.launch(LOKASI);
        } else {
            mintaLokasiLatar.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }
    }

    private void sesudahIzinLokasi() {
        Context k = requireContext();
        boolean keLatar = lanjutKeLatar;
        lanjutKeLatar = false;
        if (keLatar && Izin.lokasi(k)) {
            mintaLokasiLatar.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        } else if (!Izin.lokasi(k)) {
            tawarkanSetelan("Izin lokasi masih ditolak. Izinkan lewat Izin › Lokasi di setelan aplikasi.",
                    Izin.setelanAplikasi(k));
        }
        perbarui();
    }

    private void sesudahIzinLokasiLatar(boolean boleh) {
        Context k = requireContext();
        if (boleh) {
            Izin.setelLokasiLatar(k, true);
        } else {
            tawarkanSetelan("Lokasi di latar belakang belum diizinkan. Untuk menyalakannya, buka "
                    + "Izin › Lokasi lalu pilih \"Izinkan sepanjang waktu\".", Izin.setelanAplikasi(k));
        }
        perbarui();
    }

    // dipakai kalau sistem sudah tidak mau menampilkan dialog izinnya lagi
    private void tawarkanSetelan(String pesan, Intent setelan) {
        new MaterialAlertDialogBuilder(requireActivity())
                .setMessage(pesan)
                .setPositiveButton("Buka setelan", (d, w) -> Izin.buka(requireActivity(), setelan))
                .setNegativeButton("Tutup", null)
                .show();
    }

    private static void isi(BarisIzinBinding baris, String judul, String keterangan) {
        baris.judul.setText(judul);
        baris.keterangan.setText(keterangan);
    }

    private static void tampil(BarisIzinBinding baris, boolean tampil) {
        baris.getRoot().setVisibility(tampil ? View.VISIBLE : View.GONE);
    }

    private static void keadaan(BarisIzinBinding baris, boolean baik, String ya, String tidak, String tombol) {
        baris.status.setText(baik ? ya : tidak);
        warnai(baris.status, baik);
        baris.tombol.setText(tombol);
        baris.tombol.setVisibility(baik ? View.GONE : View.VISIBLE);
    }

    /// null = netral: keadaan yang memang pilihan, bukan kekurangan
    private static void warnai(TextView status, Boolean baik) {
        int attr = baik == null ? android.R.attr.textColorSecondary
                : baik ? androidx.appcompat.R.attr.colorPrimary
                : androidx.appcompat.R.attr.colorError;
        status.setTextColor(MaterialColors.getColor(status, attr));
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("lanjutKeLatar", lanjutKeLatar);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ikatan = null;
    }
}
