package nie.translator.rtranslator.standby

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.blankj.utilcode.util.LogUtils

// StandbyManager.kt
object StandbyManager {
    private const val STANDBY_DELAY_MS = 120_000L // 1 minute
    private var handler: Handler? = null
    private var standbyRunnable: Runnable? = null
    private var isStandbyVisible = false

    fun start(context: Context) {
        if (handler == null) {
            handler = Handler(Looper.getMainLooper())
        }

        standbyRunnable = Runnable {
            LogUtils.d("StandbyManager", "standbyRunnable showStandby")
            showStandby(context)
        }

        resetTimer(context)
    }

    fun resetTimer(context: Context) {
        handler?.removeCallbacks(standbyRunnable!!)
        if (isStandbyVisible) {
            hideStandby(context)
        }
        handler?.postDelayed(standbyRunnable!!, STANDBY_DELAY_MS)
    }

    private fun showStandby(context: Context) {
        if (!isStandbyVisible) {
            isStandbyVisible = true
            StandbyWindow.show(context){
                resetTimer(context) // 隐藏后自动开始计时
            }
        }
    }

    private fun hideStandby(context: Context) {
        if (isStandbyVisible) {
            LogUtils.d("StandbyManager", "hideStandby")
            isStandbyVisible = false
            StandbyWindow.hide()
            resetTimer(context) // <- 添加此行实现隐藏后自动开始倒计时
        }
    }

    fun hideOrReset(context: Context) {
        if (isStandbyVisible) {
            hideStandby(context)
        } else {
            resetTimer(context)
        }
    }
}

