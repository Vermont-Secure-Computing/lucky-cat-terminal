// ApiKeyInterceptor.kt
package com.vermont.possin.network

import android.content.Context
import com.vermont.possin.ApiKeyStore
import okhttp3.Interceptor
import okhttp3.Response

class ApiKeyInterceptor(private val appContext: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val key = ApiKeyStore.get(appContext) ?: ""
        val newReq = req.newBuilder()
            .header("x-api-key", key)
            .build()
        return chain.proceed(newReq)
    }
}
