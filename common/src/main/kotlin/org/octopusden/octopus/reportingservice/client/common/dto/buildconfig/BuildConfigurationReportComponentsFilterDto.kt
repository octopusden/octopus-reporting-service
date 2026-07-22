package org.octopusden.octopus.reportingservice.client.common.dto.buildconfig

import org.octopusden.octopus.reportingservice.client.common.validation.NoBlankElements

data class BuildConfigurationReportComponentsFilterDto(
    @field:NoBlankElements(message = "includeSystems must not contain blank values")
    val includeSystems: Set<String> = emptySet(),
    @field:NoBlankElements(message = "excludeComponents must not contain blank values")
    val excludeComponents: Set<String> = emptySet(),
)
