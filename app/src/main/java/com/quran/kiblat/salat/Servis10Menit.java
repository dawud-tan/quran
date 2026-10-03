package com.quran.kiblat.salat;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.core.content.ContextCompat;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaStyleNotificationHelper;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Objects;

//kasus, locationManager dipause karena pakai BroadcastReceiver.
public class Servis10Menit extends Service {
    /**
     * Jeda sebelum langkah sesudah nada dikerjakan. Sisi asli mengembalikan
     * lama bunyinya persis, tapi bingkai terakhir masih harus mengalir keluar
     * dari penyangga aliran dulu.
     */
    private static final long MARGIN_HABIS = 250L;

    private final long[] DEFAULT_VIBRATE_PATTERN = {0, 1000, 200, 1000};
    private final Handler penjadwal = new Handler(Looper.getMainLooper());
    private MediaSession mediaSession;
    private ExoPlayer exoPlayer;
    private NadaT3 nada;
    private boolean fromBooting;
    private String nama, waktu, lokasitks;
    private int currentVolume;
    private AudioManager audioManager;

    @Override
    public void onCreate() {
        super.onCreate();

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_ALARM)
                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                .build();

        exoPlayer = new ExoPlayer.Builder(this)
                .build();
        exoPlayer.setAudioAttributes(audioAttributes, false);
        exoPlayer.setPlayWhenReady(true);

        // ExoPlayer sekarang hanya kebagian tarhim waktu subuh; nada isyarat
        // T3-nya dibangkitkan sendiri lewat Oboe.
        nada = new NadaT3();

        try {
            SecureRandom secureRandom = SecureRandom.getInstanceStrong();
            mediaSession = new MediaSession.Builder(this, exoPlayer)
                    .setId(Integer.toString(secureRandom.nextInt()))
                    .build();
        } catch (NoSuchAlgorithmException ignored) {
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        nama = intent.getStringExtra("nama");
        waktu = intent.getStringExtra("waktu");
        lokasitks = intent.getStringExtra("lokasi");
        fromBooting = intent.getBooleanExtra("fromBooting", false);
        if (nama == null) nama = "";
        if (waktu == null) waktu = "";
        if (lokasitks == null) lokasitks = "";

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        Uri subuhUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + BuildConfig.APPLICATION_ID + "/" + R.raw.tarhim);

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

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        String channelId = "10menitlagi";
        NotificationChannel channel = new NotificationChannel(channelId + nama, "10menitlagi" + nama, NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("10 menit lagi " + nama);
        channel.setVibrationPattern(DEFAULT_VIBRATE_PATTERN);
        channel.enableLights(true);
        channel.enableVibration(true);
        channel.setLightColor(0xff00ff00);
        channel.setBypassDnd(true);

        notificationManager.createNotificationChannel(channel);

        Resources res = getResources();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId + nama)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(res, R.mipmap.ic_launcher))
                .setContentTitle("10 menit lagi " + nama + " " + waktu)
                .setContentText(lokasitks)
                .setChannelId(channelId + nama)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE);

        NotificationManager managerCompat = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE || managerCompat.canUseFullScreenIntent()) {
            Intent fsi = new Intent(this, RingActivity.class);
            fsi.putExtra("nama", nama);
            fsi.putExtra("waktu", waktu);
            fsi.putExtra("lokasi", lokasitks);
            fsi.putExtra("kelas", "Servis10Menit");
            fsi.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(this, 1,
                    fsi, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setFullScreenIntent(fullScreenPendingIntent, true);
        }

        int maks = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maks, 0);

        // setShowActionsInCompactView(0) menaruh "Matikan" di tampilan ringkas
        // juga, bukan cuma waktu notifikasinya dibentangkan — alarm yang tidak
        // berhenti sendiri harus bisa dimatikan tanpa dibentangkan dulu.
        builder.setStyle(new MediaStyleNotificationHelper.MediaStyle(mediaSession)
                .setShowActionsInCompactView(0));

        Intent dismissIntent = new Intent(this, SiaranDismiss10Menit.class);
        PendingIntent dpi = PendingIntent.getBroadcast(
                this,
                1,
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        builder.setDeleteIntent(dpi);

        // Tombol mati yang selalu kelihatan. Nada T3 tidak lewat MediaSession,
        // jadi tombol jeda bawaan MediaStyle tidak menyentuhnya — dan lagi,
        // yang dibutuhkan alarm memang "matikan", bukan "jeda". Ini penting
        // sekarang: nadanya tidak berhenti sendiri lagi.
        builder.addAction(R.drawable.ic_matikan, "Matikan", dpi);

        ServiceCompat.startForeground(this, 1, builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION | ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);

        // Sesudah startForeground, tidak sebelumnya: sejak Android 10 lokasi
        // hanya boleh diakses oleh servis yang sudah benar-benar berada di
        // latar depan dengan tipe location, dan segarkanJadwal() di bawah
        // meminta pembaruan GPS.
        //
        // onStartCommand bisa dipanggil lagi selagi servisnya masih hidup,
        // jadi tugas lama dibuang dulu — kalau tidak, sisa tugas dari
        // panggilan sebelumnya akan memutus nada yang baru saja dimulai.
        penjadwal.removeCallbacksAndMessages(null);
        boolean subuh = nama.equals("subuh");
        long durasi = nada.mulai(subuh ? 1 : NadaT3.TANPA_BATAS);

        if (subuh) {
            // Satu putaran T3 sebagai pembuka, lalu tarhim; durasi > 0 karena
            // putarannya dibatasi. Jadwalnya dihitung ulang begitu tarhim
            // habis, lewat Player.Listener di atas.
            Runnable sesudahNada = () -> {
                nada.berhenti();
                exoPlayer.setMediaItem(MediaItem.fromUri(subuhUri));
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
            segarkanJadwal(false);
        }

        return START_NOT_STICKY;
    }

    private void panggil10Menit() {
        segarkanJadwal(true);
    }

    /**
     * Menyegarkan lokasi lalu menghitung ulang jadwal.
     * <p>
     * Sengaja getLastKnownLocation, bukan meminta fix GPS baru. Jadwal salat
     * nyaris tidak peka terhadap posisi: digeser 1 km ke arah mana pun,
     * ketujuh waktunya tidak berubah satu detik pun; 10 km baru menggeser satu
     * slot satu menit. Bandingkan dengan ihtiyati yang 2 menit. Menunggu fix
     * GPS sungguhan berarti menahan servis tanpa batas waktu demi ketelitian
     * yang tidak pernah terlihat, jadi ini selesai seketika dan tidak bisa
     * menggantung.
     *
     * @param tutup {@code true}: servisnya berhenti sesudah jadwalnya dihitung
     *              ulang — penutupan sesudah tarhim subuh. {@code false}: cuma
     *              menyegarkan, nadanya terus berbunyi.
     */
    private void segarkanJadwal(boolean tutup) {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Siaran10Menit::onStartCommand");

        SharedPreferences sharedPref = getSharedPreferences("pref", Context.MODE_PRIVATE);
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location lokasi = Util.lokasiTerakhir(sharedPref);
        try {
            wakeLock.acquire(600000);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Location smntra = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (smntra == null) {
                    smntra = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
                if (smntra != null) {
                    lokasi = smntra;
                    Util.simpanLokasi(sharedPref, lokasi);
                }
            }
            Util.cekJadwal(this, lokasi, fromBooting);
        } finally {
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
            if (tutup) {
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, currentVolume, 0);
                stopSelf();
            }
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
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Servis10Menit::onTaskRemoved");

            SharedPreferences sharedPref = getSharedPreferences("pref", Context.MODE_PRIVATE);
            Location lokasi = Util.lokasiTerakhir(sharedPref);
            try {
                wakeLock.acquire(600000);
                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
                        && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    Location smntra = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (smntra != null) {
                        lokasi = smntra;
                    }
                    if (smntra == null) {
                        lokasi = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    }
                    Util.simpanLokasi(sharedPref, Objects.requireNonNull(lokasi));
                }
                Util.cekJadwal(getApplicationContext(), lokasi, fromBooting);
            } finally {
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, currentVolume, 0);
                if (wakeLock.isHeld()) {
                    wakeLock.release();
                }
                stopSelf();
            }
        }
    }

    @Override
    public void onDestroy() {
        penjadwal.removeCallbacksAndMessages(null);
        if (nada != null) {
            nada.lepas();
            nada = null;
        }

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Servis10Menit::onTaskRemoved");

        SharedPreferences sharedPref = getSharedPreferences("pref", Context.MODE_PRIVATE);
        Location lokasi = Util.lokasiTerakhir(sharedPref);
        try {
            wakeLock.acquire(600000);
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Location smntra = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (smntra != null) {
                    lokasi = smntra;
                }
                if (smntra == null) {
                    lokasi = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
                Util.simpanLokasi(sharedPref, Objects.requireNonNull(lokasi));
            }
            Util.cekJadwal(getApplicationContext(), lokasi, fromBooting);
        } finally {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, currentVolume, 0);
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }
            stopSelf();
        }
        if (mediaSession != null) {
            exoPlayer.release();
            mediaSession.release();
            mediaSession = null;
        }
        super.onDestroy();
    }

}