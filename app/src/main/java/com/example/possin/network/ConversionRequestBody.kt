package com.example.possin.network

data class ConversionRequestBody(
    val price: String,
    val currency: String,
    val chain: String
)