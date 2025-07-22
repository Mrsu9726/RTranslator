package com.translate.myapplication.runpoad

import android.Manifest
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import java.io.File
import java.io.FileOutputStream

class WavRecorder(private val context: Context) {
    private var isRecording = false
    private var recorder: AudioRecord? = null
    private val sampleRate = 16000
    private val channels = AudioFormat.CHANNEL_IN_MONO
    private val encoding = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channels, encoding)

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start(onWavReady: (File) -> Unit) {
        isRecording = true
        recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channels,
            encoding,
            bufferSize
        ).apply { startRecording() }

        Thread {
            while (isRecording) {
                val rawFile = File(context.cacheDir, "slice_${System.currentTimeMillis()}.pcm")
                val outputStream = FileOutputStream(rawFile)
                val buffer = ByteArray(bufferSize)

                val startTime = System.currentTimeMillis()
                while (System.currentTimeMillis() - startTime < 2000 && isRecording) {
                    val read = recorder!!.read(buffer, 0, buffer.size)
                    if (read > 0) outputStream.write(buffer, 0, read)
                }
                outputStream.close()

                if (!isSilent(rawFile)) {
                    val wavFile = convertPcmToWav(rawFile)
                    onWavReady(wavFile)
                } else {
                    println("检测到静音片段，跳过上传")
                }
                rawFile.delete()
            }
        }.start()
    }

    fun stop() {
        isRecording = false
        recorder?.stop()
        recorder?.release()
        recorder = null
    }

    private fun convertPcmToWav(pcmFile: File): File {
        val wavFile = File(pcmFile.parent, pcmFile.name.replace(".pcm", ".wav"))
        val pcmData = pcmFile.readBytes()
        val totalAudioLen = pcmData.size.toLong()
        val totalDataLen = totalAudioLen + 36
        val longSampleRate = 16000L
        val channels = 1
        val byteRate = 16 * longSampleRate * channels / 8

        val header = ByteArray(44)
        writeWavHeader(header, totalAudioLen, totalDataLen, longSampleRate, channels, byteRate)

        FileOutputStream(wavFile).use { out ->
            out.write(header)
            out.write(pcmData)
        }

        return wavFile
    }

    private fun writeWavHeader(
        header: ByteArray,
        totalAudioLen: Long,
        totalDataLen: Long,
        sampleRate: Long,
        channels: Int,
        byteRate: Long
    ) {
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // PCM = 1
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (2 * channels).toByte()
        header[33] = 0
        header[34] = 16
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()
    }

    private fun isSilent(pcmFile: File, silenceThreshold: Int = 1000): Boolean {
        val bytes = pcmFile.readBytes()
        if (bytes.isEmpty()) return true

        var max = 0
        for (i in bytes.indices step 2) {
            val low = bytes[i].toInt() and 0xFF
            val high = bytes[i + 1].toInt()
            val sample = (high shl 8) or low
            max = max.coerceAtLeast(kotlin.math.abs(sample))
        }
        return max < silenceThreshold
    }
}