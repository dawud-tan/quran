package com.quran.kiblat.salat.alarm;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Penerima alarm dari {@link PenjadwalAdzan}: meneruskan ekstranya ke servis
 * latar depan yang membunyikan alarm.
 * <p>
 * Dua turunannya sengaja tetap dua kelas (dua komponen di manifest).
 * PendingIntent keduanya memakai kode permintaan yang sama, dan hanya
 * komponennya yang membuat keduanya dua alarm yang terpisah — sehingga
 * PenjadwalAdzan bisa memasang yang satu sambil mencabut yang lain.
 */
abstract class SiaranAlarm extends BroadcastReceiver {

    /**
     * Servis yang dijalankan saat alarm ini berbunyi.
     */
    protected abstract Class<? extends Service> servis();

    @Override
    public final void onReceive(Context context, Intent intent) {
        // Dimulai dari alarm tepat waktu, jadi boleh memulai servis latar
        // depan walaupun aplikasinya sedang di latar belakang.
        context.startForegroundService(new Intent(context, servis())
                .putExtra(PenjadwalAdzan.EKSTRA_NAMA, PembantuServis.ekstra(intent, PenjadwalAdzan.EKSTRA_NAMA))
                .putExtra(PenjadwalAdzan.EKSTRA_WAKTU, PembantuServis.ekstra(intent, PenjadwalAdzan.EKSTRA_WAKTU))
                .putExtra(PenjadwalAdzan.EKSTRA_LOKASI, PembantuServis.ekstra(intent, PenjadwalAdzan.EKSTRA_LOKASI)));
    }
}
