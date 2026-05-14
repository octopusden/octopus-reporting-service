package org.octopusden.octopus.reportingservice.client.common.dto.buildconfig

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank

data class BuildConfigurationReportRequestDto(
    @field:NotBlank(message = "rootProjectId must not be blank")
    val rootProjectId: String,
    @field:Valid
    val componentsFilter: BuildConfigurationReportComponentsFilterDto,
    @field:Valid
    val checks: BuildConfigurationReportChecksDto
)