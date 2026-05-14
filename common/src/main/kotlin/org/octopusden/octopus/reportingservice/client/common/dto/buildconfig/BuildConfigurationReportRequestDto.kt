package org.octopusden.octopus.reportingservice.client.common.dto.buildconfig

import jakarta.validation.constraints.NotBlank

data class BuildConfigurationReportRequestDto(
    @field:NotBlank(message = "rootProjectId must not be blank")
    val rootProjectId: String,
    val componentsFilter: BuildConfigurationReportComponentsFilterDto,
    val checks: BuildConfigurationReportChecksDto
)