package org.octopusden.octopus.reportingservice.client

data class ReportingServiceClientConfig(
    val baseUrl: String,
    val connectTimeoutMs: Long = 60_000,
    val readTimeoutMs: Long = 100_000,
)
