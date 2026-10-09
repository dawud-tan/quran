package com.quran.kiblat.salat.alarm;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;

import androidx.core.app.NotificationCompat;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;

import com.quran.kiblat.salat.BuildConfig;
import com.quran.kiblat.salat.R;
import com.quran.kiblat.salat.izin.Izin;

/**
 * Bagian yang sama persis di {@link Servis10Menit} dan {@link ServisAdzan}:
 * pemutar di aliran alarm, saluran notifikasi, dan notifikasinya sendiri
 * beserta layar penuhnya. Dulu disalin di kedua servis.
 */
final class PembantuServis {

    private static final long[] POLA_GETAR = {0, 1000, 200, 1000};
    /**
     * Kode yang sama dipakai kedua servis untuk layar penuhnya: hanya satu
     * AktivitasDering yang tampil, dan yang terbaru menimpa ekstranya.
     */
    private static final int KODE_LAYAR_PENUH = 1;
    private static final int KODE_MATIKAN = 1;

    private PembantuServis() {
    }

    /**
     * ExoPlayer di aliran alarm (USAGE_ALARM), bukan media: volume alarm yang
     * dinaikkan ke maksimum hanya berlaku di aliran itu, dan pengguna yang
     * membisukan media tetap mendengar adzan.
     */
    static ExoPlayer buatPemutar(Context context) {
        ExoPlayer pemutar = new ExoPlayer.Builder(context).build();
        pemutar.setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(C.USAGE_ALARM)
                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                .build(), false);
        pemutar.setPlayWhenReady(true);
        return pemutar;
    }

    /**
     * @param id harus berbeda untuk setiap sesi yang hidup bersamaan dalam
     *           satu proses. Kedua servis bisa hidup bersamaan (nada 10 menit
     *           yang belum dimatikan saat adzan tiba), jadi masing-masing
     *           memakai nama kelasnya sendiri.
     */
    static MediaSession buatSesi(Context context, ExoPlayer pemutar, String id) {
        return new MediaSession.Builder(context, pemutar).setId(id).build();
    }

    static Uri uriRaw(int idBerkas) {
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://"
                + BuildConfig.APPLICATION_ID + "/" + idBerkas);
    }

    /**
     * Membuat saluran notifikasinya (atau memakai yang sudah ada), lalu
     * menyiapkan notifikasi alarm lengkap dengan layar penuh kalau diizinkan.
     * Gaya (MediaStyle), tombol, dan delete intent diatur pemanggil.
     *
     * @param saluran      id sekaligus nama saluran
     * @param sepuluhMenit true dari Servis10Menit; menentukan servis mana
     *                     yang dimatikan tombol di layar penuh
     */
    static NotificationCompat.Builder siapkanNotifikasi(Service servis, String saluran,
                                                        String keteranganSaluran, String judul,
                                                        String isi, boolean sepuluhMenit) {
        NotificationChannel channel = new NotificationChannel(saluran, saluran, NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(keteranganSaluran);
        channel.setVibrationPattern(POLA_GETAR);
        channel.enableLights(true);
        channel.enableVibration(true);
        channel.setLightColor(0xff00ff00);
        channel.setBypassDnd(true);
        servis.getSystemService(NotificationManager.class).createNotificationChannel(channel);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(servis, saluran)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(servis.getResources(), R.mipmap.ic_launcher))
                .setContentTitle(judul)
                .setContentText(isi)
                .setChannelId(saluran)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE);

        if (Izin.layarPenuh(servis)) {
            Intent fsi = AktivitasDering.niat(servis, judul, isi, sepuluhMenit)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            builder.setFullScreenIntent(PendingIntent.getActivity(servis, KODE_LAYAR_PENUH, fsi,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT), true);
        }
        return builder;
    }

    /**
     * Untuk tombol "Matikan" dan untuk notifikasi yang digeser.
     *
     * @param aksi {@link SiaranMatikan#AKSI_10_MENIT} atau
     *             {@link SiaranMatikan#AKSI_ADZAN}. Aksinya beda, jadi kedua
     *             servis mendapat PendingIntent yang terpisah.
     */
    static PendingIntent niatMatikan(Context context, String aksi) {
        return PendingIntent.getBroadcast(context, KODE_MATIKAN,
                new Intent(context, SiaranMatikan.class).setAction(aksi),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /**
     * Ekstra Intent yang tidak pernah null: kosong kalau tidak dibawa.
     */
    static String ekstra(Intent intent, String nama) {
        String nilai = intent == null ? null : intent.getStringExtra(nama);
        return nilai == null ? "" : nilai;
    }
}
