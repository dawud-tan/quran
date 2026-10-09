package com.quran.kiblat.salat.ui.kiblat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textview.MaterialTextView;
import com.quran.kiblat.salat.Geodesic;
import com.quran.kiblat.salat.Izin;
import com.quran.kiblat.salat.Util;
import com.quran.kiblat.salat.databinding.FragmenKiblatBinding;
import com.quran.kiblat.salat.ui.util.BayanganKiblat;
import com.quran.kiblat.salat.ui.util.MedanMagnetBumi;
import com.quran.kiblat.salat.ui.util.PrayTime;
import com.quran.kiblat.salat.ui.util.SolarPosition;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;

public class FragmenKiblat extends Fragment {

    Location lokasiku, mKabaLocation;
    // raw inputs from Android sensors
    float[] m_NormGravityVector;    // Normalised gravity vector, (i.e. length of this vector is 1), which points straight up into space
    float[] m_NormMagFieldValues;   // Normalised magnetic field vector, (i.e. length of this vector is 1)
    boolean m_OrientationOK;        // set true if m_azimuth_radians and m_pitch_radians have successfully been calculated following a call to onSensorChanged(...)
    double azimutSumbuMinZ;
    /// Penanda supaya peringatan akurasi tidak muncul berulang-ulang.
    private boolean sudahIngatkanAkurasi;
    /// Tanggal + posisi yang bayangan kiblatnya sudah dihitung, supaya tiap
    /// pembaruan GPS kecil tidak memicu pemindaian sehari penuh lagi.
    private String kunciBayangan;
    private FragmenKiblatBinding binding;
    private ShapeableImageView kabaView, cardinalPointsView;
    private final SensorEventListener mSensorEventListener = new SensorEventListener() {
        // values calculated once gravity and magnetic field vectors are available
        final float[] m_NormEastVector;       // normalised cross product of raw gravity vector with magnetic field values, points east
        final float[] m_NormNorthVector;      // Normalised vector pointing to magnetic north
        // raw inputs from Android sensors
        float m_Norm_Gravity;           // length of raw gravity vector received in onSensorChanged(...).  NB: should be about 10
        float[] m_NormGravityVector;    // Normalised gravity vector, (i.e. length of this vector is 1), which points straight up into space
        float m_Norm_MagField;          // length of raw magnetic field vector received in onSensorChanged(...).
        float m_azimuth_radians;        // angle of the device from magnetic north

        {
            m_NormEastVector = new float[3];
            m_NormNorthVector = new float[3];
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            if (sensor == null || sensor.getType() != Sensor.TYPE_MAGNETIC_FIELD) {
                return;
            }
            boolean meragukan = accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE
                    || accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW;
            // Sekali saja per kunjungan: onAccuracyChanged bisa dipanggil
            // berkali-kali selama sensornya masih goyah.
            if (meragukan && !sudahIngatkanAkurasi && isResumed() && binding != null) {
                sudahIngatkanAkurasi = true;
                Util.peringatanKompas(requireContext(), accuracy);
            }
        }

        //caranya Paul Doust https://stackoverflow.com/questions/16317599/android-compass-that-can-compensate-for-tilt-and-pitch/16386066#16386066
        @Override
        public void onSensorChanged(SensorEvent event) {

            int SensorType = event.sensor.getType();
            switch (SensorType) {
                case Sensor.TYPE_GRAVITY:
                    if (m_NormGravityVector == null) m_NormGravityVector = new float[3];
                    System.arraycopy(event.values, 0, m_NormGravityVector, 0, m_NormGravityVector.length);
                    m_Norm_Gravity = (float) Math.sqrt(m_NormGravityVector[0] * m_NormGravityVector[0] + m_NormGravityVector[1] * m_NormGravityVector[1] + m_NormGravityVector[2] * m_NormGravityVector[2]);
                    for (int i = 0; i < m_NormGravityVector.length; i++)
                        m_NormGravityVector[i] /= m_Norm_Gravity;
                    break;
                case Sensor.TYPE_MAGNETIC_FIELD:
                    if (m_NormMagFieldValues == null) m_NormMagFieldValues = new float[3];
                    System.arraycopy(event.values, 0, m_NormMagFieldValues, 0, m_NormMagFieldValues.length);
                    m_Norm_MagField = (float) Math.sqrt(m_NormMagFieldValues[0] * m_NormMagFieldValues[0] + m_NormMagFieldValues[1] * m_NormMagFieldValues[1] + m_NormMagFieldValues[2] * m_NormMagFieldValues[2]);
                    teksMikrotesla.setText(String.format(Locale.getDefault(), "%.2f µT", m_Norm_MagField));
                    for (int i = 0; i < m_NormMagFieldValues.length; i++)
                        m_NormMagFieldValues[i] /= m_Norm_MagField;
                    break;
            }
            if (m_NormGravityVector != null && m_NormMagFieldValues != null) {

                // first calculate the horizontal vector that points due east
                float East_x = m_NormMagFieldValues[1] * m_NormGravityVector[2] - m_NormMagFieldValues[2] * m_NormGravityVector[1];
                float East_y = m_NormMagFieldValues[2] * m_NormGravityVector[0] - m_NormMagFieldValues[0] * m_NormGravityVector[2];
                float East_z = m_NormMagFieldValues[0] * m_NormGravityVector[1] - m_NormMagFieldValues[1] * m_NormGravityVector[0];
                float norm_East = (float) Math.sqrt(East_x * East_x + East_y * East_y + East_z * East_z);
                if (m_Norm_Gravity * m_Norm_MagField * norm_East < 0.1f) {  // Typical values are  > 100.
                    m_OrientationOK = false; // device is close to free fall (or in space?), or close to magnetic north pole.
                } else {
                    m_NormEastVector[0] = East_x / norm_East;
                    m_NormEastVector[1] = East_y / norm_East;
                    m_NormEastVector[2] = East_z / norm_East;

                    // next calculate the horizontal vector that points due north
                    float M_dot_G = (m_NormGravityVector[0] * m_NormMagFieldValues[0] + m_NormGravityVector[1] * m_NormMagFieldValues[1] + m_NormGravityVector[2] * m_NormMagFieldValues[2]);
                    float North_x = m_NormMagFieldValues[0] - m_NormGravityVector[0] * M_dot_G;
                    float North_y = m_NormMagFieldValues[1] - m_NormGravityVector[1] * M_dot_G;
                    float North_z = m_NormMagFieldValues[2] - m_NormGravityVector[2] * M_dot_G;
                    float norm_North = (float) Math.sqrt(North_x * North_x + North_y * North_y + North_z * North_z);
                    m_NormNorthVector[0] = North_x / norm_North;
                    m_NormNorthVector[1] = North_y / norm_North;
                    m_NormNorthVector[2] = North_z / norm_North;

                    // take account of screen rotation away from its natural rotation
                    Display display = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                        display = requireContext().getDisplay();
                    else {
                        display = requireActivity().getWindowManager().getDefaultDisplay();
                    }
                    int rotation = display.getRotation();
                    float screen_adjustment = switch (rotation) {
                        case Surface.ROTATION_0 -> 0;
                        case Surface.ROTATION_90 -> (float) Math.PI / 2;
                        case Surface.ROTATION_180 -> (float) Math.PI;
                        case Surface.ROTATION_270 -> 3 * (float) Math.PI / 2;
                        default -> 0;
                    };
                    // NB: the rotation matrix has now effectively been calculated. It consists of the three vectors m_NormEastVector[], m_NormNorthVector[] and m_NormGravityVector[]

                    // calculate all the required angles from the rotation matrix
                    // NB: see https://math.stackexchange.com/questions/381649/whats-the-best-3d-angular-co-ordinate-system-for-working-with-smartfone-apps
                    float sin = m_NormEastVector[1] - m_NormNorthVector[0], cos = m_NormEastVector[0] + m_NormNorthVector[1];
                    m_azimuth_radians = (float) (sin != 0 && cos != 0 ? Math.atan2(sin, cos) : 0);
                    m_azimuth_radians += screen_adjustment;
                    m_OrientationOK = true;

                    azimutSumbuMinZ = Math.toDegrees(m_azimuth_radians);
                    if (lokasiku != null && mKabaLocation != null) {
                        MedanMagnetBumi geoField = new MedanMagnetBumi(
                                Double.valueOf(lokasiku.getLatitude()).floatValue(),
                                Double.valueOf(lokasiku.getLongitude()).floatValue(),
                                Double.valueOf(lokasiku.getAltitude()).floatValue(),
                                System.currentTimeMillis());

                        azimutSumbuMinZ = azimutSumbuMinZ - geoField.getDeclination();

                        double kutubUtara = -azimutSumbuMinZ;
                        cardinalPointsView.setRotation(Double.valueOf(kutubUtara).floatValue());

                        if (LocationCompat.hasMslAltitude(lokasiku)) {
                            lokasiku.setAltitude(LocationCompat.getMslAltitudeMeters(lokasiku));
                        }
                        double sudutAzimut = Geodesic.determineIndonesianQiblaDirection(lokasiku.getLatitude(), lokasiku.getLongitude());
                        sudutAzimut = (sudutAzimut < 0) ? sudutAzimut + 360 : sudutAzimut;
                        teksSudutAzimut.setText(String.format(Locale.getDefault(), "%.2f°", sudutAzimut));

                        double sudutBaru = sudutAzimut - azimutSumbuMinZ;
                        kabaView.setRotation(Double.valueOf(sudutBaru).floatValue());
                    }
                }
            }
        }
    };
    private MaterialTextView akurasiGps, mdpl, teksSudutAzimut, teksMikrotesla,
            sholatSubuh, sholatDzuhur, sholatAshar, sholatMaghrib, sholatIsya;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private SensorManager mSensorManager = null;
    private Sensor mGravity;
    private Sensor mMagnetometer;

    /**
     * Spanduk sekali-pakai gantinya dialog yang dulu menyembul dari onResume.
     * Dialog yang muncul sebelum penggunanya sempat melihat layar hampir pasti
     * ditutup tanpa dibaca; spanduk menunggu sampai dia sendiri yang tertarik.
     */
    private void aturSpandukKompas() {
        SharedPreferences sharedPref = requireContext()
                .getSharedPreferences("pref", Context.MODE_PRIVATE);
        if (!Util.perluSpandukKompas(sharedPref)) {
            binding.spandukKompas.spandukKompas.setVisibility(View.GONE);
            return;
        }
        binding.spandukKompas.spandukKompas.setVisibility(View.VISIBLE);

        binding.spandukKompas.spandukPesan.setOnClickListener(v -> {
            Util.tandaiSpandukKompas(sharedPref);
            binding.spandukKompas.spandukKompas.setVisibility(View.GONE);
            Util.peringatanKompas(requireContext(), -1);
        });
        binding.spandukKompas.spandukTutup.setOnClickListener(v -> {
            Util.tandaiSpandukKompas(sharedPref);
            binding.spandukKompas.spandukKompas.setVisibility(View.GONE);
        });
    }

    /**
     * Mencari saat bayangan benda tegak jatuh tepat di garis kiblat hari ini.
     * <p>
     * Dipindai di utas latar: satu hari berarti ribuan hitungan SPA, dan kalau
     * hari ini kosong pencariannya maju sampai sebulan ke depan.
     */
    private void aturBayanganKiblat(Location lokasi) {
        double azimut = Geodesic.determineIndonesianQiblaDirection(
                lokasi.getLatitude(), lokasi.getLongitude());
        if (Double.isNaN(azimut)) {
            binding.bayanganKiblat.setText("-");
            kunciBayangan = null;
            return;
        }
        if (azimut < 0) {
            azimut += 360;
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
        final double azimutKiblat = azimut;
        binding.bayanganKiblat.setText("menghitung...");

        Util.diLatar(
                () -> BayanganKiblat.cariBerikutnya(lat, lon, elevasi, hariIni, zona,
                        azimutKiblat, 30),
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
                            "%s%s (%s, matahari %.0f\u00b0)", hari, jam, arah, saat.tinggi));
                });
    }

    private void aturJadwal(Location lokasiku) {
        aturBayanganKiblat(lokasiku);

        SharedPreferences sharedPref = requireContext().getSharedPreferences("pref", Context.MODE_PRIVATE);
        //dirakit di sini, bukan di onCreateView, supaya ihtiyati dan cara hitung Ashar yang
        //baru diubah lewat Pengaturan Adzan langsung terpakai saat fragmen ini kembali tampil
        PrayTime prayers = Util.penghitungJadwal(sharedPref);

        ZoneId z = ZoneId.systemDefault();
        ZonedDateTime sekarang = ZonedDateTime.now(z);
        binding.hari.setText(Util.tanggalWeton(
                        sekarang.getYear(),
                        sekarang.getMonth(),
                        sekarang.getDayOfMonth())
                .format(sekarang));

        SolarPosition.SPAData spa = new SolarPosition.SPAData();

        spa.year = sekarang.getYear();
        spa.month = sekarang.getMonthValue();
        spa.day = sekarang.getDayOfMonth();
        spa.hour = sekarang.getHour();
        spa.minute = sekarang.getMinute();
        spa.second = sekarang.getSecond();

        spa.timezone = sekarang.getOffset().getTotalSeconds() / 3600.0;

        spa.longitude = lokasiku.getLongitude();
        spa.latitude = lokasiku.getLatitude();

        spa.delta_ut1 = 0;
        spa.delta_t = SolarPosition.DELTA_T;
        spa.elevation = lokasiku.getAltitude();
        spa.pressure = 1000;
        spa.temperature = 27.7;
        spa.slope = 0;
        spa.azm_rotation = 180;
        spa.atmos_refract = 0.5667;
        spa.function = SolarPosition.SPA.SPA_ZA_RTS;

        ArrayList<ZonedDateTime> prayerTimes = prayers.getDatePrayerTimes(spa);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("kk:mm:ss z");

        MaterialTextView[] tampilan = {sholatSubuh, sholatDzuhur, sholatAshar, sholatMaghrib, sholatIsya};

        //di lintang tinggi matahari bisa tidak terbit atau tidak terbenam seharian, dan SPA
        //mengisi -99999 sebagai penanda, yang kalau diteruskan jadi jam ngawur
        if (!SolarPosition.rtsSah(spa)) {
            for (MaterialTextView satu : tampilan) {
                satu.setText("-");
            }
            return;
        }

        sholatSubuh.setText(prayerTimes.get(0).format(dtf));
        sholatDzuhur.setText(prayerTimes.get(2).format(dtf));
        sholatAshar.setText(prayerTimes.get(3).format(dtf));
        sholatMaghrib.setText(prayerTimes.get(5).format(dtf));
        sholatIsya.setText(prayerTimes.get(6).format(dtf));

        //waktunya tetap ditampilkan, hanya dicoret kalau adzannya dimatikan lewat Pengaturan Adzan
        for (int i = 0; i < tampilan.length; i++) {
            tandaiAdzan(tampilan[i], Util.adzanAktif(sharedPref, i));
        }
    }

    private void tandaiAdzan(MaterialTextView tampilan, boolean aktif) {
        tampilan.setAlpha(aktif ? 1f : 0.4f);
        tampilan.setPaintFlags(aktif
                ? tampilan.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG
                : tampilan.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
    }

    @SuppressLint("MissingPermission")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmenKiblatBinding.inflate(inflater, container, false);

        akurasiGps = binding.akurasiGps;
        mdpl = binding.mdpl;
        teksSudutAzimut = binding.sudutAzimut;
        teksMikrotesla = binding.mikrotesla;

        sholatSubuh = binding.sholatSubuh;
        sholatDzuhur = binding.sholatDzuhur;
        sholatAshar = binding.sholatAshar;
        sholatMaghrib = binding.sholatMaghrib;
        sholatIsya = binding.sholatIsya;

        kabaView = binding.gambar;
        cardinalPointsView = binding.gambar2;

        aturSpandukKompas();

        lokasiku = new Location("lokasiku");
        lokasiku.setLatitude(-2.548925);
        lokasiku.setLongitude(118.014864);
        lokasiku.setAltitude(0);
        lokasiku.setAccuracy(1000000);
        binding.koordinat.setText(
                Util.getLatitudeAsDMS(lokasiku, 9) + ", " + Util.getLongitudeAsDMS(lokasiku, 9)
        );
        akurasiGps.setText(String.format(Locale.getDefault(), "%.2f meter", lokasiku.getAccuracy()));
        if (LocationCompat.hasMslAltitude(lokasiku)) {
            lokasiku.setAltitude(LocationCompat.getMslAltitudeMeters(lokasiku));
        }
        mdpl.setText(String.format(Locale.getDefault(), "%.2f meter", lokasiku.getAltitude()));

        aturJadwal(lokasiku);

        mKabaLocation = new Location("Kaba");
        mKabaLocation.setLatitude(21.42252407d);
        mKabaLocation.setLongitude(39.8261822d);
        mKabaLocation.setAltitude(289.49323057d);//tinggi kabah MDPL (meter di atas permukaan laut)
        mKabaLocation.setAccuracy(0.1f);

        m_NormGravityVector = m_NormMagFieldValues = null;
        m_OrientationOK = false;
        SharedPreferences sharedPref = requireContext().getSharedPreferences("pref", Context.MODE_PRIVATE);

        locationListener = location -> {
            lokasiku = location;
            Util.simpanLokasi(sharedPref, lokasiku);
            binding.koordinat.setText(
                    Util.getLatitudeAsDMS(lokasiku, 9) + ", " + Util.getLongitudeAsDMS(lokasiku, 9)
            );
            if (LocationCompat.hasMslAltitude(lokasiku)) {
                lokasiku.setAltitude(LocationCompat.getMslAltitudeMeters(lokasiku));
            }
            mdpl.setText(String.format(Locale.getDefault(), "%.2f meter", lokasiku.getAltitude()));
            akurasiGps.setText(String.format(Locale.getDefault(), "%.2f meter", lokasiku.getAccuracy()));
            aturJadwal(lokasiku);
        };

        locationManager = (LocationManager) requireActivity().getSystemService(Context.LOCATION_SERVICE);

        // Lewat Izin: dulu getLastKnownLocation dipanggil tanpa memeriksa izin, jadi
        // layar ini mogok (SecurityException) kalau izin lokasi ditolak. Disimpan dulu
        // lalu dibaca balik, supaya ketinggian yang tidak dibawa fix jaringan/perkiraan
        // terisi dari simpanan, sama dengan yang dipakai jadwal yang dibunyikan.
        Location smntra = Izin.lokasiTerkini(requireContext(), false);
        if (smntra != null) {
            Util.simpanLokasi(sharedPref, smntra);
        }
        lokasiku = Util.lokasiTerakhir(sharedPref);
        binding.koordinat.setText(
                Util.getLatitudeAsDMS(lokasiku, 9) + ", " + Util.getLongitudeAsDMS(lokasiku, 9)
        );
        akurasiGps.setText(String.format(Locale.getDefault(), "%.2f meter", lokasiku.getAccuracy()));
        if (LocationCompat.hasMslAltitude(lokasiku)) {
            lokasiku.setAltitude(LocationCompat.getMslAltitudeMeters(lokasiku));
        }
        mdpl.setText(String.format(Locale.getDefault(), "%.2f meter", lokasiku.getAltitude()));
        aturJadwal(lokasiku);
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                && Util.isAirplaneModeOff(requireContext())) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 600000, 1, locationListener);
        }
        return binding.getRoot();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onResume() {
        super.onResume();

        //kembali dari dialog Pengaturan Adzan: coretan waktu yang dimatikan ikut diperbarui
        if (binding != null && lokasiku != null) {
            aturJadwal(lokasiku);
        }

        sudahIngatkanAkurasi = false;

        mSensorManager = (SensorManager) requireActivity().getSystemService(Context.SENSOR_SERVICE);

        this.mGravity = this.mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        if (mGravity != null) {
            mSensorManager.registerListener(mSensorEventListener, this.mGravity, SensorManager.SENSOR_DELAY_NORMAL);
        }
        this.mMagnetometer = this.mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (mMagnetometer != null) {
            mSensorManager.registerListener(mSensorEventListener, this.mMagnetometer, SensorManager.SENSOR_DELAY_NORMAL);
        }

        m_NormGravityVector = new float[3];
        m_NormMagFieldValues = new float[3];
        m_OrientationOK = false;

    }

    @SuppressLint("MissingPermission")
    @Override
    public void onPause() {
        m_NormGravityVector = m_NormMagFieldValues = null;
        m_OrientationOK = false;
        this.mSensorManager.unregisterListener(this.mSensorEventListener, this.mGravity);
        this.mSensorManager.unregisterListener(this.mSensorEventListener, this.mMagnetometer);

        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        locationManager.removeUpdates(locationListener);
    }

}