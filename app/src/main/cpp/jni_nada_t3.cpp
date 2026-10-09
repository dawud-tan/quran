#include <jni.h>

#include "nada_t3.h"

/*
 * Jembatan ke com.quran.kiblat.salat.NadaT3.
 *
 * Penunjuk mesinnya dibawa bolak-balik sebagai jlong, bukan disimpan di ladang
 * objek Java, jadi sisi asli tidak pernah perlu mencari ladang atau memegang
 * rujukan global — dan R8 tidak punya apa pun untuk dipangkas selain nama
 * kelas dan metode asli, yang sudah dijaga proguard-rules.pro.
 */
extern "C" {

JNIEXPORT jlong JNICALL
Java_com_quran_kiblat_salat_alarm_NadaT3_buat(JNIEnv * /*env*/, jclass /*kelas*/) {
    return reinterpret_cast<jlong>(new NadaT3());
}

JNIEXPORT jlong JNICALL
Java_com_quran_kiblat_salat_alarm_NadaT3_mulai(JNIEnv * /*env*/, jclass /*kelas*/,
                                         jlong penunjuk, jint putaran) {
    if (penunjuk == 0) {
        return 0;
    }
    return reinterpret_cast<NadaT3 *>(penunjuk)->mulai(static_cast<int32_t>(putaran));
}

JNIEXPORT void JNICALL
Java_com_quran_kiblat_salat_alarm_NadaT3_berhenti(JNIEnv * /*env*/, jclass /*kelas*/,
                                            jlong penunjuk) {
    if (penunjuk != 0) {
        reinterpret_cast<NadaT3 *>(penunjuk)->berhenti();
    }
}

JNIEXPORT jboolean JNICALL
Java_com_quran_kiblat_salat_alarm_NadaT3_sedangMain(JNIEnv * /*env*/, jclass /*kelas*/,
                                              jlong penunjuk) {
    if (penunjuk == 0) {
        return JNI_FALSE;
    }
    return reinterpret_cast<NadaT3 *>(penunjuk)->sedangMain() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_com_quran_kiblat_salat_alarm_NadaT3_hapus(JNIEnv * /*env*/, jclass /*kelas*/,
                                         jlong penunjuk) {
    // Penghancurnya memanggil berhenti(), yang menunggu panggilbalik yang
    // sedang berjalan selesai sebelum alirannya ditutup.
    delete reinterpret_cast<NadaT3 *>(penunjuk);
}

}  // extern "C"
