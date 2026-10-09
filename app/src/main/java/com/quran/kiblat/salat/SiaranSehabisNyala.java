package com.quran.kiblat.salat;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Memasang ulang alarm sesudah ponsel dinyalakan, dan begitu pengguna
 * memberi izin alarm tepat waktu — tanpa harus membuka aplikasinya dulu.
 * <p>
 * Dikerjakan langsung di sini: lokasinya cuma dibaca dari tembolok dan
 * setAlarmClock dipanggil sebelum alamatnya dicari, jadi tidak ada yang perlu
 * ditunda ke WorkManager.
 */
public class SiaranSehabisNyala extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String aksi = intent == null ? null : intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(aksi)) {
            Util.segarkanJadwal(context, true, true);
        } else if (AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED.equals(aksi)
                && Izin.alarmTepat(context)) {
            Util.segarkanJadwal(context, true, false);
        }
    }
}
