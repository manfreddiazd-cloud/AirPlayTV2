#include <jni.h>

extern "C" JNIEXPORT jstring JNICALL
Java_com_airplaytv_receiver_NativeBridge_nativeVersion(JNIEnv *env, jobject /* this */) {
    return env->NewStringUTF("airplaytv-native 0.1 (protocolo AirPlay pendiente)");
}
