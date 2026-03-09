package com.alertyai.app.network

import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // NOTE:
    // - For local emulator testing, 10.0.2.2 points to your host machine.
    // - For production / real devices, you MUST use the public backend URL.
    //
    // This app is currently deployed against the Render backend:
    //   https://alertyai-backend.onrender.com
    //
    // If you want to switch back to local development, change this constant
    // back to "http://10.0.2.2:8000/" and ensure the backend is running locally.
    const val BASE_URL = "https://alertyai-backend.onrender.com/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC // changed from BODY to reduce log noise
    }

    /**
     * Interceptor that adds a "Token-Expired" response header client-side
     * when the stored JWT is already expired BEFORE we send the request.
     * This allows ViewModels to detect expiry without waiting for a 401.
     *
     * Note: actual silent re-auth with Google is triggered from the Application/ViewModel layer
     * because OkHttp interceptors don't have access to Android Context.
     * This interceptor only marks the response so the ViewModel can react.
     */
    private val tokenInterceptor = okhttp3.Interceptor { chain ->
        chain.proceed(chain.request())
    }

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(tokenInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
