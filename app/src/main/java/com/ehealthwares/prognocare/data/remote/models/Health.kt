package com.ehealthwares.prognocare.data.remote.models

import kotlinx.serialization.Serializable

@Serializable
data class HealthStatus(
    val status: String = "ok",
    val service: String = "emr",
    val uptime: Double = 0.0,
    val timestamp: String = "",
)