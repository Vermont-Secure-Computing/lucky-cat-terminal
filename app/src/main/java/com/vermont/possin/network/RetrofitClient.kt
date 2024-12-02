package com.vermont.possin.network

import android.content.Context
import android.util.Log
import com.vermont.possin.utils.PropertiesUtil
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "https://dogpay.mom/"

    private lateinit var retrofit: Retrofit

    fun getApiService(context: Context): ApiService {
        if (!::retrofit.isInitialized) {
            val apiKey = PropertiesUtil.getProperty(context, "api_key")
            Log.d("APIKEY", apiKey.toString())

            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor { chain: Interceptor.Chain ->
                    val original: Request = chain.request()
                    val requestBuilder: Request.Builder = original.newBuilder()
                        .header("x-api-key", apiKey ?: "")
                        .header("Connection", "keep-alive") // Ensure keep-alive
                        .header("User-Agent", "okhttp/4.9.3") // Add user agent
                    val request: Request = requestBuilder.build()
                    chain.proceed(request)
                }
                .addInterceptor(loggingInterceptor) // Log request/response
                .connectTimeout(30, TimeUnit.SECONDS) // Connection timeout
                .readTimeout(30, TimeUnit.SECONDS) // Read timeout
                .writeTimeout(30, TimeUnit.SECONDS) // Write timeout
                .retryOnConnectionFailure(true) // Retry on failure
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit.create(ApiService::class.java)
    }
}
