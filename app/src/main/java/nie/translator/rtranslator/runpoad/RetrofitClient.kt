package com.translate.myapplication.runpoad

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "https://ziosqbvyp19qvq-7860.proxy.runpod.net/"
    private val resultListType: Type = object : TypeToken<List<ResultItem>>() {}.type

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(resultListType, ResultDeserializer())
        .create()


    val api: WhisperApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    // 允许 HTTP 重定向
                    .followRedirects(true)
                    // 允许 HTTPS 重定向
                    .followSslRedirects(true)
                    .build()
            )
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(WhisperApi::class.java)
    }

    val senceVoiceApi: SenceVoiceApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(
                OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    // 允许 HTTP 重定向
                    .followRedirects(true)
                    // 允许 HTTPS 重定向
                    .followSslRedirects(true)
                    .build()
            )
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(SenceVoiceApi::class.java)
    }
}
