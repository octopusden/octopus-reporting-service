package org.octopusden.octopus.reportingservice.service.impl

import feign.FeignException
import org.octopusden.octopus.infrastructure.teamcity.client.TeamcityClient
import org.octopusden.octopus.infrastructure.teamcity.client.dto.TeamcityBuildType
import org.octopusden.octopus.infrastructure.teamcity.client.dto.TeamcityProject
import org.octopusden.octopus.infrastructure.teamcity.client.dto.locator.ProjectLocator
import org.octopusden.octopus.reportingservice.client.common.exception.ExternalServiceException
import org.octopusden.octopus.reportingservice.client.common.exception.NotFoundException
import org.octopusden.octopus.reportingservice.domain.BuildConfiguration
import org.octopusden.octopus.reportingservice.domain.BuildConfigurationParameter
import org.octopusden.octopus.reportingservice.domain.BuildConfigurationProject
import org.octopusden.octopus.reportingservice.domain.BuildConfigurationStep
import org.octopusden.octopus.reportingservice.service.TeamCityService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class TeamCityServiceImpl(
    private val client: TeamcityClient
) : TeamCityService {

    override fun findSubprojects(rootProjectId: String): List<BuildConfigurationProject> {
        logger.info("findSubprojects: rootProjectId='{}'", rootProjectId)
        val fields = """
            count,
            href,
            nextHref,
            prevHref,
            project(
                id,
                name,
                href,
                webUrl,
                archived,
                parameters(property(name,value)),
                buildTypes(
                    buildType(
                        id,
                        templates(buildType(id,step)),
                        parameters(property(name,value)),
                        steps(step(id,name,type,disabled,properties(property(name,value))))
                    )
                )
            )
        """.trimIndent().replace("\\s+".toRegex(), "")
        val result = mutableListOf<BuildConfigurationProject>()

        val rootLocator = ProjectLocator(
            id = rootProjectId,
            archived = false
        )
        result += callTeamCity("getProjects(root='$rootProjectId')") {
            client.getProjectsWithLocatorAndFields(rootLocator, fields)
        }.projects
            .asSequence()
            .filter { it.buildTypes?.buildTypes.orEmpty().isNotEmpty() }
            .mapNotNull { it.toBuildConfigurationProjectOrNull() }
            .toList()

        var start = 0
        while (true) {
            val locator = ProjectLocator(
                affectedProject = ProjectLocator(id = rootProjectId),
                archived = false,
                count = DEFAULT_PAGE_SIZE,
                start = start
            )
            val page = callTeamCity("getProjects(affected='$rootProjectId', start=$start)") {
                client.getProjectsWithLocatorAndFields(locator, fields)
            }
            result += page.projects
                .asSequence()
                .filter { it.buildTypes?.buildTypes.orEmpty().isNotEmpty() }
                .mapNotNull { it.toBuildConfigurationProjectOrNull() }
                .toList()
            if (page.nextHref == null) {
                break
            }
            start += DEFAULT_PAGE_SIZE
        }
        logger.info("findSubprojects: total collected={} for rootProjectId='{}'", result.size, rootProjectId)
        return result
    }

    override fun getTemplateByProjectIdAndTemplateId(projectId: String, templateId: String): BuildConfiguration {
        logger.info("getTemplateByProjectIdAndTemplateId: projectId='{}' templateId='{}'", projectId, templateId)
        val fields = """
            project(
                id,
                name,
                href,
                webUrl,
                templates(
                    buildType(
                        id,
                        templates(buildType(id,step)),
                        parameters(property(name,value)),
                        steps(step(id,name,type,disabled,properties(property(name,value))))
                    )
                )
            )
        """.trimIndent().replace("\\s+".toRegex(), "")
        val locator = ProjectLocator(id = projectId)
        val result = callTeamCity("getTemplate(projectId='$projectId', templateId='$templateId')") {
            client.getProjectsWithLocatorAndFields(locator, fields)
        }.projects.find { it.id == projectId }
            ?.templates?.buildTypes
            ?.find { it.id == templateId }?.toBuildConfiguration()
            ?: run {
                throw NotFoundException("Not found template with templateId='$templateId' in project with projectId='$projectId'!")
            }
        logger.info(
            "getTemplateByProjectIdAndTemplateId: loaded template '{}'; amount parameters = {}, amount steps = {}",
            templateId, result.parameters.size, result.steps.size
        )
        return result
    }

    private fun <T> callTeamCity(operation: String, block: () -> T): T = try {
        block()
    } catch (e: FeignException.NotFound) {
        throw NotFoundException("TeamCity entity not found: $operation")
    } catch (e: Exception) {
        throw ExternalServiceException("TeamCity call failed: $operation", e)
    }

    private fun TeamcityProject.toBuildConfigurationProjectOrNull(): BuildConfigurationProject? {
        val componentName = parameters?.properties.orEmpty()
            .find { it.name == TC_PROPERTY_COMPONENT_NAME }
            ?.value.orEmpty()
        if (componentName.isEmpty()) {
            return null
        }
        val buildTypes = buildTypes?.buildTypes.orEmpty()
            .map { it.toBuildConfiguration() }
            .toSet()
        return BuildConfigurationProject(
            id = id,
            name = name,
            webUrl = webUrl,
            componentId = componentName,
            buildConfigurations = buildTypes
        )
    }

    private fun TeamcityBuildType.toBuildConfiguration(): BuildConfiguration {
        val templateIds = templates?.buildTypes.orEmpty()
            .map { it.id }
            .toSet()
        val parameters = parameters?.properties.orEmpty()
            .map {
                BuildConfigurationParameter(
                    name = it.name,
                    value = it.value.orEmpty()
                )
            }
        val steps = steps?.steps.orEmpty()
            .map {
                BuildConfigurationStep(
                    id = it.id,
                    name = it.name,
                    disabled = it.disabled ?: run {
                        logger.warn(
                            "TeamCity step '{}' in buildType '{}' has no disabled field; treating as enabled", it.id, id
                        )
                        false
                    }
                )
            }
        return BuildConfiguration(
            buildTypeId = id,
            templateIds = templateIds,
            parameters = parameters,
            steps = steps
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TeamCityServiceImpl::class.java)

        private const val TC_PROPERTY_COMPONENT_NAME = "COMPONENT_NAME"
        private const val DEFAULT_PAGE_SIZE = 200
    }
}