package com.translate.myapplication.runpoad

import okhttp3.MultipartBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface WhisperApi {
    @Multipart
    @POST("transcription")
    suspend fun uploadAudio(
        @Part file: MultipartBody.Part,
        @Query("model_size") modelSize: String, // 添加 model_size 查询参数
        @Query("lang") lang: String // 添加 lang 查询参数
    ): UploadAudioResponse

    @GET("task/{id}")
    suspend fun queryTask(@Path("id") id: String): TranscriptionResponse
}

data class UploadAudioResponse(val identifier: String)
