package nie.translator.rtranslator.standby

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import nie.translator.rtranslator.R
import nie.translator.rtranslator.livedata.GlobalLiveDataManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// StandbyWindow.kt
@SuppressLint("StaticFieldLeak")
// StandbyWindow.kt
object StandbyWindow {
    private var windowManager: WindowManager? = null
    private var standbyView: View? = null
    private var standbyBgIv: ImageView? = null
    private var clockHandler: Handler? = null
    private var clockRunnable: Runnable? = null
    private var onHiddenCallback: (() -> Unit)? = null

    fun show(context: Context, onHidden: (() -> Unit)? = null) {
        if (standbyView != null) return

        onHiddenCallback = onHidden
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val inflater = LayoutInflater.from(context)
        standbyView = inflater.inflate(R.layout.window_standby, null)
        standbyBgIv = standbyView?.findViewById(R.id.standby_bg_iv)
        if (GlobalLiveDataManager.is_night_theme.value == true) {
            standbyBgIv?.setImageResource(R.mipmap.standby_night_bg)
        } else {
            standbyBgIv?.setImageResource(R.mipmap.standby_bg)
        }
        // 点击隐藏逻辑
        standbyView?.setOnClickListener {
            hide()
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
            PixelFormat.TRANSLUCENT
        )

        standbyView?.translationY = -2000f
        windowManager?.addView(standbyView, params)
        standbyView?.animate()?.translationY(0f)?.setDuration(500)?.start()

        startClockUpdater(context)
    }

    fun hide() {
        standbyView?.let { view ->
            view.animate()
                ?.translationY(-2000f)
                ?.setDuration(500)
                ?.withEndAction {
                    windowManager?.removeView(view)
                    standbyView = null
                    onHiddenCallback?.invoke()
                    onHiddenCallback = null
                }?.start()
        }
        stopClockUpdater()
    }

    private fun startClockUpdater(context: Context) {
        val clockTextView = standbyView?.findViewById<TextView>(R.id.clock_tv) ?: return
        clockHandler = Handler(Looper.getMainLooper())
        clockRunnable = object : Runnable {
            override fun run() {
                val is24Hour = DateFormat.is24HourFormat(context)
                val timeFormat = if (is24Hour) "HH:mm" else "hh:mm a"
                val currentTime = SimpleDateFormat(timeFormat, Locale.getDefault()).format(Date())
                clockTextView.text = currentTime
                clockHandler?.postDelayed(this, 1000)
            }
        }
        clockHandler?.post(clockRunnable!!)
    }

    private fun stopClockUpdater() {
        clockHandler?.removeCallbacks(clockRunnable!!)
        clockHandler = null
        clockRunnable = null
    }
}



