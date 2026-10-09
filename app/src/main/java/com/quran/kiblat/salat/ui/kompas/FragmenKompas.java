package com.quran.kiblat.salat.ui.kompas;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.provider.Settings;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.quran.kiblat.salat.hisab.MedanMagnetBumi;
import com.quran.kiblat.salat.izin.Izin;
import com.quran.kiblat.salat.lokasi.Lokasi;
import com.quran.kiblat.salat.umum.Pref;

import java.time.LocalDate;
import java.util.Locale;

/**
 * Dasar layar kompas (kiblat, arah terbit, arah tenggelam): lokasi, kompas,
 * koreksi deklinasi magnet, dan baris-baris data yang sama di ketiganya.
 * <p>
 * Turunannya cukup:
 * <ol>
 * <li>di akhir onCreateView memanggil {@link #mulaiKompas} dengan
 *     tampilannya;</li>
 * <li>mengisi {@link #perbaruiData} — dipanggil saat lokasi berubah, saat
 *     layar kembali tampil, dan saat tanggal berganti;</li>
 * <li>mengembalikan azimut sasarannya di {@link #azimutSasaran}.</li>
 * </ol>
 */
public abstract class FragmenKompas extends Fragment implements Kompas.Pendengar {

    /**
     * Pembaruan GPS selagi layar terbuka: paling cepat 10 menit sekali,
     * atau setiap bergeser 1 meter.
     */
    private static final long SELANG_GPS = 600000;
    private static final float JARAK_GPS = 1;

    /**
     * Lokasi yang sedang dipakai, sudah dibaca balik dari simpanan (MSL).
     */
    protected Location lokasiku;
    protected SharedPreferences sharedPref;

    private Tampilan tampilan;
    private Kompas kompas;
    private LocationManager locationManager;
    private final LocationListener pendengarGps = this::lokasiDariGps;
    /**
     * Deklinasi magnet di lokasi saat ini, derajat. Dihitung sekali per
     * lokasi, bukan pada setiap kejadian sensor.
     */
    private float deklinasi;
    private LocalDate tanggalData;
    /// Penanda supaya peringatan akurasi tidak muncul berulang-ulang.
    private boolean sudahIngatkanAkurasi;

    /**
     * View yang sama di ketiga tata letak.
     *
     * @param jarum      gambar yang diputar ke arah sasaran
     * @param mawarAngin gambar mata angin, diputar ke utara sejati
     */
    public record Tampilan(ImageView jarum, ImageView mawarAngin, TextView koordinat,
                              TextView akurasiGps, TextView mdpl, TextView sudutAzimut,
                              TextView mikrotesla) {
    }

    /**
     * Data turunan untuk lokasi (dan tanggal) ini perlu dihitung ulang.
     */
    protected abstract void perbaruiData(Location lokasi);

    /**
     * Azimut sasaran dari utara sejati, derajat, atau NaN kalau tidak ada.
     */
    protected abstract double azimutSasaran();

    /**
     * true untuk layar kiblat; menentukan isi {@link PeringatanKompas}.
     */
    protected abstract boolean layarKiblat();

    /**
     * Dipanggil turunan di akhir onCreateView.
     */
    protected void mulaiKompas(Tampilan tampilan) {
        this.tampilan = tampilan;
        sharedPref = Pref.dari(requireContext());
        kompas = new Kompas(requireActivity(), this);
        locationManager = requireContext().getSystemService(LocationManager.class);

        // Lewat Lokasi.perbarui: dulu getLastKnownLocation dipanggil tanpa memeriksa izin,
        // jadi layar ini mogok (SecurityException) kalau izin lokasi ditolak.
        terimaLokasi(Lokasi.perbarui(requireContext(), false));
        mintaPembaruanGps();
    }

    @SuppressLint("MissingPermission") // Izin.lokasiTepat sudah diperiksa
    private void mintaPembaruanGps() {
        if (Izin.lokasiTepat(requireContext())
                && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                && !modePesawat()) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, SELANG_GPS,
                    JARAK_GPS, pendengarGps);
        }
    }

    private boolean modePesawat() {
        return Settings.Global.getInt(requireContext().getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0;
    }

    private void lokasiDariGps(Location gps) {
        if (tampilan == null) {
            return;
        }
        // disimpan lalu dibaca balik, sama dengan yang dipakai jadwal yang dibunyikan
        Lokasi.simpan(sharedPref, gps);
        terimaLokasi(Lokasi.terakhir(sharedPref));
    }

    private void terimaLokasi(Location lokasi) {
        lokasiku = lokasi;
        tampilan.koordinat().setText(Lokasi.teksKoordinat(lokasi));
        tampilan.akurasiGps().setText(String.format(Locale.getDefault(), "%.2f meter", lokasi.getAccuracy()));
        tampilan.mdpl().setText(String.format(Locale.getDefault(), "%.2f meter", lokasi.getAltitude()));
        deklinasi = new MedanMagnetBumi((float) lokasi.getLatitude(), (float) lokasi.getLongitude(),
                (float) lokasi.getAltitude(), System.currentTimeMillis()).getDeclination();
        segarkanData();
    }

    private void segarkanData() {
        tanggalData = LocalDate.now();
        perbaruiData(lokasiku);
        double sasaran = azimutSasaran();
        tampilan.sudutAzimut().setText(Double.isNaN(sasaran)
                ? "-"
                : String.format(Locale.getDefault(), "%.2f°", sasaran));
    }

    @Override
    public void onResume() {
        super.onResume();
        sudahIngatkanAkurasi = false;
        // kembali dari dialog Pengaturan Adzan, atau dari latar sesudah lewat tengah malam
        if (tampilan != null) {
            segarkanData();
        }
        kompas.mulai();
    }

    @Override
    public void onPause() {
        kompas.berhenti();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        locationManager.removeUpdates(pendengarGps);
        tampilan = null;
    }

    @Override
    public void medanMagnet(float mikrotesla) {
        if (tampilan != null) {
            tampilan.mikrotesla().setText(String.format(Locale.getDefault(), "%.2f µT", mikrotesla));
        }
    }

    @Override
    public void arahMagnetik(double azimut) {
        if (tampilan == null) {
            return;
        }
        if (!LocalDate.now().equals(tanggalData)) {
            // lewat tengah malam selagi layar terbuka: jadwal dan arah matahari ganti hari
            segarkanData();
        }
        double utaraSejati = azimut - deklinasi;
        tampilan.mawarAngin().setRotation((float) -utaraSejati);
        double sasaran = azimutSasaran();
        if (!Double.isNaN(sasaran)) {
            tampilan.jarum().setRotation((float) (sasaran - utaraSejati));
        }
    }

    @Override
    public void akurasiRendah(int akurasi) {
        // Sekali saja per kunjungan: onAccuracyChanged bisa dipanggil
        // berkali-kali selama sensornya masih goyah.
        if (!sudahIngatkanAkurasi && isResumed() && tampilan != null) {
            sudahIngatkanAkurasi = true;
            PeringatanKompas.tampilkan(requireContext(), akurasi, layarKiblat());
        }
    }
}
