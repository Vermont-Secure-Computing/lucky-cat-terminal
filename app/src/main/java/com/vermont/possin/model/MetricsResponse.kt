package com.vermont.possin.model

data class MetricsResponse(
    val data: MetricsData?
)

data class MetricsData(
    val status: String,
    val other_info: String?
)
