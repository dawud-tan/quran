package com.quran.kiblat.salat.alarm;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaStyleNotificationHelper;

import com.quran.kiblat.salat.R;

/**
 * Adzan tepat pada waktunya, diputar ExoPlayer dari res/raw. Berhenti
 * sendiri begitu adzannya habis.
 */
public class ServisAdzan extends Service {
    private MediaSession mediaSession;
    private ExoPlayer exoPlayer;

    /**
     * Dipakai tombol di AktivitasDering dan oleh {@link SiaranMatikan}.
     * Adzan tidak menaikkan volume, jadi cukup stopService.
     */
    public static void matikan(Context context) {
        context.stopService(new Intent(context, ServisAdzan.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        exoPlayer = PembantuServis.buatPemutar(this);
        // Didaftarkan sekali di sini, bukan di onStartCommand yang bisa
        // dipanggil berkali-kali selagi servisnya hidup.
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                int playbackState = exoPlayer.getPlaybackState();
                if (!isPlaying && playbackState == Player.STATE_READY) {
                    panggilAdzan();
                }
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if ((!exoPlayer.getPlayWhenReady()
                        || exoPlayer.getMediaItemCount() == 0
                        || playbackState == Player.STATE_ENDED)) {
                    panggilAdzan();
                }
            }
        });
        mediaSession = PembantuServis.buatSesi(this, exoPlayer, "ServisAdzan");
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        String nama = PembantuServis.ekstra(intent, PenjadwalAdzan.EKSTRA_NAMA);
        String waktu = PembantuServis.ekstra(intent, PenjadwalAdzan.EKSTRA_WAKTU);
        String lokasitks = PembantuServis.ekstra(intent, PenjadwalAdzan.EKSTRA_LOKASI);

        NotificationCompat.Builder builder = PembantuServis.siapkanNotifikasi(this, nama,
                "adzan", "Waktu " + nama + " " + waktu, lokasitks, false);
        builder.setStyle(new MediaStyleNotificationHelper.MediaStyle(mediaSession));
        builder.setDeleteIntent(PembantuServis.niatMatikan(this, SiaranMatikan.AKSI_ADZAN));

        // Cukup mediaPlayback: tipe location tidak boleh dimulai dari latar
        // belakang tanpa ACCESS_BACKGROUND_LOCATION sejak Android 14, dan
        // lokasinya toh cuma dibaca dari tembolok. Lihat Servis10Menit.
        ServiceCompat.startForeground(this, 1, builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);

        // Baru diputar sesudah servisnya di latar depan: mulai Android 17
        // pemutaran dari latar belakang tanpa servis latar depan dibisukan.
        int adzan = nama.equals("subuh") ? R.raw.adzansubuh : R.raw.adzanfull;
        exoPlayer.setMediaItem(MediaItem.fromUri(PembantuServis.uriRaw(adzan)));
        exoPlayer.prepare();
        return START_NOT_STICKY;
    }

    /**
     * Menutup servis. Jadwal berikutnya dihitung ulang di onDestroy, yang
     * dilewati semua jalan keluar — termasuk penghentian dari AktivitasDering
     * dan dari notifikasi.
     */
    private void panggilAdzan() {
        stopSelf();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(@Nullable Intent rootIntent) {
        if (!exoPlayer.getPlayWhenReady()
                || exoPlayer.getMediaItemCount() == 0
                || exoPlayer.getPlaybackState() == Player.STATE_ENDED) {

            super.onTaskRemoved(rootIntent);
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        // Dulu lewat LocationWorker. Sekarang lokasinya cuma dibaca dari
        // tembolok, jadi selesai seketika di sini; WorkManager hanya menambah
        // jeda, dan di Android 10-11 pekerjaan expedited tanpa
        // getForegroundInfo() bisa mogok.
        PenjadwalAdzan.segarkanJadwal(this, true);

        exoPlayer.release();
        mediaSession.release();
        super.onDestroy();
    }
}
