package com.quran.kiblat.salat.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.quran.kiblat.salat.BuildConfig;

/**
 * Tombol "Matikan" dan notifikasi yang digeser, untuk kedua servis.
 * Menggantikan SiaranDismiss10Menit dan SiaranDismissAdzan.
 */
public class SiaranMatikan extends BroadcastReceiver {

    static final String AKSI_10_MENIT = BuildConfig.APPLICATION_ID + ".MATIKAN_NADA_10_MENIT";
    static final String AKSI_ADZAN = BuildConfig.APPLICATION_ID + ".MATIKAN_ADZAN";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (AKSI_10_MENIT.equals(intent.getAction())) {
            // lewat servisnya, bukan stopService, supaya volume alarm sempat
            // dikembalikan selagi servisnya masih di latar depan
            Servis10Menit.matikan(context);
        } else if (AKSI_ADZAN.equals(intent.getAction())) {
            ServisAdzan.matikan(context);
        }
    }
}
