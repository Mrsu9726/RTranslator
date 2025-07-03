package nie.translator.rtranslator.voice_translation.neural_networks.voice

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

class BertTokenizer(vocabStream: InputStream) {
    private val vocab: Map<String, Int>
    private val idsToTokens: Map<Int, String>

    init {
        val reader = BufferedReader(InputStreamReader(vocabStream))
        val tempVocab = mutableMapOf<String, Int>()
        var index = 0
        reader.forEachLine { line ->
            tempVocab[line] = index
            index++
        }
        vocab = tempVocab.toMap()
        idsToTokens = vocab.entries.associate { (k, v) -> v to k }
    }

    fun tokenize(text: String): List<String> {
        val tokens = mutableListOf<String>()
        tokens.add("[CLS]")
        for (char in text) {
            tokens.add(char.toString())
        }
        tokens.add("[SEP]")
        return tokens
    }

    fun convertTokensToIds(tokens: List<String>): List<Long> {
        return tokens.map { vocab[it]?.toLong() ?: vocab["[UNK]"]?.toLong() ?: 0L }
    }

    fun convertIdsToTokens(ids: List<Long>): List<String> {
        return ids.map { idsToTokens[it.toInt()] ?: "[UNK]" }
    }

    fun encode(text: String): Triple<List<Long>, List<Long>, List<Long>> {
        val tokens = tokenize(text)
        val inputIds = convertTokensToIds(tokens)
        val attentionMask = MutableList(inputIds.size) { 1L }
        val tokenTypeIds = MutableList(inputIds.size) { 0L } // All zeros for single sentence
        return Triple(inputIds, attentionMask, tokenTypeIds)
    }

    fun decode(ids: List<Long>): String {
        val tokens = convertIdsToTokens(ids)
        return tokens.filter { it !in listOf("[CLS]", "[SEP]", "[PAD]") }
            .joinToString("")
            .replace("##", "")
    }
}