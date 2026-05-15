package org.octopusden.octopus.reportingservice.client.common.dto.buildconfig

import org.octopusden.octopus.reportingservice.client.common.validation.NoBlankElements

data class BuildConfigurationReportChecksDto(
    val buildStage: BuildStage,
    @field:NoBlankElements(message = "parameters must not contain blank values")
    val parameters: List<String> = emptyList(),
    @field:NoBlankElements(message = "steps must not contain blank values")
    val steps: List<String> = emptyList()
)