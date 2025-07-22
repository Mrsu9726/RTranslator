package nie.translator.rtranslator.runpoad

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.translate.myapplication.runpoad.RetrofitClient
import com.translate.myapplication.runpoad.WavRecorder
import kotlinx.coroutines.*
import kotlinx.coroutines.launch
import nie.translator.rtranslator.R
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class RunpodActivity : AppCompatActivity() {
    private lateinit var recorder: WavRecorder
    private var isRecording = false
    private val resultText = StringBuilder()
    private val identifierSet = mutableSetOf<String>()

    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var button: Button? = null
    private var resultView: TextView? = null

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_runpod)
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        button = findViewById(R.id.recordButton)
        resultView = findViewById(R.id.resultTextView)
        recorder = WavRecorder(this)

        button?.setOnClickListener {
            if (!isRecording) {
                resultText.clear()
                button?.text = "停止录音"
                isRecording = true
                recorder.start { wavFile ->
//                    uploadAndCache(wavFile)
                    uploadSence(wavFile)
                }
//                startPollingResult()
            } else {
                isRecording = false
                button?.text = "开始录音"
                recorder.stop()
            }
        }
    }

    private fun uploadAndCache(wavFile: File) {
        mainScope.launch(Dispatchers.IO) {
            try {
                val body = wavFile.asRequestBody("audio/wav".toMediaType())
                val part = MultipartBody.Part.createFormData("file", wavFile.name, body)
                val res = RetrofitClient.api.uploadAudio(part, "medium", "zh")
                identifierSet.add(res.identifier)
                Log.d("Whisper", "上传成功: ${res.identifier}")
            } catch (e: Exception) {
                Log.e("Whisper", "上传失败", e)
            }
        }
    }

    private fun uploadSence(wavFile: File) {
        mainScope.launch(Dispatchers.IO) {
            try {
                val body = wavFile.asRequestBody("audio/wav".toMediaType())
                val part = MultipartBody.Part.createFormData("file", wavFile.name, body)

                val requestFile = wavFile.asRequestBody("audio/wav".toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("files", wavFile.name, requestFile)

                val keysPart = wavFile.name.toRequestBody("text/plain".toMediaTypeOrNull())
                val langPart = "zh".toRequestBody("text/plain".toMediaTypeOrNull())

                val res =
                    RetrofitClient.senceVoiceApi.uploadAudio(listOf(filePart), keysPart, langPart)
                if (res.result.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        val sb = StringBuffer()
                        for (ii in res.result) {
                            sb.append(ii.clean_text)
                        }
                        resultText.append(sb.toString()).append(",")
                        resultView?.text = resultText.toString()
                    }
                }
            } catch (e: Exception) {
                Log.e("Whisper", "上传失败", e)
            }
        }
    }

    private fun startPollingResult() {
        mainScope.launch(Dispatchers.IO) {
            while (true) {
                val toRemove = mutableListOf<String>()
                // 复制一份 identifierSet 的副本进行遍历
                for (id in identifierSet.toList()) {
                    try {
                        val task = RetrofitClient.api.queryTask(id)
                        if (task.status == "completed" && task.result.isNotEmpty()) {
                            val sb = StringBuilder()
                            // 根据修改后的 ResultObject 类型调整
                            if (task.result.size > 0) {
                                for (item in task.result) {
                                    sb.append(item.text)
                                }
                            }
                            withContext(Dispatchers.Main) {
                                resultText.append(sb).append("\n")
                                resultView?.text = resultText.toString()
                            }
                            Log.d("Whisper", "识别完成: $sb")
                            toRemove.add(id)
                        }
                    } catch (e: Exception) {
                        Log.e("Whisper", "轮询失败: $id", e)
                    }
                }
                identifierSet.removeAll(toRemove)
                delay(1000)
            }
        }
    }
}