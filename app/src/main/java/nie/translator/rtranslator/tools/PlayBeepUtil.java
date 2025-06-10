package nie.translator.rtranslator.tools;

import android.media.AudioManager;
import android.media.ToneGenerator;

public class PlayBeepUtil {
    public static void playBeep() {
        ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
        toneGen.startTone(ToneGenerator.TONE_CDMA_PIP, 150); // 播放150毫秒
    }

    public static void playEndBeep() {
        ToneGenerator toneGen = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
        toneGen.startTone(ToneGenerator.TONE_CDMA_ANSWER, 150); // 播放150毫秒
    }
}
