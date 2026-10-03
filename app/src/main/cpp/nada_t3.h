#pragma once

#include <oboe/Oboe.h>

#include <array>
#include <atomic>
#include <cstdint>
#include <memory>
#include <mutex>
#include <vector>

#include "gelombang_t3.h"

/**
 * Pemutar nada isyarat T3 lewat Oboe: gelombang kotak 520 Hz pita-terbatas
 * dengan pola gelombang::POLA, menggantikan res/raw/t3_*.ogg.
 *
 * Seluruh sintesis dikerjakan di mulai(), di utas pemanggil. Penyangga
 * keluaran hanya menyalin dari letupan yang sudah jadi, per potongan, jadi
 * panggilbalik waktu-nyata tidak pernah mengunci, mengalokasi, menghitung
 * sinus, atau bahkan bercabang per bingkai.
 */
class NadaT3 : public oboe::AudioStreamDataCallback,
               public oboe::AudioStreamErrorCallback {
public:
    NadaT3() = default;

    ~NadaT3() override;

    // Oboe memegang penunjuk `this` mentah selama aliran hidup, jadi objek ini
    // tidak boleh dipindah atau disalin.
    NadaT3(const NadaT3 &) = delete;

    NadaT3 &operator=(const NadaT3 &) = delete;

    NadaT3(NadaT3 &&) = delete;

    NadaT3 &operator=(NadaT3 &&) = delete;

    /**
     * Membuka aliran dan memulai pola.
     *
     * @param putaran berapa kali pola diulang; <= 0 berarti terus-menerus
     *                sampai berhenti() dipanggil.
     * @return  > 0  lama seluruh pemutaran dalam milidetik (dibulatkan ke
     *               atas). Servis10Menit memakainya untuk menjadwalkan apa
     *               yang terjadi sesudah nada habis — tidak ada panggilbalik
     *               JNI dari utas audio.
     *          == 0 berhasil, tapi tanpa batas: tidak ada apa pun untuk
     *               dijadwalkan.
     *          < 0  alirannya gagal dibuka.
     */
    int64_t mulai(int32_t putaran);

    /// Menghentikan dan menutup aliran. Aman dipanggil saat sudah berhenti.
    void berhenti();

    /// true selama pola belum habis dan alirannya masih hidup.
    bool sedangMain() const { return mPolaAktif.load(std::memory_order_acquire); }

    oboe::DataCallbackResult onAudioReady(oboe::AudioStream *aliran,
                                          void *dataAudio,
                                          int32_t jumlahBingkai) override;

    /**
     * Oboe sudah membubarkan alirannya sendiri sebelum ini dipanggil, biasanya
     * karena perangkat keluarannya hilang (headset dicabut, Bluetooth putus).
     */
    void onErrorAfterClose(oboe::AudioStream *aliran, oboe::Result galat) override;

private:
    static constexpr std::size_t JUMLAH_LANGKAH = gelombang::POLA.size();

    /// Menutup aliran. Pemanggil harus sedang memegang mKunci.
    void tutupAliran();

    // ---- utas pengendali; dijaga mKunci ------------------------------------

    std::mutex mKunci;
    std::shared_ptr<oboe::AudioStream> mAliran;

    // ---- ditulis sebelum requestStart(), sesudah itu hanya dibaca ----------

    int32_t mKanal = 2;

    /// Satu letupan nyala 0,5 detik, lengkap dengan peredam tepi. Setiap
    /// langkah nyala panjangnya sama, jadi penyangga ini dipakai ulang.
    std::vector<float> mLetupan;

    std::array<int64_t, JUMLAH_LANGKAH> mBingkaiLangkah{};

    // ---- utas audio --------------------------------------------------------

    /// Dimatikan panggilbalik begitu putaran terakhir habis, dan dibaca
    /// Servis10Menit lewat sedangMain().
    std::atomic<bool> mPolaAktif{false};

    std::size_t mIndeksLangkah = 0;
    int64_t mPosisiLangkah = 0;
    /// Sisa putaran, atau TANPA_BATAS untuk berbunyi terus.
    int32_t mSisaPutaran = 0;

    static constexpr int32_t TANPA_BATAS = -1;
};
