// RetrofitClient.kt
package com.vermont.possin.network

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "https://dogpay.mom/"

    @Volatile private var retrofit: Retrofit? = null
    @Volatile private var api: ApiService? = null

    fun getApiService(context: Context): ApiService {
        api?.let { return it }

        synchronized(this) {
            api?.let { return it }

            val appCtx = context.applicationContext

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(ApiKeyInterceptor(appCtx))   // <-- always injects fresh key
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            api = retrofit!!.create(ApiService::class.java)
            return api!!
        }
    }

    // Call this after saving a new key if you want a hard reset
    fun invalidate() {
        synchronized(this) {
            retrofit = null
            api = null
        }
    }
}
