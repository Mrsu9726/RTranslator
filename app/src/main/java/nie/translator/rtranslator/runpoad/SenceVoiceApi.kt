package com.translate.myapplication.runpoad

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface SenceVoiceApi {
    @Multipart
    @POST("api/v1/asr")
    suspend fun uploadAudio(
        @Part files: List<MultipartBody.Part>,     // 多个音频文件
        @Part("keys") keys: RequestBody,           // 用逗号分隔的文件名
        @Part("lang") lang: RequestBody            // 语言 zh/en
    ): SenseVoiceResponse
}

