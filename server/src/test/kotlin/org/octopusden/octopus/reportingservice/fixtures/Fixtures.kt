package org.octopusden.octopus.reportingservice.fixtures

import org.octopusden.octopus.components.registry.core.dto.ComponentV2
import org.octopusden.octopus.infrastructure.teamcity.client.dto.TeamcityBuildType
import org.octopusden.octopus.infrastructure.teamcity.client.dto.TeamcityBuildTypes
import org.octopusden.octopus.infrastructure.teamcity.client.dto.TeamcityProject
import org.octopusden.octopus.infrastructure.teamcity.client.dto.TeamcityProjects
import org.octopusden.octopus.infrastructure.teamcity.client.dto.TeamcityProperties
import org.octopusden.octopus.infrastructure.teamcity.client.dto.TeamcityProperty
import org.octopusden.octopus.infrastructure.teamcity.client.dto.TeamcityStep
import org.octopusden.octopus.infrastructure.teamcity.client.dto.TeamcitySteps
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildConfigurationCheckResultDto
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildConfigurationComponentReportDto
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildConfigurationConstants
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildConfigurationReportChecksDto
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildConfigurationReportComponentsFilterDto
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildConfigurationReportRequestDto
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildConfigurationReportResponseDto
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildStage
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.CheckType
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.ComponentReportStatus
import org.octopusden.octopus.reportingservice.domain.BuildConfiguration
import org.octopusden.octopus.reportingservice.domain.BuildConfigurationParameter
import org.octopusden.octopus.reportingservice.domain.BuildConfigurationProject
import org.octopusden.octopus.reportingservice.domain.BuildConfigurationStep

object Fixtures {
    // Components Registry
    fun component(
        id: String,
        owner: String = "owner",
    ): ComponentV2 = ComponentV2(id = id, name = id, componentOwner = owner)

    // Internal domain
    fun project(
        id: String,
        componentId: String,
        webUrl: String = "http://tc/$id",
        name: String = id,
        buildConfigurations: Set<BuildConfiguration> = emptySet(),
    ): BuildConfigurationProject =
        BuildConfigurationProject(
            id = id,
            name = name,
            webUrl = webUrl,
            componentId = componentId,
            buildConfigurations = buildConfigurations,
        )

    fun build(
        buildTypeId: String,
        templateIds: Set<String> = emptySet(),
        parameters: List<BuildConfigurationParameter> = emptyList(),
        steps: List<BuildConfigurationStep> = emptyList(),
    ): BuildConfiguration =
        BuildConfiguration(
            buildTypeId = buildTypeId,
            templateIds = templateIds,
            parameters = parameters,
            steps = steps,
        )

    fun param(
        name: String,
        value: String,
    ): BuildConfigurationParameter = BuildConfigurationParameter(name = name, value = value)

    fun step(
        name: String,
        disabled: Boolean = false,
        id: String = "s_$name",
    ): BuildConfigurationStep = BuildConfigurationStep(id = id, name = name, disabled = disabled)

    // TeamCity raw DTOs
    fun tcProject(
        id: String,
        componentName: String? = null,
        buildTypes: List<TeamcityBuildType> = emptyList(),
        webUrl: String = "http://tc/$id",
    ): TeamcityProject =
        TeamcityProject(
            id = id,
            name = id,
            href = "/$id",
            webUrl = webUrl,
            archived = false,
            parameters = componentName?.let {
                TeamcityProperties(properties = listOf(TeamcityProperty(name = "COMPONENT_NAME", value = it)))
            },
            buildTypes = TeamcityBuildTypes(buildTypes = buildTypes),
        )

    fun tcBuildType(
        id: String,
        templateIds: Set<String> = emptySet(),
        parameters: Map<String, String> = emptyMap(),
        steps: List<TeamcityStep> = emptyList(),
    ): TeamcityBuildType =
        TeamcityBuildType(
            id = id,
            templates = TeamcityBuildTypes(buildTypes = templateIds.map { TeamcityBuildType(id = it) }),
            parameters = TeamcityProperties(
                properties = parameters.map { (k, v) -> TeamcityProperty(name = k, value = v) },
            ),
            steps = TeamcitySteps(steps = steps),
        )

    fun tcStep(
        name: String,
        disabled: Boolean = false,
        id: String = "s_$name",
    ): TeamcityStep =
        TeamcityStep(
            id = id,
            name = name,
            type = "custom",
            disabled = disabled,
            properties = TeamcityProperties(),
        )

    fun tcProjectsPage(
        nextHref: String? = null,
        projects: List<TeamcityProject> = emptyList(),
    ): TeamcityProjects = TeamcityProjects(nextHref = nextHref, projects = projects)

    // Build Configuration Report public API DTOs
    fun request(
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

    fun response(
        rootProjectId: String = ROOT_PROJECT_ID,
        result: List<BuildConfigurationComponentReportDto> = emptyList(),
    ): BuildConfigurationReportResponseDto = BuildConfigurationReportResponseDto(rootProjectId = rootProjectId, result = result)

    fun componentReport(
        componentId: String,
        status: ComponentReportStatus = ComponentReportStatus.OK,
        buildConfigurationUrl: String? = null,
        buildTypeId: String? = null,
        checks: List<BuildConfigurationCheckResultDto> = emptyList(),
    ): BuildConfigurationComponentReportDto =
        BuildConfigurationComponentReportDto(
            componentId = componentId,
            status = status,
            buildConfigurationUrl = buildConfigurationUrl,
            buildTypeId = buildTypeId,
            checks = checks,
        )

    fun checkResult(
        type: CheckType,
        name: String,
        actual: String,
        expected: String,
        status: Boolean = actual == expected && actual != BuildConfigurationConstants.NOT_DEFINED,
    ): BuildConfigurationCheckResultDto =
        BuildConfigurationCheckResultDto(
            checkType = type,
            checkName = name,
            actualValue = actual,
            expectedValue = expected,
            status = status,
        )

    // Common constants
    const val ROOT_PROJECT_ID = "RootProject"
    const val BASE_PROJECT_ID = "RDDepartment"
    const val BUILD_TEMPLATE_ID = "CDCompileUTGradle"
    const val SYSTEM = "TEST_SYSTEM"
    const val COMPONENT_A = "componentA"
    const val COMPONENT_A_PROJECT_ID = "RootProject_ComponentA"
    const val COMPONENT_A_PROJECT_URL = "http://tc/RootProject_ComponentA"
    const val COMPONENT_A_BUILD_ID = "RootProject_ComponentA_Build"
}
