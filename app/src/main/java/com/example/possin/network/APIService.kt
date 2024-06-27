package com.example.possin.network

import com.example.possin.model.ConversionResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST


interface ApiService {
    @POST("terminal/conversion")
    fun postConversion(
        @Body requestBody: ConversionRequestBody
    ): Call<ConversionResponse>
}