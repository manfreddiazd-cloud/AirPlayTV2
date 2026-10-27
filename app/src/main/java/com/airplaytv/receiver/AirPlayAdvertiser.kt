package com.airplaytv.receiver

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.provider.Settings

/**
 * Anuncia el dispositivo por mDNS/Bonjour como receptor AirPlay (_airplay._tcp)
 * y RAOP (_raop._tcp) para que el iPhone lo muestre en "Duplicar pantalla".
 */
class AirPlayAdvertiser(
    private val context: Context,
    private val name: String,
    private val port: Int,
    private val onLog: (String) -> Unit
) {
    private val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val listeners = mutableListOf<NsdManager.RegistrationListener>()

    private val deviceId: String by lazy {
        val raw = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "0"
        val hex = (raw + "000000000000").filter { it.isLetterOrDigit() }.take(12).uppercase()
        val fixed = "02" + hex.substring(2)
        fixed.chunked(2).joinToString(":")
    }

    fun start() {
        val features = "0x5A7FFFF7,0x1E"
        val servicePort: Int = port
        val deviceName: String = name

        val airplay = NsdServiceInfo()
        airplay.serviceName = deviceName
        airplay.serviceType = "_airplay._tcp"
        airplay.port = servicePort
        airplay.setAttribute("deviceid", deviceId)
        airplay.setAttribute("features", features)
        airplay.setAttribute("model", "AppleTV3,2")
        airplay.setAttribute("srcvers", "220.68")
        airplay.setAttribute("flags", "0x4")
        airplay.setAttribute("vv", "2")
        // TODO: "pk" (clave publica Ed25519, 64 hex) - lo aporta la capa nativa
        register(airplay, "AirPlay")

        val raop = NsdServiceInfo()
        raop.serviceName = deviceId.replace(":", "") + "@" + deviceName
        raop.serviceType = "_raop._tcp"
        raop.port = servicePort
        raop.setAttribute("txtvers", "1")
        raop.setAttribute("ch", "2")
        raop.setAttribute("cn", "0,1,2,3")
        raop.setAttribute("da", "true")
        raop.setAttribute("et", "0,3,5")
        raop.setAttribute("ft", features)
        raop.setAttribute("md", "0,1,2")
        raop.setAttribute("am", "AppleTV3,2")
        raop.setAttribute("pw", "false")
        raop.setAttribute("sr", "44100")
        raop.setAttribute("ss", "16")
        raop.setAttribute("sv", "false")
        raop.setAttribute("tp", "UDP")
        raop.setAttribute("vn", "65537")
        raop.setAttribute("vs", "220.68")
        raop.setAttribute("sf", "0x4")
        register(raop, "RAOP")
    }

    private fun register(info: NsdServiceInfo, label: String) {
        val l = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(i: NsdServiceInfo) {
                onLog("mDNS $label registrado: ${i.serviceName}")
            }
            override fun onRegistrationFailed(i: NsdServiceInfo, code: Int) {
                onLog("mDNS $label fallo ($code)")
            }
            override fun onServiceUnregistered(i: NsdServiceInfo) {}
            override fun onUnregistrationFailed(i: NsdServiceInfo, code: Int) {}
        }
        listeners.add(l)
        nsd.registerService(info, NsdManager.PROTOCOL_DNS_SD, l)
    }

    fun stop() {
        listeners.forEach { runCatching { nsd.unregisterService(it) } }
        listeners.clear()
    }
}
