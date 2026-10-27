package com.airplaytv.receiver

import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

/**
 * Servidor TCP en el puerto AirPlay. Por ahora solo registra la primera
 * peticion que envia el iPhone (diagnostico). Aqui se enchufara el protocolo
 * real (pair-setup, fp-setup, stream de espejo).
 */
class ConnectionListener(
    private val port: Int,
    private val onLog: (String) -> Unit
) {
    @Volatile private var running = false
    private var server: ServerSocket? = null

    fun start() {
        running = true
        thread(name = "airplay-listener", isDaemon = true) {
            try {
                server = ServerSocket(port).also { s ->
                    onLog("Escuchando en el puerto $port")
                    while (running) {
                        val client = s.accept()
                        handle(client)
                    }
                }
            } catch (e: Exception) {
                if (running) onLog("Error del servidor: ${e.message}")
            }
        }
    }

    private fun handle(client: Socket) {
        thread(isDaemon = true) {
            try {
                client.soTimeout = 3000
                val ip = client.inetAddress.hostAddress
                val line = client.getInputStream().bufferedReader().readLine() ?: "(vacio)"
                onLog("iPhone $ip -> $line")
            } catch (e: Exception) {
                onLog("Conexion cerrada: ${e.message}")
            } finally {
                runCatching { client.close() }
            }
        }
    }

    fun stop() {
        running = false
        runCatching { server?.close() }
    }
}
