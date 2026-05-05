package org.octopusden.octopus.reportingservice.client.common.dto.buildconfig

data class BuildConfigurationReportComponentsFilterDto(
    val includeSystems: Set<String> = emptySet(),
    val excludeComponents: Set<String> = emptySet()
)