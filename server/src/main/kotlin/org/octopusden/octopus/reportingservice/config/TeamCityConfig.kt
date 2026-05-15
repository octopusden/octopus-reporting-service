package org.octopusden.octopus.reportingservice.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "teamcity")
data class TeamCityConfig(
    val url: String,
    val user: String,
    val password: String
)