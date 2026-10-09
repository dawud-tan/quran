package com.quran.kiblat.salat.alarm;

import android.app.Service;

/**
 * Alarm tepat waktu adzan → {@link ServisAdzan}.
 */
public class SiaranNotifikasiAdzan extends SiaranAlarm {
    @Override
    protected Class<? extends Service> servis() {
        return ServisAdzan.class;
    }
}
