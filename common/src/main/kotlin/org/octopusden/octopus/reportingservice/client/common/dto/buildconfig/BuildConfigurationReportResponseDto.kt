package org.octopusden.octopus.reportingservice.client.common.dto.buildconfig

data class BuildConfigurationReportResponseDto(
    val rootProjectId: String,
    val result: List<BuildConfigurationComponentReportDto>,
)
