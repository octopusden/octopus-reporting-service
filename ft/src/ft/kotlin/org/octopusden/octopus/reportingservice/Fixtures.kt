package org.octopusden.octopus.reportingservice

import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildConfigurationReportChecksDto
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildConfigurationReportComponentsFilterDto
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildConfigurationReportRequestDto
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildStage

object Fixtures {
    const val ROOT_PROJECT_ID = "RootProject"
    const val BASE_PROJECT_ID = "RDDepartment"
    const val SYSTEM = "TEST_SYSTEM"

    fun requestBuildConfigurationReport(
        rootProjectId: String = ROOT_PROJECT_ID,
        systems: Set<String> = setOf(SYSTEM),
        excludeComponents: Set<String> = emptySet(),
        stage: BuildStage = BuildStage.BUILD,
        parameters: List<String> = emptyList(),
        steps: List<String> = emptyList(),
    ): BuildConfigurationReportRequestDto =
        BuildConfigurationReportRequestDto(
            rootProjectId = rootProjectId,
            componentsFilter = BuildConfigurationReportComponentsFilterDto(
                includeSystems = systems,
                excludeComponents = excludeComponents,
            ),
            checks = BuildConfigurationReportChecksDto(
                buildStage = stage,
                parameters = parameters,
                steps = steps,
            ),
        )
}
