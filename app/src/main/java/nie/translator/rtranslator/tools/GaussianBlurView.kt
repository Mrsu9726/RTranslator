package nie.translator.rtranslator.tools

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.annotation.FloatRange
import kotlin.math.min

class GaussianBlurView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var blurRadius: Float = 25f // 模糊强度，越大越模糊
    private var cornerRadius: Float = 0f // 圆角大小
    private var blurBitmap: Bitmap? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    fun setBlurRadius(@FloatRange(from = 0.1, to = 25.0) radius: Float) {
        blurRadius = radius
        invalidate()
    }

    fun setCornerRadius(radius: Float) {
        cornerRadius = radius
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (width == 0 || height == 0) return

        if (blurBitmap == null || blurBitmap?.width != width || blurBitmap?.height != height) {
            blurBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }

        val offscreenCanvas = Canvas(blurBitmap!!)
        offscreenCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        // 截取当前 View 背后的区域
        rootView?.let { root ->
            val rootBitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
            val rootCanvas = Canvas(rootBitmap)
            root.draw(rootCanvas)

            val srcRect = Rect(left, top, right, bottom)
            val dstRect = Rect(0, 0, width, height)
            offscreenCanvas.drawBitmap(rootBitmap, srcRect, dstRect, null)

            rootBitmap.recycle()
        }

        val blurred = blurBitmap?.let { fastBlur(it, blurRadius.toInt(), true) }
        blurred?.let {
            path.reset()
            path.addRoundRect(0f, 0f, width.toFloat(), height.toFloat(), cornerRadius, cornerRadius, Path.Direction.CW)
            canvas.save()
            canvas.clipPath(path)
            canvas.drawBitmap(it, 0f, 0f, paint)
            canvas.restore()
        }
    }

    // Fast blur 算法 (Stack Blur)
    private fun fastBlur(sentBitmap: Bitmap, radius: Int, canReuseInBitmap: Boolean): Bitmap? {
        val bitmap = if (canReuseInBitmap) sentBitmap else sentBitmap.copy(sentBitmap.config!!, true)

        if (radius < 1) {
            return null
        }

        val w = bitmap.width
        val h = bitmap.height
        val pix = IntArray(w * h)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)

        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1

        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        val vmin = IntArray(maxOf(w, h))

        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        val dv = IntArray(256 * div / 2 + 1)
        for (i in dv.indices) {
            dv[i] = i / div
        }

        yi = 0
        for (y in 0 until h) {
            rsum = 0; gsum = 0; bsum = 0
            for (i in -radius..radius) {
                p = pix[yi + min(wm, maxOf(i, 0))]
                rsum += (p shr 16) and 0xff
                gsum += (p shr 8) and 0xff
                bsum += p and 0xff
            }
            for (x in 0 until w) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]

                if (y == 0) vmin[x] = min(x + radius + 1, wm)
                p = pix[y * w + vmin[x]]

                rsum += ((p shr 16) and 0xff) - ((pix[yi] shr 16) and 0xff)
                gsum += ((p shr 8) and 0xff) - ((pix[yi] shr 8) and 0xff)
                bsum += (p and 0xff) - (pix[yi] and 0xff)

                yi++
            }
        }

        for (x in 0 until w) {
            rsum = 0; gsum = 0; bsum = 0; yp = -radius * w
            for (i in -radius..radius) {
                yi = maxOf(0, yp) + x
                rsum += r[yi]
                gsum += g[yi]
                bsum += b[yi]
                yp += w
            }
            yi = x
            for (y in 0 until h) {
                pix[yi] = (0xff shl 24) or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]
                if (x == 0) vmin[y] = min(y + radius + 1, hm) * w
                p = x + vmin[y]
                rsum += r[p] - r[yi]
                gsum += g[p] - g[yi]
                bsum += b[p] - b[yi]
                yi += w
            }
        }

        bitmap.setPixels(pix, 0, w, 0, 0, w, h)
        return bitmap
    }
}
