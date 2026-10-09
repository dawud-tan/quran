package com.quran.kiblat.salat.ui.kompas;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.view.Display;
import android.view.Surface;

/**
 * Kompas dengan kompensasi kemiringan, dari sensor gravitasi dan magnet.
 * <p>
 * Caranya Paul Doust:
 * https://stackoverflow.com/questions/16317599/android-compass-that-can-compensate-for-tilt-and-pitch/16386066#16386066
 * Vektor timur = medan magnet × gravitasi, vektor utara = medan magnet
 * dikurangi proyeksinya ke gravitasi; ketiganya membentuk matriks rotasi.
 * <p>
 * Dulu disalin utuh di tiga layar (kiblat, terbit, tenggelam). Hasilnya
 * azimut <b>magnetik</b>; koreksi deklinasi ke utara sejati dikerjakan
 * {@link FragmenKompas}, karena butuh lokasi.
 */
public final class Kompas implements SensorEventListener {

    public interface Pendengar {
        /**
         * Kuat medan magnet, mikrotesla. Medan bumi di Indonesia sekitar
         * 40–45 µT; jauh di atas itu berarti ada besi atau magnet di dekat ponsel.
         */
        void medanMagnet(float mikrotesla);

        /**
         * Azimut sumbu −Z layar (arah atas layar) dari utara magnetik, derajat.
         */
        void arahMagnetik(double azimut);

        /**
         * Magnetometer melaporkan ketelitian rendah atau tidak bisa dipercaya.
         */
        void akurasiRendah(int akurasi);
    }

    private final Activity aktivitas;
    private final Pendengar pendengar;
    private final SensorManager sensorManager;
    private final Sensor gravitasi;
    private final Sensor magnetometer;

    private final float[] gravitasiNormal = new float[3];    // panjang 1, menunjuk ke atas
    private final float[] medanNormal = new float[3];        // panjang 1
    private final float[] timurNormal = new float[3];
    private final float[] utaraNormal = new float[3];
    private float panjangGravitasi;  // sekitar 9,8
    private float panjangMedan;      // mikrotesla
    private boolean adaGravitasi, adaMedan;

    public Kompas(Activity aktivitas, Pendengar pendengar) {
        this.aktivitas = aktivitas;
        this.pendengar = pendengar;
        sensorManager = aktivitas.getSystemService(SensorManager.class);
        gravitasi = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    public void mulai() {
        adaGravitasi = adaMedan = false;
        if (gravitasi != null) {
            sensorManager.registerListener(this, gravitasi, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (magnetometer != null) {
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    public void berhenti() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (sensor == null || sensor.getType() != Sensor.TYPE_MAGNETIC_FIELD) {
            return;
        }
        if (accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE
                || accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
            pendengar.akurasiRendah(accuracy);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_GRAVITY:
                panjangGravitasi = normalkan(event.values, gravitasiNormal);
                adaGravitasi = true;
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                panjangMedan = normalkan(event.values, medanNormal);
                adaMedan = true;
                pendengar.medanMagnet(panjangMedan);
                break;
            default:
                return;
        }
        if (!adaGravitasi || !adaMedan) {
            return;
        }

        float[] g = gravitasiNormal;
        float[] m = medanNormal;

        // vektor mendatar yang menunjuk ke timur
        float timurX = m[1] * g[2] - m[2] * g[1];
        float timurY = m[2] * g[0] - m[0] * g[2];
        float timurZ = m[0] * g[1] - m[1] * g[0];
        float panjangTimur = (float) Math.sqrt(timurX * timurX + timurY * timurY + timurZ * timurZ);
        if (panjangGravitasi * panjangMedan * panjangTimur < 0.1f) {
            // Lazimnya > 100. Ponsel hampir jatuh bebas, atau dekat kutub magnet.
            return;
        }
        timurNormal[0] = timurX / panjangTimur;
        timurNormal[1] = timurY / panjangTimur;
        timurNormal[2] = timurZ / panjangTimur;

        // vektor mendatar yang menunjuk ke utara
        float mDotG = g[0] * m[0] + g[1] * m[1] + g[2] * m[2];
        float utaraX = m[0] - g[0] * mDotG;
        float utaraY = m[1] - g[1] * mDotG;
        float utaraZ = m[2] - g[2] * mDotG;
        float panjangUtara = (float) Math.sqrt(utaraX * utaraX + utaraY * utaraY + utaraZ * utaraZ);
        utaraNormal[0] = utaraX / panjangUtara;
        utaraNormal[1] = utaraY / panjangUtara;
        utaraNormal[2] = utaraZ / panjangUtara;

        // matriks rotasinya sekarang timurNormal, utaraNormal, gravitasiNormal;
        // lihat https://math.stackexchange.com/questions/381649/whats-the-best-3d-angular-co-ordinate-system-for-working-with-smartfone-apps
        float sin = timurNormal[1] - utaraNormal[0], cos = timurNormal[0] + utaraNormal[1];
        float azimutRadian = (float) (sin != 0 && cos != 0 ? Math.atan2(sin, cos) : 0);
        azimutRadian += koreksiPutaranLayar();

        pendengar.arahMagnetik(Math.toDegrees(azimutRadian));
    }

    /**
     * Sudut putaran layar dari posisi alaminya, radian.
     */
    private float koreksiPutaranLayar() {
        @SuppressWarnings("deprecation") // getDefaultDisplay() hanya untuk API 29; >= 30 pakai getDisplay()
        Display display = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                ? aktivitas.getDisplay()
                : aktivitas.getWindowManager().getDefaultDisplay();
        return switch (display.getRotation()) {
            case Surface.ROTATION_90 -> (float) Math.PI / 2;
            case Surface.ROTATION_180 -> (float) Math.PI;
            case Surface.ROTATION_270 -> 3 * (float) Math.PI / 2;
            default -> 0;
        };
    }

    /**
     * Menyalin {@code nilai} ke {@code hasil} dengan panjang 1.
     *
     * @return panjang aslinya
     */
    private static float normalkan(float[] nilai, float[] hasil) {
        float panjang = (float) Math.sqrt(nilai[0] * nilai[0] + nilai[1] * nilai[1] + nilai[2] * nilai[2]);
        for (int i = 0; i < 3; i++) {
            hasil[i] = nilai[i] / panjang;
        }
        return panjang;
    }
}
