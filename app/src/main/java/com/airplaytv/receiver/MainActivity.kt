package com.airplaytv.receiver

import android.content.Context
import android.content.res.ColorStateList
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.net.Inet4Address
import java.net.NetworkInterface

class MainActivity : AppCompatActivity(), SurfaceHolder.Callback {

    companion object {
        const val PORT = 7000
        const val DEVICE_NAME = "TV AirPlay"
    }

    private enum class State { STARTING, VISIBLE, ATTEMPT, ERROR }

    private lateinit var home: View
    private lateinit var statusDot: View
    private lateinit var statusText: TextView
    private lateinit var networkText: TextView
    private lateinit var activityText: TextView

    private var advertiser: AirPlayAdvertiser? = null
    private var listener: ConnectionListener? = null
    private var multicastLock: WifiManager.MulticastLock? = null
    private val log = ArrayDeque<String>()
    private val decoder = VideoDecoder()
    private var state = State.STARTING

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)

        home = findViewById(R.id.home)
        statusDot = findViewById(R.id.statusDot)
        statusText = findViewById(R.id.statusText)
        networkText = findViewById(R.id.networkText)
        activityText = findViewById(R.id.activityText)
        findViewById<TextView>(R.id.deviceNameText).text = "Elige «$DEVICE_NAME» en la lista de dispositivos"
        findViewById<SurfaceView>(R.id.surface).holder.addCallback(this)
        setState(State.STARTING)
    }

    override fun onStart() {
        super.onStart()
        val wifi = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        multicastLock = wifi.createMulticastLock("airplaytv").apply {
            setReferenceCounted(false)
            acquire()
        }

        val ip = localIp()
        networkText.text = if (ip != null) "Red local: $ip" else "Sin red: conecta el Chromecast a Wi‑Fi"
        if (ip == null) setState(State.ERROR)

        listener = ConnectionListener(PORT) { onEvent(it) }.also { it.start() }
        advertiser = AirPlayAdvertiser(this, DEVICE_NAME, PORT) { onEvent(it) }.also { it.start() }
        addLog("Núcleo: " + NativeBridge.nativeVersion())
    }

    override fun onStop() {
        advertiser?.stop()
        listener?.stop()
        multicastLock?.let { if (it.isHeld) it.release() }
        decoder.stop()
        super.onStop()
    }

    private fun onEvent(msg: String) = runOnUiThread {
        addLog(msg)
        when {
            msg.startsWith("iPhone") -> setState(State.ATTEMPT)
            msg.contains("fallo", ignoreCase = true) -> setState(State.ERROR)
            msg.contains("registrado") -> if (state == State.STARTING) setState(State.VISIBLE)
            else -> Unit
        }
    }

    private fun setState(s: State) {
        state = s
        val (text, color) = when (s) {
            State.STARTING -> "Iniciando…" to 0xFFFFC107.toInt()
            State.VISIBLE -> "Visible en tu red, esperando al iPhone" to 0xFF4CAF50.toInt()
            State.ATTEMPT -> "iPhone detectado" to 0xFF2D7DFF.toInt()
            State.ERROR -> "Problema de red" to 0xFFF44336.toInt()
        }
        statusText.text = text
        statusDot.backgroundTintList = ColorStateList.valueOf(color)
    }

    private fun addLog(msg: String) {
        log.addLast(msg)
        while (log.size > 3) log.removeFirst()
        activityText.text = log.joinToString("\n")
    }

    /** Muestra u oculta la pantalla de inicio (para cuando llegue video real). */
    fun showVideo(visible: Boolean) {
        home.visibility = if (visible) View.GONE else View.VISIBLE
        activityText.visibility = if (visible) View.GONE else View.VISIBLE
    }

    private fun localIp(): String? = runCatching {
        NetworkInterface.getNetworkInterfaces().toList()
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { !it.isLoopbackAddress && it is Inet4Address }
            ?.hostAddress
    }.getOrNull()

    override fun surfaceCreated(holder: SurfaceHolder) {}
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
    override fun surfaceDestroyed(holder: SurfaceHolder) { decoder.stop() }
}
