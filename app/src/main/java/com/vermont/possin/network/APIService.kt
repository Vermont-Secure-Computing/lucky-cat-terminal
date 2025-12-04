/*
 * Copyright 2024–2025 Vermont Secure Computing and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
http://www.apache.org/licenses/LICENSE-2.0

 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

    @POST("terminal/xmr/create-wallet")
    fun createMoneroWallet(
        @Body walletRequestBody: MoneroWalletRequestBody
    ): Call<ApiResponse>

    @POST("terminal/xmr/delete-wallet")
    fun deleteMoneroWallet(
        @Body deleteRequestBody: MoneroDeleteWalletRequestBody
    ): Call<ApiResponse>
}