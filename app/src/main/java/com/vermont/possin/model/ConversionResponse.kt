package com.vermont.possin.model

import com.google.gson.annotations.SerializedName

data class ConversionResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("conversionRate") val conversionRate: Double,
    // Add other fields as necessary
    val feeStatus: String,
    val status: String
)
