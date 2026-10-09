package com.quran.kiblat.salat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.quran.kiblat.salat.databinding.AktivitasUtamaBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class AktivitasUtama extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private AppBarConfiguration mAppBarConfiguration;
    private AktivitasUtamaBinding binding;
    private NumberFormat numberFormat;
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

        DrawerLayout drawer = binding.drawerLayout;
        ViewCompat.setOnApplyWindowInsetsListener(binding.bagianIsiUtama, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            mlp.leftMargin = insets.left;
            mlp.bottomMargin = insets.bottom;
            mlp.rightMargin = insets.right;
            v.setLayoutParams(mlp);
            return WindowInsetsCompat.CONSUMED;
        });

        NavigationView navView = binding.navView;
        navView.setNavigationItemSelectedListener(this);

        Locale locale = new Locale.Builder().setLanguageTag("ar-SA-u-nu-arab").build();
        numberFormat = NumberFormat.getNumberInstance(locale);

        Menu daftarMenu = navView.getMenu();
        daftarMenu.removeGroup(2);
        JSONArray daftarSurat = daftarSurat(getApplicationContext());
        int jumlahSurat = daftarSurat.length();
        for (short i = 0; i < jumlahSurat; i++) {
            try {
                JSONObject jo = daftarSurat.getJSONObject(i);
                daftarMenu.add(1, jo.getInt("id"), Menu.NONE, jo.getString("surat_name") + " (" + jo.getString("surat_text") + ")");
            } catch (JSONException ignored) {
            }
        }
        navView.invalidate();

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.bagian_isi_utama);
        NavController navController = Objects.requireNonNull(navHostFragment).getNavController();

        sharedPref = getSharedPreferences("pref", Context.MODE_PRIVATE);
        String mode = sharedPref.getString("mode", null);
        int bindingAdapterPosition = sharedPref.getInt("bindingAdapterPosition", 0);

        Bundle b = new Bundle();
        if (mode != null) {
            if (mode.equals("id_surat")) {
                b.putInt("id_surat", sharedPref.getInt("suratke", 0));
                b.putString("judul", sharedPref.getString("judul", null));
            } else {
                b.putInt("id_juz", sharedPref.getInt("juzke", 0));
            }
            b.putInt("bindingAdapterPosition", bindingAdapterPosition);
        } else {
            b.putInt("id_surat", 1);
            b.putString("judul", "Al-Fatihah");
        }

        navController.setGraph(R.navigation.mobile_navigation, b);

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                navController.getGraph())
                .setOpenableLayout(drawer)
                .build();
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);

        AppCompatImageButton wa = binding.navView.getHeaderView(0).findViewById(R.id.wa);
        wa.setOnClickListener(v -> {
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

        AppCompatImageButton gmail = binding.navView.getHeaderView(0).findViewById(R.id.gmail);
        gmail.setOnClickListener(v -> {
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

        AppCompatImageButton peta = binding.navView.getHeaderView(0).findViewById(R.id.maps);
        peta.setOnClickListener(v -> {
            Uri gmmIntentUri = Uri.parse("geo:0,0?q=rumah@7.39588,110.828549");
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            startActivity(mapIntent);
        });

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
            Util.segarkanJadwal(this, false, false);
            ingatkanAlarmTepat();
            return;
        }

        String[] izin = Izin.izinAwal(this);
        boolean alarm = Izin.alarmTepat(this);
        if (izin.length == 0 && alarm) {
            // pemasangan lama yang izinnya sudah lengkap: tidak ada yang perlu dijelaskan
            Izin.tandaiSudahTanya(this);
            Util.segarkanJadwal(this, false, false);
            return;
        }

        List<String> kurang = Arrays.asList(izin);
        StringBuilder isi = new StringBuilder("Supaya adzan berbunyi tepat waktu dan sesuai tempat Anda, aplikasi ini memerlukan:\n");
        if (kurang.contains(Manifest.permission.POST_NOTIFICATIONS)) {
            isi.append("\n\u2022 Notifikasi \u2014 menampilkan adzan beserta tombol Matikan.");
        }
        if (kurang.contains(Manifest.permission.ACCESS_FINE_LOCATION)) {
            isi.append("\n\u2022 Lokasi saat aplikasi dipakai \u2014 menghitung jadwal salat dan arah kiblat. ")
                    .append("Pilih lokasi tepat: lokasi perkiraan tidak membawa ketinggian.");
        }
        if (!alarm) {
            isi.append("\n\u2022 Alarm & pengingat \u2014 membunyikan adzan tepat pada waktunya. ")
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
                    Util.segarkanJadwal(this, false, false);
                })
                .show();
    }

    private void sesudahIzinAwal() {
        Util.segarkanJadwal(this, false, false);
        // Alarm tepat waktu dan layar penuh tidak bisa diminta lewat dialog, hanya lewat
        // layar setelan. Dulu ketiga layar setelan dibuka bertumpuk sekaligus; sekarang
        // daftar izinnya yang menjelaskan masing-masing dan membawa ke sana satu per satu.
        if (!Izin.alarmTepat(this) || !Izin.layarPenuh(this)) {
            bukaIzinAplikasi();
        }
    }

    // satu-satunya pengingat yang muncul lagi: adzan yang dinyalakan tapi tidak bisa dijadwalkan
    private void ingatkanAlarmTepat() {
        if (Izin.alarmTepat(this) || !Util.adaAdzanAktif(sharedPref)) {
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.getItem(0).setChecked(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        NavigationView navView = binding.navView;
        DrawerLayout drawerLayout = binding.drawerLayout;
        Menu daftarMenu = navView.getMenu();

        if (R.id.surat == id) {
            daftarMenu.removeGroup(2);
            JSONArray daftarSurat = daftarSurat(getApplicationContext());
            int jumlahSurat = daftarSurat.length();
            for (short i = 0; i < jumlahSurat; i++) {
                try {
                    JSONObject jo = daftarSurat.getJSONObject(i);
                    daftarMenu.add(1, jo.getInt("id"), Menu.NONE, jo.getString("surat_name") + " (" + jo.getString("surat_text") + ")");
                } catch (JSONException ignored) {
                }
            }
            navView.invalidate();

            drawerLayout.openDrawer(GravityCompat.START);
            return true;
        }

        if (R.id.juz == id) {
            daftarMenu.removeGroup(1);
            for (short i = 1; i <= 30; i++) {
                daftarMenu.add(2, i, Menu.NONE, "Juz " + numberFormat.format(i));
            }
            navView.invalidate();

            drawerLayout.openDrawer(GravityCompat.START);
            return true;
        }

        if (R.id.kiblat == id) {
            NavController navController = Navigation.findNavController(this, R.id.bagian_isi_utama);
            navController.popBackStack(R.id.nav_kiblat, true);
            navController.navigate(R.id.nav_kiblat);
            return true;
        }

        if (R.id.terbit == id) {
            NavController navController = Navigation.findNavController(this, R.id.bagian_isi_utama);
            navController.popBackStack(R.id.nav_terbit, true);
            navController.navigate(R.id.nav_terbit);
            return true;
        }

        if (R.id.tenggelam == id) {
            NavController navController = Navigation.findNavController(this, R.id.bagian_isi_utama);
            navController.popBackStack(R.id.nav_tenggelam, true);
            navController.navigate(R.id.nav_tenggelam);
            return true;
        }

        if (R.id.pengaturan_adzan == id) {
            new PengaturanAdzan().show(getSupportFragmentManager(), PengaturanAdzan.TAG);
            return true;
        }

        if (R.id.izin_aplikasi == id) {
            bukaIzinAplikasi();
            return true;
        }

        if (R.id.ketepatan_kompas == id) {
            Util.peringatanKompas(this, -1);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.bagian_isi_utama);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
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

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id_item = item.getItemId();
        String judul = Objects.requireNonNull(item.getTitle()).toString();
        int id_grup = item.getGroupId();

        NavController navController = Navigation.findNavController(this, R.id.bagian_isi_utama);
        navController.popBackStack(R.id.nav_ayat, true); //hapus dulu sebelum navigasi ke fragment sama

        Bundle b = new Bundle();
        switch (id_grup) {
            case 1:
                b.putInt("id_surat", id_item);
                b.putString("judul", judul);
                break;
            case 2:
                b.putInt("id_juz", id_item);
                break;
        }
        navController.navigate(R.id.nav_ayat, b);

        DrawerLayout drawer = binding.drawerLayout;
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }
        return true;
    }
}
