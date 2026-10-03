package com.quran.kiblat.salat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SiaranNotifikasiAdzan extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String nama = intent.getStringExtra("nama");
        String waktu = intent.getStringExtra("waktu");
        String lokasitks = intent.getStringExtra("lokasi");
        boolean fromBooting = intent.getBooleanExtra("fromBooting", false);

        if (nama == null) nama = "";
        if (waktu == null) waktu = "";
        if (lokasitks == null) lokasitks = "";

        Intent lokServis = new Intent(context, ServisAdzan.class);
        lokServis.putExtra("nama", nama);
        lokServis.putExtra("waktu", waktu);
        lokServis.putExtra("lokasi", lokasitks);
        lokServis.putExtra("fromBooting", fromBooting);

        context.startForegroundService(lokServis);
    }
}