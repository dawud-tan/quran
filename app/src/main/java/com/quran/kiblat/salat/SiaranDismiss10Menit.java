package com.quran.kiblat.salat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SiaranDismiss10Menit extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        context.stopService(new Intent(context, Servis10Menit.class));
    }
}