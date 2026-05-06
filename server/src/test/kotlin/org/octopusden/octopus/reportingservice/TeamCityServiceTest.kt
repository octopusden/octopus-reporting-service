package org.octopusden.octopus.reportingservice

import com.fasterxml.jackson.core.type.TypeReference
import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.octopusden.octopus.infrastructure.teamcity.client.TeamcityClient
import org.octopusden.octopus.infrastructure.teamcity.client.dto.TeamcityBuildType
import org.octopusden.octopus.infrastructure.teamcity.client.dto.TeamcityBuildTypes
import org.octopusden.octopus.infrastructure.teamcity.client.dto.TeamcityProject
import org.octopusden.octopus.infrastructure.teamcity.client.dto.TeamcityProjects
import org.octopusden.octopus.infrastructure.teamcity.client.dto.TeamcityProperties
import org.octopusden.octopus.infrastructure.teamcity.client.dto.TeamcityProperty
import org.octopusden.octopus.infrastructure.teamcity.client.dto.TeamcityStep
import org.octopusden.octopus.infrastructure.teamcity.client.dto.TeamcitySteps
import org.octopusden.octopus.infrastructure.teamcity.client.dto.locator.ProjectLocator
import org.octopusden.octopus.reportingservice.client.common.exception.NotFoundException
import org.octopusden.octopus.reportingservice.dto.BuildConfiguration
import org.octopusden.octopus.reportingservice.dto.BuildConfigurationProject
import org.octopusden.octopus.reportingservice.service.impl.TeamCityServiceImpl
import org.octopusden.octopus.reportingservice.util.TestUtils

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TeamCityServiceTest {

    private lateinit var client: TeamcityClient
    private lateinit var service: TeamCityServiceImpl

    @BeforeEach
    fun setUp() {
        client = mock()
        service = TeamCityServiceImpl(client = client)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("findSubprojectsArguments")
    fun findSubprojectsTest(
        @Suppress("UNUSED_PARAMETER") caseName: String,
        setup: (TeamcityClient) -> Unit,
        rootProjectId: String,
        expectedResourceFile: String
    ) {
        setup(client)
        val actual = service.findSubprojects(rootProjectId)
        val expected = TestUtils.loadObject(
            "$FIND_SUBPROJECTS_ROOT/$expectedResourceFile",
            object : TypeReference<List<BuildConfigurationProject>>() {}
        )
        assertEquals(expected, actual)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getTemplateArguments")
    fun getTemplateByProjectIdAndTemplateIdTest(
        @Suppress("UNUSED_PARAMETER") caseName: String,
        setup: (TeamcityClient) -> Unit,
        projectId: String,
        templateId: String,
        expectedResourceFile: String
    ) {
        setup(client)
        val actual = service.getTemplateByProjectIdAndTemplateId(projectId, templateId)
        val expected = TestUtils.loadObject(
            "$TEMPLATES_ROOT/$expectedResourceFile",
            object : TypeReference<BuildConfiguration>() {}
        )
        assertEquals(expected, actual)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("getTemplateFailsArguments")
    fun getTemplateByProjectIdAndTemplateIdFailsTest(
        @Suppress("UNUSED_PARAMETER") caseName: String,
        setup: (TeamcityClient) -> Unit,
        projectId: String,
        templateId: String,
        exceptionClass: Class<out Throwable>
    ) {
        setup(client)
        assertThrows(exceptionClass) {
            service.getTemplateByProjectIdAndTemplateId(projectId, templateId)
        }
    }

    companion object {
        private const val FIND_SUBPROJECTS_ROOT = "teamcity/findSubprojects"
        private const val TEMPLATES_ROOT = "teamcity/getTemplates"

        private fun locatorWithId(expectedId: String): ProjectLocator =
            argThat { this.id == expectedId && this.affectedProject == null }

        private fun locatorWithAffectedProject(expectedAffectedId: String): ProjectLocator =
            argThat { this.affectedProject?.id == expectedAffectedId }

        private fun setupFindSubprojectsMocks(
            rootProjectId: String,
            rootProjects: List<TeamcityProject>,
            childrenPages: List<TeamcityProjects> =
                listOf(TeamcityProjects(nextHref = null, projects = emptyList()))
        ): (TeamcityClient) -> Unit = { c ->
            whenever(c.getProjectsWithLocatorAndFields(locatorWithId(rootProjectId), any()))
                .thenReturn(TeamcityProjects(projects = rootProjects))
            var stub = whenever(c.getProjectsWithLocatorAndFields(locatorWithAffectedProject(rootProjectId), any()))
            childrenPages.forEach { page -> stub = stub.thenReturn(page) }
        }

        private fun setupGetTemplateMocks(
            projectId: String,
            templates: List<TeamcityBuildType>
        ): (TeamcityClient) -> Unit = { c ->
            val project = TeamcityProject(
                id = projectId,
                name = projectId,
                href = "/$projectId",
                webUrl = "http://tc/$projectId",
                archived = false,
                templates = TeamcityBuildTypes(buildTypes = templates)
            )
            whenever(c.getProjectsWithLocatorAndFields(locatorWithId(projectId), any()))
                .thenReturn(TeamcityProjects(projects = listOf(project)))
        }

        private fun setupEmptyProjectsMocks(projectId: String): (TeamcityClient) -> Unit = { c ->
            whenever(c.getProjectsWithLocatorAndFields(locatorWithId(projectId), any()))
                .thenReturn(TeamcityProjects(projects = emptyList()))
        }

        @JvmStatic
        @Suppress("unused")
        private fun findSubprojectsArguments(): Stream<Arguments> = Stream.of(
            Arguments.of(
                "singleRootProject",
                setupFindSubprojectsMocks(
                    rootProjectId = "RootProject_ComponentA",
                    rootProjects = listOf(
                        TeamcityProject(
                            id = "RootProject_ComponentA",
                            name = "RootProject_ComponentA",
                            href = "/RootProject_ComponentA",
                            webUrl = "http://tc/RootProject_ComponentA",
                            archived = false,
                            parameters = TeamcityProperties(
                                properties = listOf(TeamcityProperty(name = "COMPONENT_NAME", value = "componentA"))
                            ),
                            buildTypes = TeamcityBuildTypes(
                                buildTypes = listOf(
                                    TeamcityBuildType(
                                        id = "RootProject_ComponentA_Build",
                                        parameters = TeamcityProperties(
                                            properties = listOf(TeamcityProperty(name = "P1", value = "v1"))
                                        ),
                                        templates = TeamcityBuildTypes(
                                            buildTypes = listOf(TeamcityBuildType(id = "CDCompileUTGradle"))
                                        ),
                                        steps = TeamcitySteps(
                                            steps = listOf(
                                                TeamcityStep(
                                                    id = "s1",
                                                    name = "Compile",
                                                    type = "custom",
                                                    disabled = false,
                                                    properties = TeamcityProperties()
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                ),
                "RootProject_ComponentA",
                "singleRootProject.json"
            ),
            Arguments.of(
                "skipProjectsWithoutBuildTypes",
                setupFindSubprojectsMocks(
                    rootProjectId = "RootProject",
                    rootProjects = listOf(
                        TeamcityProject(
                            id = "RootProject_NoBuilds",
                            name = "No",
                            href = "/RootProject_NoBuilds",
                            webUrl = "http://tc/No",
                            archived = false,
                            parameters = TeamcityProperties(
                                properties = listOf(TeamcityProperty(name = "COMPONENT_NAME", value = "no"))
                            ),
                            buildTypes = TeamcityBuildTypes(buildTypes = emptyList())
                        ),
                        TeamcityProject(
                            id = "RootProject_Ok",
                            name = "RootProject_Ok",
                            href = "/RootProject_Ok",
                            webUrl = "http://tc/Ok",
                            archived = false,
                            parameters = TeamcityProperties(
                                properties = listOf(TeamcityProperty(name = "COMPONENT_NAME", value = "ok"))
                            ),
                            buildTypes = TeamcityBuildTypes(
                                buildTypes = listOf(TeamcityBuildType(id = "RootProject_Ok_Build"))
                            )
                        )
                    )
                ),
                "RootProject",
                "skipProjectsWithoutBuildTypes.json"
            ),
            Arguments.of(
                "skipProjectWithoutComponentName",
                setupFindSubprojectsMocks(
                    rootProjectId = "RootProject",
                    rootProjects = listOf(
                        TeamcityProject(
                            id = "RootProject_X",
                            name = "X",
                            href = "/x",
                            webUrl = "http://tc/X",
                            archived = false,
                            buildTypes = TeamcityBuildTypes(listOf(TeamcityBuildType(id = "b"))),
                            parameters = TeamcityProperties(
                                properties = listOf(TeamcityProperty(name = "OTHER", value = "v"))
                            )
                        )
                    )
                ),
                "RootProject",
                "empty.json"
            ),
            Arguments.of(
                "paginatedChildren",
                setupFindSubprojectsMocks(
                    rootProjectId = "RootProject",
                    rootProjects = listOf(
                        TeamcityProject(
                            id = "RootProject",
                            name = "RootProject",
                            href = "/RootProject",
                            webUrl = "http://tc/RootProject",
                            archived = false,
                            parameters = TeamcityProperties(
                                properties = listOf(TeamcityProperty(name = "COMPONENT_NAME", value = "s-root"))
                            ),
                            buildTypes = TeamcityBuildTypes(
                                buildTypes = listOf(TeamcityBuildType(id = "RootProject_Build"))
                            )
                        )
                    ),
                    childrenPages = listOf(
                        TeamcityProjects(
                            nextHref = "/next",
                            projects = listOf(
                                TeamcityProject(
                                    id = "RootProject_A",
                                    name = "RootProject_A",
                                    href = "/RootProject_A",
                                    webUrl = "http://tc/A",
                                    archived = false,
                                    parameters = TeamcityProperties(
                                        properties = listOf(TeamcityProperty(name = "COMPONENT_NAME", value = "a"))
                                    ),
                                    buildTypes = TeamcityBuildTypes(
                                        buildTypes = listOf(TeamcityBuildType(id = "RootProject_A_Build"))
                                    )
                                )
                            )
                        ),
                        TeamcityProjects(
                            nextHref = null,
                            projects = listOf(
                                TeamcityProject(
                                    id = "RootProject_B",
                                    name = "RootProject_B",
                                    href = "/RootProject_B",
                                    webUrl = "http://tc/B",
                                    archived = false,
                                    parameters = TeamcityProperties(
                                        properties = listOf(TeamcityProperty(name = "COMPONENT_NAME", value = "b"))
                                    ),
                                    buildTypes = TeamcityBuildTypes(
                                        buildTypes = listOf(TeamcityBuildType(id = "RootProject_B_Build"))
                                    )
                                )
                            )
                        )
                    )
                ),
                "RootProject",
                "paginatedChildren.json"
            )
        )

        @JvmStatic
        @Suppress("unused")
        private fun getTemplateArguments(): Stream<Arguments> = Stream.of(
            Arguments.of(
                "gradleTemplate",
                setupGetTemplateMocks(
                    projectId = "RDDepartment",
                    templates = listOf(
                        TeamcityBuildType(
                            id = "CDCompileUTGradle",
                            parameters = TeamcityProperties(
                                properties = listOf(TeamcityProperty(name = "XRAY", value = "true"))
                            ),
                            steps = TeamcitySteps(
                                steps = listOf(
                                    TeamcityStep(
                                        id = "s1",
                                        name = "Compile",
                                        type = "custom",
                                        disabled = false,
                                        properties = TeamcityProperties()
                                    )
                                )
                            )
                        )
                    )
                ),
                "RDDepartment",
                "CDCompileUTGradle",
                "gradleTemplate.json"
            )
        )

        @JvmStatic
        @Suppress("unused")
        private fun getTemplateFailsArguments(): Stream<Arguments> = Stream.of(
            Arguments.of(
                "templateIdAbsent",
                setupGetTemplateMocks(
                    projectId = "RDDepartment",
                    templates = listOf(TeamcityBuildType(id = "SomeOtherTemplate"))
                ),
                "RDDepartment",
                "Missing",
                NotFoundException::class.java
            ),
            Arguments.of(
                "projectAbsent",
                setupEmptyProjectsMocks("RDDepartment"),
                "RDDepartment",
                "Any",
                NotFoundException::class.java
            )
        )
    }
}