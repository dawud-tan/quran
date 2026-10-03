#include "nada_t3.h"

#include <android/log.h>

#include <algorithm>
#include <cmath>
#include <string>

#define TAG_LOG "NadaT3"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG_LOG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG_LOG, __VA_ARGS__)

NadaT3::~NadaT3() {
    berhenti();
}

// ---------------------------------------------------------------------------
// mulai()
// ---------------------------------------------------------------------------

int64_t NadaT3::mulai(int32_t putaran) {
    std::lock_guard<std::mutex> jaga(mKunci);

    const bool tanpaBatas = putaran <= 0;

    // Membangun ulang berarti aliran baru; laju cupliknya bisa saja beda.
    tutupAliran();

    oboe::AudioStreamBuilder pembangun;
    pembangun.setDirection(oboe::Direction::Output)
            ->setPerformanceMode(oboe::PerformanceMode::LowLatency)
            ->setSharingMode(oboe::SharingMode::Exclusive)
            ->setFormat(oboe::AudioFormat::Float)
                    // Menjamin panggilbalik selalu melihat float, sekalipun
                    // format asli perangkatnya I16; Oboe yang menyisipkan
                    // penyesuaiannya.
            ->setFormatConversionAllowed(true)
            ->setChannelCount(oboe::ChannelCount::Stereo)
                    // Wajib: tanpa ini nada keluar di jalur media, bukan alarm,
                    // sehingga volume alarm yang sudah dinaikkan Servis10Menit
                    // ke maksimum tidak berpengaruh sama sekali — dan nada
                    // tetap bisu kalau pengguna sedang mengheningkan media.
            ->setUsage(oboe::Usage::Alarm)
            ->setContentType(oboe::ContentType::Sonification)
            ->setDataCallback(this)
            ->setErrorCallback(this);

    oboe::Result hasil = pembangun.openStream(mAliran);
    if (hasil != oboe::Result::OK) {
        LOGE("openStream gagal: %s", oboe::convertToText(hasil));
        mAliran.reset();
        return -1;
    }

    // Semua di bawah ini diturunkan dari apa yang benar-benar diberikan Oboe,
    // bukan dari apa yang diminta.
    const int32_t lajuCuplik = mAliran->getSampleRate();
    mKanal = mAliran->getChannelCount();

    if (lajuCuplik <= 0 || mKanal <= 0) {
        LOGE("Setelan aliran tidak masuk akal: %d Hz, %d kanal", lajuCuplik, mKanal);
        tutupAliran();
        return -1;
    }

    mLetupan = gelombang::bangunLetupan(lajuCuplik);
    if (mLetupan.empty()) {
        LOGE("Laju cuplik %d Hz terlalu rendah untuk %lld Hz",
             lajuCuplik, static_cast<long long>(gelombang::FREKUENSI));
        tutupAliran();
        return -1;
    }

    const auto bingkaiNyala = static_cast<int64_t>(mLetupan.size());
    int64_t totalBingkai = 0;
    for (std::size_t i = 0; i < JUMLAH_LANGKAH; ++i) {
        // Langkah nyala memakai penyangga letupan apa adanya, jadi panjangnya
        // harus persis sama — itu yang membuat mPosisiLangkah bisa dipakai
        // langsung sebagai indeks ke mLetupan di panggilbalik.
        mBingkaiLangkah[i] = gelombang::POLA[i].nyala
                             ? bingkaiNyala
                             : std::max<int64_t>(
                        std::llround(gelombang::POLA[i].detik * lajuCuplik), 1);
        totalBingkai += mBingkaiLangkah[i];
    }

    // Penyangga dua kali ledakan: tetap latensi rendah, tapi masih tahan satu
    // penjadwalan yang meleset. Alarm ini kerap berbunyi tepat saat perangkat
    // baru bangun dari Doze, jadi marjin itu ada gunanya.
    const int32_t ledakan = mAliran->getFramesPerBurst();
    if (ledakan > 0) {
        mAliran->setBufferSizeInFrames(ledakan * 2);
    }

    // Menyetel ulang keadaan pemutaran sebelum panggilbalik mungkin berjalan.
    mIndeksLangkah = 0;
    mPosisiLangkah = 0;
    mSisaPutaran = tanpaBatas ? TANPA_BATAS : putaran;
    mPolaAktif.store(true, std::memory_order_release);

    hasil = mAliran->requestStart();
    if (hasil != oboe::Result::OK) {
        LOGE("requestStart gagal: %s", oboe::convertToText(hasil));
        tutupAliran();
        return -1;
    }

    LOGI("Mulai: %lld Hz, %s putaran, aliran %d Hz %d kanal, letupan %lld bingkai",
         static_cast<long long>(gelombang::FREKUENSI),
         tanpaBatas ? "tanpa batas" : std::to_string(putaran).c_str(),
         lajuCuplik, mKanal, static_cast<long long>(bingkaiNyala));

    if (tanpaBatas) {
        return 0;
    }
    // Dibulatkan ke atas supaya penjadwal di sisi Java tidak pernah mendahului
    // bingkai terakhir.
    return (totalBingkai * putaran * 1000 + lajuCuplik - 1) / lajuCuplik;
}

// ---------------------------------------------------------------------------
// berhenti() / tutupAliran()
// ---------------------------------------------------------------------------

void NadaT3::berhenti() {
    std::lock_guard<std::mutex> jaga(mKunci);
    tutupAliran();
}

void NadaT3::tutupAliran() {
    mPolaAktif.store(false, std::memory_order_release);

    if (mAliran) {
        // Keduanya menunggu sampai panggilbalik yang sedang berjalan selesai;
        // itulah yang membuat keadaan utas audio aman disentuh sesudahnya.
        mAliran->stop();
        mAliran->close();
        mAliran.reset();
    }
}

// ---------------------------------------------------------------------------
// onErrorAfterClose()
// ---------------------------------------------------------------------------

void NadaT3::onErrorAfterClose(oboe::AudioStream * /*aliran*/, oboe::Result galat) {
    LOGE("Aliran ditutup sistem: %s", oboe::convertToText(galat));

    // Oboe sudah membubarkan alirannya dan memanggil ini dari utasnya sendiri.
    // Sengaja tanpa kunci dan tanpa menyentuh mAliran: berhenti() yang berjalan
    // bersamaan bisa sedang tertahan di dalam AudioStream::stop() sambil
    // memegang mKunci, dan meraihnya di sini akan saling mengunci. mulai()
    // atau berhenti() berikutnya yang akan menyetel ulang penunjuknya.
    mPolaAktif.store(false, std::memory_order_release);
}

// ---------------------------------------------------------------------------
// onAudioReady() — utas waktu-nyata. Tanpa kunci, tanpa alokasi, tanpa log.
// ---------------------------------------------------------------------------

oboe::DataCallbackResult NadaT3::onAudioReady(oboe::AudioStream * /*aliran*/,
                                              void *dataAudio,
                                              int32_t jumlahBingkai) {
    auto *keluar = static_cast<float *>(dataAudio);
    const int32_t kanal = mKanal;
    const auto total = static_cast<std::size_t>(jumlahBingkai) * kanal;

    if (!mPolaAktif.load(std::memory_order_acquire)) {
        std::fill_n(keluar, total, 0.0f);
        return oboe::DataCallbackResult::Continue;
    }

    // Dikerjakan per potongan, bukan per bingkai: satu potongan adalah sisa
    // langkah yang sedang berjalan, dan isinya cuma salinan lurus dari
    // mLetupan atau nol. Tidak ada aritmetika bentuk gelombang di sini.
    int32_t ditulis = 0;
    while (ditulis < jumlahBingkai) {
        const int64_t sisaLangkah = mBingkaiLangkah[mIndeksLangkah] - mPosisiLangkah;
        const auto n = static_cast<int32_t>(
                std::min<int64_t>(jumlahBingkai - ditulis, sisaLangkah));
        float *blok = keluar + static_cast<std::size_t>(ditulis) * kanal;

        if (gelombang::POLA[mIndeksLangkah].nyala) {
            const float *sumber = mLetupan.data() + mPosisiLangkah;
            if (kanal == 1) {
                std::copy_n(sumber, n, blok);
            } else {
                for (int32_t i = 0; i < n; ++i) {
                    const float cuplikan = sumber[i];
                    float *bingkai = blok + static_cast<std::size_t>(i) * kanal;
                    for (int32_t c = 0; c < kanal; ++c) {
                        bingkai[c] = cuplikan;
                    }
                }
            }
        } else {
            std::fill_n(blok, static_cast<std::size_t>(n) * kanal, 0.0f);
        }

        ditulis += n;
        mPosisiLangkah += n;

        if (mPosisiLangkah >= mBingkaiLangkah[mIndeksLangkah]) {
            mPosisiLangkah = 0;
            if (++mIndeksLangkah >= JUMLAH_LANGKAH) {
                mIndeksLangkah = 0;
                // TANPA_BATAS tidak pernah dikurangi, jadi pola terus berulang
                // sampai berhenti() menutup alirannya.
                if (mSisaPutaran > 0 && --mSisaPutaran == 0) {
                    std::fill_n(keluar + static_cast<std::size_t>(ditulis) * kanal,
                                (static_cast<std::size_t>(jumlahBingkai) - ditulis) * kanal,
                                0.0f);
                    mPolaAktif.store(false, std::memory_order_release);
                    // Satu-satunya cara yang didukung untuk berhenti dari utas
                    // audio. Memanggil AudioStream::stop() di sini akan saling
                    // mengunci.
                    return oboe::DataCallbackResult::Stop;
                }
            }
        }
    }

    return oboe::DataCallbackResult::Continue;
}
