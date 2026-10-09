package com.quran.kiblat.salat.alarm;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

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

import com.quran.kiblat.salat.BuildConfig;
import com.quran.kiblat.salat.R;
import com.quran.kiblat.salat.izin.Izin;

/**
 * Pengingat 10 menit sebelum adzan: nada isyarat T3 dari {@link NadaT3}.
 * <p>
 * Selain subuh, nadanya berbunyi terus sampai pengguna mematikannya. Subuh:
 * satu putaran nada, lalu tarhim lewat ExoPlayer, lalu servisnya selesai.
 */
public class Servis10Menit extends Service {
    /**
     * Jeda sebelum langkah sesudah nada dikerjakan. Sisi asli mengembalikan
     * lama bunyinya persis, tapi bingkai terakhir masih harus mengalir keluar
     * dari penyangga aliran dulu.
     */
    private static final long MARGIN_HABIS = 250L;
    /**
     * Permintaan mematikan nada dari luar servis: tombol AktivitasDering, aksi
     * "Matikan" di notifikasi, dan notifikasi yang digeser. Lihat {@link #matikan}.
     */
    private static final String AKSI_MATIKAN = BuildConfig.APPLICATION_ID + ".MATIKAN_10_MENIT";
    /**
     * Batas nada kalau notifikasinya tidak tampil: 45 putaran x 4 detik =
     * 3 menit, lama nada sebelum ia dibuat berbunyi terus sampai dimatikan.
     */
    private static final int PUTARAN_TANPA_NOTIFIKASI = 45;

    private final Handler penjadwal = new Handler(Looper.getMainLooper());
    private MediaSession mediaSession;
    private ExoPlayer exoPlayer;
    private NadaT3 nada;
    private int volumeSebelumnya;
    private boolean volumeDinaikkan;
    private AudioManager audioManager;

    /**
     * Mematikan nada lewat servisnya sendiri, bukan stopService.
     * <p>
     * Volume alarm harus dikembalikan selagi servisnya masih di latar depan.
     * Mulai Android 17, setStreamVolume dari aplikasi yang tidak punya layar
     * terlihat maupun servis latar depan diabaikan diam-diam — dan di onDestroy
     * servisnya sudah turun dari latar depan, jadi volumenya akan tertinggal
     * di maksimum.
     */
    public static void matikan(Context context) {
        try {
            context.startService(new Intent(context, Servis10Menit.class).setAction(AKSI_MATIKAN));
        } catch (IllegalStateException ex) {
            // servisnya sudah tidak jalan dan aplikasi di latar: tidak ada yang perlu dimatikan
            context.stopService(new Intent(context, Servis10Menit.class));
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        audioManager = getSystemService(AudioManager.class);

        // ExoPlayer sekarang hanya kebagian tarhim waktu subuh; nada isyarat
        // T3-nya dibangkitkan sendiri lewat Oboe.
        exoPlayer = PembantuServis.buatPemutar(this);
        // Didaftarkan sekali di sini, bukan di onStartCommand yang bisa
        // dipanggil berkali-kali selagi servisnya hidup.
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                int playbackState = exoPlayer.getPlaybackState();
                if (!isPlaying && playbackState == Player.STATE_READY) {
                    panggil10Menit();
                }
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                // Cuma tarhim subuh yang lewat exoPlayer sekarang. Dulu
                // cabang ini mengecualikan subuh, jadi begitu tarhimnya habis
                // tidak ada yang menghitung ulang jadwal dan servisnya
                // menggantung sampai dibubarkan.
                if (playbackState == Player.STATE_ENDED) {
                    panggil10Menit();
                }
            }
        });
        mediaSession = PembantuServis.buatSesi(this, exoPlayer, "Servis10Menit");
        nada = new NadaT3();
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && AKSI_MATIKAN.equals(intent.getAction())) {
            panggil10Menit();
            return START_NOT_STICKY;
        }

        String nama = PembantuServis.ekstra(intent, PenjadwalAdzan.EKSTRA_NAMA);
        String waktu = PembantuServis.ekstra(intent, PenjadwalAdzan.EKSTRA_WAKTU);
        String lokasitks = PembantuServis.ekstra(intent, PenjadwalAdzan.EKSTRA_LOKASI);

        String saluran = "10menitlagi" + nama;
        NotificationCompat.Builder builder = PembantuServis.siapkanNotifikasi(this, saluran,
                "10 menit lagi " + nama, "10 menit lagi " + nama + " " + waktu, lokasitks, true);

        // setShowActionsInCompactView(0) menaruh "Matikan" di tampilan ringkas
        // juga, bukan cuma waktu notifikasinya dibentangkan — alarm yang tidak
        // berhenti sendiri harus bisa dimatikan tanpa dibentangkan dulu.
        builder.setStyle(new MediaStyleNotificationHelper.MediaStyle(mediaSession)
                .setShowActionsInCompactView(0));

        // Tombol mati yang selalu kelihatan. Nada T3 tidak lewat MediaSession,
        // jadi tombol jeda bawaan MediaStyle tidak menyentuhnya — dan lagi,
        // yang dibutuhkan alarm memang "matikan", bukan "jeda". Ini penting
        // sekarang: nadanya tidak berhenti sendiri lagi.
        PendingIntent matikan = PembantuServis.niatMatikan(this, SiaranMatikan.AKSI_10_MENIT);
        builder.setDeleteIntent(matikan);
        builder.addAction(R.drawable.ic_matikan, "Matikan", matikan);

        // Cukup mediaPlayback. Tipe location dulu ikut dipasang, padahal sejak
        // Android 14 servis bertipe location tidak boleh dimulai dari latar
        // belakang tanpa ACCESS_BACKGROUND_LOCATION — alarm ini selalu dimulai
        // dari latar, jadi siapa pun yang tidak memberi "izinkan sepanjang
        // waktu" kena SecurityException tepat saat adzan. Lokasinya sendiri
        // cuma getLastKnownLocation, yang tidak butuh tipe apa pun.
        ServiceCompat.startForeground(this, 1, builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);

        // Sesudah startForeground, tidak sebelumnya: mulai Android 17 volume
        // dari latar belakang hanya boleh diubah oleh servis yang sudah di
        // latar depan (dan, untuk targetSdk 37, memegang izin alarm tepat
        // dengan aliran USAGE_ALARM). Sebelum itu panggilannya diabaikan.
        naikkanVolume();

        // onStartCommand bisa dipanggil lagi selagi servisnya masih hidup,
        // jadi tugas lama dibuang dulu — kalau tidak, sisa tugas dari
        // panggilan sebelumnya akan memutus nada yang baru saja dimulai.
        penjadwal.removeCallbacksAndMessages(null);
        boolean subuh = nama.equals("subuh");
        // Tanpa notifikasi yang tampil (izinnya ditolak atau salurannya
        // dimatikan) tidak ada tombol Matikan dan layar penuhnya pun tidak
        // muncul, jadi nada tanpa batas cuma bisa dihentikan dengan paksa-
        // berhenti. Di situ nadanya dibatasi.
        boolean bisaDimatikan = Izin.notifikasiTampil(this, saluran);
        long durasi = nada.mulai(subuh ? 1 : bisaDimatikan ? NadaT3.TANPA_BATAS : PUTARAN_TANPA_NOTIFIKASI);

        if (subuh) {
            // Satu putaran T3 sebagai pembuka, lalu tarhim; durasi > 0 karena
            // putarannya dibatasi. Jadwalnya dihitung ulang begitu tarhim
            // habis, lewat Player.Listener di onCreate.
            Uri tarhim = PembantuServis.uriRaw(R.raw.tarhim);
            Runnable sesudahNada = () -> {
                nada.berhenti();
                exoPlayer.setMediaItem(MediaItem.fromUri(tarhim));
                exoPlayer.prepare();
            };
            if (durasi > 0) {
                penjadwal.postDelayed(sesudahNada, durasi + MARGIN_HABIS);
            } else {
                // Alirannya gagal dibuka. Jangan sampai tarhimnya ikut batal.
                sesudahNada.run();
            }
        } else if (durasi < 0) {
            // Tidak ada nada sama sekali, jadi tidak akan pernah ada yang
            // mematikannya. Tutup sekarang supaya rantai alarmnya tetap jalan.
            panggil10Menit();
        } else {
            // Selain subuh nadanya berbunyi terus sampai pengguna menekan
            // "Matikan", jadi sengaja tidak ada apa pun yang dijadwalkan.
            // Jadwalnya dihitung ulang sekarang juga supaya rantai alarmnya
            // sudah terpasang walaupun servisnya nanti mati mendadak.
            PenjadwalAdzan.segarkanJadwal(this, true);
            if (durasi > 0) {
                // nadanya dibatasi karena notifikasinya tidak tampil, lihat di atas
                penjadwal.postDelayed(this::panggil10Menit, durasi + MARGIN_HABIS);
            }
        }

        return START_NOT_STICKY;
    }

    /**
     * Menutup servis. Jadwal berikutnya dihitung ulang di onDestroy, yang
     * dilewati semua jalan keluar.
     */
    private void panggil10Menit() {
        pulihkanVolume();
        stopSelf();
    }

    private void naikkanVolume() {
        // dipanggil lagi kalau onStartCommand datang dua kali: yang diingat
        // tetap volume sebelum alarm, bukan volume maksimum yang baru dipasang
        if (!volumeDinaikkan) {
            volumeSebelumnya = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
            volumeDinaikkan = true;
        }
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0);
    }

    /**
     * Harus dipanggil selagi servisnya masih di latar depan, lihat {@link #matikan}.
     */
    private void pulihkanVolume() {
        if (volumeDinaikkan) {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, volumeSebelumnya, 0);
            volumeDinaikkan = false;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(@Nullable Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        // Nada T3 tidak lewat exoPlayer, jadi keadaannya harus ikut ditanya —
        // tanpa itu getMediaItemCount() == 0 langsung benar dan alarm yang
        // sedang berbunyi ikut dimatikan begitu aplikasi digeser dari daftar.
        if ((nada == null || !nada.sedangMain())
                && (!exoPlayer.getPlayWhenReady()
                || exoPlayer.getMediaItemCount() == 0
                || exoPlayer.getPlaybackState() == Player.STATE_ENDED)) {
            panggil10Menit();
        }
    }

    @Override
    public void onDestroy() {
        penjadwal.removeCallbacksAndMessages(null);
        if (nada != null) {
            nada.lepas();
            nada = null;
        }

        // Jalan terakhir. Biasanya volumenya sudah dikembalikan sebelum ini;
        // kalau belum (servisnya dihentikan sistem), di Android 17 panggilan
        // ini bisa diabaikan karena servisnya sudah turun dari latar depan.
        pulihkanVolume();
        PenjadwalAdzan.segarkanJadwal(this, true);
        exoPlayer.release();
        mediaSession.release();
        super.onDestroy();
    }

}
