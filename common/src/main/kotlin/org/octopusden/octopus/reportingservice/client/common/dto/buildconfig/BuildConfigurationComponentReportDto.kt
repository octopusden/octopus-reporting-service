package org.octopusden.octopus.reportingservice.client.common.dto.buildconfig

data class BuildConfigurationComponentReportDto(
    val componentId: String,
    val status: ComponentReportStatus,
    val buildConfigurationUrl: String? = null,
    val buildTypeId: String? = null,
    val checks: List<BuildConfigurationCheckResultDto> = emptyList(),
)
