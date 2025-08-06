package nie.translator.rtranslator.voice_translation.neural_networks.voice;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Helper class to run the Python FireRedASR model via subprocess.
 * It converts audio chunks to a temporary WAV file and executes the
 * fireredasr/cli_transcribe.py script. The Python environment and
 * model directory must be present on the device.
 */
public class FireRedAsrRecognizer {
    private final String pythonBinary;
    private final String modelDir;
    private final String asrType;
    private final Context context;

    public FireRedAsrRecognizer(Context context, String pythonBinary, String modelDir, String asrType) {
        this.context = context;
        this.pythonBinary = pythonBinary;
        this.modelDir = modelDir;
        this.asrType = asrType;
    }

    /**
     * Recognize speech from a float PCM array using FireRedASR.
     */
    public String recognize(float[] data, int sampleRate) throws IOException, InterruptedException {
        File temp = File.createTempFile("firered_tmp", ".wav", context.getCacheDir());
        writeWavFile(data, sampleRate, temp);
        ProcessBuilder pb = new ProcessBuilder(
                pythonBinary,
                context.getFilesDir().getAbsolutePath() + "/fireredasr/cli_transcribe.py",
                modelDir,
                temp.getAbsolutePath(),
                asrType
        );
        pb.redirectErrorStream(true);
        Process p = pb.start();
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        StringBuilder out = new StringBuilder();
        while ((line = br.readLine()) != null) {
            out.append(line);
        }
        br.close();
        int code = p.waitFor();
        if (code != 0) {
            Log.e("FireRedASR", "Exit code: " + code);
        }
        //noinspection ResultOfMethodCallIgnored
        temp.delete();
        return out.toString().trim();
    }

    private static void writeWavFile(float[] data, int sampleRate, File file) throws IOException {
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(file))) {
            int byteRate = sampleRate * 2;
            int dataLen = data.length * 2;
            // RIFF header
            out.writeBytes("RIFF");
            out.writeInt(Integer.reverseBytes(36 + dataLen));
            out.writeBytes("WAVE");
            out.writeBytes("fmt ");
            out.writeInt(Integer.reverseBytes(16));
            out.writeShort(Short.reverseBytes((short) 1));
            out.writeShort(Short.reverseBytes((short) 1));
            out.writeInt(Integer.reverseBytes(sampleRate));
            out.writeInt(Integer.reverseBytes(byteRate));
            out.writeShort(Short.reverseBytes((short) 2));
            out.writeShort(Short.reverseBytes((short) 16));
            out.writeBytes("data");
            out.writeInt(Integer.reverseBytes(dataLen));
            for (float v : data) {
                short s = (short) Math.max(Math.min(v * 32767f, 32767f), -32768f);
                out.writeShort(Short.reverseBytes(s));
            }
        }
    }
}
