
package nie.translator.rtranslator.voice_translation.neural_networks.voice

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.extensions.OrtxPackage
import android.annotation.SuppressLint
import android.content.Context
import java.nio.LongBuffer

@SuppressLint("SuspiciousIndentation")
class OnnxCorrector(context: Context) {
    private val ortEnvironment: OrtEnvironment = OrtEnvironment.getEnvironment()
    private lateinit var ortSession: OrtSession
    private lateinit var tokenizer: BertTokenizer

    init {
        // Load vocab.txt
        val vocabStream = context.assets.open("vocab.txt")
        tokenizer = BertTokenizer(vocabStream)

        // Load ONNX model
//        val modelStream = context.assets.open("Whisper_corrector.onnx")
//        val modelBytes = modelStream.readBytes()
        var detokenizerPath = context.getFilesDir().getPath() + "/Whisper_corrector.onnx"
        val initSessionOptions = OrtSession.SessionOptions()
                    initSessionOptions.registerCustomOpLibrary(OrtxPackage.getLibraryPath())
                    initSessionOptions.setCPUArenaAllocator(false);
                    initSessionOptions.setMemoryPatternOptimization(false);
                    initSessionOptions.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.NO_OPT);
        ortSession = ortEnvironment.createSession(detokenizerPath,initSessionOptions)
    }

    fun correctSentence(text: String): String {
        val (inputIds, attentionMask, tokenTypeIds) = tokenizer.encode(text)

        val inputIdsBuffer = LongBuffer.wrap(inputIds.toLongArray())
        val attentionMaskBuffer = LongBuffer.wrap(attentionMask.toLongArray())
        val tokenTypeIdsBuffer = LongBuffer.wrap(tokenTypeIds.toLongArray())

        val inputIdsTensor = OnnxTensor.createTensor(ortEnvironment, inputIdsBuffer, longArrayOf(1, inputIds.size.toLong()))
        val attentionMaskTensor = OnnxTensor.createTensor(ortEnvironment, attentionMaskBuffer, longArrayOf(1, attentionMask.size.toLong()))
//        val tokenTypeIdsTensor = OnnxTensor.createTensor(ortEnvironment, tokenTypeIdsBuffer, longArrayOf(1, tokenTypeIds.size.toLong()))

//        val inputs = mapOf("input_ids" to inputIdsTensor, "attention_mask" to attentionMaskTensor, "token_type_ids" to tokenTypeIdsTensor)
        val inputs = mapOf("input_ids" to inputIdsTensor, "attention_mask" to attentionMaskTensor)
        val outputs = ortSession.run(inputs)

        // The output is float[][][], need to find argmax to get the predicted token IDs
        val logits = outputs.use { it.get(0).value } as Array<Array<FloatArray>>
        val predictedIds = mutableListOf<Long>()

        // Assuming batch size is 1 and sequence length is the second dimension
        for (i in logits[0].indices) {
            var maxLogit = Float.NEGATIVE_INFINITY
            var predictedId: Long = 0
            for (j in logits[0][i].indices) {
                if (logits[0][i][j] > maxLogit) {
                    maxLogit = logits[0][i][j]
                    predictedId = j.toLong()
                }
            }
            predictedIds.add(predictedId)
        }

        val originalTokens = tokenizer.tokenize(text)
        val originalIds = tokenizer.convertTokensToIds(originalTokens)

        val correctedIds = mutableListOf<Long>()
        for (i in originalIds.indices) {
            if (originalTokens[i] in listOf("[CLS]", "[SEP]", "[PAD]")) {
                correctedIds.add(originalIds[i])
            } else {
                correctedIds.add(predictedIds[i])
            }
        }

        return tokenizer.decode(correctedIds)
    }

    fun close() {
        ortSession.close()
        ortEnvironment.close()
    }
}


