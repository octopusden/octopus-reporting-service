package org.octopusden.octopus.reportingservice.service.impl

import org.octopusden.octopus.components.registry.core.dto.ComponentV2
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildConfigurationCheckResultDto
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildConfigurationComponentReportDto
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildConfigurationConstants.NOT_DEFINED
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildConfigurationReportRequestDto
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.BuildConfigurationReportResponseDto
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.CheckType
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.ComponentReportStatus
import org.octopusden.octopus.reportingservice.client.common.dto.buildconfig.StepState
import org.octopusden.octopus.reportingservice.config.BuildConfigurationReportConfig
import org.octopusden.octopus.reportingservice.domain.BuildConfiguration
import org.octopusden.octopus.reportingservice.domain.BuildConfigurationProject
import org.octopusden.octopus.reportingservice.service.BuildConfigurationReportService
import org.octopusden.octopus.reportingservice.service.ComponentsRegistryService
import org.octopusden.octopus.reportingservice.service.TeamCityService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Locale

@Service
class BuildConfigurationReportServiceImpl(
    private val config: BuildConfigurationReportConfig,
    private val teamCityService: TeamCityService,
    private val componentsRegistryService: ComponentsRegistryService
) : BuildConfigurationReportService {

    override fun generateReport(request: BuildConfigurationReportRequestDto): BuildConfigurationReportResponseDto {
        if (request.checks.steps.isEmpty() && request.checks.parameters.isEmpty()) {
            logger.info("generateReport: both checks.parameters and checks.steps are empty")
            return BuildConfigurationReportResponseDto(
                rootProjectId = request.rootProjectId,
                result = emptyList()
            )
        }
        val components = getComponentsAfterFilter(request)
        if (components.isEmpty()) {
            logger.info("generateReport: no components after filtering")
            return BuildConfigurationReportResponseDto(
                rootProjectId = request.rootProjectId,
                result = emptyList()
            )
        }
        logger.info("generateReport: components found {}", components.size)
        val stageTemplates = getBuildStageTemplates(request)
        val projectsByComponentId = teamCityService.findSubprojects(request.rootProjectId)
            .groupBy { it.componentId }
        val result = components.map { component ->
            buildComponentReport(
                componentId = component.id,
                projects = projectsByComponentId[component.id].orEmpty(),
                stageTemplates = stageTemplates,
                request = request
            )
        }.sortedBy { it.componentId.lowercase(Locale.ROOT) }
        logger.info(
            "generateReport: done. rootProjectId='{}', total component reports={}",
            request.rootProjectId, result.size
        )
        return BuildConfigurationReportResponseDto(
            rootProjectId = request.rootProjectId,
            result = result
        )
    }

    private fun getComponentsAfterFilter(request: BuildConfigurationReportRequestDto): List<ComponentV2> {
        val all = componentsRegistryService.getComponentsBySystems(request.componentsFilter.includeSystems)
        return all.filter { !request.componentsFilter.excludeComponents.contains(it.id) }
    }

    private fun getBuildStageTemplates(request: BuildConfigurationReportRequestDto): Map<String, BuildConfiguration> {
        val stageTemplateIds = config.getTemplates(request.checks.buildStage).toSet()
        return stageTemplateIds.associateWith { templateId ->
            teamCityService.getTemplateByProjectIdAndTemplateId(
                projectId = config.baseProjectId,
                templateId = templateId
            )
        }
    }

    private fun buildComponentReport(
        componentId: String,
        projects: List<BuildConfigurationProject>,
        stageTemplates: Map<String, BuildConfiguration>,
        request: BuildConfigurationReportRequestDto
    ): BuildConfigurationComponentReportDto {
        if (projects.isEmpty()) {
            return BuildConfigurationComponentReportDto(
                componentId = componentId,
                status = ComponentReportStatus.NO_PROJECT
            )
        }
        val matchedProjectAndBuild = projects.firstNotNullOfOrNull { project ->
            val matched = findBuildConfigurationForStage(project, stageTemplates.keys)
            matched?.let { project to it }
        }
        if (matchedProjectAndBuild == null) {
            logger.info(
                "buildComponentReport: no build configuration inherited from stage templates {} " +
                        "for component '{}'. Searched projects: {}",
                stageTemplates.keys, componentId,
                projects.joinToString { "${it.id}(buildConfigurations=${it.buildConfigurations.joinToString { "${it.buildTypeId}(templates=${it.templateIds})" }})" }
            )
            return BuildConfigurationComponentReportDto(
                componentId = componentId,
                status = ComponentReportStatus.NO_BUILD_CONFIGURATION
            )
        }
        val (project, matched) = matchedProjectAndBuild
        val (buildConfiguration, matchedTemplateId) = matched
        val template = stageTemplates.getValue(matchedTemplateId)
        val checks = buildList {
            addAll(checkParameters(buildConfiguration, template, request.checks.parameters))
            addAll(checkSteps(buildConfiguration, template, request.checks.steps))
        }
        return BuildConfigurationComponentReportDto(
            componentId = componentId,
            status = ComponentReportStatus.OK,
            buildConfigurationUrl = project.webUrl,
            buildTypeId = buildConfiguration.buildTypeId,
            checks = checks
        )
    }

    private fun checkParameters(
        buildConfiguration: BuildConfiguration,
        template: BuildConfiguration,
        parameterNames: List<String>
    ): List<BuildConfigurationCheckResultDto> = parameterNames.map { parameterName ->
        val expectedValue = template.parameters.find { it.name == parameterName }?.value ?: NOT_DEFINED
        val actualValue = buildConfiguration.parameters.find { it.name == parameterName }?.value ?: NOT_DEFINED
        BuildConfigurationCheckResultDto(
            checkType = CheckType.PARAMETER,
            checkName = parameterName,
            actualValue = actualValue,
            expectedValue = expectedValue,
            status = actualValue == expectedValue && actualValue != NOT_DEFINED
        )
    }

    private fun checkSteps(
        buildConfiguration: BuildConfiguration,
        template: BuildConfiguration,
        stepNames: List<String>
    ): List<BuildConfigurationCheckResultDto> = stepNames.map { stepName ->
        val expectedValue = template.steps.find { it.name == stepName }
            ?.disabled
            ?.toStepState()
            ?: NOT_DEFINED
        val actualValue = buildConfiguration.steps.find { it.name == stepName }
            ?.disabled
            ?.toStepState()
            ?: NOT_DEFINED
        BuildConfigurationCheckResultDto(
            checkType = CheckType.STEP,
            checkName = stepName,
            actualValue = actualValue,
            expectedValue = expectedValue,
            status = actualValue == expectedValue && actualValue != NOT_DEFINED
        )
    }

    private fun Boolean.toStepState(): String =
        if (this) StepState.DISABLED.name else StepState.ENABLED.name

    private fun findBuildConfigurationForStage(
        project: BuildConfigurationProject,
        stageTemplateIds: Set<String>
    ): Pair<BuildConfiguration, String>? {
        project.buildConfigurations.forEach { buildConfig ->
            val matched = buildConfig.templateIds.firstOrNull { it in stageTemplateIds }
            if (matched != null) {
                return buildConfig to matched
            }
        }
        return null
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BuildConfigurationReportServiceImpl::class.java)
    }
}