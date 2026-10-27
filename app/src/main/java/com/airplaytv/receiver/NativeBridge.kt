package com.airplaytv.receiver

/** Puente JNI hacia el nucleo C/C++ (aqui se integrara UxPlay). */
object NativeBridge {
    init {
        System.loadLibrary("airplaytv")
    }

    external fun nativeVersion(): String
}
