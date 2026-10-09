package com.quran.kiblat.salat.alarm;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.quran.kiblat.salat.databinding.AktivitasDeringBinding;

/**
 * Layar penuh di atas layar kunci saat alarm berbunyi, dengan satu tombol
 * untuk mematikannya. Dibuka lewat full-screen intent dari notifikasi kedua
 * servis, lihat {@link PembantuServis#siapkanNotifikasi}.
 */
public class AktivitasDering extends AppCompatActivity {

    private static final String EKSTRA_JUDUL = "judul";
    private static final String EKSTRA_ISI = "isi";
    private static final String EKSTRA_SEPULUH_MENIT = "sepuluhMenit";

    /**
     * @param sepuluhMenit true kalau dibuka oleh {@link Servis10Menit}, false
     *                     oleh {@link ServisAdzan}
     */
    static Intent niat(Context context, String judul, String isi, boolean sepuluhMenit) {
        return new Intent(context, AktivitasDering.class)
                .putExtra(EKSTRA_JUDUL, judul)
                .putExtra(EKSTRA_ISI, isi)
                .putExtra(EKSTRA_SEPULUH_MENIT, sepuluhMenit);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AktivitasDeringBinding binding = AktivitasDeringBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setShowWhenLocked(true);
        setTurnScreenOn(true);

        Intent intent = getIntent();
        boolean sepuluhMenit = intent.getBooleanExtra(EKSTRA_SEPULUH_MENIT, false);
        binding.judul.setText(PembantuServis.ekstra(intent, EKSTRA_JUDUL));
        binding.isinya.setText(PembantuServis.ekstra(intent, EKSTRA_ISI));

        binding.activityRingDismiss.setOnClickListener(v -> {
            if (sepuluhMenit) {
                Servis10Menit.matikan(this);
            } else {
                ServisAdzan.matikan(this);
            }
            finish();
        });

        setVolumeControlStream(AudioManager.STREAM_ALARM);
    }
}
