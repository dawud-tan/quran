package com.quran.kiblat.salat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.work.BackoffPolicy;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class SiaranSehabisNyala extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && intent.getAction() != null && intent.getAction().equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED)) {

            Data.Builder builder = new Data.Builder();
            builder.putBoolean("fromBooting", true);
            OneTimeWorkRequest workRequest =
                    new OneTimeWorkRequest.Builder(LocationWorker.class)
                            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                            .setBackoffCriteria(
                                    BackoffPolicy.LINEAR,
                                    OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                                    TimeUnit.MILLISECONDS)
                            .setInputData(builder.build())
                            .build();

            WorkManager.getInstance(context)
                    .enqueueUniqueWork("LocationWork", ExistingWorkPolicy.REPLACE, workRequest);
        }
    }
}