package com.vermont.possin.model

import com.google.gson.annotations.SerializedName

data class ConversionResponse(

    @SerializedName("success")
    val success: Boolean,

    @SerializedName("conversionRate")
    val conversionRate: Double?,

    @SerializedName("relayInvoice")
    val relayInvoice: String?,

    @SerializedName("feeStatus")
    val feeStatus: String?,

    @SerializedName("status")
    val status: String?,

    @SerializedName("error")
    val error: String?
)