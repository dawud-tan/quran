package com.quran.kiblat.salat.ui.tenggelam;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textview.MaterialTextView;
import com.quran.kiblat.salat.Izin;
import com.quran.kiblat.salat.Util;
import com.quran.kiblat.salat.databinding.FragmenTenggelamBinding;
import com.quran.kiblat.salat.ui.util.MedanMagnetBumi;
import com.quran.kiblat.salat.ui.util.SolarPosition;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

public class FragmenTenggelam extends Fragment {

    private final Handler handler = new Handler(Looper.getMainLooper());
    Location lokasiku;
    ActivityResultLauncher<String> activityResultLauncher;
    DateTimeFormatter dtf = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM);
    // raw inputs from Android sensors
    float[] m_NormGravityVector;    // Normalised gravity vector, (i.e. length of this vector is 1), which points straight up into space
    float[] m_NormMagFieldValues;   // Normalised magnetic field vector, (i.e. length of this vector is 1)
    boolean m_OrientationOK;        // set true if m_azimuth_radians and m_pitch_radians have successfully been calculated following a call to onSensorChanged(...)
    double azimutSumbuMinZ;
    /// Penanda supaya peringatan akurasi tidak muncul berulang-ulang.
    private boolean sudahIngatkanAkurasi;
    private FragmenTenggelamBinding binding;
    private ShapeableImageView arahTenggelam, cardinalPointsView;
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
            // berkali-kali selama sensornya masih goyah. Layar ini memakai
            // magnetometer yang sama dengan layar kiblat, jadi masalahnya sama,
            // cuma patokan jaraknya yang beda — karena itu kiblat = false.
            if (meragukan && !sudahIngatkanAkurasi && isResumed() && binding != null) {
                sudahIngatkanAkurasi = true;
                Util.peringatanKompas(requireContext(), accuracy, false);
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
                    if (lokasiku != null) {

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
                        double sudutAzimut = arahTenggelam(lokasiku);
                        teksSudutAzimut.setText(String.format(Locale.getDefault(), "%.2f°", sudutAzimut));

                        double sudutBaru = sudutAzimut - azimutSumbuMinZ;
                        arahTenggelam.setRotation(Double.valueOf(sudutBaru).floatValue());
                    }
                }
            }
        }
    };
    private MaterialTextView akurasiGps, mdpl, teksSudutAzimut, teksMikrotesla, waktuTenggelam;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private SensorManager mSensorManager = null;
    private Sensor mGravity;
    private Sensor mMagnetometer;

    @SuppressLint("MissingPermission")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmenTenggelamBinding.inflate(inflater, container, false);

        akurasiGps = binding.akurasiGps;
        mdpl = binding.mdpl;
        teksSudutAzimut = binding.sudutAzimut;
        teksMikrotesla = binding.mikrotesla;
        waktuTenggelam = binding.waktuTenggelam;
        arahTenggelam = binding.gambar;
        cardinalPointsView = binding.gambar2;

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

        m_NormGravityVector = m_NormMagFieldValues = null;
        m_OrientationOK = false;
        SharedPreferences sharedPref = requireContext().getSharedPreferences("pref", Context.MODE_PRIVATE);

        locationListener = location -> {
            lokasiku = location;
            Util.simpanLokasi(sharedPref, lokasiku);
            binding.koordinat.setText(
                    Util.getLatitudeAsDMS(lokasiku, 9) + ", " + Util.getLongitudeAsDMS(lokasiku, 9)
            );
            akurasiGps.setText(String.format(Locale.getDefault(), "%.2f meter", lokasiku.getAccuracy()));
            if (LocationCompat.hasMslAltitude(lokasiku)) {
                lokasiku.setAltitude(LocationCompat.getMslAltitudeMeters(lokasiku));
            }
            mdpl.setText(String.format(Locale.getDefault(), "%.2f meter", lokasiku.getAltitude()));
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
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                && Util.isAirplaneModeOff(requireContext())) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 600000, 1, locationListener);
        }

        return binding.getRoot();
    }

    private double arahTenggelam(Location lok) {
        SolarPosition.SPAData spa = new SolarPosition.SPAData();
        ZoneId z = ZoneId.systemDefault();
        ZonedDateTime sekarang = ZonedDateTime.now(z);
        binding.hari.setText(Util.tanggalWeton(
                        sekarang.getYear(),
                        sekarang.getMonth(),
                        sekarang.getDayOfMonth())
                .format(sekarang));

        spa.year = sekarang.getYear();
        spa.month = sekarang.getMonthValue();
        spa.day = sekarang.getDayOfMonth();
        spa.hour = sekarang.getHour();
        spa.minute = sekarang.getMinute();
        spa.second = sekarang.getSecond();

        spa.timezone = sekarang.getOffset().getTotalSeconds() / 3600.0;

        spa.longitude = lok.getLongitude();
        spa.latitude = lok.getLatitude();

        spa.delta_ut1 = 0;
        spa.delta_t = SolarPosition.DELTA_T;
        spa.elevation = lok.getAltitude();
        spa.pressure = 1000;
        spa.temperature = 27.7;
        spa.slope = 0;
        spa.azm_rotation = 180;
        spa.atmos_refract = 0.5667;
        spa.function = SolarPosition.SPA.SPA_ALL;

        SolarPosition.spaCalculate(spa);

        //di lintang tinggi matahari bisa tidak terbit atau tidak terbenam seharian, dan SPA
        //mengisi -99999 sebagai penanda. Nilai itu akan ditolak LocalTime.of(), jadi dihadang
        //di sini dan arah lama dibiarkan apa adanya
        if (!SolarPosition.rtsSah(spa)) {
            waktuTenggelam.setText("-");
            return 0;
        }

        double minSet = 60.0 * new BigDecimal(spa.sunset).subtract(new BigDecimal((int) spa.sunset)).floatValue();
        double secSet = 60.0 * new BigDecimal(minSet).subtract(new BigDecimal((int) minSet)).floatValue();

        spa.hour = Double.valueOf(spa.sunset).intValue();
        spa.minute = Double.valueOf(minSet).intValue();
        spa.second = secSet;

        LocalTime localTime = LocalTime.of(spa.hour, spa.minute, Double.valueOf(spa.second).intValue());
        waktuTenggelam.setText(dtf.format(localTime));

        SolarPosition.spaCalculate(spa);

        return spa.azimuth;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onResume() {
        super.onResume();
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