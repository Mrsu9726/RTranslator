package nie.translator.rtranslator.onboarding

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.LogUtils
import nie.translator.rtranslator.Global
import nie.translator.rtranslator.R
import nie.translator.rtranslator.tools.CustomLocale

/**
 * 在这里实现引导操作
 * 主语言
 * WiFi
 * 收音测试
 */
class OnboardingActivity : FragmentActivity() {
    var languageRecycle: RecyclerView? = null
    var languageAdapter: LanguageAdapter? = null
    var nextButton: Button? = null
    val allCountries = ArrayList<CountryOrRegion>()
    var global: Global? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)
        val decorView = window.decorView
        decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        initData()
        initView()
    }

    private fun initView() {
        languageRecycle = findViewById(R.id.language_recycler_view)
        languageAdapter = LanguageAdapter(this)
        languageAdapter?.setSelectedCountries(allCountries)
        global?.getFirstLanguage(false, object : Global.GetLocaleListener {
            override fun onSuccess(result: CustomLocale?) {
                LogUtils.d("OnboardingActivity", "onSuccess: ${result?.locale?.toLanguageTag()}")
                languageAdapter?.setSelectLocal(result?.locale?.language)
            }

            override fun onFailure(reasons: IntArray?, value: Long) {
                LogUtils.d("OnboardingActivity", "onFailure: $value")
                languageAdapter?.setSelectLocal("CN")
            }
        })
        languageAdapter?.setCallback(object : PickCallback {
            override fun onPick(countryOrRegion: CountryOrRegion?) {
                val locale = CustomLocale(countryOrRegion?.locale)
                global?.setFirstLanguage(locale)
                global?.setSecondLanguage(locale)
            }

        })
        languageRecycle?.layoutManager = LinearLayoutManager(this)
        languageRecycle?.adapter = languageAdapter
    }

    private fun initData() {
        CountryOrRegion.load(this).apply {
            allCountries.clear()
            allCountries.addAll(CountryOrRegion.getAll())
        }
        global = getApplication() as Global
    }
}