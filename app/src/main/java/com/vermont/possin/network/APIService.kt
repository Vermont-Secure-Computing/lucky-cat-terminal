package com.vermont.possin.network

import com.vermont.possin.model.ApiResponse
import com.vermont.possin.model.ConversionResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST


interface ApiService {
    @POST("terminal/conversion")
    fun postConversion(
        @Body requestBody: ConversionRequestBody
    ): Call<ConversionResponse>

    @GET("terminal/api-details")
    fun getApiDetails(
        @Header("x-api-key") apiKey: String
    ): Call<ApiResponse>

    @GET("metrics")
    fun getMetrics(): Call<String>
}