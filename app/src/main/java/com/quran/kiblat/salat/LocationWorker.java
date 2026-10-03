package com.quran.kiblat.salat;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class LocationWorker extends Worker {
    private final LocationManager locationManager;
    private final SharedPreferences sharedPref;
    private final Context konteks;

    public LocationWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        sharedPref = context.getSharedPreferences("pref", Context.MODE_PRIVATE);
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        konteks = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        boolean fromBooting = getInputData().getBoolean("fromBooting", false);
        Location lokasi = Util.lokasiTerakhir(sharedPref);
        if (ContextCompat.checkSelfPermission(konteks, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(konteks, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Location smntra = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (smntra != null) {
                lokasi = smntra;
            }
            Util.simpanLokasi(sharedPref, lokasi);
        }
        Util.cekJadwal(getApplicationContext(), lokasi, fromBooting);
        return Result.success();
    }
}