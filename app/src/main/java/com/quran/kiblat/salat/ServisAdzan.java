package com.quran.kiblat.salat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaStyleNotificationHelper;
import androidx.work.BackoffPolicy;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkManager;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

//kasus, locationManager dipause karena pakai BroadcastReceiver.
public class ServisAdzan extends Service {
    private final long[] DEFAULT_VIBRATE_PATTERN = {0, 1000, 200, 1000};
    private MediaSession mediaSession;
    private ExoPlayer exoPlayer;
    private boolean fromBooting;

    @Override
    public void onCreate() {
        super.onCreate();
        exoPlayer = new ExoPlayer.Builder(this)
                .build();
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_ALARM)
                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                .build();

        exoPlayer.setAudioAttributes(audioAttributes, false);
        exoPlayer.setPlayWhenReady(true);

        try {
            SecureRandom secureRandom = SecureRandom.getInstanceStrong();
            mediaSession = new MediaSession.Builder(this, exoPlayer)
                    .setId(Integer.toString(secureRandom.nextInt()))
                    .build();
        } catch (NoSuchAlgorithmException e) {
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        String nama = Objects.requireNonNull(intent).getStringExtra("nama");
        String waktu = intent.getStringExtra("waktu");
        String lokasitks = intent.getStringExtra("lokasi");
        fromBooting = intent.getBooleanExtra("fromBooting", false);
        if (nama == null) nama = "";
        if (waktu == null) waktu = "";
        if (lokasitks == null) lokasitks = "";

        Uri soundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + BuildConfig.APPLICATION_ID + "/" + R.raw.adzanfull);
        if (nama.equals("subuh")) {
            soundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + BuildConfig.APPLICATION_ID + "/" + R.raw.adzansubuh);
        }

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

        NotificationManager managerCompat = getSystemService(NotificationManager.class);

        NotificationChannel channel = new NotificationChannel(nama, nama, NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("adzan");
        channel.setVibrationPattern(DEFAULT_VIBRATE_PATTERN);
        channel.enableLights(true);
        channel.enableVibration(true);
        channel.setLightColor(0xff00ff00);
        channel.setBypassDnd(true);

        managerCompat.createNotificationChannel(channel);


        Resources res = getResources();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, nama)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(res, R.mipmap.ic_launcher))
                .setContentTitle("Waktu " + nama + " " + waktu)
                .setContentText(lokasitks)
                .setChannelId(nama)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE || managerCompat.canUseFullScreenIntent()) {
            Intent fsi = new Intent(this, RingActivity.class);
            fsi.putExtra("nama", nama);
            fsi.putExtra("waktu", waktu);
            fsi.putExtra("lokasi", lokasitks);
            fsi.putExtra("kelas", "ServisAdzan");
            fsi.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(this, 1,
                    fsi, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setFullScreenIntent(fullScreenPendingIntent, true);
        }

        exoPlayer.setMediaItem(MediaItem.fromUri(soundUri));
        exoPlayer.prepare();

        builder.setStyle(new MediaStyleNotificationHelper.MediaStyle(mediaSession));

        Intent dismissIntent = new Intent(this, SiaranDismissAdzan.class);
        PendingIntent dpi = PendingIntent.getBroadcast(
                this,
                1,
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        builder.setDeleteIntent(dpi);
        //perlu panggil startForeground
        ServiceCompat.startForeground(this, 1, builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION | ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        return START_NOT_STICKY;
    }

    private void panggilAdzan() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ServisAdzan::onStartCommand");
        wakeLock.acquire(600000);
        Data.Builder builder = new Data.Builder();
        builder.putBoolean("fromBooting", fromBooting);
        OneTimeWorkRequest workRequest =
                new OneTimeWorkRequest.Builder(LocationWorker.class)
                        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        .setBackoffCriteria(
                                BackoffPolicy.LINEAR,
                                OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                                TimeUnit.MILLISECONDS)
                        .setInputData(builder.build())
                        .build();

        WorkManager.getInstance(getApplicationContext())
                .enqueueUniqueWork("ServisAdzanOnStartCommand", ExistingWorkPolicy.REPLACE, workRequest);

        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
        stopSelf();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(@Nullable Intent rootIntent) {
        Player player = mediaSession.getPlayer();
        if (!player.getPlayWhenReady()
                || player.getMediaItemCount() == 0
                || player.getPlaybackState() == Player.STATE_ENDED) {

            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ServisAdzan::onTaskRemoved");

            wakeLock.acquire(600000);
            Data.Builder builder = new Data.Builder();
            builder.putBoolean("fromBooting", fromBooting);
            OneTimeWorkRequest workRequest =
                    new OneTimeWorkRequest.Builder(LocationWorker.class)
                            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                            .setBackoffCriteria(
                                    BackoffPolicy.LINEAR,
                                    OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                                    TimeUnit.MILLISECONDS)
                            .setInputData(builder.build())
                            .build();

            WorkManager.getInstance(getApplicationContext())
                    .enqueueUniqueWork("ServisAdzanOnTaskRemoved", ExistingWorkPolicy.REPLACE, workRequest);
            if (wakeLock.isHeld()) {
                wakeLock.release();
            }

            super.onTaskRemoved(rootIntent);
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ServisAdzan::onTaskRemoved");

        wakeLock.acquire(600000);
        Data.Builder builder = new Data.Builder();
        builder.putBoolean("fromBooting", fromBooting);
        OneTimeWorkRequest workRequest =
                new OneTimeWorkRequest.Builder(LocationWorker.class)
                        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        .setBackoffCriteria(
                                BackoffPolicy.LINEAR,
                                OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                                TimeUnit.MILLISECONDS)
                        .setInputData(builder.build())
                        .build();

        WorkManager.getInstance(getApplicationContext())
                .enqueueUniqueWork("ServisAdzanOnTaskRemoved", ExistingWorkPolicy.REPLACE, workRequest);
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }

        if (mediaSession != null) {
            mediaSession.getPlayer().release();
            mediaSession.release();
            mediaSession = null;
        }
        super.onDestroy();
    }
}