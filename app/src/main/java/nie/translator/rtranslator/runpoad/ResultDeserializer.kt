package com.translate.myapplication.runpoad

import android.util.Log
import com.google.gson.*
import java.lang.reflect.Type

class ResultDeserializer : JsonDeserializer<List<ResultItem>> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): List<ResultItem> {
        if (json == null || json.isJsonNull) {
            return emptyList()
        }

        if (json.isJsonObject) {
            // 如果是空对象，返回空列表
            return emptyList()
        }

        if (json.isJsonArray) {
            val list = mutableListOf<ResultItem>()
            val jsonArray = json.asJsonArray
            for (element in jsonArray) {
                try {
                    val resultItem = context?.deserialize<ResultItem>(element, ResultItem::class.java)
                    resultItem?.let { list.add(it) }
                } catch (e: JsonSyntaxException) {
                    // 记录错误日志
                    Log.e("ResultDeserializer", "Failed to deserialize ResultItem: ${e.message}")
                }
            }
            return list
        }

        // 若不是数组或对象，返回空列表
        Log.e("ResultDeserializer", "Unexpected JSON type: ${json.javaClass.simpleName}")
        return emptyList()
    }
}