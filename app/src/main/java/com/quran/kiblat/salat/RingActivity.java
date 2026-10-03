package com.quran.kiblat.salat;

import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.quran.kiblat.salat.databinding.AktivitasDeringBinding;

import java.util.Objects;

public class RingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        com.quran.kiblat.salat.databinding.AktivitasDeringBinding binding = AktivitasDeringBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setShowWhenLocked(true);
        setTurnScreenOn(true);

        Intent intent = getIntent();

        String kelas = intent.getStringExtra("kelas");
        String nama = intent.getStringExtra("nama");
        String waktu = intent.getStringExtra("waktu");
        String lokasitks = intent.getStringExtra("lokasi");
        if (nama == null) nama = "";
        if (waktu == null) waktu = "";
        if (lokasitks == null) lokasitks = "";

        String judul = Objects.requireNonNull(kelas).equals("Servis10Menit") ? "10 menit lagi " + nama + " " + waktu : "Waktu " + nama + " " + waktu;
        binding.judul.setText(judul);
        binding.isinya.setText(lokasitks);

        binding.activityRingDismiss.setOnClickListener(v -> {
            if (kelas.equals("Servis10Menit")) {
                stopService(new Intent(this, Servis10Menit.class));
            } else {
                stopService(new Intent(this, ServisAdzan.class));
            }

            finish();
        });

        setVolumeControlStream(AudioManager.STREAM_ALARM);
    }
}