package org.octopusden.octopus.reportingservice.client.common.dto.buildconfig

data class BuildConfigurationReportResponseDto(
    val request: BuildConfigurationReportRequestDto,
    val result: List<BuildConfigurationComponentReportDto>
)
