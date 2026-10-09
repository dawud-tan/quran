package com.quran.kiblat.salat.umum;

import android.os.Handler;
import android.os.Looper;

import androidx.core.os.HandlerCompat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Utas latar bersama untuk seluruh proses: dua utas pekerja dan satu
 * pengantar ke utas utama.
 * <p>
 * Dipakai untuk pencarian alamat (jaringan) dan pemindaian bayangan kiblat
 * (ribuan hitungan SPA). Jangan membuat kolam utas baru untuk kerja sekecil
 * itu; pakai yang ini.
 */
public final class Latar {

    private static final ExecutorService PEKERJA = Executors.newFixedThreadPool(2);
    private static final Handler UTAMA = HandlerCompat.createAsync(Looper.getMainLooper());

    private Latar() {
    }

    /**
     * Mengerjakan {@code kerja} di utas latar, lalu menyerahkan hasilnya ke
     * {@code selesai} di utas utama.
     */
    public static <T> void kerjakan(Supplier<T> kerja, Consumer<T> selesai) {
        PEKERJA.execute(() -> {
            T hasil = kerja.get();
            UTAMA.post(() -> selesai.accept(hasil));
        });
    }

    /**
     * Mengerjakan {@code kerja} di utas latar tanpa menunggu hasil.
     */
    public static void jalankan(Runnable kerja) {
        PEKERJA.execute(kerja);
    }

    /**
     * Mengerjakan {@code kerja} di utas utama.
     */
    public static void keUtama(Runnable kerja) {
        UTAMA.post(kerja);
    }
}
