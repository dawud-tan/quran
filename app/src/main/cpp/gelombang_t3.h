#pragma once

#include <array>
#include <cstdint>
#include <vector>

/**
 * Gelombang isyarat T3 — murni hitungan, tanpa Oboe dan tanpa Android, jadi
 * berkas ini bisa dikompilasi dan dijalankan dengan g++ biasa di luar Gradle
 * untuk memeriksa bentuk gelombangnya secara angka (lihat .claude/agents/
 * build-verifier.md). Itu satu-satunya cara memverifikasi nada tanpa perangkat,
 * persis seperti PrayTime/SolarPosition untuk hisab waktu salat.
 *
 * Nada yang dibangkitkan menggantikan res/raw/t3_441khz.ogg dan t3_48khz.ogg.
 */
namespace gelombang {

/**
 * Frekuensi dasar, dalam hertz. Sengaja bilangan bulat: seluruh ketelitian
 * pembangkit ini bersandar pada 520/lajuCuplik yang bisa disederhanakan
 * menjadi pecahan bulat p/q.
 *
 * NFPA 72 memilih 520 Hz karena nada rendah jauh lebih ampuh membangunkan
 * orang tidur daripada nada tinggi.
 */
    inline constexpr int64_t FREKUENSI = 520;

/**
 * Amplitudo puncak gelombang kotak ideal, sebelum riak Gibbs.
 *
 * 0,5 adalah amplitudo bawaan GenerateSquareWave.java-nya Nayuki: di sana
 * koefisiennya sin(k*d*pi)*2/(k*pi) dan tidak ada penskalaan apa pun
 * sesudahnya, jadi gelombangnya berayun antara +0,5 dan -0,5. Nilai di sini
 * dinyatakan sebagai amplitudo puncak, dan 0,5 mengembalikan keluaran Nayuki
 * persis.
 *
 * Catatan kenyaringan: berkas ogg lama terukur RMS 0,78, yang ini 0,495 —
 * sekitar 4 dB lebih pelan. Volume alarm tetap dipaksa maksimum oleh
 * Servis10Menit, jadi kalau di perangkat terasa kurang keras, naikkan angka
 * ini; itu satu-satunya yang perlu diubah. Puncaknya 1,179 x amplitudo
 * (riak Gibbs), jadi 0,84 adalah batas sebelum terpotong.
 */
    inline constexpr double AMPLITUDO = 0.5;

/// Lama satu letupan nyala, dalam detik.
    inline constexpr double DETIK_NYALA = 0.5;

/**
 * Peredam tepi letupan, dalam detik.
 *
 * Deret kosinus Nayuki berangkat dari puncak dataran, bukan dari titik silang
 * nol, jadi tanpa peredam gerbangnya memotong di amplitudo penuh dan berbunyi
 * 'klik' di pengeras suara — persis seperti berkas ogg lama. 5 milidetik
 * kosinus terangkat menghabiskannya, dan itu 1% dari letupan 0,5 detik, jadi
 * bentuk isyaratnya tidak berubah.
 */
    inline constexpr double DETIK_REDAM = 0.005;

/// Satu langkah pola: menyala atau diam, sekian detik.
    struct Langkah {
        bool nyala;
        double detik;
    };

/**
 * Pola isyarat T3 (ISO 8201 / NFPA 72): tiga letupan pendek lalu jeda
 * panjang, tepat 4 detik satu putaran. Ubah iramanya di sini dan tidak di
 * tempat lain.
 *
 * Servis10Menit memutarnya 45 kali, jadi 45 x 4 detik = 3 menit pas.
 */
    inline constexpr std::array<Langkah, 6> POLA{{
                                                         {true, 0.5}, {false, 0.5},
                                                         {true, 0.5}, {false, 0.5},
                                                         {true, 0.5}, {false, 1.5}
                                                 }};

/// Jumlah bingkai satu letupan nyala pada laju cuplik tertentu.
    int64_t bingkaiNyala(int32_t lajuCuplik);

/**
 * Membangun satu letupan 0,5 detik gelombang kotak 520 Hz pita-terbatas,
 * lengkap dengan peredam tepi, siap disalin apa adanya ke penyangga keluaran.
 *
 * Seluruh biaya sintesis dibayar di sini, sekali, di luar utas waktu-nyata.
 * Panggilbalik audio tinggal menyalin — tidak ada satu pun sinus, kali, atau
 * cabang per bingkai di sana.
 *
 * @return penyangga sepanjang bingkaiNyala(lajuCuplik), atau kosong kalau
 *         laju cupliknya tidak masuk akal (<= 2 x 520 Hz).
 */
    std::vector<float> bangunLetupan(int32_t lajuCuplik);

}  // namespace gelombang