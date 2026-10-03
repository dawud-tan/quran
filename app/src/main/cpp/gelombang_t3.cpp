#include "gelombang_t3.h"

#include <algorithm>
#include <cmath>
#include <numeric>

namespace gelombang {
namespace {

/**
 * Penjumlah Neumaier: menyimpan sisa pembulatan setiap penjumlahan di
 * penampung kedua, lalu mengembalikannya di akhir.
 *
 * Deret Fourier di bawah menjumlah 23 suku yang besarnya terpaut 1:46
 * (harmonisa ke-45 cuma 1/45 suku dasar). Dijumlah lugas, suku-suku kecil
 * kehilangan bit terbawahnya. Dengan kompensasi ini hasilnya sepadan dengan
 * penjumlahan presisi tak hingga yang dibulatkan sekali — sudah mentok pada
 * apa yang bisa diwakili double, apalagi float tempat hasilnya disimpan.
 */
    class JumlahTerkompensasi {
    public:
        void tambah(double nilai) {
            const double sementara = mJumlah + nilai;
            // Suku yang lebih kecil yang kehilangan bit; cabang ini memilih
            // pengurangan mana yang eksak.
            if (std::fabs(mJumlah) >= std::fabs(nilai)) {
                mKoreksi += (mJumlah - sementara) + nilai;
            } else {
                mKoreksi += (nilai - sementara) + mJumlah;
            }
            mJumlah = sementara;
        }

        double nilai() const { return mJumlah + mKoreksi; }

    private:
        double mJumlah = 0.0;
        double mKoreksi = 0.0;
    };

/**
 * Tabel kosinus satu putaran penuh: kosinus[j] = cos(2*pi*j/q).
 *
 * Kosinus, bukan sinus, karena GenerateSquareWave.java-nya Nayuki menjumlah
 * cos(k*phi): gelombangnya berangkat dari puncak dataran di fase nol, bukan
 * dari titik silang nol. Bunyinya sama saja, tapi kalau tujuannya menyamai
 * keluaran acuan, fasenya ikut disamakan.
 *
 * Separuh atas dicerminkan dari separuh bawah, bukan dihitung ulang: hemat
 * separuh panggilan sin/cos, sudutnya tidak pernah lewat pi sehingga ulp-nya
 * kecil, dan tabelnya simetris persis seperti kosinus yang sebenarnya.
 */
    std::vector<double> bangunTabelKosinus(int64_t q) {
        std::vector<double> kosinus(static_cast<std::size_t>(q), 0.0);
        const int64_t separuh = q / 2;

        kosinus[0] = 1.0;  // cos(0), satu tepat
        for (int64_t j = 1; j <= separuh; ++j) {
            kosinus[static_cast<std::size_t>(j)] =
                    std::cos(2.0 * M_PI * static_cast<double>(j) / static_cast<double>(q));
        }
        if (q % 2 == 0) {
            kosinus[static_cast<std::size_t>(separuh)] = -1.0;  // cos(pi), -1 tepat
        }
        for (int64_t j = separuh + 1; j < q; ++j) {
            kosinus[static_cast<std::size_t>(j)] = kosinus[static_cast<std::size_t>(q - j)];
        }
        return kosinus;
    }

}  // namespace

int64_t bingkaiNyala(int32_t lajuCuplik) {
    return std::llround(DETIK_NYALA * static_cast<double>(lajuCuplik));
}

std::vector<float> bangunLetupan(int32_t lajuCuplik) {
    std::vector<float> letupan;
    if (lajuCuplik <= 0) {
        return letupan;
    }

    // Harmonisa ganjil tertinggi yang masih di bawah Nyquist: k * 520 < laju/2.
    // Memotong di sini yang membuat gelombangnya 'pita-terbatas' — tidak ada
    // satu pun komponen yang melipat balik jadi alias, beda dengan gelombang
    // kotak naif yang langsung diambil dari tanda sinus.
    const int64_t laju = lajuCuplik;
    const int64_t kMaks = (laju - 1) / (2 * FREKUENSI);
    const int64_t bingkai = bingkaiNyala(lajuCuplik);
    if (kMaks < 1 || bingkai <= 0) {
        return letupan;
    }

    // --- fase eksak ---------------------------------------------------------
    // 520/laju disederhanakan jadi p/q. Cuplikan gelombangnya karena itu
    // berulang tepat setiap q bingkai (q = 1200 pada 48 kHz, 2205 pada
    // 44,1 kHz), dan fase bingkai ke-n adalah (n*p) mod q — bilangan bulat.
    // Tidak ada akumulator pecahan yang bisa melenceng, jadi nada ke-45 sama
    // persis dengan nada pertama, bit demi bit.
    const int64_t pembagi = std::gcd(FREKUENSI, laju);
    const int64_t p = FREKUENSI / pembagi;
    const int64_t q = laju / pembagi;

    const std::vector<double> kosinus = bangunTabelKosinus(q);

    // --- deret Fourier gelombang kotak -------------------------------------
    // Bentuk yang dipakai GenerateSquareWave.java-nya Nayuki:
    //
    //   a_0   = d - 0,5
    //   a_k   = sin(k*d*pi) * 2 / (k*pi)
    //   x(phi) = a_0 + SUM_k a_k * cos(k*phi)
    //
    // Siklus kerja di sini tetap d = 0,5, dan di situ a_0 = 0 (tidak ada
    // komponen searah sama sekali) dan a_k = 0 untuk setiap k genap, sehingga
    // yang perlu dijumlah cuma harmonisa ganjil. Untuk k ganjil,
    // sin(k*pi/2) = +1, -1, +1, ... bergantian tiap dua langkah; tandanya itu
    // yang membedakan bentuk kosinus ini dari sekadar |a_k|.
    //
    // Dihitung untuk satu putaran q bingkai saja, lalu diulang; sisanya
    // salinan persis.
    std::vector<double> koefisien(static_cast<std::size_t>(kMaks) + 1, 0.0);
    for (int64_t k = 1; k <= kMaks; k += 2) {
        // 4*AMPLITUDO/(k*pi) dengan AMPLITUDO 0,5 menghasilkan 2/(k*pi),
        // yakni koefisien Nayuki persis.
        const double besar = 4.0 * AMPLITUDO / (M_PI * static_cast<double>(k));
        koefisien[static_cast<std::size_t>(k)] = ((k % 4) == 1) ? besar : -besar;
    }

    std::vector<double> satuPutaran(static_cast<std::size_t>(q), 0.0);
    const int64_t kTertinggi = (kMaks % 2 == 0) ? kMaks - 1 : kMaks;
    int64_t fase = 0;
    for (int64_t n = 0; n < q; ++n) {
        JumlahTerkompensasi jumlah;
        // Dari harmonisa tertinggi turun ke terendah: suku terkecil masuk
        // duluan, jadi tidak ada yang tenggelam oleh suku dasar.
        for (int64_t k = kTertinggi; k >= 1; k -= 2) {
            // (k*fase) mod q tetap bilangan bulat, jadi sudut setiap harmonisa
            // juga eksak — tidak ada galat pengecilan rentang di libm.
            const auto indeks = static_cast<std::size_t>((k * fase) % q);
            jumlah.tambah(koefisien[static_cast<std::size_t>(k)] * kosinus[indeks]);
        }
        satuPutaran[static_cast<std::size_t>(n)] = jumlah.nilai();

        fase += p;              // p < q selalu, karena laju > 520
        if (fase >= q) {
            fase -= q;
        }
    }

    // --- tebarkan sepanjang letupan, sekalian diredam tepinya ---------------
    int64_t bingkaiRedam = std::llround(DETIK_REDAM * static_cast<double>(lajuCuplik));
    bingkaiRedam = std::clamp<int64_t>(bingkaiRedam, 0, bingkai / 2);

    letupan.resize(static_cast<std::size_t>(bingkai));
    int64_t indeksPutaran = 0;
    for (int64_t n = 0; n < bingkai; ++n) {
        double nilai = satuPutaran[static_cast<std::size_t>(indeksPutaran)];

        if (bingkaiRedam > 0) {
            const int64_t jarak = std::min(n, bingkai - 1 - n);
            if (jarak < bingkaiRedam) {
                nilai *= 0.5 * (1.0 - std::cos(M_PI * static_cast<double>(jarak)
                                               / static_cast<double>(bingkaiRedam)));
            }
        }

        // Satu-satunya pembulatan yang tersisa, setengah ulp float.
        letupan[static_cast<std::size_t>(n)] = static_cast<float>(nilai);

        if (++indeksPutaran >= q) {
            indeksPutaran = 0;
        }
    }
    return letupan;
}

}  // namespace gelombang
