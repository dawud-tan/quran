package com.quran.kiblat.salat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
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

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.quran.kiblat.salat.databinding.AktivitasUtamaBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

public class AktivitasUtama extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private AppBarConfiguration mAppBarConfiguration;
    private AktivitasUtamaBinding binding;
    private NumberFormat numberFormat;
    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = AktivitasUtamaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        CoordinatorLayout koordinator = binding.koordinator;
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

        LocationManager lm = gpsHidup();

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        ActivityResultLauncher<String> activityResultBgLocPerm =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> mulaiJadwal(lm));
        NotificationManager managerCompat = getSystemService(NotificationManager.class);
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {//api 34 ke atas
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || !alarmManager.canScheduleExactAlarms()
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    || !managerCompat.canUseFullScreenIntent()
                    || !powerManager.isIgnoringBatteryOptimizations(getPackageName())
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                ActivityResultLauncher<String[]> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), results -> {
                    if (!alarmManager.canScheduleExactAlarms()) {
                        Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    }

                    if (!managerCompat.canUseFullScreenIntent()) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    }

                    if (!powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
                        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    }

                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        mintaLokasiBG(koordinator, activityResultBgLocPerm);
                    }
                });

                activityResultLauncher.launch(new String[]{
                        Manifest.permission.POST_NOTIFICATIONS,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                });
            } else {
                mulaiJadwal(lm);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {//api 33 ke atas
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || !alarmManager.canScheduleExactAlarms()
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    || !powerManager.isIgnoringBatteryOptimizations(getPackageName())
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityResultLauncher<String[]> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), results -> {

                    if (!alarmManager.canScheduleExactAlarms()) {
                        Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    }

                    if (!powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
                        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    }

                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        mintaLokasiBG(koordinator, activityResultBgLocPerm);
                    }
                });

                activityResultLauncher.launch(new String[]{
                        Manifest.permission.POST_NOTIFICATIONS,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                });
            } else {
                mulaiJadwal(lm);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { //api 31 ke atas
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || !alarmManager.canScheduleExactAlarms()
                    || !powerManager.isIgnoringBatteryOptimizations(getPackageName())
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityResultLauncher<String[]> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), results -> {
                    if (!powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
                        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    }

                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        mintaLokasiBG(koordinator, activityResultBgLocPerm);
                    }
                });

                activityResultLauncher.launch(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                });
            } else {
                mulaiJadwal(lm);
            }
        } else { //dari Red Velvet ke bawah
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || !powerManager.isIgnoringBatteryOptimizations(getPackageName())
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityResultLauncher<String[]> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), results -> {
                    if (!powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
                        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    }

                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        mintaLokasiBG(koordinator, activityResultBgLocPerm);
                    }
                });

                activityResultLauncher.launch(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                });
            } else {
                mulaiJadwal(lm);
            }
        }

        setVolumeControlStream(AudioManager.STREAM_ALARM);
    }

    private void mintaLokasiBG(CoordinatorLayout koordinator, ActivityResultLauncher<String> activityResultBgLocPerm) {
        Snackbar.make(koordinator, "Selalu izinkan akses lokasi, agar waktu adzan tepat lokasi", Snackbar.LENGTH_INDEFINITE).setTextMaxLines(2).setAction(
                "Buka Pengaturan", v -> activityResultBgLocPerm.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)).show();
    }

    private void mulaiJadwal(LocationManager locationManager) {
        Location lokasi = Util.lokasiTerakhir(sharedPref);

        Location smntra = lokasiTerakhirDari(locationManager, LocationManager.GPS_PROVIDER);
        if (smntra == null) {
            smntra = lokasiTerakhirDari(locationManager, LocationManager.NETWORK_PROVIDER);
        }
        //kalau dua-duanya kosong (mis. baru dipasang, GPS belum pernah dapat sinyal)
        //dipakai lokasi simpanan, jangan sampai null-nya diteruskan dan bikin mogok
        if (smntra != null) {
            lokasi = smntra;
            Util.simpanLokasi(sharedPref, lokasi);
        }

        Util.cekJadwal(this, lokasi, false);
    }

    @SuppressLint("MissingPermission")
    private Location lokasiTerakhirDari(LocationManager locationManager, String penyedia) {
        try {
            return locationManager.isProviderEnabled(penyedia) ?
                    locationManager.getLastKnownLocation(penyedia) : null;
        } catch (Exception ex) {
            return null; //penyedia tidak ada di perangkat ini
        }
    }

    private LocationManager gpsHidup() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
        }

        if (!gps_enabled) {
            new AlertDialog.Builder(this)
                    .setMessage("GPS tidak aktif")
                    .setPositiveButton("Buka Pengaturan Lokasi", (paramDialogInterface, paramInt) -> startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                    .setNegativeButton("Batal", (dialog, which) -> {

                    })
                    .show();
        }
        return lm;
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
