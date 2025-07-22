package com.translate.myapplication.runpoad

data class SenseVoiceResponse(
    val result: List<SenseVoiceResult>
)

data class SenseVoiceResult(
    val key: String,
    val text: String,
    val raw_text: String,
    val clean_text: String,
)
