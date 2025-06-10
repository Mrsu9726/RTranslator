package nie.translator.rtranslator.livedata

import androidx.lifecycle.MutableLiveData

object GlobalLiveDataManager {
    /**
     * 声音分贝范围划分
     * 0 dB：人耳能听到的最小声音，接近听觉阈值。
     * 30 dB：安静的图书馆、轻声耳语，属于非常安静的环境。
     * 60 dB：正常交谈的声音，日常交流环境。
     * 85 dB：繁忙街道上的交通噪音，长时间暴露在该强度声音中可能损害听力。
     * 100 dB：施工现场、迪斯科舞厅的音量，短时间暴露就可能对听力造成伤害。
     * 120 dB：摇滚音乐会、喷气式飞机起飞时的噪音，会让人感到疼痛，长时间或近距离接触会导致永久性听力损失
     */
    val sound_decibel = MutableLiveData<Double>()

    //是否是夜色主题
    val is_night_theme = MutableLiveData<Boolean>(false)

    val show_standby = MutableLiveData<Boolean>(false)
    //自动模式，手动模式
    val  manual_model = MutableLiveData<Boolean>(false)
}