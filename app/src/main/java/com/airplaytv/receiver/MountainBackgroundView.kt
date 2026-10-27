package com.airplaytv.receiver

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.util.AttributeSet
import android.view.View

/**
 * Fondo de cordillera (estilo Andes de Chile) dibujado por codigo, sin imagenes externas.
 * La escena se pinta en un bitmap pequeno y se escala (suaviza); en Android 12+ ademas
 * se aplica un desenfoque real.
 */
class MountainBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var scene: Bitmap? = null
    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val dst = Rect()

    init {
        if (Build.VERSION.SDK_INT >= 31) {
            setRenderEffect(RenderEffect.createBlurEffect(10f, 10f, Shader.TileMode.CLAMP))
        }
    }

    override fun onDraw(canvas: Canvas) {
        val bmp = scene ?: buildScene(SCENE_W, SCENE_H).also { scene = it }
        dst.set(0, 0, width, height)
        canvas.drawBitmap(bmp, null, dst, bitmapPaint)
    }

    private class Lcg(private var x: Long) {
        fun next(): Float {
            x = (x * 1103515245L + 12345L) and 0x7fffffffL
            return x / 2147483648f
        }
    }

    private fun buildScene(w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val wf = w.toFloat()
        val hf = h.toFloat()

        p.shader = LinearGradient(
            0f, 0f, 0f, hf,
            intArrayOf(0xFF14307A.toInt(), 0xFF5B8AC9.toInt(), 0xFFB9CBE0.toInt(), 0xFFF0C9A0.toInt()),
            floatArrayOf(0f, 0.45f, 0.70f, 1f),
            Shader.TileMode.CLAMP
        )
        c.drawRect(0f, 0f, wf, hf, p)
        p.shader = null

        layer(c, p, wf, hf, 7L, 0.62f, 0.28f, 0xFF4A6590.toInt(), 245)
        layer(c, p, wf, hf, 21L, 0.74f, 0.20f, 0xFF34507F.toInt(), 200)
        layer(c, p, wf, hf, 42L, 0.88f, 0.12f, 0xFF1B2B4E.toInt(), 0)
        return bmp
    }

    private fun layer(
        c: Canvas, p: Paint, w: Float, h: Float,
        seed: Long, base: Float, amp: Float, color: Int, snowAlpha: Int
    ) {
        val rnd = Lcg(seed)
        val n = 28
        val path = Path()
        path.moveTo(0f, h)
        for (i in 0..n) {
            val r = rnd.next()
            val height = if (i % 2 == 0) 0.55f + 0.45f * r else 0.15f + 0.30f * r
            path.lineTo(i * w / n, (base - amp * height) * h)
        }
        path.lineTo(w, h)
        path.close()

        p.style = Paint.Style.FILL
        p.color = color
        c.drawPath(path, p)

        if (snowAlpha > 0) {
            val snowTop = (base - amp * 0.95f) * h
            val snowBottom = (base - amp * 0.45f) * h
            val top = (snowAlpha shl 24) or 0x00FFFFFF
            p.shader = LinearGradient(0f, snowTop, 0f, snowBottom, top, 0x00FFFFFF, Shader.TileMode.CLAMP)
            c.save()
            c.clipPath(path)
            c.drawRect(0f, 0f, w, h, p)
            c.restore()
            p.shader = null
        }
    }

    companion object {
        private const val SCENE_W = 384
        private const val SCENE_H = 216
    }
}
