package com.quran.kiblat.salat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SiaranDismiss10Menit extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // lewat servisnya, bukan stopService, supaya volume alarm sempat
        // dikembalikan selagi servisnya masih di latar depan
        Servis10Menit.matikan(context);
    }
}