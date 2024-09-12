package com.example.possin.model

data class MetricsResponse(
    val data: MetricsData?
)

data class MetricsData(
    val status: String,
    val other_info: String?
)
