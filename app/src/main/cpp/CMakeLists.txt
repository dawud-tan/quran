cmake_minimum_required(VERSION 4.1.2)

project(nada_t3 LANGUAGES CXX)

# Oboe datang dari AAR com.google.oboe:oboe sebagai paket prefab, jadi
# android.buildFeatures.prefab di app/build.gradle harus menyala — tanpa itu
# find_package di bawah ini gagal dengan "Could not find a package
# configuration file provided by oboe".
find_package(oboe REQUIRED CONFIG)

add_library(nada_t3 SHARED
        gelombang_t3.cpp
        nada_t3.cpp
        jni_nada_t3.cpp)

target_compile_features(nada_t3 PRIVATE cxx_std_17)
target_compile_options(nada_t3 PRIVATE -Wall -Wextra)

# Sengaja tanpa -ffast-math: deret Fourier di gelombang_t3.cpp mengandalkan
# penjumlahan terkompensasi, yang justru dibuang oleh pengoptimalan itu.

target_link_libraries(nada_t3 PRIVATE
        oboe::oboe
        log)