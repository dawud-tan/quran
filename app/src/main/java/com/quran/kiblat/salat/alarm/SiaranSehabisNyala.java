package com.quran.kiblat.salat.alarm;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.quran.kiblat.salat.izin.Izin;

/**
 * Memasang ulang alarm tanpa harus membuka aplikasinya dulu:
 * <ul>
 * <li>sesudah ponsel dinyalakan (alarm hilang saat ponsel mati);</li>
 * <li>begitu pengguna memberi izin alarm tepat waktu di setelan sistem;</li>
 * <li>sesudah aplikasinya diperbarui. Alarm yang dipasang versi lama tetap
 *     tertunda, tetapi kalau nama kelas penerimanya berubah alarm itu
 *     berbunyi ke komponen yang sudah tidak ada dan hilang tanpa suara.</li>
 * </ul>
 * Dikerjakan langsung di sini: lokasinya cuma dibaca dari tembolok dan
 * setAlarmClock dipanggil sebelum alamatnya dicari, jadi tidak ada yang perlu
 * ditunda ke WorkManager.
 */
public class SiaranSehabisNyala extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String aksi = intent == null ? null : intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(aksi)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(aksi)) {
            PenjadwalAdzan.segarkanJadwal(context, true);
        } else if (AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED.equals(aksi)
                && Izin.alarmTepat(context)) {
            PenjadwalAdzan.segarkanJadwal(context, true);
        }
    }
}
