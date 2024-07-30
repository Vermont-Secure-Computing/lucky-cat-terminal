package com.example.possin.network

import android.content.Context
import android.util.Log
import com.example.possin.utils.PropertiesUtil
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://dogpay.mom/"

    private lateinit var retrofit: Retrofit

    fun getApiService(context: Context): ApiService {
        if (!::retrofit.isInitialized) {
            val apiKey = PropertiesUtil.getProperty(context, "api_key")
            Log.d("APIKEY", apiKey.toString())
            val client = OkHttpClient.Builder()
                .addInterceptor { chain: Interceptor.Chain ->
                    val original: Request = chain.request()
                    val requestBuilder: Request.Builder = original.newBuilder()
                        .header("x-api-key", apiKey ?: "")
                    val request: Request = requestBuilder.build()
                    chain.proceed(request)
                }
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
