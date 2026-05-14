package org.octopusden.octopus.reportingservice.client.common.dto.buildconfig

data class BuildConfigurationReportChecksDto(
    val buildStage: BuildStage,
    val parameters: List<String> = emptyList(),
    val steps: List<String> = emptyList()
)