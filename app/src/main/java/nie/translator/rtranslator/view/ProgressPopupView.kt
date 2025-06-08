package nie.translator.rtranslator.view

import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import com.lxj.xpopup.core.CenterPopupView
import nie.translator.rtranslator.R

class ProgressPopupView(context: Context) : CenterPopupView(context) {
    private var title: String = "下载进度"
    private var progress = 0

    private lateinit var tvTitle: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvPercent: TextView
    private lateinit var btnConfirm: Button

    override fun getImplLayoutId(): Int = R.layout.popup_progress

    override fun onCreate() {
        super.onCreate()
        tvTitle = findViewById(R.id.tvTitle)
        progressBar = findViewById(R.id.progressBar)
        tvPercent = findViewById(R.id.tvPercent)
        btnConfirm = findViewById(R.id.btnConfirm)

        tvTitle.text = title
        setProgress(progress)

        btnConfirm.setOnClickListener { dismiss() }
    }

    fun setTitle(title: String) {
        this.title = title
        if (this::tvTitle.isInitialized) tvTitle.text = title
    }

    fun setProgress(progress: Int) {
        this.progress = progress
        if (this::progressBar.isInitialized && this::tvPercent.isInitialized && this::btnConfirm.isInitialized) {
            progressBar.progress = progress
            tvPercent.text = "$progress%"
            if (progress >= 100) {
                btnConfirm.visibility = VISIBLE
            } else {
                btnConfirm.visibility = GONE
            }
        }
    }
}
