package com.example.possin.model

import com.google.gson.annotations.SerializedName
data class ConversionResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("conversionRate") val conversionRate: Double
    // Add other fields as necessary
)
