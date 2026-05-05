package org.octopusden.octopus.reportingservice.client.common.dto.buildconfig

data class BuildConfigurationReportRequestDto(
    val rootProjectId: String,
    val componentsFilter: BuildConfigurationReportComponentsFilterDto,
    val checks: BuildConfigurationReportChecksDto
)