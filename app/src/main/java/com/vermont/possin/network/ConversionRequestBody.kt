package com.vermont.possin.network

data class ConversionRequestBody(
    val price: String,
    val currency: String,
    val chain: String
)