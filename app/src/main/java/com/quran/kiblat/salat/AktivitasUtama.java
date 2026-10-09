package com.quran.kiblat.salat;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.quran.kiblat.salat.alarm.PenjadwalAdzan;
import com.quran.kiblat.salat.databinding.AktivitasUtamaBinding;
import com.quran.kiblat.salat.izin.Izin;
import com.quran.kiblat.salat.izin.PengaturanIzin;
import com.quran.kiblat.salat.jadwal.JadwalSalat;
import com.quran.kiblat.salat.jadwal.PengaturanAdzan;
import com.quran.kiblat.salat.ui.ayat.PosisiBaca;
import com.quran.kiblat.salat.ui.ayat.SumberQuran;
import com.quran.kiblat.salat.ui.kompas.PeringatanKompas;
import com.quran.kiblat.salat.umum.Pref;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Satu-satunya aktivitas peluncur: laci navigasi (daftar surat atau juz),
 * menu opsi, dan wadah navigasi untuk keempat layar.
 */
public class AktivitasUtama extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    /**
     * Grup di menu laci. Hanya satu yang ada pada satu waktu; yang lain
     * dibuang dengan removeGroup.
     */
    private static final int GRUP_SURAT = 1;
    private static final int GRUP_JUZ = 2;

    private AppBarConfiguration mAppBarConfiguration;
    private AktivitasUtamaBinding binding;
    private NumberFormat angkaArab;
    private SharedPreferences sharedPref;
    // didaftarkan tanpa syarat, supaya hasilnya tetap sampai walaupun aktivitasnya
    // dibuat ulang selagi dialog izin sistem terbuka
    private final ActivityResultLauncher<String[]> mintaIzinAwal = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), hasil -> sesudahIzinAwal());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = AktivitasUtamaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        sharedPref = Pref.dari(this);
        angkaArab = NumberFormat.getNumberInstance(new Locale.Builder().setLanguageTag("ar-SA-u-nu-arab").build());

        ViewCompat.setOnApplyWindowInsetsListener(binding.bagianIsiUtama, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            mlp.leftMargin = insets.left;
            mlp.bottomMargin = insets.bottom;
            mlp.rightMargin = insets.right;
            v.setLayoutParams(mlp);
            return WindowInsetsCompat.CONSUMED;
        });

        binding.navView.setNavigationItemSelectedListener(this);
        isiDaftarSurat();

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.bagian_isi_utama);
        NavController navController = Objects.requireNonNull(navHostFragment).getNavController();
        navController.setGraph(R.navigation.mobile_navigation, PosisiBaca.argumenAwal(sharedPref));

        mAppBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph())
                .setOpenableLayout(binding.drawerLayout)
                .build();
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);

        pasangTombolKontak(binding.navView.getHeaderView(0));
        setVolumeControlStream(AudioManager.STREAM_ALARM);

        // Hanya waktu aplikasi dibuka dari awal; memutar layar tidak perlu
        // bertanya lagi atau menghitung ulang jadwal.
        if (savedInstanceState == null) {
            mulai();
        }
    }

    /**
     * Izin tidak lagi ditanyakan setiap kali aplikasi dibuka. Penjelasannya
     * muncul sekali saja, dan hanya untuk izin yang memang belum ada; sesudah
     * itu semuanya ada di menu Izin Aplikasi. Yang diingatkan lagi di sini
     * cuma keadaan yang membuat adzan sama sekali tidak berbunyi.
     */
    private void mulai() {
        if (!Izin.perluTanyaAwal(this)) {
            PenjadwalAdzan.segarkanJadwal(this, false);
            ingatkanAlarmTepat();
            return;
        }

        String[] izin = Izin.izinAwal(this);
        boolean alarm = Izin.alarmTepat(this);
        if (izin.length == 0 && alarm) {
            // pemasangan lama yang izinnya sudah lengkap: tidak ada yang perlu dijelaskan
            Izin.tandaiSudahTanya(this);
            PenjadwalAdzan.segarkanJadwal(this, false);
            return;
        }

        List<String> kurang = Arrays.asList(izin);
        StringBuilder isi = new StringBuilder("Supaya adzan berbunyi tepat waktu dan sesuai tempat Anda, aplikasi ini memerlukan:\n");
        if (kurang.contains(Manifest.permission.POST_NOTIFICATIONS)) {
            isi.append("\n• Notifikasi — menampilkan adzan beserta tombol Matikan.");
        }
        if (kurang.contains(Manifest.permission.ACCESS_FINE_LOCATION)) {
            isi.append("\n• Lokasi saat aplikasi dipakai — menghitung jadwal salat dan arah kiblat. ")
                    .append("Pilih lokasi tepat: lokasi perkiraan tidak membawa ketinggian.");
        }
        if (!alarm) {
            isi.append("\n• Alarm & pengingat — membunyikan adzan tepat pada waktunya. ")
                    .append("Izin ini diberikan lewat layar setelan, dari daftar sesudah ini.");
        }
        isi.append("\n\nLokasi di latar belakang tidak diminta. Semua izin bisa dilihat dan diubah ")
                .append("kapan saja lewat menu Izin Aplikasi.");

        new MaterialAlertDialogBuilder(this)
                .setTitle("Izin untuk Adzan")
                .setMessage(isi)
                .setCancelable(false)
                .setPositiveButton("Lanjut", (d, w) -> {
                    Izin.tandaiSudahTanya(this);
                    if (izin.length > 0) {
                        mintaIzinAwal.launch(izin);
                    } else {
                        sesudahIzinAwal();
                    }
                })
                .setNegativeButton("Nanti", (d, w) -> {
                    Izin.tandaiSudahTanya(this);
                    PenjadwalAdzan.segarkanJadwal(this, false);
                })
                .show();
    }

    private void sesudahIzinAwal() {
        PenjadwalAdzan.segarkanJadwal(this, false);
        // Alarm tepat waktu dan layar penuh tidak bisa diminta lewat dialog, hanya lewat
        // layar setelan. Dulu ketiga layar setelan dibuka bertumpuk sekaligus; sekarang
        // daftar izinnya yang menjelaskan masing-masing dan membawa ke sana satu per satu.
        if (!Izin.alarmTepat(this) || !Izin.layarPenuh(this)) {
            bukaIzinAplikasi();
        }
    }

    // satu-satunya pengingat yang muncul lagi: adzan yang dinyalakan tapi tidak bisa dijadwalkan
    private void ingatkanAlarmTepat() {
        if (Izin.alarmTepat(this) || !JadwalSalat.adaAdzanAktif(sharedPref)) {
            return;
        }
        Snackbar.make(binding.koordinator, "Adzan tidak akan berbunyi: izin Alarm & pengingat belum diberikan.",
                        Snackbar.LENGTH_INDEFINITE)
                .setTextMaxLines(3)
                .setAction("Atur", v -> bukaIzinAplikasi())
                .show();
    }

    private void bukaIzinAplikasi() {
        if (getSupportFragmentManager().findFragmentByTag(PengaturanIzin.TAG) == null) {
            new PengaturanIzin().show(getSupportFragmentManager(), PengaturanIzin.TAG);
        }
    }

    /**
     * Tombol WhatsApp, surel, dan peta di kepala laci navigasi.
     */
    private void pasangTombolKontak(View kepala) {
        kepala.findViewById(R.id.wa).setOnClickListener(v -> {
            String url = "https://wa.me/6282225268957";
            try {
                getPackageManager().getPackageInfo("com.whatsapp", PackageManager.GET_ACTIVITIES);
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                i.setPackage("com.whatsapp");
                startActivity(i);
            } catch (PackageManager.NameNotFoundException e) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            }
        });

        kepala.findViewById(R.id.gmail).setOnClickListener(v -> {
            Intent emailSelectorIntent = new Intent(Intent.ACTION_SENDTO);
            emailSelectorIntent.setDataAndType(Uri.parse("mailto:muhammad.dawud91@gmail.com"), "message/rfc822");

            final Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"muhammad.dawud91@gmail.com"});
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "hello");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "hi");
            emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            emailIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            emailIntent.setSelector(emailSelectorIntent);

            if (emailIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(emailIntent);
            }
        });

        kepala.findViewById(R.id.maps).setOnClickListener(v -> {
            Uri gmmIntentUri = Uri.parse("geo:0,0?q=rumah@7.39588,110.828549");
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            startActivity(mapIntent);
        });
    }

    /**
     * Mengisi laci dengan 114 surat, menggantikan daftar juz kalau ada.
     */
    private void isiDaftarSurat() {
        Menu daftarMenu = binding.navView.getMenu();
        daftarMenu.removeGroup(GRUP_JUZ);
        daftarMenu.removeGroup(GRUP_SURAT);
        JSONArray daftarSurat = SumberQuran.daftarSurat(this);
        int jumlahSurat = daftarSurat == null ? 0 : daftarSurat.length();
        for (int i = 0; i < jumlahSurat; i++) {
            try {
                JSONObject jo = daftarSurat.getJSONObject(i);
                daftarMenu.add(GRUP_SURAT, jo.getInt("id"), Menu.NONE,
                        jo.getString("surat_name") + " (" + jo.getString("surat_text") + ")");
            } catch (JSONException ignored) {
            }
        }
        binding.navView.invalidate();
    }

    /**
     * Mengisi laci dengan 30 juz, menggantikan daftar surat.
     */
    private void isiDaftarJuz() {
        Menu daftarMenu = binding.navView.getMenu();
        daftarMenu.removeGroup(GRUP_SURAT);
        daftarMenu.removeGroup(GRUP_JUZ);
        for (int i = 1; i <= SumberQuran.JUMLAH_JUZ; i++) {
            daftarMenu.add(GRUP_JUZ, i, Menu.NONE, "Juz " + angkaArab.format(i));
        }
        binding.navView.invalidate();
    }

    /**
     * Membuka layar tujuan dari awal, membuang salinan lamanya di tumpukan.
     */
    private void bukaLayar(int tujuan, Bundle argumen) {
        NavController navController = Navigation.findNavController(this, R.id.bagian_isi_utama);
        navController.popBackStack(tujuan, true);
        navController.navigate(tujuan, argumen);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.getItem(0).setChecked(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (R.id.surat == id) {
            isiDaftarSurat();
            binding.drawerLayout.openDrawer(GravityCompat.START);
        } else if (R.id.juz == id) {
            isiDaftarJuz();
            binding.drawerLayout.openDrawer(GravityCompat.START);
        } else if (R.id.kiblat == id) {
            bukaLayar(R.id.nav_kiblat, null);
        } else if (R.id.terbit == id) {
            bukaLayar(R.id.nav_terbit, null);
        } else if (R.id.tenggelam == id) {
            bukaLayar(R.id.nav_tenggelam, null);
        } else if (R.id.pengaturan_adzan == id) {
            new PengaturanAdzan().show(getSupportFragmentManager(), PengaturanAdzan.TAG);
        } else if (R.id.izin_aplikasi == id) {
            bukaIzinAplikasi();
        } else if (R.id.ketepatan_kompas == id) {
            PeringatanKompas.tampilkan(this, -1, true);
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.bagian_isi_utama);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        bukaLayar(R.id.nav_ayat, item.getGroupId() == GRUP_JUZ
                ? PosisiBaca.argumenJuz(item.getItemId())
                : PosisiBaca.argumenSurat(item.getItemId(), Objects.requireNonNull(item.getTitle()).toString()));

        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        }
        return true;
    }
}
