package com.quran.kiblat.salat.alarm;

import android.app.Service;

/**
 * Alarm 10 menit sebelum adzan → {@link Servis10Menit}.
 */
public class SiaranSepuluhMenitLalu extends SiaranAlarm {
    @Override
    protected Class<? extends Service> servis() {
        return Servis10Menit.class;
    }
}
