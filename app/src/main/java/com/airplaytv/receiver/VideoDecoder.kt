package com.airplaytv.receiver

import android.media.MediaCodec
import android.media.MediaFormat
import android.view.Surface
import java.nio.ByteBuffer

/** Decodificador H.264 por hardware que dibuja directo en la Surface. */
class VideoDecoder {
    private var codec: MediaCodec? = null

    fun start(surface: Surface, width: Int, height: Int, sps: ByteArray?, pps: ByteArray?) {
        stop()
        val mime = MediaFormat.MIMETYPE_VIDEO_AVC
        val format = MediaFormat.createVideoFormat(mime, width, height)
        sps?.let { format.setByteBuffer("csd-0", ByteBuffer.wrap(it)) }
        pps?.let { format.setByteBuffer("csd-1", ByteBuffer.wrap(it)) }
        codec = MediaCodec.createDecoderByType(mime).apply {
            configure(format, surface, null, 0)
            start()
        }
    }

    /** Recibe un paquete H.264 (formato Annex B) ya descifrado. */
    fun feed(data: ByteArray, ptsUs: Long) {
        val c = codec ?: return
        val index = c.dequeueInputBuffer(10_000)
        if (index >= 0) {
            val buf = c.getInputBuffer(index) ?: return
            buf.clear()
            buf.put(data)
            c.queueInputBuffer(index, 0, data.size, ptsUs, 0)
        }
        val info = MediaCodec.BufferInfo()
        var out = c.dequeueOutputBuffer(info, 0)
        while (out >= 0) {
            c.releaseOutputBuffer(out, true)
            out = c.dequeueOutputBuffer(info, 0)
        }
    }

    fun stop() {
        runCatching { codec?.stop() }
        runCatching { codec?.release() }
        codec = null
    }
}
